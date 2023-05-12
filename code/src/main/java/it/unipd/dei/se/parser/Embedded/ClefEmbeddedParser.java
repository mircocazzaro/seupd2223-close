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
package it.unipd.dei.se.parser.Embedded;

import com.google.gson.*;
import it.unipd.dei.se.parser.Text.ClefParser;
import it.unipd.dei.se.parser.DocumentParser;
import it.unipd.dei.se.parser.Text.ParsedTextDocument;
import it.unipd.dei.se.utils.JArrayConvertor;

import java.io.*;
import java.util.stream.Stream;

/**
 * A parser for documents. This parser is used to parse the documents in the CLEF(LongEval Lab).
 * @author CLOSE GROUP
 * @version 1.0
 */
public class ClefEmbeddedParser extends DocumentParser {

    private static final GsonBuilder builder = new GsonBuilder();

    /**
     * Creates a new parser.
     */
    public ClefEmbeddedParser() {
        builder.registerTypeAdapter(
                ParsedEmbeddedDocument.class, // The type of the object to deserialize.
                (JsonDeserializer<ParsedEmbeddedDocument>) (json, typeOfT, context) -> {
                    // Get the id and the body of the document.
                    String id = json.getAsJsonObject().get(ParsedEmbeddedDocument.Fields.ID).getAsString();
                    JsonArray body = json.getAsJsonObject().get(ParsedEmbeddedDocument.Fields.BODY).getAsJsonArray();
                    return new ParsedEmbeddedDocument(id, JArrayConvertor.toFloatArray(body));
                });
    }

    /**
     * Returns a stream of parsed documents.
     *
     * @param in the reader to read from.
     * @return a stream of parsed documents.
     * @throws IOException if an I/O error occurs.
     */
    public Stream<ParsedEmbeddedDocument> getDocumentStream(final Reader in) throws IOException {
        return DocumentParser.readJsonFromFile(builder.create(), ParsedEmbeddedDocument.class, in);
    }

    /**
     * Main method to try the parser.
     *
     * @param args arguments for the method.
     * @throws IOException if an I/O error occurs.
     */
    public static void main(String[] args) throws Exception {
        // Read the documents from a file.
        Reader reader = new FileReader(
                "data/collector_kodicare_32.txt.json"
        );

        // Create a new parser.
        Stream<ParsedTextDocument> parsedDocumentStream = new ClefParser().getDocumentStream(reader);

        // Print the documents.
        parsedDocumentStream.forEach(System.out::println);
    }

}
