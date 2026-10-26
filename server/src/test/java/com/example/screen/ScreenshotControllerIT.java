package com.example.screen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест ScreenshotController: поднимается весь контекст Spring,
 * реальная цепочка Spring Security и Hibernate (H2 в памяти).
 * Мокается только ScreenshotService, так как java.awt.Robot недоступен на CI без экрана.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScreenshotControllerIT {

    private static final byte[] FAKE_JPEG = {(byte) 0xFF, (byte) 0xD8, 1, 2, 3, (byte) 0xFF, (byte) 0xD9};

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @MockBean ScreenshotService screenshotService;

    @BeforeEach
    void setUp() throws Exception {
        users.deleteAll();
        users.save(new AppUser("tester", encoder.encode("secret")));
        when(screenshotService.capture()).thenReturn(FAKE_JPEG);
    }

    @Test
    void withoutCredentials_returns401() throws Exception {
        mvc.perform(get("/api/screenshot"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPassword_returns401() throws Exception {
        mvc.perform(get("/api/screenshot").with(httpBasic("tester", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUser_returns401() throws Exception {
        mvc.perform(get("/api/screenshot").with(httpBasic("nobody", "secret")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUser_returns401() throws Exception {
        AppUser u = users.findByUsername("tester").orElseThrow();
        u.setEnabled(false);
        users.save(u);

        mvc.perform(get("/api/screenshot").with(httpBasic("tester", "secret")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validCredentials_returnsJpeg() throws Exception {
        mvc.perform(get("/api/screenshot").with(httpBasic("tester", "secret")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(FAKE_JPEG));
    }

    @Test
    void controllerCall_isWrittenToLogFile() throws Exception {
        mvc.perform(get("/api/screenshot").with(httpBasic("tester", "secret")))
                .andExpect(status().isOk());

        Path logFile = Path.of("target/test-logs/controller-calls.log");
        assertThat(logFile).exists();
        assertThat(Files.readString(logFile))
                .contains("ScreenshotController.screenshot")
                .contains("user=tester")
                .contains("GET /api/screenshot");
    }
}
