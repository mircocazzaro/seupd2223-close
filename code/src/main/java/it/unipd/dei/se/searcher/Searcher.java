/*
 *  Copyright 2021-2023 University of Padua, Italy
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package it.unipd.dei.se.searcher;

import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;
import com.google.gson.Gson;
import it.unipd.dei.se.analyzer.DocEmbeddings;
import it.unipd.dei.se.parser.Embedded.ParsedEmbeddedDocument;
import it.unipd.dei.se.parser.Text.ParsedTextDocument;
import it.unipd.dei.se.utils.ReRanker;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.queryparser.xml.builders.TermQueryBuilder;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Searches a document collection.
 *
 * @author CLOSE GROUP
 * @version 1.00
 * @since 1.00
 */
public class Searcher {

    /**
     * The fields of the typical TREC topics.
     */
    private static final class TOPIC_FIELDS {

        /**
         * The title of a topic.
         */
        public static final String TITLE = "title";

        public static final String EMB = "embeddings";
    }


    /**
     * The identifier of the run
     */
    private final String runID;

    /**
     * The run to be written
     */
    private final PrintWriter run;

    /**
     * The index reader
     */
    private final DirectoryReader reader;

    /**
     * The stored fields of the index.
     */
    private final StoredFields storedFields;
    /**
     * The index searcher.
     */
    private final IndexSearcher searcher;

    /**
     * The topics to be searched
     */
    private final QualityQuery[] topics;

    /**
     * The query parser
     */
    private final QueryParser qp;

    /**
     * The maximum number of documents to retrieve
     */
    private final int maxDocsRetrieved;

    /**
     * The total elapsed time.
     */
    private long elapsedTime = Long.MIN_VALUE;

    /**
     * use embeddings or not
     */
    private final boolean useEmbeddings;

    /**
     * text embeddings for re-ranking
     */
    private ReRanker reRanker =null;

    /**
     * Creates a new searcher.
     *
     * @param analyzer         the {@code Analyzer} to be used.
     * @param similarity       the {@code Similarity} to be used.
     * @param indexPath        the directory where containing the index to be searched.
     * @param topicsFile       the file containing the topics to search for.
     * @param expectedTopics   the total number of topics expected to be searched.
     * @param runID            the identifier of the run to be created.
     * @param runPath          the path where to store the run.
     * @param maxDocsRetrieved the maximum number of documents to be retrieved.
     * @param useEmbeddings    use embeddings or not
     * @param reRankModel      model name for re-ranking
     * @throws NullPointerException     if any of the parameters is {@code null}.
     * @throws IllegalArgumentException if any of the parameters assumes invalid values.
     * @throws IOException for any I/O error
     * @throws ModelNotFoundException if the model for re-ranking has not been found
     * @throws MalformedModelException if the model for re-ranking is not setup correctly.
     */
    public Searcher(final Analyzer analyzer, final Similarity similarity, final String indexPath,
                    final String topicsFile, final int expectedTopics, final String runID, final String runPath,
                    final int maxDocsRetrieved, boolean useEmbeddings, final String reRankModel) throws IOException, ModelNotFoundException, MalformedModelException {

        if (analyzer == null) {
            throw new NullPointerException("Analyzer cannot be null.");
        }

        if (similarity == null) {
            throw new NullPointerException("Similarity cannot be null.");
        }

        if (indexPath == null) {
            throw new NullPointerException("Index path cannot be null.");
        }

        if (indexPath.isEmpty()) {
            throw new IllegalArgumentException("Index path cannot be empty.");
        }

        final Path indexDir = Paths.get(indexPath);
        if (!Files.isReadable(indexDir)) {
            throw new IllegalArgumentException(
                    String.format("Index directory %s cannot be read.", indexDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(indexDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to search the index.",
                    indexDir.toAbsolutePath().toString()));
        }

        try {
            reader = DirectoryReader.open(FSDirectory.open(indexDir));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to create the index reader for directory %s: %s.",
                    indexDir.toAbsolutePath().toString(), e.getMessage()), e);
        }

