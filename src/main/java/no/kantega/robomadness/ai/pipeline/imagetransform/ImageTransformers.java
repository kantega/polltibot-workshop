package no.kantega.robomadness.ai.pipeline.imagetransform;

import org.datavec.image.data.ImageWritable;
import org.datavec.image.loader.ImageLoader;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.transform.RandomCropTransform;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.function.Function;

public class ImageTransformers {

    public static final Function<BufferedImage, BufferedImage> convertToArgb =
        bufImg -> {
            BufferedImage convertedImg = new BufferedImage(bufImg.getWidth(), bufImg.getHeight(), BufferedImage.TYPE_INT_ARGB);
            convertedImg.getGraphics().drawImage(bufImg, 0, 0, null);
            return convertedImg;
        };

    public static final Function<BufferedImage, BufferedImage> convertFromArgb =
        bufImg -> {
            BufferedImage convertedImg = new BufferedImage(bufImg.getWidth(), bufImg.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            convertedImg.getGraphics().drawImage(bufImg, 0, 0, null);
            return convertedImg;
        };


    public static Function<BufferedImage, BufferedImage> randomCrop(Random random, int width, int heigth) {
        return bufferedImage ->
            bufferedImage.getSubimage(
                random.nextInt(bufferedImage.getWidth() - width),
                random.nextInt(bufferedImage.getHeight() - heigth),
                width,
                heigth
            );
    }

    public static Function<BufferedImage, INDArray> convertRGBToInput =
        bufferedImage -> {
            INDArray arr = new ImageLoader(bufferedImage.getHeight(), bufferedImage.getWidth(), 3).asMatrix(bufferedImage);
            return arr;
        };

    public static Function<BufferedImage, BufferedImage> makeGray = img -> {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int col     = img.getRGB(x, y);
                int red     = (col & 0xff0000) / 65536;
                int green   = (col & 0xff00) / 256;
                int blue    = (col & 0xff);
                int graycol = (red + green + blue) / 3;
                img.setRGB(x, y, toColor(graycol, graycol, graycol, 255));
            }
        }
        return img;
    };
    private static int toColor(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                ((b & 0xFF) << 0);


    }
    public static Function<String, BufferedImage> fromBase64 =
        base64 ->
        {
            try {
                return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

    public static Function<BufferedImage, BufferedImage> removeTransparency =
        inputImg -> {
            BufferedImage noAlpha = new BufferedImage(inputImg.getWidth(), inputImg.getHeight(), BufferedImage.TYPE_INT_RGB);
            noAlpha.getGraphics().setColor(Color.WHITE);
            noAlpha.getGraphics().fillRect(0, 0, noAlpha.getWidth(), noAlpha.getHeight());
            noAlpha.getGraphics().drawImage(inputImg, 0, 0, null);
            return noAlpha;
        };

    public static Function<ImageWritable, ImageWritable> randomCrop(int width, int height, long seed) {
        RandomCropTransform transform = new RandomCropTransform(null, seed, height, width);
        return img -> transform.transform(img, null);
    }

    public static Function<ImageWritable, INDArray> toINDArray() {
        NativeImageLoader imgLoader = new NativeImageLoader();

        return imageWritable -> {
            try {
                return imgLoader.asMatrix(imageWritable);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
