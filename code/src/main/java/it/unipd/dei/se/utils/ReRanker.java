package it.unipd.dei.se.utils;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import it.unipd.dei.se.parser.Text.ParsedTextDocument;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.ScoreDoc;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.util.*;

/**
 * The re-ranking model to re-rank documents.
 * @author CLOSE GROUP
 * @version 1.0
 */

public class ReRanker {

    /**
     * The model
     */
    private final Predictor<String[], float[][]> predictor;

    /**
     * The stored fields of the index
     */
    private final StoredFields storedFields;

    /**
     * Create a new text embedding object for the given model
     *
     * @param storedFields get the stored fields of the index
     * @param model_name   the name of the model to use
     * @throws ModelNotFoundException  if the model is not found
     * @throws MalformedModelException if the model is malformed
     * @throws IOException             if an I/O error occurs
     */
    public ReRanker(StoredFields storedFields, String model_name) throws ModelNotFoundException, MalformedModelException, IOException {
        // Create the criteria for the model
        Criteria<String[], float[][]> criteria = Criteria.builder()
                .setTypes(String[].class, float[][].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/" + model_name)
                .optEngine("PyTorch")
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .optProgress(new ProgressBar())
                .build();

        ZooModel<String[], float[][]> model = ModelZoo.loadModel(criteria);

        // Create a predictor to perform inference
        this.predictor = model.newPredictor();

        this.storedFields = storedFields;
    }

    /**
     * Get the embeddings of the given documents
     *
     * @param documents the documents to get the embeddings of
     * @return the embeddings of the documents
     * @throws TranslateException if an error occurs during translation
     */
    public List<INDArray> get_embeddings(String[] documents) throws TranslateException {
            List<INDArray> embeddings = new ArrayList<>();
            for (float[][] results : predictor.batchPredict(Collections.singletonList(documents))) {
                for (float[] result : results) {
                    embeddings.add(Nd4j.create(result));
                }
            }
            return embeddings;
    }

    /**
     * Sort the given score docs by similarity to the given query
     *
     * @param query     the query to check similarity to
     * @param scoreDocs the list of documents to sort
     * @param sum_queries list of additional queries used to calculate the similarity score
     * @return the sorted score docs
     * @throws TranslateException if an error occurs during translation
     * @throws IOException        if an I/O error occurs
     */
    public ScoreDoc[] sort(String query, List<String> sum_queries, ScoreDoc[] scoreDocs) throws TranslateException, IOException {
        // Create the fields to get the body of the documents
        final Set<String> fields = new HashSet<>();
        fields.add(ParsedTextDocument.Fields.BODY);

        String[] documents = new String[scoreDocs.length + 1];
        for (int i = 0; i < scoreDocs.length; i++) {
            // Get the body of the document and add it to the list of documents
            documents[i] = storedFields.document(scoreDocs[i].doc, fields).get(ParsedTextDocument.Fields.BODY);
        }

        // Add the query to the list of documents, Join sum_queries with a space
        documents[scoreDocs.length] = query + " " +String.join(" ", sum_queries);

        // Get the embeddings of the documents
        List<INDArray> embeddings = get_embeddings(documents);
        // Get the query embedding
        INDArray query_embedding = embeddings.remove(embeddings.size() - 1);

        // Calculate the similarity between the query and the documents
        for (int i = 0; i < embeddings.size(); i++) {
            INDArray de = embeddings.get(i);
            
            // Calculate the cosine similarity between the query and the document, the higher, is better
            double similarity = de.mul(query_embedding).sumNumber().doubleValue() / (de.norm2Number().doubleValue() * query_embedding.norm2Number().doubleValue());

            // Calculate the Manhattan similarity between the query and the document, the lower, is better
            // double similarity = de.sub(query_embedding).norm1Number().doubleValue();

            // Calculate the Jaccard similarity between the query and the document, the higher, is better
            // double similarity = de.mul(query_embedding).sumNumber().doubleValue() / de.add(query_embedding).sub(de.mul(query_embedding)).sumNumber().doubleValue();

            // Calculate the Pearson correlation coefficient similarity between the query and the document, the higher, is better
            // double similarity = de.sub(de.meanNumber()).mul(query_embedding.sub(query_embedding.meanNumber())).sumNumber().doubleValue() / (de.stdNumber().doubleValue() * query_embedding.stdNumber().doubleValue());

            // Calculate the Mahalanobis distance similarity between the query and the document, the lower, is better
            // double similarity = de.sub(query_embedding).mmul(de.sub(query_embedding).transpose()).sumNumber().doubleValue();

            // Change the score of the doc to the similarity
            scoreDocs[i].score = (float) (similarity * scoreDocs[i].score);
        }

        // Sort the score docs by similarity
        Arrays.sort(scoreDocs, Comparator.comparingDouble(sd -> -sd.score));

        return scoreDocs;
    }

    /**
     * Close the resources used by the re-ranker
     */
    public void close() {
        // Clean up resources
        this.predictor.close();
    }


    /**
     * Method to print the device that will use the model
     * @param args the arguments for the method.
     */
    public static void main(String[] args) {
        System.out.println(Device.fromName("cuda"));
    }

}