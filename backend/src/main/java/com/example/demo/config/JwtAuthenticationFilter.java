package com.example.demo.config;

import com.example.demo.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 요청 헤더 확인
            System.out.println("Request Path: " + request.getRequestURI());
            System.out.println("Request Method: " + request.getMethod());
            System.out.println("Authorization Header: " + request.getHeader("Authorization"));

            // JWT 토큰 추출
            String token = jwtService.extractTokenFromRequest(request);
            if (token == null) {
                throw new JwtException("Authorization 헤더가 없거나 잘못되었습니다.");
            }
//            System.out.println("Extracted Token: " + token);

            // JWT 검증 및 이메일 추출
            String email = jwtService.getEmailFromJWT(token); // JWT에서 userId 추출
//            System.out.println("Extracted Email from Token: " + email);


            // SecurityContext에 인증 정보 설정
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException e) {
            System.err.println("JWT 검증 실패: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // 로그인, 회원가입 경로는 필터 제외
        return path.startsWith("/kakao/login-url") || path.startsWith("/kakao/callback") || path.startsWith("/member/signup") || path.startsWith("/member/login") || path.startsWith("/report/all") || path.startsWith("/ws");
    }

}
