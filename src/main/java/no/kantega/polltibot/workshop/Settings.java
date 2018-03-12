package no.kantega.polltibot.workshop;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Settings {

    public static final int maxWords = 20;
    public static final int miniBatchSize = 24;
    public static final int maxepochs = 100;

    public static final Path fastTextPath =
            Paths.get(System.getProperty("user.home") + "/data/neuralnet/wiki.no.vec").toAbsolutePath();

    static final Path modelPath =
            Paths.get(System.getProperty("user.home") + "/data/neuralnet/model.net").toAbsolutePath();


}
