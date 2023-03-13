/*
 * Copyright (c) 2017-2018, guanquan.wang@yandex.com All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ttzero.excel.entity;

import org.ttzero.excel.manager.Const;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Excel does not have a watermark function. The watermark here
 * only designs the Worksheet background image. This setting
 * will be ignored when printing.
 *
 * @author guanquan.wang at 2018-01-26 15:23
 */
public class WaterMark {
    private final Path imagePath;
    private boolean temp;

    public WaterMark(String word) {
        imagePath = createWaterMark(word);
    }

    public WaterMark(Path imagePath) {
        this.imagePath = imagePath;
    }

    public WaterMark(InputStream inputStream) throws IOException {
        imagePath = createTemp();
        Files.copy(inputStream, imagePath, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path get() {
        return imagePath;
    }

    /**
     * Create a water mark with string value
     *
     * @param mark the mark value
     * @return WaterMark
     */
    public static WaterMark of(String mark) {
        return new WaterMark(mark);
    }

    /**
     * Create a water mark with location image
     *
     * @param path the image location path
     * @return WaterMark
     */
    public static WaterMark of(Path path) {
        return new WaterMark(path);
    }

    /**
     * Create a water mark with InputStream image
     *
     * @param is the image InputStream
     * @return WaterMark
     * @throws IOException if io error occur
     */
    public static WaterMark of(InputStream is) throws IOException {
        return new WaterMark(is);
    }

    /**
     * Make a picture with string value
     *
     * @param watermark mark value
     * @return the temp image path
     */
    private Path createWaterMark(String watermark) {
        try {
            Path temp = createTemp();
            int width = 510;
            int height = 300;

            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            // Setting white background color
            int minx = bi.getMinX();
            int miny = bi.getMinY();
            for (int i = minx; i < width; i++) {
                for (int j = miny; j < height; j++) {
                    bi.setRGB(i, j, 0xffffff);
                }
            }

            Graphics2D g2d = bi.createGraphics();
            g2d.setColor(new Color(200, 200, 200));
            g2d.setStroke(new BasicStroke(1));
            g2d.setFont(new java.awt.Font("华文细黑", java.awt.Font.ITALIC, 50));
            g2d.rotate(Math.toRadians(-10));

            for (int i = 1; i < 10; i++) {
                g2d.drawString(watermark, 0, 60 * i);
            }
            // Setting alpha
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g2d.dispose();
            ImageIO.write(bi, "png", temp.toFile());
            return temp;
        } catch (IOException e) {
            throw new ExcelWriteException("Create Water Mark error.", e);
        }
    }

    private Path createTemp() throws IOException {
        temp = true;
        return Files.createTempFile("waterMark", "png");
    }

    public boolean delete() {
        if (imagePath != null && temp) {
            try {
                Files.deleteIfExists(imagePath);
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the picture suffix
     */
    public String getSuffix() {
        String suffix = null;
        if (temp) {
            suffix = Const.Suffix.PNG;
        } else if (imagePath != null) {
            String name = imagePath.getFileName().toString();
            int n;
            if ((n = name.lastIndexOf('.')) > 0) {
                suffix = name.substring(n);
            }
        }
        return suffix != null ? suffix : Const.Suffix.PNG;
    }

    /**
     * content-type
     *
     * @return string
     */
    public String getContentType() {
        String suffix = getSuffix().substring(1).toUpperCase();
        Field[] fields = Const.ContentType.class.getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().equals(suffix)) {
                try {
                    return f.get(null).toString();
                } catch (IllegalAccessException e) {
                    // Empty
                }
            }
        }
        return Const.ContentType.PNG;
    }
}
