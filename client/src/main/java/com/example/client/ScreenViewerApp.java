package com.example.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScreenViewerApp {

    private static final int DEFAULT_PORT = 8080;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private String url;
    private String authHeader;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScreenViewerApp().start());
    }

    // ---------------------------------------------------------------- вход

    private void start() {
        JTextField hostField = new JTextField("localhost", 18);
        JTextField loginField = new JTextField(18);
        JPasswordField passField = new JPasswordField(18);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("IP (или IP:порт):"));
        form.add(hostField);
        form.add(new JLabel("Логин:"));
        form.add(loginField);
        form.add(new JLabel("Пароль:"));
        form.add(passField);

        while (true) {
            int res = JOptionPane.showConfirmDialog(null, form, "Подключение к серверу",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) {
                System.exit(0);
            }

            String host = hostField.getText().trim();
            String login = loginField.getText();
            String password = new String(passField.getPassword());
            if (host.isEmpty() || login.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Укажите IP и логин", "Ошибка", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            url = "http://" + (host.contains(":") ? host : host + ":" + DEFAULT_PORT) + "/api/screenshot";
            String creds = login + ":" + password;
            authHeader = "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));

            try {
                // авторизация + первый скриншот
                BufferedImage first = fetchScreenshot();
                showViewer(first, host);
                return;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ------------------------------------------------------------- запрос

    private BufferedImage fetchScreenshot() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", authHeader)
                .GET()
                .build();

        HttpResponse<byte[]> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new Exception("Не удалось связаться с сервером: " + e.getMessage(), e);
        }

        int code = response.statusCode();
        if (code == 401) {
            throw new Exception("Неверный логин или пароль");
        }
        if (code != 200) {
            throw new Exception("Сервер вернул HTTP " + code);
        }
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(response.body()));
        if (img == null) {
            throw new Exception("Ответ сервера не является изображением");
        }
        return img;
    }

    // ------------------------------------------------------------- окно

    private void showViewer(BufferedImage firstImage, String host) {
        ImagePanel panel = new ImagePanel();
        panel.setImage(firstImage);
        JLabel status = new JLabel(" Подключено");
        status.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

        JFrame frame = new JFrame("Экран " + host);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(panel, BorderLayout.CENTER);
        frame.add(status, BorderLayout.SOUTH);
        frame.setSize(1024, 640);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "screenshot-poller");
            t.setDaemon(true);
            return t;
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                scheduler.shutdownNow();
            }
        });

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        // fixedDelay: следующий запрос стартует через 1 с после завершения предыдущего,
        // поэтому запросы не накапливаются, если сеть медленная
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                BufferedImage img = fetchScreenshot();
                String time = LocalTime.now().format(fmt);
                SwingUtilities.invokeLater(() -> {
                    panel.setImage(img);
                    status.setText(" Обновлено: " + time + "   (" + img.getWidth() + "×" + img.getHeight() + ")");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> status.setText(" Ошибка: " + e.getMessage()));
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /** Панель, рисующая картинку с сохранением пропорций. */
    private static class ImagePanel extends JPanel {
        private BufferedImage image;

        ImagePanel() {
            setBackground(Color.DARK_GRAY);
        }

        void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) return;

            double scale = Math.min((double) getWidth() / image.getWidth(),
                                    (double) getHeight() / image.getHeight());
            int w = (int) (image.getWidth() * scale);
            int h = (int) (image.getHeight() * scale);
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, x, y, w, h, null);
        }
    }
}
