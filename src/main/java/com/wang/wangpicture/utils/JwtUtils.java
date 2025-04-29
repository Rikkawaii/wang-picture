package com.wang.wangpicture.utils;

import cn.hutool.json.JSONUtil;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import com.wang.wangpicture.model.dto.spaceuser.SpaceUserAddRequest;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
@Slf4j
public class JwtUtils {
    public static final long EXPIRE = 7200000L; // 2小时有效期
    public static final String USER_SECRET = "ukc8BDbRigUDaY6pZFfWus2jZWLPHO";
    public static final String INVITE_SECRET = "ukc4BXbPigUDvY6pZFfLus2jZFLEHO";

    public JwtUtils() {
    }
// 生成用户凭证
    public static String getJwtTokenForUser(Long id, String userName) {
        String JwtToken = Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("alg", "HS256")
                .setSubject("login-user")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .claim("id", id).claim("userName", userName)
                .signWith(SignatureAlgorithm.HS256, USER_SECRET)
                .compact();
        return JwtToken;
    }
// 生成邀请凭证
    public static String getJwtTokenForInvite(SpaceUserAddRequest spaceUserAddRequest) {
        String jsonStr = JSONUtil.toJsonStr(spaceUserAddRequest);
        String JwtToken = Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("alg", "HS256")
                .setSubject("invite-user")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .claim("spaceUserAddRequest", jsonStr)
                .signWith(SignatureAlgorithm.HS256, INVITE_SECRET)
                .compact();
        return JwtToken;
    }
    // 验证用户凭证
    public static void checkTokenForUser(String jwtToken) {
        checkToken(jwtToken, USER_SECRET);
    }
    // 验证邀请凭证
    public static void checkTokenForInvite(String jwtToken) {
        checkToken(jwtToken, INVITE_SECRET);
    }
    // 验证token
    private static void checkToken(String jwtToken, String secret) {
//        if (StringUtils.isEmpty(jwtToken)) {
//            return false;
//        }
        try {
            // 解析并验证 JWT
            Jwts.parser().setSigningKey(secret).parseClaimsJws(jwtToken);
        } catch (ExpiredJwtException e) {
            // JWT 已过期
            log.error("JWT token expired: {}", jwtToken);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "token已过期");
        } catch (SignatureException e) {
            // 签名无效
            log.error("JWT signature is invalid: {}", jwtToken);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "签名无效");
        } catch (MalformedJwtException e) {
            // JWT 格式不正确
            log.error("Malformed JWT token: {}", jwtToken);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "token格式不正确");
        } catch (JwtException e) {
            // 其他 JWT 异常
            log.error("JWT validation failed: {}", jwtToken);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "token验证失败");
        } catch (Exception e) {
            // 捕获其他异常
            log.error("Unexpected error while validating JWT token: {}", jwtToken, e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "token验证失败");
        }
    }


    public static Long getUserIdByToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        Cookie[] var2 = cookies;
        int var3 = cookies.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Cookie cookie = var2[var4];
            if (cookie.getName().equals("token")) {
                String token = cookie.getValue();
                if (!StringUtils.isEmpty(token)) {
                    try {
                        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(USER_SECRET).parseClaimsJws(token);
                        Claims claims = (Claims)claimsJws.getBody();
                        return Long.parseLong(claims.get("id").toString());
                    } catch (Exception var9) {
                        var9.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    // 解析token获取邀请信息
    public static SpaceUserAddRequest getSpaceUserAddRequestByToken(String token) {
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(INVITE_SECRET).parseClaimsJws(token);
        Claims claims = (Claims)claimsJws.getBody();
        String jsonStr = claims.get("spaceUserAddRequest", String.class);
        return JSONUtil.toBean(jsonStr, SpaceUserAddRequest.class);
    }
}
