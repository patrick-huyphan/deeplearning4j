package org.deeplearning4j.parallelism.trainer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.deeplearning4j.parallelism.ParallelWrapper;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.MultiDataSet;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This trainer implementation does parallel training via gradients broadcasts.
 * After each iteration, gradients from this trainer will be propagated & applied to all other trainers
 *
 * @author raver119@gmail.com
 */
@Builder
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class SymmetricTrainer extends DefaultTrainer {
    @Builder.Default protected GradientsExtractor extractor = new GradientsExtractor();

    @Override
    protected void fit(DataSet dataSet) {
        super.fit(dataSet);

        // gradients should be extracted here
        // and broadcasted to all trainers

        while (!extractor.gradients.isEmpty()) {
            INDArray grads = extractor.gradients.poll();

            parallelWrapper.broadcastGradients(grads);
        }
    }

    @Override
    protected void fit(MultiDataSet dataSet) {
        super.fit(dataSet);

        // gradients should be extracted here
    }

    @Override
    protected void postInit() {
        super.postInit();

        replicatedModel.addListener(extractor);
    }


    /**
     * This class is suited for gradients extraction out of the model
     *
     * PLEASE NOTE: It operates on gradients as a whole, not partial gradients for layers
     */
    protected static class GradientsExtractor implements TrainingListener {
        protected Queue<INDArray> gradients = new ConcurrentLinkedQueue<>();

        @Override
        public void onEpochStart(Model model) {
            // no-op
        }

        @Override
        public void onEpochEnd(Model model) {
            // no-op
        }

        @Override
        public void onForwardPass(Model model, List<INDArray> activations) {
            // no-op
        }

        @Override
        public void onForwardPass(Model model, Map<String, INDArray> activations) {
            // no-op
        }

        @Override
        public void onGradientCalculation(Model model) {
            // no-op
        }

        /**
         * In this method we extract gradients from the model
         *
         * @param model Model
         */
        @Override
        public void onBackwardPass(Model model) {
            // Beware: this code block operates out of workspaces
            Gradient gradient = model.gradient();
            INDArray array = gradient.gradient();

            // TODO: we want to push make gradient copy, and push it to host memory here

            gradients.add(array);
        }

        @Override
        public boolean invoked() {
            return false;
        }

        @Override
        public void invoke() {
            // no-op
        }

        @Override
        public void iterationDone(Model model, int iteration) {
            // no-op
        }
    }


    public static class SymmetricTrainerBuilder extends DefaultTrainerBuilder {
        @Override
        public SymmetricTrainerBuilder originalModel(Model originalModel) {
            return (SymmetricTrainerBuilder) super.originalModel(originalModel);
        }

        @Override
        public SymmetricTrainerBuilder replicatedModel(Model replicatedModel) {
            return (SymmetricTrainerBuilder) super.replicatedModel(replicatedModel);
        }

        @Override
        public SymmetricTrainerBuilder queue(LinkedBlockingQueue<DataSet> queue) {
            return (SymmetricTrainerBuilder) super.queue(queue);
        }

        @Override
        public SymmetricTrainerBuilder queueMDS(LinkedBlockingQueue<MultiDataSet> queueMDS) {
            return (SymmetricTrainerBuilder) super.queueMDS(queueMDS);
        }

        @Override
        public SymmetricTrainerBuilder running(AtomicInteger running) {
            return (SymmetricTrainerBuilder) super.running(running);
        }

        @Override
        public SymmetricTrainerBuilder threadId(int threadId) {
            return (SymmetricTrainerBuilder) super.threadId(threadId);
        }

        @Override
        public SymmetricTrainerBuilder shouldUpdate(AtomicBoolean shouldUpdate) {
            return (SymmetricTrainerBuilder) super.shouldUpdate(shouldUpdate);
        }

        @Override
        public SymmetricTrainerBuilder shouldStop(AtomicBoolean shouldStop) {
            return (SymmetricTrainerBuilder) super.shouldStop(shouldStop);
        }

        @Override
        public SymmetricTrainerBuilder thrownException(Exception thrownException) {
            return (SymmetricTrainerBuilder) super.thrownException(thrownException);
        }

        @Override
        public SymmetricTrainerBuilder useMDS(boolean useMDS) {
            return (SymmetricTrainerBuilder) super.useMDS(useMDS);
        }

        @Override
        public SymmetricTrainerBuilder onRootModel(boolean onRootModel) {
            return (SymmetricTrainerBuilder) super.onRootModel(onRootModel);
        }

        @Override
        public SymmetricTrainerBuilder parallelWrapper(ParallelWrapper parallelWrapper) {
            return (SymmetricTrainerBuilder) super.parallelWrapper(parallelWrapper);
        }

        @Override
        public SymmetricTrainerBuilder averagingFrequency(int frequency) {
            return (SymmetricTrainerBuilder) super.averagingFrequency(frequency);
        }
    }
}
