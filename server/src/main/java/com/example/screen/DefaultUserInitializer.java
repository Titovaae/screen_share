package com.example.screen;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Создаёт первого пользователя, если таблица пуста. */
@Component
public class DefaultUserInitializer implements CommandLineRunner {

    private final AppUserRepository repository;
    private final PasswordEncoder encoder;
    private final String login;
    private final String password;

    public DefaultUserInitializer(AppUserRepository repository, PasswordEncoder encoder,
                                  @Value("${app.default-user.login}") String login,
                                  @Value("${app.default-user.password}") String password) {
        this.repository = repository;
        this.encoder = encoder;
        this.login = login;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.save(new AppUser(login, encoder.encode(password)));
            System.out.println("Создан пользователь по умолчанию: " + login);
        }
    }
}
