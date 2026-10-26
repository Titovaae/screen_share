package com.example.screen;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ScreenServerApplication {

    public static void main(String[] args) {
        // headless=false нужен, чтобы java.awt.Robot мог снимать экран
        new SpringApplicationBuilder(ScreenServerApplication.class)
                .headless(false)
                .run(args);
    }
}
