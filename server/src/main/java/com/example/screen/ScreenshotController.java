package com.example.screen;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScreenshotController {

    private final ScreenshotService service;

    public ScreenshotController(ScreenshotService service) {
        this.service = service;
    }

    @GetMapping("/screenshot")
    public ResponseEntity<byte[]> screenshot() throws Exception {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .body(service.capture());
    }
}
