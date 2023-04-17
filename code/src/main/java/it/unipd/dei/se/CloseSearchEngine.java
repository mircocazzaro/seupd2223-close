package it.unipd.dei.se;

import java.util.Arrays;
import java.util.Collections;

import it.unipd.dei.se.parser.ClefParser;
import it.unipd.dei.se.searcher.Searcher;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;

import it.unipd.dei.se.indexer.DirectoryIndexer;

public class CloseSearchEngine {
    public static void main(String[] args) throws Exception {
        if (args.length != 3 || Collections.frequency(Arrays.asList(args), null) > 0) {
            throw new IllegalArgumentException(
                    "Usage: java -jar close-1.00-jar-with-dependencies.jar <collection path> <topic path> <index path>"
            );
        }
        final String collectionPath = args[0];
        final String topicPath = args[1];
        final String indexPath = args[2];

        final int ramBuffer = 256;
        final int expectedTopics = 50;
        final int expectedDocs = 528155;
        final int maxDocsRetrieved = 1000;

        final String extension = "txt";
        final String runPath = "experiment";
        final String runID = "seupd2223-close";
        final String charsetName = "ISO-8859-1";

        final Similarity sim = new BM25Similarity();

        // analyzer -> we should implement some analyzer in analyzer package
        final Analyzer closeAnalyzer = CustomAnalyzer.builder().withTokenizer(
                StandardTokenizerFactory.class
        ).addTokenFilter(
                LowerCaseFilterFactory.class
        ).addTokenFilter(
                StopFilterFactory.class
        ).build();

        // indexing
        final DirectoryIndexer directoryIndexer = new DirectoryIndexer(
                closeAnalyzer, sim, ramBuffer, indexPath, collectionPath, extension, charsetName, expectedDocs, ClefParser.class
        );
        directoryIndexer.index();

        // searching
        final Searcher searcher = new Searcher(
                closeAnalyzer, sim, indexPath, topicPath, expectedTopics, runID, runPath, maxDocsRetrieved
        );
        searcher.search();

    }
}
