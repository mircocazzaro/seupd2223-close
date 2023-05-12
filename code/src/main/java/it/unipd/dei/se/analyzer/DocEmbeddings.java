package it.unipd.dei.se.analyzer;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;
import java.util.List;


import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import java.io.File;
import java.io.IOException;

/**
 * Class for embeddings for the documents
 *
 * @author Close Group
 * @version 1.00
 * @since 1.00
 */
public class DocEmbeddings {
    private static DocEmbeddings instance;
    private Word2Vec model;
    private DocEmbeddings() {
        model = WordVectorSerializer.readWord2VecModel(
                "/Users/farzad/Downloads/frWac_no_postag_no_phrase_500_cbow_cut100.bin"
        );
    }

    /**
     * Get embeddings for a query.
     * @param query the query from which we get the embeddings.
     * @return the embeddings for the query.
     */
    public float[] getEmbeddingForQuery(String query) {
        String[] words = query.split("\\s+");
        float[] queryEmbedding = new float[words.length];
        for (int i = 0; i < words.length; i++) {
            queryEmbedding[i] = Float.parseFloat(words[i].replace("\\", ""));
        }
        return queryEmbedding;
    }

    /**
     * Get an instance of DocEmbeddings
     * @return instance of DocEmbeddings or new instance of DocEmbeddings
     */
    public static synchronized DocEmbeddings getInstance() {
        if (instance == null) {
            instance = new DocEmbeddings();
        }
        return instance;
    }

    /**
     * get the Word2Vec model
     * @return the Word2Vec model
     */
    public Word2Vec getModel() {
        return model;
    }

    /**
     * Generate embeddings for a given document
     * @param doc the document from which the embeddings are generated.
     * @return the generated embeddings.
     */
    public INDArray generateDocEmbedding(String doc) {
        List<String> tokens = Arrays.asList(doc.split("\\s+"));
        INDArray embeddings = Nd4j.zeros(tokens.size(), model.getLayerSize());
        int validTokens = 0;

        for (String token : tokens) {
            if (model.hasWord(token)) {
                embeddings.putRow(validTokens, model.getWordVectorMatrix(token));
                validTokens++;
            }
        }

        return embeddings.mean(0);
    }

    /**
     * Main method to try the class and get embeddings for a text
     * @param args the arguments of the method.
     */
    public static void main(String[] args) {
        try {
            String doc = "aeroport bordeaux";
            INDArray docEmbedding = DocEmbeddings.getInstance().generateDocEmbedding(doc);
            System.out.println(docEmbedding.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}