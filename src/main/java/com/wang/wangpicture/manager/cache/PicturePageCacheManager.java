package com.wang.wangpicture.manager.cache;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.wang.wangpicture.model.dto.picture.PictureQueryRequest;
import com.wang.wangpicture.model.entity.Picture;
import com.wang.wangpicture.model.vo.PictureVO;
import com.wang.wangpicture.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.time.Duration;

import static com.wang.wangpicture.constant.PictureVOPageCacheConstant.*;


@Component
@Slf4j
public class PicturePageCacheManager {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    @Lazy//todo:懒加载，解决循环依赖问题
    private PictureService pictureService;
    @Resource
    private RedissonClient redissonClient;
    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L) // 最大 10000 条
            // 缓存 5 分钟后移除
            .expireAfterWrite(Duration.ofSeconds(PAGE_CACHE_EXPIRE))
            .build();

    private final Cache<String, String> localCache = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000)// 最大 10000 条
            .expireAfter(new Expiry<Object, Object>() {
                // 缓存 5 分钟(300 秒)后移除
                long expireTime = PAGE_CACHE_EXPIRE;

                // 以下方法返回的都是纳秒
                @Override
                public long expireAfterCreate(Object key, Object value, long currentTime) {
                    // 创建时延长过期时间
                    return SECONDS_UNIT.toNanos(expireTime);
                }

                @Override
                public long expireAfterUpdate(Object key, Object value, long currentTime, @NonNegative long currentDuration) {
                    // 更新时延长过期时间
                    return SECONDS_UNIT.toNanos(expireTime);
                }

                @Override
                public long expireAfterRead(Object key, Object value, long currentTime, @NonNegative long currentDuration) {
                    // 读取时延长过期时间
                    return SECONDS_UNIT.toNanos(expireTime);
                }
            })
            .build();

    // todo: 待实现删除缓存
    public void deleteCache(PictureQueryRequest pictureQueryRequest) {
    }

    /**
     * 获取本地缓存
     * @param pictureQueryRequest
     * @return
     */
    public Page<PictureVO> getCaffeineCache(PictureQueryRequest pictureQueryRequest) {
        String redisKey = getCacheKey(pictureQueryRequest);
        // 如果本地缓存中有数据，则直接返回缓存数据
        String cacheValue = LOCAL_CACHE.getIfPresent(redisKey);
        if (cacheValue != null) {
            Page<PictureVO> cachePage = JSON.parseObject(cacheValue, Page.class);
            return cachePage;
        }
        return null;
    }

    /**
     * 获取Redis缓存，如果缓存中没有数据，则查询数据库，并重构缓存
     * @param pictureQueryRequest
     * @return
     */
    public Page<PictureVO> getRedisCache(PictureQueryRequest pictureQueryRequest) {
        String redisKey = getCacheKey(pictureQueryRequest);
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String cacheValue = ops.get(redisKey);
        // 如果缓存中有数据，则先重构本地缓存，再返回数据
        if (cacheValue != null) {
            LOCAL_CACHE.put(redisKey, cacheValue);
            Page<PictureVO> cachePage = JSON.parseObject(cacheValue, Page.class);
            return cachePage;
        }
        // 缓存中没有数据,多个请求同时查询数据库，只允许一个请求查询数据库并重构缓存，其它请求等待锁。
        // 加锁
        String lockKey = getLockKey(pictureQueryRequest);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，最多等待 2 秒，上锁成功查询数据库并重构缓存，否则返回空数据(防止缓存击穿)
            boolean isLock = lock.tryLock(2, -1, SECONDS_UNIT);
            if (!isLock) {
                // todo: 其它降级策略
                // 未获取到锁，则直接返回空数据
                return null;
            }
            // 获取锁成功，再次检查缓存（防止其他线程已经更新缓存）
            Page<PictureVO> pictureVOPage = getCaffeineCache(pictureQueryRequest);
            if (pictureVOPage != null) {
                return pictureVOPage;
            }
            // 缓存中没有数据，则查询数据库，重构Redis缓存，重构本地缓存，并返回数据。
            // 查询数据库
            pictureVOPage = getPictureVOPage(pictureQueryRequest);
            // 得到缓存数据(如果pictureVOPage==null,则返回空字符串应对缓存穿透)
            cacheValue = pictureVOPage == null ? "" : JSON.toJSONString(pictureVOPage);
            // 重构 Redis 缓存（5 - 10 分钟随机过期，防止雪崩）
            long cacheExpireTime = PAGE_CACHE_EXPIRE + RandomUtil.randomInt(0, 300);
            ops.set(redisKey, cacheValue, cacheExpireTime, SECONDS_UNIT);
            // 重构本地缓存
            LOCAL_CACHE.put(redisKey, cacheValue);
            return pictureVOPage;
        } catch (InterruptedException e) {
            log.error("分布式锁重构缓存出错", e);
            return null;
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询数据库，并封装成Page<PictureVO>
     * @param pictureQueryRequest
     * @return
     */
    private Page<PictureVO> getPictureVOPage(PictureQueryRequest pictureQueryRequest) {
        // 查询数据库
        int size = pictureQueryRequest.getPageSize();
        int current = pictureQueryRequest.getCurrent();
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);
        return pictureVOPage;
    }

    /**
     * 获取分布锁的key
     * @param pictureQueryRequest
     * @return
     */
    private String getLockKey(PictureQueryRequest pictureQueryRequest) {
        String queryCondition = JSON.toJSONString(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String lockKey = PAGE_CACHE_LOCK_KEY + hashKey;
        return lockKey;
    }

    /**
     * 获取Redis缓存的key
     * @param pictureQueryRequest
     * @return
     */
    private String getCacheKey(PictureQueryRequest pictureQueryRequest) {
        String queryCondition = JSON.toJSONString(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String redisKey = PAGE_CACHE_KEY + hashKey;
        return redisKey;
    }

}
