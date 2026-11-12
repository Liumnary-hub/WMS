package com.example.api.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.*;

public final class JwtTokenUtil {
    //在request的header中的名字
    public final static String TOKEN_HEADER = "Authorization";

    //一个星期过期
    public final static long REMEMBER_EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 7;

    //一天过期
    public final static long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    // 应用密钥，生产环境用 JWT_SECRET 覆盖
    private static final String APP_SECRET = System.getenv().getOrDefault(
            "JWT_SECRET", "logistics-dev-secret-change-me");

    private static final String PREFIX = "logistics:";

    // 角色权限声明
    private static final String ROLE_CLAIMS = "roles";

    //检验token是否合法
    public static boolean checkToken(String token) {
        return token != null && !"null".equals(token) && !token.isEmpty() && token.startsWith(PREFIX);
    }

    /**
     * 生成Token
     */
    public static String createToken(String username, String[] roles, long expiration) {
        Map<String, Object> map = new HashMap<>();
        map.put(ROLE_CLAIMS, roles);
        return PREFIX + Jwts.builder()
                .setClaims(map)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, APP_SECRET)
                .setSubject(username)
                .compact();
    }

    /**
     * 获取token body
     */
    private static Claims getTokenClaims(String token) {
        token = token.substring(PREFIX.length());
        Claims claims = null;
        try {
            claims = Jwts.parser()
                    .setSigningKey(APP_SECRET)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            //过期、签名错误、格式错误统一按无效令牌处理
        }
        return claims;
    }

    /** 从Token中获取username */
    public static String getUsername(String token) {
        Claims claims = getTokenClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从Token中获取用户角色
     */
    public static List<String> getTokenRoles(String token) {
        List<String> roles = new ArrayList<>();
        Claims claims = getTokenClaims(token);
        Object object = claims != null ? claims.get(ROLE_CLAIMS) : null;
        if (object instanceof ArrayList<?>) {
            for (Object o : (List<?>) object) {
                roles.add((String) o);
            }
        }
        return roles;
    }

    /**
     * 校验Token是否过期
     */
    public static boolean isExpiration(String token) {
        Claims claims = getTokenClaims(token);
        return claims == null || claims.getExpiration() == null
                || claims.getExpiration().before(new Date());
    }

}

