package com.example.demo.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;

    // 비밀 키를 @Value로 주입받아 SecretKey로 변환
    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }
    public String createJWT(String email) {
        return Jwts.builder()
                .claim("email", email) // 사용자 식별자로 email 사용
                .signWith(key)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 1일 유효
                .compact();
    }

    // Authorization 헤더에서 JWT 추출
    public String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtException("Authorization 헤더가 없거나 잘못되었습니다.");
        }
        return authHeader.substring(7); // "Bearer " 이후의 토큰 반환
    }


    // JWT에서 email 추출 및 검증
    public String getEmailFromJWT(String token) {
        Jws<Claims> claims = parseToken(token); // 토큰 검증
        return claims.getBody().get("email", String.class);
    }

    // JWT 서명 및 만료 시간 검증
    public void validateJWT(String token) {
        parseToken(token); // 만료, 서명, 형식 검증
    }

    // JWT 파싱 로직
    private Jws<Claims> parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            throw new JwtException("JWT 토큰이 만료되었습니다.", e);
        } catch (SignatureException e) {
            throw new JwtException("JWT 서명이 유효하지 않습니다.", e);
        } catch (Exception e) {
            throw new JwtException("JWT 형식이 잘못되었습니다.", e);
        }
    }
}