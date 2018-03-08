package no.kantega.polltibot.ai.pipeline.clustering;


/**
 * This class implements Entropy LSH for the cosine distance, in order to preserve memory for large datasets.
 *
 * Entropy SLH is the LSH scheme of
 *
 * _Entropy based nearest neighbor search in high dimensions_
 * R Panigrahy - SIAM 2006
 * https://arxiv.org/pdf/cs/0510019.pdf
 *
 * To read more about LSH, in particular for the Cosine distance, see
 * chapter 3 of :
 * _Mining Massive Datasets_, Anand Rajaraman and Jeffrey Ullman
 * http://www.mmds.org/
 *
 * The original development of LSH for the cosine distance is from
 * Similarity estimation techniques from rounding algorithms
 * MS Charikar - STOCS, 2002
 *
 * Note for high-precision or distributed settings, you should not
 * use this and rather extend this to layered LSH ( https://arxiv.org/abs/1210.7057 )
 *
 */
public class LSH {


    public static final long LARGE_PRIME =  433494437;



    /**
     * Hash a signature.
     * The signature is divided in s stages (or bands). Each stage is hashed to
     * one of the b buckets.
     * @param signature
     * @return An vector of s integers (between 0 and b-1)
     */
    public static int[] hashSignature(final int stages, final int buckets, final int[] signature) {

        // Create an accumulator for each stage
        int[] hash = new int[stages];

        // Number of rows per stage
        int rows = signature.length / stages;

        for (int i = 0; i < signature.length; i++) {
            int stage = Math.min(i / rows, stages - 1);
            hash[stage] = (int)
                    ((hash[stage] + (long) signature[i] * LARGE_PRIME)
                            % buckets);

        }

        return hash;
    }

    /**
     * Hash a signature.
     * The signature is divided in s stages (or bands). Each stage is hashed to
     * one of the b buckets.
     * @param signature
     * @return An vector of s integers (between 0 and b-1)
     */
    public static int[] hashSignature(final int stages, final int buckets, final boolean[] signature) {

        // Create an accumulator for each stage
        long[] acc = new long[stages];
        for (int i = 0; i < stages; i++) {
            acc[i] = 0;
        }

        // Number of rows per stage
        int rows = signature.length / stages;

        for (int i = 0; i < signature.length; i++) {
            long v = 0;
            if (signature[i]) {
                v = (i + 1) * LARGE_PRIME;
            }

            // current stage
            int j = Math.min(i / rows, stages - 1);
            acc[j] = (acc[j] + v) % Integer.MAX_VALUE;
        }

        int[] r = new int[stages];
        for (int i = 0; i < stages; i++) {
            r[i] = (int) (acc[i] % buckets);
        }

        return r;
    }
}