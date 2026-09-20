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
        Settings settings = loadSettings();

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            window.setBackground(new Color(0, 0, 0, 0));
        }

        panel.applySizes(settings.logoHeight, settings.fontSize);
        panel.setOnSizeChanged(() -> {
            window.pack();
            saveSettings();
        });

        window.setFocusableWindowState(false);
        window.setContentPane(panel);
        window.pack();
        window.setLocation(settings.x, settings.y);
        window.setVisible(true);
        window.toBack();

        enableMouseHandling();
        setupTray();

        Timer timer = new Timer(1000, e -> {
            panel.setRemaining(Duration.between(ZonedDateTime.now(TARGET_ZONE), TARGET));
            window.toBack();
        });
        timer.setInitialDelay(0);
        timer.start();
    }

    private void enableMouseHandling() {
        MouseAdapter handler = new MouseAdapter() {
            private Point pressScreenPoint;
            private Point pressPanelPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
                pressScreenPoint = e.getLocationOnScreen();
                pressPanelPoint = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                saveSettings();
                if (e.getLocationOnScreen().distance(pressScreenPoint) < 4) {
                    panel.handleClick(pressPanelPoint);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point current = window.getLocation();
                window.setLocation(current.x + e.getX() - dragOffset.x, current.y + e.getY() - dragOffset.y);
            }
        };
        panel.addMouseListener(handler);
        panel.addMouseMotionListener(handler);
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

    private static class Settings {
        int x;
        int y;
        int logoHeight;
        float fontSize;
    }

    private Settings loadSettings() {
        Settings settings = new Settings();
        try {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(configPath())) {
                props.load(in);
            }
            settings.x = Integer.parseInt(props.getProperty("x"));
            settings.y = Integer.parseInt(props.getProperty("y"));
            settings.logoHeight = Integer.parseInt(props.getProperty("logoHeight", "60"));
            settings.fontSize = Float.parseFloat(props.getProperty("fontSize", "36"));
        } catch (Exception e) {
            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            settings.x = screen.width - 380;
            settings.y = screen.height - 205;
            settings.logoHeight = 60;
            settings.fontSize = 36f;
        }
        return settings;
    }

    private void saveSettings() {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Properties props = new Properties();
            Point loc = window.getLocation();
            props.setProperty("x", String.valueOf(loc.x));
            props.setProperty("y", String.valueOf(loc.y));
            props.setProperty("logoHeight", String.valueOf(panel.getLogoHeight()));
            props.setProperty("fontSize", String.valueOf(panel.getFontSize()));
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "GTA VI Countdown settings");
            }
        } catch (IOException ignored) {
        }
    }

    private static class CountdownPanel extends JPanel {
        private static final Color TITLE_FILL = new Color(0xB38EB7);
        private static final Color NUMBERS_FILL = new Color(0xE99AAD);
        private static final Color OUTLINE = new Color(0xFDF4EF);

        private static final int MIN_LOGO_HEIGHT = 30;
        private static final int MAX_LOGO_HEIGHT = 150;
        private static final int LOGO_STEP = 6;
        private static final float MIN_FONT_SIZE = 18f;
        private static final float MAX_FONT_SIZE = 64f;
        private static final float FONT_STEP = 2f;
        private static final double LOGO_V_I_SPLIT = 0.70;

        private static final int TOP_MARGIN = 10;
        private static final int GAP_AFTER_LOGO = 6;
        private static final int GAP_BETWEEN_LINES = 4;
        private static final int BOTTOM_MARGIN = 10;
        private static final int SIDE_MARGIN = 20;
        private static final String WIDTH_TEMPLATE = "99d 00:00:00";

        private final Rectangle logoVZone = new Rectangle();
        private final Rectangle logoIZone = new Rectangle();
        private final Rectangle hoursZone = new Rectangle();
        private final Rectangle secondsZone = new Rectangle();

        private int logoHeight = 60;
        private float fontSize = 36f;

        private String line1 = "GTA VI";
        private boolean finished = false;
        private String daysPart = "0d";
        private String hoursPart = "00";
        private String minutesPart = "00";
        private String secondsPart = "00";

        private Runnable onSizeChanged = () -> { };

        CountdownPanel() {
            setOpaque(false);
            updatePreferredSize();
        }

        void setOnSizeChanged(Runnable callback) {
            this.onSizeChanged = callback;
        }

        void applySizes(int logoHeight, float fontSize) {
            this.logoHeight = clamp(logoHeight, MIN_LOGO_HEIGHT, MAX_LOGO_HEIGHT);
            this.fontSize = clamp(fontSize, MIN_FONT_SIZE, MAX_FONT_SIZE);
            updatePreferredSize();
        }

        int getLogoHeight() {
            return logoHeight;
        }

        float getFontSize() {
            return fontSize;
        }

        void setRemaining(Duration remaining) {
            line1 = "GTA VI";
            finished = remaining.isNegative();
            if (!finished) {
                daysPart = remaining.toDays() + "d";
                hoursPart = String.format("%02d", remaining.toHours() % 24);
                minutesPart = String.format("%02d", remaining.toMinutes() % 60);
                secondsPart = String.format("%02d", remaining.getSeconds() % 60);
            }
            repaint();
        }

        void handleClick(Point p) {
            if (logoVZone.contains(p)) {
                changeLogoHeight(LOGO_STEP);
            } else if (logoIZone.contains(p)) {
                changeLogoHeight(-LOGO_STEP);
            } else if (hoursZone.contains(p)) {
                changeFontSize(FONT_STEP);
            } else if (secondsZone.contains(p)) {
                changeFontSize(-FONT_STEP);
            }
        }

        private void changeLogoHeight(int delta) {
            logoHeight = clamp(logoHeight + delta, MIN_LOGO_HEIGHT, MAX_LOGO_HEIGHT);
            updatePreferredSize();
            onSizeChanged.run();
            repaint();
        }

        private void changeFontSize(float delta) {
            fontSize = clamp(fontSize + delta, MIN_FONT_SIZE, MAX_FONT_SIZE);
            updatePreferredSize();
            onSizeChanged.run();
            repaint();
        }

        private void updatePreferredSize() {
            Font font = BASE_FONT.deriveFont(fontSize);
            FontMetrics fm = getFontMetrics(font);
            int lineHeight = fm.getAscent() + fm.getDescent();

            int logoWidth = LOGO != null ? Math.round(logoHeight * LOGO.getWidth() / (float) LOGO.getHeight()) : 0;
            int contentWidth = Math.max(logoWidth, Math.max(fm.stringWidth(line1), fm.stringWidth(WIDTH_TEMPLATE)));

            int width = contentWidth + SIDE_MARGIN * 2;
            int height = TOP_MARGIN + logoHeight + GAP_AFTER_LOGO + lineHeight + GAP_BETWEEN_LINES + lineHeight + BOTTOM_MARGIN;
            setPreferredSize(new Dimension(width, height));
        }

        private static int clamp(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }

        private static float clamp(float v, float min, float max) {
            return Math.max(min, Math.min(max, v));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (LOGO != null) {
                int logoWidth = Math.round(logoHeight * LOGO.getWidth() / (float) LOGO.getHeight());
                int logoX = (getWidth() - logoWidth) / 2;
                g2.drawImage(LOGO, logoX, TOP_MARGIN, logoWidth, logoHeight, null);

                int splitX = logoX + (int) Math.round(logoWidth * LOGO_V_I_SPLIT);
                logoVZone.setBounds(logoX, TOP_MARGIN, splitX - logoX, logoHeight);
                logoIZone.setBounds(splitX, TOP_MARGIN, logoX + logoWidth - splitX, logoHeight);
            } else {
                logoVZone.setBounds(0, 0, 0, 0);
                logoIZone.setBounds(0, 0, 0, 0);
            }

            Font font = BASE_FONT.deriveFont(fontSize);
            FontMetrics fm = g2.getFontMetrics(font);
            int lineHeight = fm.getAscent() + fm.getDescent();

            int line1Top = TOP_MARGIN + logoHeight + GAP_AFTER_LOGO;
            int line2Top = line1Top + lineHeight + GAP_BETWEEN_LINES;

            drawOutlined(g2, fm, line1, font, TITLE_FILL, OUTLINE, line1Top + fm.getAscent());

            String line2 = finished ? "JA CHEGOU!" : daysPart + " " + hoursPart + ":" + minutesPart + ":" + secondsPart;
            double line2X = drawOutlined(g2, fm, line2, font, NUMBERS_FILL, OUTLINE, line2Top + fm.getAscent());

            if (!finished) {
                String beforeHours = daysPart + " ";
                int hoursStart = (int) Math.round(line2X + fm.stringWidth(beforeHours));
                int hoursWidth = fm.stringWidth(hoursPart);
                hoursZone.setBounds(hoursStart, line2Top, hoursWidth, lineHeight);

                String beforeSeconds = beforeHours + hoursPart + ":" + minutesPart + ":";
                int secondsStart = (int) Math.round(line2X + fm.stringWidth(beforeSeconds));
                int secondsWidth = fm.stringWidth(secondsPart);
                secondsZone.setBounds(secondsStart, line2Top, secondsWidth, lineHeight);
            } else {
                hoursZone.setBounds(0, 0, 0, 0);
                secondsZone.setBounds(0, 0, 0, 0);
            }

            g2.dispose();
        }

        private double drawOutlined(Graphics2D g2, FontMetrics fm, String text, Font font, Color fill, Color outline, int baseline) {
            double x = (getWidth() - fm.stringWidth(text)) / 2.0;

            FontRenderContext frc = g2.getFontRenderContext();
            GlyphVector gv = font.createGlyphVector(frc, text);
            Shape shape = gv.getOutline((float) x, baseline);

            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(outline);
            g2.draw(shape);

            g2.setColor(fill);
            g2.fill(shape);

            return x;
        }
    }
}
