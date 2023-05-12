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


/**
 * A parser for documents. This parser is used to parse the documents in the CLEF(LongEval Lab).
 * @author CLOSE GROUP
 * @version 1.0
 */
public class ClefParser extends DocumentParser {

    
    private static  Pattern jspattern = null;
    private static  Pattern dates_Pattern = null;
    private static Pattern noise_pattern=null;


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
                    StringBuilder bodyBuilder = new StringBuilder(json.getAsJsonObject().get(ParsedTextDocument.Fields.BODY).getAsString());

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
                    while((start=bodyBuilder.indexOf("<script", start))!=-1){
                        if((end=bodyBuilder.indexOf("script>", start))!=-1){
                            end=end+7;
                            bodyBuilder.replace(start, end, "");
                            continue;
                        }
                        if((end=bodyBuilder.indexOf(">", start))!=-1){
                            end++;
                            bodyBuilder.replace(start, end, "");
                            continue;
                        }
                        start++;
                        
                    }

                    //removes JS
                    Matcher m = jspattern.matcher(bodyBuilder);
                    while(m.find()){
                        start=m.start();
                        int count=1;
                        for(int i=start; i<bodyBuilder.length(); i++){
                            if(bodyBuilder.charAt(i)=='{'){
                                count++;
                                continue;
                            }
                            if((bodyBuilder.charAt(i)=='}')){
                                if(--count==0){
                                    bodyBuilder.replace(start, i, "");
                                    m = jspattern.matcher(bodyBuilder);
                                    break;
                                    
                                }

                            }
                        }
                    }

                    //removes all caps words
                    //m= all_caps_pattern.matcher(body);

                    

                    //String body = bodyBuilder.toString();

                    
                    // HTTP/HTTPS URI PARSER
                    String uriRegex = "(https?://[\\w-]+(\\.[\\w-]+)+([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?)";
                    Pattern uriPattern = Pattern.compile(uriRegex);
                    Matcher uriMatcher = uriPattern.matcher(bodyBuilder);

                    /* 
                    while (uriMatcher.find()) {
                        String uri = uriMatcher.group();
                        body = body.replace(uri, "");
                        //System.out.println("Found a URI");
                    }
                    */

                    // NOISES PARSER
                    //body = removeNoise(body);
                    //return new ParsedTextDocument(id, uriMatcher.replaceAll(""));
                    return new ParsedTextDocument(id, removePatterns(bodyBuilder.toString()));
                });

    }

    /**
     * Clean noises in the document passed as parameter.
     * The noises this function removes are the following:
     *      - HTML tags and CSS stylesheets
     *      - XML or JSON code
     *      - Meta tags and document properties
     *      - Navigation menus
     *      - Advertisements
     *      - Footers
     *      - Noise characters
     *      - Non-ASCII characters
     *      - Social media handles
     *      - Hashtags and mentions
     *
     * @param text the text from which the noise will be removed.
     * @return the processed text.
     */
    public static String removeNoise(String text) {


        // Remove HTML tags and CSS stylesheets.
        text = text.replaceAll("<style[^>]*>[^<]*</style>|<[^>]*>", "");

        // Remove XML or JSON code.
        text = text.replaceAll("<\\?xml[^>]*>|<script[^>]*>[^<]*</script>|\\{[^\\}]*\\}", "");

        // Remove meta tags and document properties.
        text = text.replaceAll("<meta[^>]*>|<title>[^<]*</title>|<head[^>]*>|<body[^>]*>|<html[^>]*>|</head>|</body>|</html>", "");

        // Remove navigation menus.
        text = text.replaceAll("(?i)menu|nav|navigation", "");

        // Remove advertisements.
        text = text.replaceAll("(?i)advertisements?|pub|annonce", "");

        // Remove footers.
        text = text.replaceAll("(?i)footer|pied de page|mentions légales", "");

        // Remove noise characters.
        text = text.replaceAll("[^\\p{L}\\p{N}\\s]+", "");

        // Remove non-ASCII characters.
        text = text.replaceAll("[^\\p{ASCII}]", "");

        // Remove social media handles, hashtags, and mentions.
        text = text.replaceAll("(?i)@[\\w]+|#\\w+|\\bRT\\b", "");

        return text;
        
    }

    /**
     * Function to remove patterns like "word1_word2", "word1.word2",
     * and HTTP/HTTPS URIs from a string.
     * @param input the text from which the noise will be removed.
     * @return the processed text.
     */
    public String removePatterns(String input) {

        // Define regular expression pattern to match the desired patterns
        Pattern pattern = Pattern.compile("(\\w+)[_.:](\\w+)|(https?://[\\w-]+(\\.[\\w-]+)+([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?)");

        // Replace all matched patterns with a space character
        Matcher matcher = pattern.matcher(input);
        String output = matcher.replaceAll(" ");

        return output;
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

    /**
     * Main method to try the parser.
     *
     * @param args the arguments to be passed to the method.
     * @throws IOException if an I/O error occurs.
     */
    public static void main(String[] args) throws Exception {
        // Read the documents from a file.
        Reader reader = new FileReader(
                "C:/Users/39392/OneDrive/Desktop/Search Engines/collections/longeval-train-v2/French/Documents/Json/collector_kodicare_1.txt.json"
        );

        // Create a new parser.
        Stream<ParsedTextDocument> parsedDocumentStream = new ClefParser().getDocumentStream(reader);

        // Print the documents to the terminal and a text file.
        PrintWriter writer = new PrintWriter("C:/Users/39392/OneDrive/Desktop/Search Engines/collections/longeval-train-v2/French/Outputs/documents.txt", "UTF-8");
        parsedDocumentStream.forEach(document -> {
            System.out.println(document);
            writer.println(document);
        });
        writer.close();
    }


}
