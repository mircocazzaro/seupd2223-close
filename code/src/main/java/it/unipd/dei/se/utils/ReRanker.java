package it.unipd.dei.se.utils;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.engine.Engine;
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

public class ReRanker {

    /**
     * The model
     */
    private final Predictor<String, float[]> predictor;

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
        Criteria<String, float[]> criteria = Criteria.builder()
                        .setTypes(String.class, float[].class)
                        .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/" + model_name)
                        .optEngine("PyTorch")
                        .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                        .optProgress(new ProgressBar())
                        .build();

        ZooModel<String, float[]> model = ModelZoo.loadModel(criteria);

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
    public List<INDArray> get_embeddings(List<String> documents) throws TranslateException {
            // Todo: batch creating embeddings
            List<INDArray> embeddings = new ArrayList<>();
            for (String document : documents) {
                // Predict the embedding of the document
                embeddings.add(Nd4j.create(predictor.predict(document)));
            }
            return embeddings;
    }

    /**
     * Sort the given score docs by similarity to the given query
     *
     * @param query     the query to check similarity to
     * @param scoreDocs the list of documents to sort
     * @return the sorted score docs
     * @throws TranslateException if an error occurs during translation
     * @throws IOException        if an I/O error occurs
     */
    public ScoreDoc[] sort(String query, ScoreDoc[] scoreDocs) throws TranslateException, IOException {
        // Create the fields to get the body of the documents
        final Set<String> fields = new HashSet<>();
        fields.add(ParsedTextDocument.Fields.BODY);

        List<String> documents = new ArrayList<>();
        for (ScoreDoc sd : scoreDocs) {
            // Get the body of the document
            documents.add(storedFields.document(sd.doc, fields).get(ParsedTextDocument.Fields.BODY));
        }

        // Add the query to the list of documents
        documents.add(query);

        // Get the embeddings of the documents
        List<INDArray> embeddings = get_embeddings(documents);
        // Get the query embedding
        INDArray query_embedding = embeddings.remove(embeddings.size() - 1);

        // Calculate the similarity between the query and the documents
        for (int i = 0; i < embeddings.size(); i++) {
            INDArray de = embeddings.get(i);
            double similarity = de.mul(query_embedding).sumNumber().doubleValue() / (de.norm2Number().doubleValue() * query_embedding.norm2Number().doubleValue());
            // Change the score of the doc to the similarity
            scoreDocs[i].score = (float) similarity;
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


    public static void main(String[] args) {
        System.out.println(Device.fromName("cuda"));
    }

}