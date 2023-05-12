/*
 *  Copyright 2017-2023 University of Padua, Italy
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
package it.unipd.dei.se.indexer;

import com.google.gson.stream.JsonWriter;
import it.unipd.dei.se.analyzer.DocEmbeddings;
import it.unipd.dei.se.parser.*;
import it.unipd.dei.se.parser.Embedded.ClefEmbeddedParser;
import it.unipd.dei.se.parser.Embedded.ParsedEmbeddedDocument;
import it.unipd.dei.se.parser.Text.ClefParser;
import it.unipd.dei.se.parser.Text.ParsedTextDocument;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.math3.linear.*;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Stream;

/**
 * Indexes documents processing a whole directory tree.
 *
 * @author Close Group
 * @version 1.0
 */
public class DirectoryIndexer {

    /**
     * One megabyte
     */
    private static final int MBYTE = 1024 * 1024;

    /**
     * The index writer.
     */
    private final IndexWriter writer;

    /**
     * The class of the {@code DocumentParser} to be used.
     */
    private final Class<? extends DocumentParser> dpCls;

    /**
     * The directory (and subdirectories) where documents are stored.
     */
    private final Path docsDir;

    /**
     * The extension of the files to be indexed.
     */
    private final String extension;

    /**
     * The charset used for encoding documents.
     */
    private final Charset cs;

    /**
     * The total number of documents expected to be indexed.
     */
    private final long expectedDocs;

    /**
     * The start instant of the indexing.
     */
    private final long start;

    /**
     * The total number of indexed files.
     */
    private long filesCount;

    /**
     * The total number of indexed documents.
     */
    public long docsCount;

    /**
     * The total number of indexed bytes
     */
    private long bytesCount;

    private boolean useEmbeddings = false;

    /**
     * Creates a new indexer.
     *
     * @param analyzer        the {@code Analyzer} to be used.
     * @param similarity      the {@code Similarity} to be used.
     * @param ramBufferSizeMB the size in megabytes of the RAM buffer for indexing documents.
     * @param indexPath       the directory where to store the index.
     * @param docsPath        the directory from which documents have to be read.
     * @param extension       the extension of the files to be indexed.
     * @param charsetName     the name of the charset used for encoding documents.
     * @param expectedDocs    the total number of documents expected to be indexed
     * @param dpCls           the class of the {@code DocumentParser} to be used.
     * @throws NullPointerException     if any of the parameters is {@code null}.
     * @throws IllegalArgumentException if any of the parameters assumes invalid values.
     */
    public DirectoryIndexer(final Analyzer analyzer, final Similarity similarity, final int ramBufferSizeMB,
                            final String indexPath, final String docsPath, final String extension,
                            final String charsetName, final long expectedDocs,
                            final Class<? extends DocumentParser> dpCls) {

        if (dpCls == null) {
            throw new NullPointerException("Document parser class cannot be null.");
        }

        this.dpCls = dpCls;

        if (analyzer == null) {
            throw new NullPointerException("Analyzer cannot be null.");
        }

        if (similarity == null) {
            throw new NullPointerException("Similarity cannot be null.");
        }

        if (ramBufferSizeMB <= 0) {
            throw new IllegalArgumentException("RAM buffer size cannot be less than or equal to zero.");
        }

        // if the class of document parser is instance of ClefEmbeddedParser, then use the DocEmbeddingsAnalyzer
        if (dpCls.equals(ClefEmbeddedParser.class)){
            this.useEmbeddings = true;
        }

        final IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        if (!this.useEmbeddings) iwc.setSimilarity(similarity);
        iwc.setRAMBufferSizeMB(ramBufferSizeMB);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setCommitOnClose(true);
        iwc.setUseCompoundFile(true);

        if (indexPath == null) {
            throw new NullPointerException("Index path cannot be null.");
        }

        if (indexPath.isEmpty()) {
            throw new IllegalArgumentException("Index path cannot be empty.");
        }

        final Path indexDir = Paths.get(indexPath);

        // if the directory does not already exist, create it
        if (Files.notExists(indexDir)) {
            try {
                Files.createDirectories(indexDir);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("Unable to create directory %s: %s.", indexDir.toAbsolutePath(), e.getMessage()), e);
            }
        }

