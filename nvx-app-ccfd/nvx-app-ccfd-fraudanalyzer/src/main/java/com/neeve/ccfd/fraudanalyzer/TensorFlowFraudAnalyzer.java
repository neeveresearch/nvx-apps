/**
 * Copyright (c) 2018 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ccfd.fraudanalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.List;

import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Output;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Session.Runner;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;

import com.neeve.ccfd.messages.FraudAnalysisRequestMessage;
import com.neeve.ccfd.messages.TransformedPaymentTransactionDTO;
import com.neeve.ci.XRuntime;
import com.neeve.trace.Tracer;
import com.neeve.trace.Tracer.Level;
import com.neeve.util.UtlJar;

/**
 * The TensorFlow fraud analyzer.
 * 
 * This implementation uses a TensorFlow trained model to check for fraud. 
 */
public class TensorFlowFraudAnalyzer implements FraudAnalyzer {
    private static Tracer tracer = Tracer.get("ccfc.fraudanalyzer");

    private final FloatBuffer paramInputData = ByteBuffer.allocateDirect(Long.SIZE * 37).asFloatBuffer();
    private final long[] paramInputDimensions = new long[] { 1, 37 };
    private final Tensor<Float> paramKeep = Tensor.create(new Float(1.0f), Float.class);
    private final FloatBuffer resultBuffer = ByteBuffer.allocateDirect(Long.SIZE * 2).asFloatBuffer();

    private Graph graph;
    private Session session;
    private Output<?> inputData;
    private Output<?> keep;
    private Output<?> result;

    /* (non-Javadoc)
     * @see com.neeve.ccfd.fraudanalyzer.FraudAnalyzer#open()
     */
    @Override
    public final void open() throws Exception {
        File modelSourceFolder = extractModel();
        Tensor<String> checkpointPrefix = Tensors.create(modelSourceFolder.getPath() + File.separator + "checkpoint" + File.separator + "model-checkpoint.ckpt");
        SavedModelBundle b = SavedModelBundle.load(modelSourceFolder.getPath() + File.separator + "saved-model", "serve");
        this.graph = b.graph();
        this.session = new Session(graph);

        // Initialize from training checkpoint
        session.runner().feed("save/Const", checkpointPrefix).addTarget("save/restore_all").run();

        inputData = graph.operation("inputdata").output(0);
        keep = graph.operation("pkeep").output(0);
        result = graph.operation("Softmax").output(0);

        if (tracer.debug) {
            Iterator<Operation> operations = graph.operations();
            while (operations.hasNext()) {
                tracer.log("Operation: " + operations.next().name(), Tracer.Level.DEBUG);
            }
        }
    }

    private File extractModel() throws IOException {
        tracer.log("Extracting Tensor Flow model bundle", Level.INFO);
        File extractPath = new File(XRuntime.getDataDirectory(), "ml-model");
        URL modelBundleUrl = this.getClass().getResource("/ml-output.zip");
        if (modelBundleUrl == null) {
            throw new FileNotFoundException("ml-output.zip was not found on the application classpath!");
        }
        UtlJar.extractFolder(modelBundleUrl, "", extractPath);
        tracer.log("Tensor Flow model bundle extracted to " + extractPath.getCanonicalPath(), Level.INFO);
        return extractPath;
    }

    /* (non-Javadoc)
     * @see com.neeve.ccfd.fraudanalyzer.FraudAnalyzer#close()
     */
    public final void close() throws Exception {
        session.close();
    }

    /* (non-Javadoc)
     * @see com.neeve.ccfd.fraudanalyzer.FraudAnalyzer#isFraudulent(com.neeve.ccfd.messages.FraudAnalysisRequestMessage)
     */
    public final boolean isFraudulent(final FraudAnalysisRequestMessage request) {

        //Fill in the feature input data:
        paramInputData.position(0).limit(paramInputData.capacity());
        TransformedPaymentTransactionDTO transactionDetails = request.getNewTransaction();

        /*
         * Add the normalized field values to the tensor
         * 
         * Excludes: 'V28','V27','V26','V25','V24','V23','V22','V20','V15','V13','V8'
         *           which wer deemed uninteresting. 
         */
        paramInputData.put(transactionDetails.getTimeNormal());
        paramInputData.put(transactionDetails.getV1());
        paramInputData.put(transactionDetails.getV2());
        paramInputData.put(transactionDetails.getV3());
        paramInputData.put(transactionDetails.getV4());
        paramInputData.put(transactionDetails.getV5());
        paramInputData.put(transactionDetails.getV6());
        paramInputData.put(transactionDetails.getV7());
        paramInputData.put(transactionDetails.getV9());
        paramInputData.put(transactionDetails.getV10());
        paramInputData.put(transactionDetails.getV11());
        paramInputData.put(transactionDetails.getV12());
        paramInputData.put(transactionDetails.getV14());
        paramInputData.put(transactionDetails.getV16());
        paramInputData.put(transactionDetails.getV17());
        paramInputData.put(transactionDetails.getV18());
        paramInputData.put(transactionDetails.getV19());
        paramInputData.put(transactionDetails.getV21());
        paramInputData.put(transactionDetails.getAmountNormal());
        paramInputData.put(transactionDetails.getAmountMaxFraudNormal());
        paramInputData.put(transactionDetails.getV1_());
        paramInputData.put(transactionDetails.getV2_());
        paramInputData.put(transactionDetails.getV3_());
        paramInputData.put(transactionDetails.getV4_());
        paramInputData.put(transactionDetails.getV5_());
        paramInputData.put(transactionDetails.getV6_());
        paramInputData.put(transactionDetails.getV7_());
        paramInputData.put(transactionDetails.getV9_());
        paramInputData.put(transactionDetails.getV10_());
        paramInputData.put(transactionDetails.getV11_());
        paramInputData.put(transactionDetails.getV12_());
        paramInputData.put(transactionDetails.getV14_());
        paramInputData.put(transactionDetails.getV16_());
        paramInputData.put(transactionDetails.getV17_());
        paramInputData.put(transactionDetails.getV18_());
        paramInputData.put(transactionDetails.getV19_());
        paramInputData.put(transactionDetails.getV21_());

        paramInputData.flip();
        final Tensor<Float> inputDataTensor = Tensor.create(paramInputDimensions, paramInputData);

        Runner runner = session.runner();
        runner = runner.feed(inputData, inputDataTensor);
        runner = runner.feed(keep, paramKeep);

        List<Tensor<?>> results = runner.fetch(result).run();

        if (results.size() != 1) {
            throw new IllegalArgumentException("Unexpected result set size: " + results.size());
        }
        Tensor<?> t = results.get(0);

        boolean fraudPrediction = false;
        if (t.dataType() == DataType.FLOAT) {
            resultBuffer.position(0).limit(resultBuffer.capacity());
            results.get(0).writeTo(resultBuffer);
            resultBuffer.flip();
            float fraud = resultBuffer.get();
            float normal = resultBuffer.get();
            fraudPrediction = (fraud > normal);
        }
        else {
            throw new IllegalArgumentException("Unexpected result data type: " + t.dataType());
        }
        results.get(0).close();
        return fraudPrediction;
    }
}
