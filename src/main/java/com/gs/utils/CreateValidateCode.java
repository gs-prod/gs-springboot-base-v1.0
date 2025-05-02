package com.gs.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class CreateValidateCode {
    private BufferedImage image;
    private String code;
    private static final char[] CHAR_ARRAY = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789".toCharArray();
    private static final int WIDTH = 120;
    private static final int HEIGHT = 45;
    private static final int CODE_LENGTH = 4;
    private static final int LINE_COUNT = 150;

    public CreateValidateCode() {
        generateCode();
    }

    private void generateCode() {
        Random random = new Random();
        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setColor(getRandColor(200, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFont(new Font("Times New Roman", Font.ITALIC, 30));
        g.setColor(getRandColor(160, 200));
        for (int i = 0; i < LINE_COUNT; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            int xl = random.nextInt(12);
            int yl = random.nextInt(12);
            g.drawLine(x, y, x + xl, y + yl);
        }
        StringBuilder sRand = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            String rand = String.valueOf(CHAR_ARRAY[random.nextInt(CHAR_ARRAY.length)]);
            sRand.append(rand);
            g.setColor(new Color(20 + random.nextInt(110), 20 + random.nextInt(110), 20 + random.nextInt(110)));
            g.drawString(rand, 30 * i + 10, 35);
        }
        code = sRand.toString();
        g.dispose();
    }

    private Color getRandColor(int fc, int bc) {
        Random random = new Random();
        int r = fc + random.nextInt(bc - fc);
        int g = fc + random.nextInt(bc - fc);
        int b = fc + random.nextInt(bc - fc);
        return new Color(r, g, b);
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getCode() {
        return code;
    }
}
