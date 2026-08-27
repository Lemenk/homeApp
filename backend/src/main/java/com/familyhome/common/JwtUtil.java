package com.familyhome.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 工具：签发与解析用户 token。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-hours}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String generateToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + expireMillis))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * 解析 token，返回 userId；token 非法或过期返回 null。
     */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
