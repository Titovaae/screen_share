# Screen share

Веб-сервис (Spring Boot) отдаёт скриншот экрана по HTTP после авторизации по логину/паролю.
Десктоп-клиент (Java/Swing) показывает этот скриншот и обновляет его раз в секунду.

| Требование | Реализация |
|---|---|
| Spring Boot, Java 11+ | Spring Boot 3.3, Java 17 |
| Реляционная БД через Hibernate | Spring Data JPA + Hibernate, PostgreSQL |
| Авторизация | Spring Security, HTTP Basic, пользователи в БД, пароли BCrypt |
| Интеграционные тесты на контроллеры | `ScreenshotControllerIT` (MockMvc + весь контекст + H2) |
| Лог всех вызовов контроллеров в файл | `ControllerLoggingAspect` -> `logs/controller-calls.log` |
| Docker-образ | `server/Dockerfile`, `docker-compose.yml` |
| CI и публикация в Docker Hub | `.github/workflows/ci.yml` |

## Структура
    server/   Spring Boot сервис
    client/   Swing-клиент
    docker-compose.yml

## Запуск через Docker Compose (сервер + Postgres)
    docker compose up --build
    curl -u admin:admin http://localhost:8080/api/screenshot -o s.jpg

В контейнере экран виртуальный (Xvfb), снимок будет чёрным. Чтобы снимать реальный экран
Linux-хоста, раскомментируйте `DISPLAY` и проброс `/tmp/.X11-unix` в `docker-compose.yml`
(и выполните `xhost +local:docker`).

## Запуск без Docker
    createdb screen
    cd server && mvn spring-boot:run

Настройки через переменные окружения: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `APP_USER`, `APP_PASSWORD`, `LOG_DIR`.
При пустой таблице создаётся пользователь `APP_USER`/`APP_PASSWORD` (по умолчанию admin/admin).

## Клиент
    cd client && mvn package
    java -jar target/screen-client-1.0.0.jar

## Тесты
    cd server && mvn verify

## Лог вызовов контроллеров
`logs/controller-calls.log` (в Docker: `/app/logs`), ротация по дню и размеру 10 МБ, хранится 14 дней:

    2026-10-27 12:00:01.123 ScreenshotController.screenshot user=admin [GET /api/screenshot from 192.168.1.5] args=[] -> HTTP 200 body=byte[245123] in 87 ms

Запросы, отклонённые Spring Security (401), до контроллера не доходят и в этот лог не попадают.

## Публикация

### GitHub
    git remote add origin git@github.com:<user>/screen-share.git
    git push -u origin main

### Docker Hub
Автоматически: в Settings -> Secrets and variables -> Actions репозитория добавьте
`DOCKERHUB_USERNAME` и `DOCKERHUB_TOKEN` (Access Token из Docker Hub). После каждого пуша в `main`
образ `<user>/screen-server:latest` соберётся и уйдёт в Docker Hub; тег `v1.0.0` даст ещё и версионный тег.

Вручную:
    docker login
    docker build -t <user>/screen-server:latest server
    docker push <user>/screen-server:latest