        if (!Files.isWritable(indexDir)) {
            throw new IllegalArgumentException(
                    String.format("Index directory %s cannot be written.", indexDir.toAbsolutePath()));
        }

        if (!Files.isDirectory(indexDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to write the index.",
                    indexDir.toAbsolutePath()));
        }

        if (docsPath == null) {
            throw new NullPointerException("Documents path cannot be null.");
        }

        if (docsPath.isEmpty()) {
            throw new IllegalArgumentException("Documents path cannot be empty.");
        }

        final Path docsDir = Paths.get(docsPath);
        if (!Files.isReadable(docsDir)) {
            throw new IllegalArgumentException(
                    String.format("Documents directory %s cannot be read.", docsDir.toAbsolutePath()));
        }

        if (!Files.isDirectory(docsDir)) {
            throw new IllegalArgumentException(
                    String.format("%s expected to be a directory of documents.", docsDir.toAbsolutePath()));
        }

        this.docsDir = docsDir;

        if (extension == null) {
            throw new NullPointerException("File extension cannot be null.");
        }

        if (extension.isEmpty()) {
            throw new IllegalArgumentException("File extension cannot be empty.");
        }
        this.extension = extension;

        if (charsetName == null) {
            throw new NullPointerException("Charset name cannot be null.");
        }

        if (charsetName.isEmpty()) {
            throw new IllegalArgumentException("Charset name cannot be empty.");
        }

        try {
            cs = Charset.forName(charsetName);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Unable to create the charset %s: %s.", charsetName, e.getMessage()), e);
        }

        if (expectedDocs <= 0) {
            throw new IllegalArgumentException(
                    "The expected number of documents to be indexed cannot be less than or equal to zero.");
        }
        this.expectedDocs = expectedDocs;

        this.docsCount = 0;

        this.bytesCount = 0;

        this.filesCount = 0;

        try {
            writer = new IndexWriter(FSDirectory.open(indexDir), iwc);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to create the index writer in directory %s: %s.",
                    indexDir.toAbsolutePath(), e.getMessage()), e);
        }

        this.start = System.currentTimeMillis();

    }

    /**
     * Method to create embedded documents
     *
     * @throws IOException if an I/O error occurs.
     */
    public void docEmbedding() throws IOException {
        System.out.printf("%n#### Start Creating Embedded Data ####%n");
        Files.walkFileTree(docsDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                Stream<ParsedTextDocument> parsedDocumentStream = DocumentParser.create(dpCls, Files.newBufferedReader(file, cs));

                if (file.getFileName().toString().endsWith(extension)) {
                    // check if the file exists in directory
                    if (Files.exists(Paths.get("data/" + file.getFileName().toString()))) {
                        System.out.printf("%s file already exists in data directory.%n", file.getFileName().toString());
                        return FileVisitResult.CONTINUE;
                    }
                    filesCount += 1;
                    bytesCount = Files.size(file);
                    try (JsonWriter writer = new JsonWriter(new FileWriter("data/" + file.getFileName().toString()))) {
                        writer.beginArray();
                        parsedDocumentStream.forEach(pd -> {
                            try {
                                writer.beginObject();
                                writer.name(ParsedTextDocument.Fields.ID).value(pd.getIdentifier());
                                writer.name(ParsedTextDocument.Fields.BODY);
                                writer.beginArray();
                                for (float s : DocEmbeddings.getInstance().generateDocEmbedding(pd.getBody()).toFloatVector()) {
                                    writer.value(s);
                                }
                                writer.endArray();
                                writer.endObject();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        writer.endArray();

                        System.out.printf("%s file (%d Mbytes) generated in %d seconds.%n", file.getFileName().toString(), bytesCount / MBYTE, (System.currentTimeMillis() - start) / 1000);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.printf("#### Pre Process For Embedding Data Finish ####%n");
    }


    /**
     * Indexes the documents.
     *
     * @throws IOException if something goes wrong while indexing.
     */
    public void index() throws IOException {
        System.out.printf("%n#### Start indexing ####%n");

        Files.walkFileTree(docsDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(extension)) {
                    filesCount += 1;
                    bytesCount += Files.size(file);

                    // Create a stream of parsed documents from the file with the given Parser class(dpCls)
                    DocumentParser.create(dpCls, Files.newBufferedReader(file, cs)).forEach(pd -> {
                        Document doc = new Document();

                        if (useEmbeddings) {
                            // if the document is an embedded document cast it to ParsedEmbeddedDocument
                            ParsedEmbeddedDocument ped = (ParsedEmbeddedDocument) pd;

                            // add the document identifier
                            doc.add(new StringField(ParsedEmbeddedDocument.Fields.ID, ped.getIdentifier(), Field.Store.YES));

                            // add the document embedding
                            doc.add(new KnnFloatVectorField(ParsedEmbeddedDocument.Fields.EMB_BODY, ped.getBody(), VectorSimilarityFunction.EUCLIDEAN));

                        } else {
                            // if the document is a text document cast it to ParsedTextDocument
                            ParsedTextDocument ptd = (ParsedTextDocument) pd;

                            // add the document identifier
                            doc.add(new StringField(ParsedTextDocument.Fields.ID, ptd.getIdentifier(), Field.Store.YES));

                            // add the document embedding
                            doc.add(new BodyField(ptd.getBody()));
                        }


                        try {
                            writer.addDocument(doc);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        docsCount++;

                        // print progress every 10000 indexed documents
                        if (docsCount % 10000 == 0) {
                            System.out.printf("%d document(s) (%d files, %d Mbytes) indexed in %d seconds.%n",
                                    docsCount, filesCount, bytesCount / MBYTE,
                                    (System.currentTimeMillis() - start) / 1000);
                        }
                    });

                }
                return FileVisitResult.CONTINUE;
            }
        });

        writer.commit();

        writer.close();

        if (docsCount != expectedDocs) {
            System.out.printf("Expected to index %d documents; %d indexed instead.%n", expectedDocs, docsCount);
        }

        System.out.printf("%d document(s) (%d files, %d Mbytes) indexed in %d seconds.%n", docsCount, filesCount,
                bytesCount / MBYTE, (System.currentTimeMillis() - start) / 1000);

        System.out.printf("#### Indexing complete ####%n");
    }

    /**
     * Main method of the class. Just for testing purposes.
     *
     * @param args command line arguments.
     * @throws Exception if something goes wrong while indexing.
     */
    public static void main(String[] args) throws Exception {

        final int ramBuffer = 256;
        final String docsPath = "C:\\Users\\Mirco\\Desktop\\Search Engines\\publish\\English\\Documents\\Json";
        final String indexPath = "experiment/index-stop-stem";

        final String extension = "json";
        final int expectedDocs = 528155;
        final String charsetName = "ISO-8859-1";

        final Analyzer a = CustomAnalyzer.builder().withTokenizer(
                StandardTokenizerFactory.class
        ).addTokenFilter(
                LowerCaseFilterFactory.class
        ).addTokenFilter(
                StopFilterFactory.class
        ).addTokenFilter(
                PorterStemFilterFactory.class
        ).build();

        DirectoryIndexer i = new DirectoryIndexer(
                a,
                new BM25Similarity(),
                ramBuffer,
                indexPath,
                docsPath,
                extension,
                charsetName,
                expectedDocs,
                ClefParser.class
        );

        i.docEmbedding();

        //i.index();
    }

}
