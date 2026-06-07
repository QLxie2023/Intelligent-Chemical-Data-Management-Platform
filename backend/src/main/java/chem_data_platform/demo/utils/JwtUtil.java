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

    // Generate token, compatible with the latest 0.12.6 style
    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("sub", username)                    // subject
                .claim("iat", new Date(now))               // issued at
                .claim("exp", new Date(now + expiration)) // expiration
                .signWith(getSigningKey())                 // Signature
                .compact();
    }

    // Get username from token
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("sub", String.class);
    }    // Validate whether the token is valid
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

    // isTokenValid is an alias for validateToken
    public boolean isTokenValid(String token) {
        return validateToken(token);
    }
}