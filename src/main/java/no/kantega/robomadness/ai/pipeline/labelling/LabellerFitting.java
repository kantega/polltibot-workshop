package no.kantega.robomadness.ai.pipeline.labelling;

import fj.Ord;
import fj.data.Set;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.function.Function;

public interface LabellerFitting<A> {


    Function<A, INDArray> labelEncoder();

    Function<INDArray, A> labelDecoder();

    LabellerFitting<A> fit(A value);

    Set<String> labels();

    static LabellerFitting<String> categories(){
        return new CategoryLabellerFitting(Set.empty(Ord.stringOrd));
    }

    default <B> LabellerFitting<B> xmap(Function<B, A> transformInput, Function<A, B> transformOutput) {
        return new TransformingLabellerFitting<>(this, transformInput, transformOutput);
    }

    class CategoryLabellerFitting implements LabellerFitting<String> {

        final Set<String> categories;

        public CategoryLabellerFitting(Set<String> categories) {
            this.categories = categories;
        }

        public LabellerFitting<String> fit(String value) {
            return new CategoryLabellerFitting(categories.insert(value));
        }

        @Override
        public Set<String> labels() {
            return categories;
        }

        public OneHotCategoryLabelEncoder labelEncoder() {
            return new OneHotCategoryLabelEncoder(
                categories
            );
        }

        public OneHotCategoryLabelDecoder labelDecoder() {
            return new OneHotCategoryLabelDecoder(categories);
        }

    }

    class TransformingLabellerFitting<A, B> implements LabellerFitting<B> {

        final LabellerFitting<A> wrapped;
        final Function<B, A> transformInput;
        final Function<A, B> transformOutput;

        public TransformingLabellerFitting(
            LabellerFitting<A> wrapped,
            Function<B, A> transformInput,
            Function<A, B> transformOutput) {
            this.wrapped = wrapped;
            this.transformInput = transformInput;
            this.transformOutput = transformOutput;
        }

        public Function<B, INDArray> labelEncoder() {
            return transformInput.andThen(wrapped.labelEncoder());
        }

        public Function<INDArray, B> labelDecoder() {
            return wrapped.labelDecoder().andThen(transformOutput);
        }

        public LabellerFitting<B> fit(B data) {
            return new TransformingLabellerFitting<>(wrapped.fit(transformInput.apply(data)), transformInput, transformOutput);
        }

        @Override
        public Set<String> labels() {
            return wrapped.labels();
        }
    }
}
