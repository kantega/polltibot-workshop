package no.kantega.polltibot.ai.pipeline;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RecordSources {

  public static <A> RecordSource<A> fromIterable(Iterable<A> iterable){
    return () -> StreamSupport.stream(iterable.spliterator(),true);
  }

  static Supplier<Stream<Path>> files(Path base) {
    return () -> {
      try {
        return Files.walk(base);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static RecordSource<Path> files(Path basedir, String... types) {
    return () -> {
      try {
        return Files.walk(basedir).filter(path -> !path.toFile().isDirectory()).filter(path -> {
          for (String type : Arrays.asList(types)) {
            if (path.toString().endsWith(type))
              return true;
          }
          return false;
        });
      } catch (IOException e) {
        e.printStackTrace();
        return Stream.empty();
      }
    };
  }

  public static Stream<Path> relativeFiles(Path base, Predicate<Path> filter) throws IOException {
    return Files
      .walk(base)
      .filter(path -> filter.test(path))
      .map(path -> base.relativize(path));
  }


  public static final Function<Path, BufferedImage> loadImage(Path base) {
    return path -> {
      try {
        return ImageIO.read(base.resolve(path).toFile());
      } catch (IOException e) {
        throw new RuntimeException("failed to load "+base.resolve(path).toString(),e);
      }
    };
  }

  public static final BiConsumer<Path, BufferedImage> saveImage(
    Path base,
    Function<String, String> filenamemodifier,
    String type) {
    return (path, img) -> {
      Path fullPath = base.resolve(path);
      Path filename = Paths.get(filenamemodifier.apply(fullPath.getFileName().toString()));
      Path newPath  = fullPath.getParent().resolve(filename);
      newPath.toFile().mkdirs();
      try {
        ImageIO.write(img, type, newPath.toFile());
      } catch (IOException e) {
        e.printStackTrace();
      }
    };
  }


}
