package com.example.demo.controller;

import com.example.demo.service.GeoService;
import com.example.demo.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
public class LocationController {

    private final GeoService geoService;
    private final JwtService jwtService;

    // 위치 저장 API
    @PostMapping("/update")
    public ResponseEntity<?> updateUserLocation(HttpServletRequest request,
                                                @RequestParam double longitude,
                                                @RequestParam double latitude) {
        try {
            // JWT 토큰에서 이메일 추출
            String jwtToken = jwtService.extractTokenFromRequest(request);
            String email = jwtService.getEmailFromJWT(jwtToken);

            // 위치 정보 저장
            geoService.saveUserLocation(email, longitude, latitude);

            return ResponseEntity.ok("Location updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update location: " + e.getMessage());
        }
    }
}
