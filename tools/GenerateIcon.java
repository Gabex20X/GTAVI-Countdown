import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GenerateIcon {
    private static final int[] SIZES = {16, 32, 48, 256};

    public static void main(String[] args) throws IOException {
        String inputPath = args.length > 0 ? args[0] : "src/icon/GTAVI_Icon.png";
        String outputPath = args.length > 1 ? args[1] : "src/icon/app.ico";

        BufferedImage src = ImageIO.read(new File(inputPath));

        List<byte[]> pngs = new ArrayList<>();
        for (int size : SIZES) {
            BufferedImage resized = resize(src, size);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", bos);
            pngs.add(bos.toByteArray());
        }

        writeIco(pngs, outputPath);
        System.out.println("Icone gerado em: " + outputPath);
    }

    private static BufferedImage resize(BufferedImage src, int size) {
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scale = Math.min(size / (double) src.getWidth(), size / (double) src.getHeight());
        int w = (int) Math.round(src.getWidth() * scale);
        int h = (int) Math.round(src.getHeight() * scale);
        g2.drawImage(src, (size - w) / 2, (size - h) / 2, w, h, null);
        g2.dispose();
        return resized;
    }

    private static void writeIco(List<byte[]> pngs, String outputPath) throws IOException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outputPath))) {
            writeLEShort(out, 0);
            writeLEShort(out, 1);
            writeLEShort(out, pngs.size());

            int offset = 6 + pngs.size() * 16;
            for (int i = 0; i < pngs.size(); i++) {
                int size = SIZES[i];
                byte[] png = pngs.get(i);
                out.write(size == 256 ? 0 : size);
                out.write(size == 256 ? 0 : size);
                out.write(0);
                out.write(0);
                writeLEShort(out, 1);
                writeLEShort(out, 32);
                writeLEInt(out, png.length);
                writeLEInt(out, offset);
                offset += png.length;
            }
            for (byte[] png : pngs) {
                out.write(png);
            }
        }
    }

    private static void writeLEShort(OutputStream out, int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }

    private static void writeLEInt(OutputStream out, int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 24) & 0xFF);
    }
}
