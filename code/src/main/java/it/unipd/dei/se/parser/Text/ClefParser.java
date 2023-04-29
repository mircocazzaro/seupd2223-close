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
package it.unipd.dei.se.parser.Text;

import com.google.gson.*;
import it.unipd.dei.se.parser.DocumentParser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.RegEx;

import org.nd4j.linalg.cpu.nativecpu.bindings.Nd4jCpu.static_bidirectional_rnn;

/**
 * @author CLOSE GROUP
 * @version 1.0
 * <p>
 * A parser for documents. This parser is used to parse the documents in the CLEF(LongEval Lab).
 */
public class ClefParser extends DocumentParser {

    
    private static  Pattern jspattern = null;
    private static  Pattern dates_Pattern = null;


    private static final GsonBuilder builder = new GsonBuilder();

    /**
     * Creates a new parser.
     */
    public ClefParser() {

        builder.registerTypeAdapter(
                ParsedTextDocument.class, // The type of the object to deserialize.
                (JsonDeserializer<ParsedTextDocument>) (json, typeOfT, context) -> {
                    // Get the id and the body of the document.
                    String id = json.getAsJsonObject().get(ParsedTextDocument.Fields.ID).getAsString();
                    StringBuilder body = new StringBuilder(json.getAsJsonObject().get(ParsedTextDocument.Fields.BODY).getAsString());

                    /* 
                    //JAVASCRIPT PARSING
                    List<String> jspatterns = new ArrayList<String>();
                    jspatterns.add("function(");
                    jspatterns.add("function (");
                    jspatterns.add("{");
                    jspatterns.add("<script");

                    for (String jspattern : jspatterns) {
                        if (body.contains(jspattern)) {
                            String code = null;
                            try {
                                code = body.substring(body.indexOf(jspattern), body.indexOf("/script>"));
                            } catch (StringIndexOutOfBoundsException e) {
                                try {
                                    code = body.substring(body.indexOf(jspattern), body.indexOf("}"));
                                } catch (StringIndexOutOfBoundsException ex) {
                                    code = jspattern;
                                }
                            }
                            body = body.replace(code, "");
                            //System.out.println("Found some JS code");
                        }
                    }
                    */
                    
                    //compiles regular expression for JS and all caps text
                    if(jspattern==null){
                        jspattern=Pattern.compile("function.(.)[{]");
                        dates_Pattern= Pattern.compile("");
                    }
                    
                    int start=0;
                    int end=0;
                    //removes all <scripts>
                    while((start=body.indexOf("<script", start))!=-1){
                        if((end=body.indexOf("script>", start))!=-1){
                            end=end+7;
                            body.replace(start, end, "");
                            continue;
                        }
                        if((end=body.indexOf(">", start))!=-1){
                            end++;
                            body.replace(start, end, "");
                            continue;
                        }
                        start++;
                        
                    }

                    //removes JS
                    Matcher m = jspattern.matcher(body);
                    while(m.find()){
                        start=m.start();
                        int count=1;
                        for(int i=start; i<body.length(); i++){
                            if(body.charAt(i)=='{'){
                                count++;
                                continue;
                            }
                            if((body.charAt(i)=='}')){
                                if(--count==0){
                                    body.replace(start, i, "");
                                    m = jspattern.matcher(body);
                                    break;
                                    
                                }

                            }
                        }
                    }

                    //removes all caps words
                    //m= all_caps_pattern.matcher(body);

                    

                    //COUNTRY LISTS PARSER

                    /*Pattern pattern = Pattern.compile("\\b\\w+ia\\b");
                    Matcher matcher = pattern.matcher(body);
                    body = matcher.replaceAll("");

                    pattern = Pattern.compile("\\b\\w+land\\b");
                    matcher = pattern.matcher(body);
                    body = matcher.replaceAll("");

                    pattern = Pattern.compile("\\b\\w+stan\\b");
                    matcher = pattern.matcher(body);
                    body = matcher.replaceAll("");*/


                    /*// Creare un pattern che corrisponde agli URI HTTP e HTTPS
                    Pattern httpUriPattern = Pattern.compile("(https?://\\S+)");
                    Matcher matcher = httpUriPattern.matcher(body);
                    body = matcher.replaceAll("");*/

                    return new ParsedTextDocument(id, body.toString());
                });

    }

    /**
     * Returns a stream of parsed documents.
     *
     * @param in the reader to read from.
     * @return a stream of parsed documents.
     * @throws IOException if an I/O error occurs.
     */
    public Stream<ParsedTextDocument> getDocumentStream(final Reader in) throws IOException {
        return DocumentParser.readJsonFromFile(builder.create(), ParsedTextDocument.class, in);
    }

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
