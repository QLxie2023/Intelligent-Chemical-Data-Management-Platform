package chem_data_platform.demo.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 生成 token（适配 0.12.6 最新写法）
    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("sub", username)                    // subject
                .claim("iat", new Date(now))               // issued at
                .claim("exp", new Date(now + expiration)) // expiration
                .signWith(getSigningKey())                 // 签名
                .compact();
    }

    // 从 token 取用户名
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("sub", String.class);
    }    // 验证 token 是否有效
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // isTokenValid 是 validateToken 的别名
    public boolean isTokenValid(String token) {
        return validateToken(token);
    }
}