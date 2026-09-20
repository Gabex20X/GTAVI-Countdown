import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.*;

public class CountdownApp {

    private static final ZoneOffset TARGET_ZONE = ZoneOffset.ofHours(-3);
    private static final ZonedDateTime TARGET = ZonedDateTime.of(2026, 11, 19, 0, 0, 0, 0, TARGET_ZONE);
    private static final Font BASE_FONT = loadBaseFont();
    private static final BufferedImage LOGO = loadLogo();

    private final JWindow window = new JWindow();
    private final CountdownPanel panel = new CountdownPanel();
    private Point dragOffset;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CountdownApp().start());
    }

    private static Font loadBaseFont() {
        try (InputStream in = CountdownApp.class.getResourceAsStream("/fonts/Pricedown.otf")) {
            if (in != null) {
                return Font.createFont(Font.TRUETYPE_FONT, in);
            }
        } catch (FontFormatException | IOException e) {
            System.err.println("Nao foi possivel carregar Pricedown.otf, usando fonte padrao: " + e.getMessage());
        }
        return new Font("Segoe UI", Font.BOLD, 12);
    }

    private static BufferedImage loadLogo() {
        try (InputStream in = CountdownApp.class.getResourceAsStream("/icon/GTAVI_Icon.png")) {
            if (in == null) {
                return null;
            }
            BufferedImage src = ImageIO.read(in);
            BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int py = 0; py < src.getHeight(); py++) {
                for (int px = 0; px < src.getWidth(); px++) {
                    // source is a white glyph on a solid black background; use brightness as alpha to key out the black
                    int luminance = src.getRGB(px, py) & 0xFF;
                    out.setRGB(px, py, (luminance << 24) | 0xFFFFFF);
                }
            }
            return out;
        } catch (IOException e) {
            System.err.println("Nao foi possivel carregar o logo: " + e.getMessage());
            return null;
        }
    }

    private void start() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            window.setBackground(new Color(0, 0, 0, 0));
        }
        window.setAlwaysOnTop(true);
        window.setContentPane(panel);
        window.pack();
        window.setLocation(loadPosition());
        window.setVisible(true);

        enableDrag();
        setupTray();

        Timer timer = new Timer(1000, e -> panel.setRemaining(Duration.between(ZonedDateTime.now(TARGET_ZONE), TARGET)));
        timer.setInitialDelay(0);
        timer.start();
    }

    private void enableDrag() {
        MouseAdapter dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                savePosition(window.getLocation());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point current = window.getLocation();
                window.setLocation(current.x + e.getX() - dragOffset.x, current.y + e.getY() - dragOffset.y);
            }
        };
        panel.addMouseListener(dragHandler);
        panel.addMouseMotionListener(dragHandler);
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) {
            return;
        }
        PopupMenu menu = new PopupMenu();
        MenuItem exitItem = new MenuItem("Sair");
        exitItem.addActionListener(e -> System.exit(0));
        menu.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(createTrayImage(), "GTA VI Countdown", menu);
        trayIcon.setImageAutoSize(true);
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private Image createTrayImage() {
        if (LOGO != null) {
            int size = 32;
            BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = icon.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = Math.min(size / (double) LOGO.getWidth(), size / (double) LOGO.getHeight());
            int w = (int) Math.round(LOGO.getWidth() * scale);
            int h = (int) Math.round(LOGO.getHeight() * scale);
            g2.drawImage(LOGO, (size - w) / 2, (size - h) / 2, w, h, null);
            g2.dispose();
            return icon;
        }

        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xFF2DAF));
        g.fillOval(0, 0, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g.drawString("6", 5, 12);
        g.dispose();
        return image;
    }

    private Path configPath() {
        String appData = System.getenv("APPDATA");
        return Paths.get(appData, "GTA6Countdown", "position.properties");
    }

    private Point loadPosition() {
        try {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(configPath())) {
                props.load(in);
            }
            int x = Integer.parseInt(props.getProperty("x"));
            int y = Integer.parseInt(props.getProperty("y"));
            return new Point(x, y);
        } catch (Exception e) {
            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            return new Point(screen.width - 380, screen.height - 205);
        }
    }

    private void savePosition(Point point) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Properties props = new Properties();
            props.setProperty("x", String.valueOf(point.x));
            props.setProperty("y", String.valueOf(point.y));
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "GTA VI Countdown window position");
            }
        } catch (IOException ignored) {
        }
    }

    private static class CountdownPanel extends JPanel {
        private static final Color TITLE_FILL = new Color(0xB38EB7);
        private static final Color NUMBERS_FILL = new Color(0xE99AAD);
        private static final Color OUTLINE = new Color(0xFDF4EF);
        private static final float FONT_SIZE = 36f;

        private String line1 = "GTA VI";
        private String line2 = "--";

        CountdownPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(360, 165));
        }

        void setRemaining(Duration remaining) {
            line1 = "GTA VI";
            if (remaining.isNegative()) {
                line2 = "JA CHEGOU!";
            } else {
                long days = remaining.toDays();
                long hours = remaining.toHours() % 24;
                long minutes = remaining.toMinutes() % 60;
                long seconds = remaining.getSeconds() % 60;
                line2 = String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (LOGO != null) {
                int logoHeight = 60;
                int logoWidth = Math.round(logoHeight * LOGO.getWidth() / (float) LOGO.getHeight());
                g2.drawImage(LOGO, (getWidth() - logoWidth) / 2, 8, logoWidth, logoHeight, null);
            }

            Font font = BASE_FONT.deriveFont(FONT_SIZE);
            drawOutlined(g2, line1, font, TITLE_FILL, OUTLINE, 104);
            drawOutlined(g2, line2, font, NUMBERS_FILL, OUTLINE, 146);

            g2.dispose();
        }

        private void drawOutlined(Graphics2D g2, String text, Font font, Color fill, Color outline, int y) {
            FontRenderContext frc = g2.getFontRenderContext();
            GlyphVector gv = font.createGlyphVector(frc, text);
            Shape shape = gv.getOutline();
            Rectangle2D bounds = shape.getBounds2D();
            double x = (getWidth() - bounds.getWidth()) / 2 - bounds.getX();

            Graphics2D g2d = (Graphics2D) g2.create();
            g2d.translate(x, y);

            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(outline);
            g2d.draw(shape);

            g2d.setColor(fill);
            g2d.fill(shape);

            g2d.dispose();
        }
    }
}
