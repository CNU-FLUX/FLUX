package com.flux.warning.controller;
import com.flux.warning.service.GoogleMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/navigation")
public class NavigationController {

    private final GoogleMapService googleMapService;
    @Autowired  // 생성자 주입 방식
    public NavigationController(GoogleMapService googleMapService) {
        this.googleMapService = googleMapService;
    }

    @GetMapping("/directions")
    public String getDirections(
            @RequestParam String origin,
            @RequestParam String destination
    ) throws Exception {
        return googleMapService.getDirections(origin, destination);
    }

    @GetMapping("/current-location")
    public String getCurrentLocation(
            @RequestParam double lat,
            @RequestParam double lng
    ) throws Exception {
        return googleMapService.getCurrentLocationInfo(lat, lng);
    }
}
