package no.kantega.polltibot.ai.pipeline.imagetransform;

import org.apache.commons.io.IOUtils;
import org.bytedeco.javacpp.lept;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.datavec.image.data.ImageWritable;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static org.bytedeco.javacpp.lept.*;
import static org.bytedeco.javacpp.opencv_core.CV_8UC;
import static org.bytedeco.javacpp.opencv_core.mixChannels;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;

public class LoadImage implements Function<Path,ImageWritable> {
    static OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();



    public ImageWritable apply(Path path) {
        byte[] bytes = new byte[0];
        try {
            bytes = IOUtils.toByteArray(Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        opencv_core.Mat image = imdecode(new opencv_core.Mat(bytes), CV_LOAD_IMAGE_ANYDEPTH | CV_LOAD_IMAGE_ANYCOLOR);
        if (image == null || image.empty()) {
            lept.PIX pix = pixReadMem(bytes, bytes.length);
            if (pix == null) {
                throw new RuntimeException("Could not decode image from input stream");
            }
            image = convert(pix);
            pixDestroy(pix);
        }
        ImageWritable writable = new ImageWritable(converter.convert(image));
        image.deallocate();
        return writable;
    }

    static opencv_core.Mat convert(lept.PIX pix) {
        lept.PIX tempPix = null;
        if (pix.colormap() != null) {
            lept.PIX pix2 = pixRemoveColormap(pix, REMOVE_CMAP_TO_FULL_COLOR);
            tempPix = pix = pix2;
        } else if (pix.d() < 8) {
            PIX pix2 = null;
            switch (pix.d()) {
                case 1:
                    pix2 = pixConvert1To8(null, pix, (byte) 0, (byte) 255);
                    break;
                case 2:
                    pix2 = pixConvert2To8(pix, (byte) 0, (byte) 85, (byte) 170, (byte) 255, 0);
                    break;
                case 4:
                    pix2 = pixConvert4To8(pix, 0);
                    break;
                default:
                    assert false;
            }
            tempPix = pix = pix2;
        }
        int             height   = pix.h();
        int             width    = pix.w();
        int             channels = pix.d() / 8;
        opencv_core.Mat mat      = new opencv_core.Mat(height, width, CV_8UC(channels), pix.data(), 4 * pix.wpl());
        opencv_core.Mat mat2     = new opencv_core.Mat(height, width, CV_8UC(channels));
        // swap bytes if needed
        int[] swap = {0, channels - 1, 1, channels - 2, 2, channels - 3, 3, channels - 4},
            copy = {0, 0, 1, 1, 2, 2, 3, 3},
            fromTo = channels > 1 && ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN) ? swap : copy;
        mixChannels(mat, 1, mat2, 1, fromTo, Math.min(channels, fromTo.length / 2));
        if (tempPix != null) {
            pixDestroy(tempPix);
        }
        return mat2;
    }
}
