package no.kantega.polltibot.twitter;

import fj.P;
import fj.P2;
import fj.P3;
import fj.Unit;
import fj.data.List;
import no.kantega.polltibot.Corpus;
import no.kantega.polltibot.ai.pipeline.MLTask;
import org.kantega.niagara.Task;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class TwitterStore {

    private static TwitterStore instance;

    private final Connection connection;
    private final AtomicLong counter = new AtomicLong();
    static final Path dir =
            Paths.get(System.getProperty("user.home") + "/data/tweets").toAbsolutePath();

    private TwitterStore() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
        File f = dir.toFile();
        f.mkdirs();

        if (!f.exists() || !f.isDirectory())
            throw new RuntimeException(f.getAbsolutePath() + " is not a directrory");

        String dburl = "jdbc:h2:" + f.toPath().toString();
        System.out.println(dburl);

        try {
            connection = DriverManager.getConnection(dburl);
            connection
                    .prepareStatement("create table if not exists tweets(id BIGINT identity PRIMARY KEY , corpus varchar(250),text varchar(250), source clob)")
                    .execute();

            ResultSet rs =
                    connection.prepareStatement("select max(id) as c from tweets").executeQuery();

            while (rs.next())
                counter.set(rs.getLong("c"));

        } catch (SQLException e) {
            throw new RuntimeException("Could not create tweets database", e);
        }
    }

    public static synchronized TwitterStore getStore() {
        if (instance == null) {
            instance = new TwitterStore();
        }
        return instance;
    }

    public Task<Unit> add(long id, Corpus corpus, String text, String source) {
        return Task.tryTask(() -> {

            PreparedStatement q = connection.prepareStatement("select * from tweets where id = ?");
            q.setLong(1, id);

            ResultSet rs = q.executeQuery();
            if (!rs.next()) {
                PreparedStatement statement = connection.prepareStatement("insert into tweets (id,corpus,text, source) values (?,?,?, ?)");
                statement.setLong(1, id);
                statement.setString(2, corpus.name());
                statement.setString(3, text);
                statement.setString(4, source);
                statement.executeUpdate();
            }
            return Unit.unit();
        });
    }

    public MLTask<List<P3<Long, String, String>>> tweets(Corpus corpus) {
        return MLTask.trySupply(() -> {
            PreparedStatement s = connection.prepareStatement("select id,text,source from tweets where corpus = ?");
            s.setString(1, corpus.name());
            ResultSet rs = s.executeQuery();
            List.Buffer<P3<Long, String, String>> buffer = new List.Buffer<>();
            while (rs.next()) {
                buffer.snoc(P.p(rs.getLong(1), rs.getString(2), rs.getString(3)));
            }
            return buffer.toList();
        });
    }

    public MLTask<Stream<String>> corpus(Corpus corpus) {
        return tweets(corpus).map(l -> l.toJavaList().stream()).map(s -> s.map(P3::_2));
    }

    public MLTask<Unit> deleteCorpus(Corpus corpus) {
        return MLTask.trySupply(() -> {


            PreparedStatement s = connection.prepareStatement("delete from tweets where corpus = ?");
            s.setString(1, corpus.name());

            s.execute();
            return Unit.unit();

        });
    }

    public long count() {
        return counter.get();
    }
}
