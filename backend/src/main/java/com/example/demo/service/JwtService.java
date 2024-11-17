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

    public String createJWT(Long id) {
        return Jwts.builder()
                .claim("id", String.valueOf(id))
                .signWith(key)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 10000))
                .compact();
    }

    public String getJWT() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String token = request.getHeader("Authorization");

        if (token == null) {
            throw new JwtException("토큰이 유효하지 않습니다.");
        }

        return token.replace("Bearer ", "");
    }

    public String getMemberId() {
        String accessToken = getJWT();

        if (accessToken.isEmpty()) {
            throw new JwtException("토큰이 유효하지 않습니다.");
        }
        Jws<Claims> jws;

        try {
            jws = Jwts.parserBuilder() // parser() 대신 parserBuilder() 사용
                    .setSigningKey(key) // verifyWith 대신 setSigningKey 사용
                    .build() // 빌더 완료
                    .parseClaimsJws(accessToken); // parseSignedClaims 대신 parseClaimsJws 사용
        } catch (JwtException e) {
            throw new JwtException("토큰이 유효하지 않습니다.");
        }

        return jws.getBody() // getPayload() 대신 getBody() 사용
                .get("id", String.class);
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