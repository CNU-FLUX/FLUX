package com.example.demo.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public JwtService() {
    }

    public String createJWT(String email) {
        return Jwts.builder()
                .claim("email", email) // 사용자 식별자로 email 사용
                .signWith(key)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 1일 유효
                .compact();
    }

    // JWT에서 email 추출
    public String getEmailFromJWT(String token) {
        Jws<Claims> claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);

        return claims.getBody().get("email", String.class); // email 필드 추출
    }

    public void validateJWT(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder() // parser() 대신 parserBuilder() 사용
                    .setSigningKey(key) // verifyWith 대신 setSigningKey 사용
                    .build() // 빌더 완료
                    .parseClaimsJws(token);

            Date expiration = jws.getBody().getExpiration();
            if (expiration.before(new Date())) {
                throw new JwtException("해당 요청에 대한 권한이 없습니다.");
            }
        } catch (Exception e) {
            throw new JwtException("해당 요청에 대한 권한이 없습니다.");
        }
    }
}