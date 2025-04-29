package com.wang.wangpicture.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import com.wang.wangpicture.manager.auth.model.SpaceUserAuthContext;
import com.wang.wangpicture.manager.auth.model.SpaceUserPermissionConstant;
import com.wang.wangpicture.model.entity.Picture;
import com.wang.wangpicture.model.entity.Space;
import com.wang.wangpicture.model.entity.SpaceUser;
import com.wang.wangpicture.model.entity.User;
import com.wang.wangpicture.model.enums.SpaceRoleEnum;
import com.wang.wangpicture.model.enums.SpaceTypeEnum;
import com.wang.wangpicture.service.PictureService;
import com.wang.wangpicture.service.SpaceService;
import com.wang.wangpicture.service.SpaceUserService;
import com.wang.wangpicture.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.wang.wangpicture.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 *
 * 使用checkPermisson注解后会调用此类的方法，并传入当前登录的账号id和登录类型，
 * 此方法需要返回一个账号所拥有的权限码集合。
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private UserService userService;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;

    /**
     * 返回一个账号所拥有的权限码集合
     * 这个不是提供给用户调用的，而是 Sa-Token 调用的，Sa-Token 会根据此方法返回的权限码集合，
     * 来判断当前登录的账号是否有权限访问某个接口。
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 1. 仅对loginType为"space"的账号进行权限验证
        if (!"space".equals(loginType)) {
            return new ArrayList<>();
        }
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 2.如果所有字段都为空，表示查询公共图库，可以通过（但是公共图库也提供搜索功能，难免会有字段，所以对于查看图片的接口不使用注解来判断）
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        // 3. 校验登录状态（因为这里会验证登录状态，所以对于home页公共图库查看的请求,不使用这个注解）
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        //todo: 这里允许管理员任何操作（考虑是否赋予）
        if(userService.isAdmin(loginUser)){
            return ADMIN_PERMISSIONS;
        }
        Long loginUserId = loginUser.getId();
        // 4. 通过 spaceUserId 获取空间用户信息：如果上下文中存在 spaceUserId(存在则说明是删除，编辑，查看空间成员)
        Long spaceUserId = authContext.getSpaceUserId();
        SpaceUser spaceUser;
        if(spaceUserId != null){
            // spaceUser表有id,userId字段，这里的spaceUserId是spaceUser表的id
            spaceUser = spaceUserService.getById(spaceUserId);
            // 4.1 如果空间用户不存在，抛出异常
            if(spaceUser == null){
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间用户数据未找到");
            }
            // 4.2 取出当前登录用户对于的spaceUser, 如果不存在，返回空列表
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getUserId, loginUserId)
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .one();
            if(loginSpaceUser == null){
                return new ArrayList<>();
            }
            // 4.3 取出当前登录用户对于的空间权限
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 5. 如果没有spaceUserId,则通过spaceId或pictureId获取Space对象（到这一步要么是对图片进行操作，要么是邀请成员加入团队空间）
        Long spaceId = authContext.getSpaceId();
        // spaceId为null,要么是对公共图库的操作，要么有人构造请求对私有空间的图片进行操作
        if (spaceId == null) {
            // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = authContext.getPictureId();
            // 图片 id 也没有，则默认通过权限校验
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            // 公共图库，仅本人或管理员可操作
            if (spaceId == null) {
                if (picture.getUserId().equals(loginUserId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    // 不是自己的图片，仅可查看（这一条其实用不上）
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        // 6. 运行到这里说明请求传了spaceId(对空间操作) 或者 传了pictureId且包含了spaceId
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间，仅本人或管理员有权限
            if (space.getUserId().equals(loginUserId) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间，查询 SpaceUser 并获取角色和权限，这里是判断是否有权限邀请用户加入团队空间
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, loginUserId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return null;
    }

    /**
     * 从请求中获取上下文对象，收集相关信息，返回一个 SpaceUserAuthContext 对象，
     * 供getPersionList和getRoleList方法使用
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        // 获得请求对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        // 获得请求头中的 content-type
        //Header.ContentType.getValue()其实就是"Content-Type"
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        // 兼容 get 和 post 操作
        // ContentType.JSON.getValue()其实就是"application/json"
        if (ContentType.JSON.getValue().equals(contentType)) {
            // 这里有个坑，request.getInputStream()只能读取一次，第二次读取会返回空，所以可以使用包装类包装一下
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestUri = request.getRequestURI();
            String partUri = requestUri.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }
}
