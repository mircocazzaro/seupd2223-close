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
package it.unipd.dei.se.searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;

/**
 * Represents a searcher.
 *
 * @author Close Group
 * @version 1.00
 */
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