package it.unipd.dei.se.searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;

public class Searcher {
    public Searcher(
            final Analyzer analyzer, final Similarity similarity, final String indexPath,
            final String topicsFile, final int expectedTopics, final String runID, final String runPath,
            final int maxDocsRetrieved
    ) {

    }

    public void search() throws IOException, ParseException {

    }
}