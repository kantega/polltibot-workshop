package no.kantega.polltibot;

public class WorkshopMain {


    public static void main(String[] args) {

        //StreamTransformers inneholder mye nyttig

        //Lag en MLTask<Stream<String>> som laster treningsdata
        //Feks med Files.load(Path) og MLTask.loadEpoch


        //Lag en pipeline som:

        //Del i tokens (ord)

        //Oversett ord til vektor med FastTextMap

        //Sørg for at alle input er minst like lange (padRight feks?) 10
        //Hva fyller man inn tomrom med???

        //Sørg for at ingen er lenger enn 10 (truncate)

        //Lag batcher med data (batch kanskje?)

        //Legg på et trenngssteg med apply

        //La jobben repeteres, Et RNN tar lang tid, så ha to iterasjoner først

        //Legg det trente nettet i en PipelineConfig

        //Lagre config med PipelineConfig.write

    }
}
