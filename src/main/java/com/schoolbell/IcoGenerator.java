package com.schoolbell;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IcoGenerator {
    public static void main(String[] args) throws Exception {
        File inputFile = new File("src/main/resources/icon.png");
        if (!inputFile.exists()) {
            System.err.println("Input png not found at: " + inputFile.getAbsolutePath());
            return;
        }

        BufferedImage source = ImageIO.read(inputFile);
        int[] sizes = {16, 32, 48, 64, 128, 256};
        
        try (FileOutputStream fos = new FileOutputStream("icon.ico")) {
            // ICO Header
            fos.write(packShort(0)); // Reserved
            fos.write(packShort(1)); // Type (1 = Icon)
            fos.write(packShort(sizes.length)); // Number of images

            ByteArrayOutputStream[] imageBuffers = new ByteArrayOutputStream[sizes.length];
            int currentOffset = 6 + (sizes.length * 16);

            for (int i = 0; i < sizes.length; i++) {
                int size = sizes[i];
                BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(source, 0, 0, size, size, null);
                g.dispose();

                byte[] imageData;
                if (size == 256) {
                    ByteArrayOutputStream pngBuf = new ByteArrayOutputStream();
                    ImageIO.write(resized, "png", pngBuf);
                    imageData = pngBuf.toByteArray();
                } else {
                    imageData = createDib(resized);
                }

                imageBuffers[i] = new ByteArrayOutputStream();
                imageBuffers[i].write(imageData);

                // Directory entry
                fos.write(size >= 256 ? 0 : size); // Width
                fos.write(size >= 256 ? 0 : size); // Height
                fos.write(0); // Palette
                fos.write(0); // Reserved
                fos.write(packShort(1)); // Planes
                fos.write(packShort(32)); // Bit count
                fos.write(packInt(imageData.length)); // Size
                fos.write(packInt(currentOffset)); // Offset

                currentOffset += imageData.length;
            }

            for (ByteArrayOutputStream buf : imageBuffers) {
                fos.write(buf.toByteArray());
            }
        }
        System.out.println("Generated multi-size icon.ico successfully.");
    }

    private static byte[] createDib(BufferedImage image) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // BITMAPINFOHEADER (40 bytes)
        baos.write(packInt(40)); // biSize
        baos.write(packInt(width)); // biWidth
        baos.write(packInt(height * 2)); // biHeight (XOR + AND)
        baos.write(packShort(1)); // biPlanes
        baos.write(packShort(32)); // biBitCount (32-bit RGBA)
        baos.write(packInt(0)); // biCompression (BI_RGB)
        
        int xorSize = width * height * 4;
        int andRowBytes = ((width + 31) / 32) * 4;
        int andSize = andRowBytes * height;
        baos.write(packInt(xorSize + andSize)); // biSizeImage
        
        baos.write(packInt(0)); // biXPelsPerMeter
        baos.write(packInt(0)); // biYPelsPerMeter
        baos.write(packInt(0)); // biClrUsed
        baos.write(packInt(0)); // biClrImportant
        
        // XOR mask: bottom-up BGRA
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                
                baos.write(b);
                baos.write(g);
                baos.write(r);
                baos.write(a);
            }
        }
        
        // AND mask: bottom-up 1-bit per pixel (all 0s, aligned to 4 bytes per row)
        byte[] andRow = new byte[andRowBytes];
        for (int y = 0; y < height; y++) {
            baos.write(andRow);
        }
        
        return baos.toByteArray();
    }

    private static byte[] packShort(int v) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) v).array();
    }

    private static byte[] packInt(int v) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array();
    }
}
