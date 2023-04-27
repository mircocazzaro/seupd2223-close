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
package it.unipd.dei.se;

import it.unipd.dei.se.parser.Embedded.ClefEmbeddedParser;
import it.unipd.dei.se.searcher.Searcher;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;

import it.unipd.dei.se.indexer.DirectoryIndexer;

/**
 * The main class of the search engine.
 *
 * @author Close Group
 * @version 1.00
 * @since 1.00
 */
public class CloseSearchEngine {
    public static void main(String[] args) throws Exception {
        /*
         * Check the command line arguments.
         * The first argument is the path to the collection.
         * The second argument is the path to the topics.
         * The third argument is the path to the index.
         */
        /*if (args.length != 3 || Collections.frequency(Arrays.asList(args), null) > 0) {
            throw new IllegalArgumentException(
                    "Usage: java -jar close-1.00-jar-with-dependencies.jar <collection path> <topic path> <index path>"
            );
        }*/
        final String collectionPath = "/Users/farzad/Projects/uni/search_engine/fun/output/all-MiniLM-L6-v2";;
        final String topicPath = "test/train.trec";
        final String indexPath ="experiment/all-MiniLM-L6-v2";

        // ram buffer size
        final int ramBuffer = 256;

        // number of topics and documents in the collection
        final int expectedTopics = 50;

        // number of documents to be retrieved
        final int expectedDocs = 528155;

        // maximum number of documents to be retrieved
        final int maxDocsRetrieved = 1000;

        // extension of the files to be indexed
        final String extension = "json";

        // run path and run id
        final String runPath = "experiment";
        final String runID = "seupd2223-close";

        // charset name of the files to be indexed
        final String charsetName = "ISO-8859-1";

        // creating the similarity to be used for ranking the documents
        final Similarity sim = new BM25Similarity();

        // creating the analyzer to be used for indexing and searching the collection
        final Analyzer closeAnalyzer = CustomAnalyzer.builder().withTokenizer(
                StandardTokenizerFactory.class
        ).addTokenFilter(
                LowerCaseFilterFactory.class
        ).addTokenFilter(
                StopFilterFactory.class
        ).addTokenFilter(StemmerOverrideFilterFactory.class).build();

        // indexing the collection of documents in the specified path and with the specified extension
        final DirectoryIndexer directoryIndexer = new DirectoryIndexer(
                closeAnalyzer,
                sim,
                ramBuffer,
                indexPath,
                collectionPath,
                extension,
                charsetName,
                expectedDocs,
                ClefEmbeddedParser.class
        );
        //directoryIndexer.index();

        // searching the topics in the specified path and with the specified extension
        final Searcher searcher = new Searcher(
                closeAnalyzer,
                sim,
                indexPath,
                topicPath,
                expectedTopics,
                runID,
                runPath,
                maxDocsRetrieved,
                true
        );
        searcher.search();

    }
}
