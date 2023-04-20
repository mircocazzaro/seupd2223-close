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
package it.unipd.dei.se.parser;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.io.*;
import java.util.stream.Stream;

/**
 * @author CLOSE GROUP
 * @version 1.0
 * <p>
 * A parser for documents. This parser is used to parse the documents in the CLEF(LongEval Lab).
 */
public class ClefParser extends DocumentParser {

    private static final GsonBuilder builder = new GsonBuilder();

    /**
     * Creates a new parser.
     */
    public ClefParser() {
        // Register a custom deserializer for ParsedDocument.
        builder.registerTypeAdapter(
                ParsedDocument.class, // The type of the object to deserialize.
                (JsonDeserializer<ParsedDocument>) (json, typeOfT, context) -> {
                    // Get the id and the body of the document.
                    String id = json.getAsJsonObject().get(ParsedDocument.Fields.ID).getAsString();
                    String body = json.getAsJsonObject().get(ParsedDocument.Fields.BODY).getAsString();

                    // Return a new ParsedDocument.
                    return new ParsedDocument(id, body);
                });
    }

    /**
     * Returns a stream of parsed documents.
     *
     * @param in the reader to read from.
     * @return a stream of parsed documents.
     * @throws IOException if an I/O error occurs.
     */
    public Stream<ParsedDocument> getDocumentStream(final Reader in) throws IOException {
        return DocumentParser.readJsonFromFile(builder.create(), ParsedDocument.class, in);
    }

    public static void main(String[] args) throws Exception {
        // Read the documents from a file.
        Reader reader = new FileReader(
                "/Users/farzad/Projects/uni/search_engine/seupd2223-close/code/src/main/java/it/unipd/dei/se/parser/collector_kodicare_1.txt.json"
        );

        // Create a new parser.
        Stream<ParsedDocument> parsedDocumentStream = new ClefParser().getDocumentStream(reader);

        // Print the documents.
        parsedDocumentStream.forEach(System.out::println);
    }

}