        try {
            storedFields = reader.storedFields();
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to create the stored fields for directory %s: %s.",
                    indexDir.toAbsolutePath().toString(), e.getMessage()), e);
        }

        this.useEmbeddings = useEmbeddings;

        searcher = new IndexSearcher(reader);

        if (!useEmbeddings) searcher.setSimilarity(similarity);

        if (topicsFile == null) {
            throw new NullPointerException("Topics file cannot be null.");
        }

        if (topicsFile.isEmpty()) {
            throw new IllegalArgumentException("Topics file cannot be empty.");
        }

        try {
            BufferedReader in = Files.newBufferedReader(Paths.get(topicsFile), StandardCharsets.UTF_8);

            topics = new ClefQueryParser().readQueries(in);


            in.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to process topic file %s: %s.", topicsFile, e.getMessage()), e);
        }

        if (expectedTopics <= 0) {
            throw new IllegalArgumentException(
                    "The expected number of topics to be searched cannot be less than or equal to zero.");
        }

        if (topics.length != expectedTopics) {
            System.out.printf("Expected to search for %s topics; %s topics found instead.", expectedTopics,
                    topics.length);
        }

        qp = new QueryParser(ParsedTextDocument.Fields.BODY, analyzer);

        if (runID == null) {
            throw new NullPointerException("Run identifier cannot be null.");
        }

        if (runID.isEmpty()) {
            throw new IllegalArgumentException("Run identifier cannot be empty.");
        }

        this.runID = runID;


        if (runPath == null) {
            throw new NullPointerException("Run path cannot be null.");
        }

        if (runPath.isEmpty()) {
            throw new IllegalArgumentException("Run path cannot be empty.");
        }

        final Path runDir = Paths.get(runPath);
        if (!Files.isWritable(runDir)) {
            throw new IllegalArgumentException(
                    String.format("Run directory %s cannot be written.", runDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(runDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to write the run.",
                    runDir.toAbsolutePath().toString()));
        }

        Path runFile = runDir.resolve(runID + ".txt");
        try {
            run = new PrintWriter(Files.newBufferedWriter(runFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE));
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to open run file %s: %s.", runFile.toAbsolutePath(), e.getMessage()), e);
        }

        if (maxDocsRetrieved <= 0) {
            throw new IllegalArgumentException(
                    "The maximum number of documents to be retrieved cannot be less than or equal to zero.");
        }

        this.maxDocsRetrieved = maxDocsRetrieved;

        if (reRankModel != null) {
            this.reRanker = new ReRanker(storedFields, reRankModel);
        }
    }

    /**
     * Returns the total elapsed time.
     *
     * @return the total elapsed time.
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    /**
     * /** Searches for the specified topics.
     *
     * @throws IOException    if something goes wrong while searching.
     * @throws ParseException if something goes wrong while parsing topics.
     */
    public void search() throws IOException, ParseException {

        System.out.printf("%n#### Start searching ####%n");

        // the start time of the searching
        final long start = System.currentTimeMillis();

        final Set<String> idField = new HashSet<>();
        idField.add(ParsedTextDocument.Fields.ID);

        BooleanQuery.Builder bq = null;
        TermQueryBuilder tq = null;
        Query q = null;
        TopDocs docs = null;
        ScoreDoc[] sd = null;
        String docID = null;

        // the set of document identifiers already retrieved
        try {
            for (QualityQuery t : topics) {
                HashSet<String> docIDs = new HashSet<>();

                System.out.printf("Searching for topic %s.%n", t.getQueryID());

                List<String> queries = null;
                String query = QueryParserBase.escape(t.getValue(TOPIC_FIELDS.TITLE));

                if (useEmbeddings) {
//                    float[] qe = DocEmbeddings.getInstance().generateDocEmbedding(query).toFloatVector();
                    float[] qe = DocEmbeddings.getInstance().getEmbeddingForQuery(query);
                    q = new KnnFloatVectorQuery(ParsedEmbeddedDocument.Fields.EMB_BODY, qe, 1000);
                } else {
                    bq = new BooleanQuery.Builder();

                    queries = getExpansion(t.getQueryID());

                    List<Query> lq = new ArrayList<Query>();

                    for (String qr : queries) {
                        lq.add(qp.parse(qr));
                    }

                    for (Query query1 : lq) {
                        bq.add(query1,  BooleanClause.Occur.SHOULD);
                    }

                    Query mainquery = new BoostQuery(qp.parse(query), 14.68f*lq.size());
                    bq.add(mainquery, BooleanClause.Occur.MUST);

                    q = bq.build();
                    System.out.println("Added " + queries.size() + " queries for " + t.getQueryID());

                }

                docs = searcher.search(q, maxDocsRetrieved);

                if (reRanker == null) {
                    sd = docs.scoreDocs;
                } else {
                    sd = reRanker.sort(query, queries, docs.scoreDocs);
                }

                for (int i = 0, n = sd.length; i < n; i++) {
                    docID = storedFields.document(sd[i].doc, idField).get(ParsedTextDocument.Fields.ID);

                    // if the document has already been retrieved, skip it -> avoid duplicates
                    if (docIDs.contains(docID)) {
                        continue;
                    }

                    // otherwise, add it to the set of retrieved documents
                    docIDs.add(docID);

                    run.printf(Locale.ENGLISH, "%s\tQ0\t%s\t%d\t%.6f\t%s%n", t.getQueryID(), docID, i, sd[i].score, runID);
                }

                run.flush();

            }
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        } finally {
            run.close();

            reader.close();

            reRanker.close();
        }

        elapsedTime = System.currentTimeMillis() - start;

        System.out.printf("%d topic(s) searched in %d seconds.%n", topics.length, elapsedTime / 1000);

        System.out.printf("#### Searching complete ####%n");
    }

    /**
     * Main method of the class. Just for testing purposes.
     *
     * @param args command line arguments.
     * @throws Exception if something goes wrong while indexing.
     */
    public static void main(String[] args) throws Exception {

        final String topics = "C:\\Users\\Mirco\\Desktop\\Search Engines\\publish\\English\\Queries\\train.trec";

        final String indexPath = "experiment/index-stop-stem";

        final String runPath = "experiment";

        final String runID = "seupd2223-helloTipster-stop-nostem";

        final int maxDocsRetrieved = 1000;

        final Analyzer a = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class).addTokenFilter(
                LowerCaseFilterFactory.class).addTokenFilter(StopFilterFactory.class).build();

        Searcher s = new Searcher(a, new BM25Similarity(), indexPath, topics, 50, runID, runPath, maxDocsRetrieved, false, null);

        s.search();


    }


    /**
     * Method to get the expansions for a query given the id of a query.
     * @param queryID the id of the query from which we get expansion
     * @return list of expansions
     * @throws IOException for any I/O error.
     */
    public List<String> getExpansion (String queryID) throws IOException {
        Gson gson = new Gson();
        BufferedReader reader = new BufferedReader(new FileReader("python_scripts/result.json"));
        HashMap<String, ArrayList<String>> hmap = gson.fromJson(reader, HashMap.class);

        ArrayList <String> list = hmap.get(queryID);


        return list.stream().limit(3).collect(Collectors.toList());
    }



}

//TRY OTHER SIMILARITY NOW WITH QUERY EXPANSION

