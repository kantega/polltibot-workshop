package no.kantega.polltibot.ai.pipeline.persistence;

import fj.data.List;
import fj.data.Set;
import no.kantega.polltibot.ai.pipeline.training.MLTask;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class PipelineConfig implements Serializable {

    public final HashMap<String, Object> values;
    public final MultiLayerNetwork net;

    public PipelineConfig(MultiLayerNetwork net, HashMap<String, Object> values) {
        this.net = net;
        this.values = values;
    }

    public static PipelineConfig newEmptyConfig(MultiLayerNetwork net) {
        return new PipelineConfig(net, new HashMap<>());
    }

    public static MLTask<PipelineConfig> read(byte[] input) {
        return MLTask.trySupply(() -> {

            ZipInputStream zipInputStream = new ZipInputStream(new CloseShieldInputStream(new ByteArrayInputStream(input)));

            ZipEntry configEntry = zipInputStream.getNextEntry();
            String config = readConfig(zipInputStream);
            ZipEntry paramEntry = zipInputStream.getNextEntry();
            INDArray params = readCoefficients(zipInputStream);
            zipInputStream.getNextEntry();
            HashMap<String, Object> pipelineConfig = readPipelineConfig(zipInputStream);

            MultiLayerConfiguration confFromJson = MultiLayerConfiguration.fromJson(config);
            MultiLayerNetwork network = new MultiLayerNetwork(confFromJson);
            network.init(params, false);

            return new PipelineConfig(network, pipelineConfig);
        });
    }

    private static String readConfig(ZipInputStream zipInputStream) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));
        String line;
        StringBuilder js = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            js.append(line).append("\n");
        }
        return js.toString();
    }


    private static INDArray readCoefficients(ZipInputStream zipInputStream) throws IOException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(zipInputStream));
        return Nd4j.read(dis);
    }

    @SuppressWarnings("unchecked")
    private static HashMap<String, Object> readPipelineConfig(ZipInputStream zipInputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(zipInputStream);
        return (HashMap<String, Object>) ois.readObject();
    }

    public static MLTask<byte[]> write(PipelineConfig config) {
        return MLTask.trySupply(() -> {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipOutputStream zipfile = new ZipOutputStream(new CloseShieldOutputStream(bos));

            // Save configuration as JSON
            String json = config.net.getLayerWiseConfigurations().toJson();
            ZipEntry netConfig = new ZipEntry("configuration.json");
            zipfile.putNextEntry(netConfig);
            zipfile.write(json.getBytes());

            // Save parameters as binary
            ZipEntry coefficients = new ZipEntry("coefficients.bin");
            zipfile.putNextEntry(coefficients);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(zipfile));
            Nd4j.write(config.net.params(), dos);
            dos.flush();

            ZipEntry pipelineConfig = new ZipEntry("params.bin");
            zipfile.putNextEntry(pipelineConfig);
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(zipfile));
            oos.writeObject(config.values);

            oos.flush();

            zipfile.close();
            return bos.toByteArray();
        });
    }

    public PipelineConfig set(String key, String value) {
        return setObject(key, value);
    }

    public double getDouble(String key) {
        return (double) get(key);
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public List<String> getList(String key) {
        return (List<String>) get(key);
    }

    public Set<String> getSet(String key) {
        return (Set<String>) get(key);
    }


    private Object get(String key) {
        return values.get(key);
    }

    public PipelineConfig set(String key, double value) {
        return setObject(key, value);
    }

    public PipelineConfig set(String key, List<String> value) {
        return setObject(key, value);
    }

    public PipelineConfig set(String key, Set<String> value) {
        return setObject(key, value);
    }

    private PipelineConfig setObject(String key, Object value) {
        values.put(key, value);
        return this;
    }


}
