package com.example.screen;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ScreenshotService {

    private Robot robot;

    /** Возвращает JPEG со всеми мониторами. */
    public synchronized byte[] capture() throws IOException, AWTException {
        if (robot == null) {
            robot = new Robot();
        }
        Rectangle area = new Rectangle();
        for (GraphicsDevice d : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            area = area.union(d.getDefaultConfiguration().getBounds());
        }
        BufferedImage img = robot.createScreenCapture(area);

        // JPEG не поддерживает альфа-канал
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(rgb, "jpg", out);
        return out.toByteArray();
    }
}
