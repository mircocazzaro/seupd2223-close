package it.unipd.dei.se.indexer;

import it.unipd.dei.se.parser.DocumentParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;

public class DirectoryIndexer {
    public DirectoryIndexer(
            final Analyzer analyzer, final Similarity similarity, final int ramBufferSizeMB,
            final String indexPath, final String docsPath, final String extension,
            final String charsetName, final long expectedDocs,
            final Class<? extends DocumentParser> dpCls
    ) {
        // TODO: implement Indexer
    }

    public void index() throws IOException {

    }
}
