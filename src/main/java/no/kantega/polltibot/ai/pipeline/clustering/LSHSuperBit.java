package no.kantega.polltibot.ai.pipeline.clustering;



import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.Serializable;


public class LSHSuperBit implements Serializable {
    private SuperBit sb;
    private final int stages;
    private final int buckets;


    /**
     * LSH implementation relying on SuperBit, to bin vectors s times (stages)
     * in b buckets (per stage), in a space with n dimensions. Input vectors
     * with a high cosine similarity have a high probability of falling in the
     * same bucket...
     * <p>
     * Supported input types:
     * - double[]
     * - sparseIntegerVector
     * - int[]
     * - others to come...
     *
     * @param stages     stages
     * @param buckets    buckets (per stage)
     * @param dimensions dimensionality
     * @param seed       random number generator seed. using the same value will
     *                   guarantee identical hashes across object instantiations
     */
    public LSHSuperBit(
            final int stages,
            final int buckets,
            final int dimensions,
            final long seed) {

        this.buckets = buckets;
        this.stages = stages;

        int code_length = stages * buckets / 2;
        int superbit = computeSuperBit(stages, buckets, dimensions);

        this.sb = new SuperBit(
                dimensions, superbit, code_length / superbit, seed);
    }

    /**
     * Compute the superbit value.
     *
     * @param stages
     * @param buckets
     * @param dimensions
     * @return
     */
    private int computeSuperBit(
            final int stages, final int buckets, final int dimensions) {

        // SuperBit code length
        int code_length = stages * buckets / 2;
        int superbit; // superbit value
        for (superbit = dimensions; superbit >= 1; superbit--) {
            if (code_length % superbit == 0) {
                break;
            }
        }

        if (superbit == 0) {
            throw new IllegalArgumentException(
                    "Superbit is 0 with parameters: s=" + stages
                            + " b=" + buckets + " n=" + dimensions);
        }

        return superbit;
    }



    public final int[] hash(final INDArray vector) {
        return LSH.hashSignature(stages,buckets,sb.signature(vector));
    }
    public final int[] hash(final double[]  vector) {
        return LSH.hashSignature(stages,buckets,sb.signature(vector));
    }

}
