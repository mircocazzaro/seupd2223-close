package it.unipd.dei.se.analyzer;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.fr.FrenchLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.*;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPLemmatizerFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizerFactory;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.apache.lucene.util.CharsRef;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static it.unipd.dei.se.analyzer.AnalyzerUtil.*;

/**
 * An analyzer to analyze the parsed documents and topics of the CLEF(LongEval Lab) collection
 * and to prepare them for the Indexing and Searching phases.
 * @author CLOSE GROUP
 * @version 1.0
 */
public class CloseAnalyzer extends Analyzer {
    /**
     * The class loader of this class. Needed for reading files from the {@code resource} directory.
     */
    private static final ClassLoader CL = CloseAnalyzer.class.getClassLoader();


    /**
     * The different possible types of tokenizer
     * WhitespaceTokenizer
     * LetterTokenizer
     * StandardTokenizer
     */
    public enum TokenizerType
    {
        /**
         * WhitespaceTokenizer
         */
        Whitespace,
        /**
         * LetterTokenizer
         */
        Letter,
        /**
         * StandardTokenizer
         */
        Standard,


    }

    /**
     * The different possible types of tokenizer:
     */
    public enum StemFilterType
    {
        /**
         * EnglishMinimalStmFilter
         */
        EnglishMinimal,
        /**
         * PorterStemFilter
         */
        Porter,
        /**
         * KStemFilter
         */
        K,
        /**
         * FrenchLightStemFilter
         */
        French,
        /**
         * LovinsStemFilter
         */
        Lovins
    }

    private final TokenizerType tokenizerType;

    private final  StemFilterType stemFilterType;


    //params for length Filter
    private final  Integer minLength;
    private final  Integer maxLength;

    private final  boolean isEnglishPossessiveFilter;

    private final  String stopFilterListName; //include .txt at the end like "smart.txt"

    private final  Integer nGramFilterSize; //set to null if don't want to use

    private final  Integer shingleFilterSize;  //set to null if don't want to use

    private final boolean useNLPFilter;

    private final boolean lemmatization;

    private final boolean frenchElisionFilter;

    /**
     * Creates a new instance of the analyzer.
     * @param frenchElisionFilter boolean to indicate whether to use or not use the French elision filter
     * @param lemmatization boolean to indicate whether to use or not lemmatization
     * @param isEnglishPossessiveFilter boolean to indicate whether to use or not use the English possessive filter
     * @param maxLength maximum length of the token
     * @param minLength minimum length of the token
     * @param nGramFilterSize size of the n-grams (if null, the filter is not applied)
     * @param shingleFilterSize size of the shingles (if null, the filter is not applied)
     * @param stemFilterType type of stemming to use
     * @param stopFilterListName name of the file in the "resources" folder to use as a word stop list
     * @param tokenizerType type of tokenizer to use
     * @param useNLPFilter boolean to indicate whether to use or not use the NLP filter
     */
    public CloseAnalyzer(TokenizerType tokenizerType, int minLength, int maxLength,
                         boolean isEnglishPossessiveFilter, String stopFilterListName, StemFilterType stemFilterType,
                         Integer nGramFilterSize, Integer shingleFilterSize,
                         boolean useNLPFilter, boolean lemmatization, boolean frenchElisionFilter)
    {
        super();

        this.tokenizerType = tokenizerType;


        this.minLength = minLength;
        this.maxLength = maxLength;

        this.isEnglishPossessiveFilter = isEnglishPossessiveFilter;

        this.stopFilterListName = stopFilterListName;

        this.stemFilterType = stemFilterType;

        this.nGramFilterSize = nGramFilterSize;

        this.shingleFilterSize = shingleFilterSize;

        this.useNLPFilter = useNLPFilter;

        this.lemmatization = lemmatization;

        this.frenchElisionFilter = frenchElisionFilter;
    }



    @Override
    protected TokenStreamComponents createComponents(String s) {
        Tokenizer source = null;
        TokenStream tokens = null;



        switch (tokenizerType) {
            case Whitespace:
                source = new WhitespaceTokenizer();
                break;

            case Letter:
                source = new LetterTokenizer();
                break;

            case Standard:
                source = new StandardTokenizer();
                break;
        }

        tokens = new LowerCaseFilter(source);

        //tokens = new ASCIIFoldingFilter(source);

        if (minLength != null && maxLength != null) {
            tokens = new LengthFilter(tokens, minLength, maxLength);
        }

        if (isEnglishPossessiveFilter) {
            tokens = new EnglishPossessiveFilter(tokens);
        }

        if (stopFilterListName != null) {
            tokens = new StopFilter(tokens, loadStopList(stopFilterListName));
        }



      /*  SynonymMap.Builder builder = new SynonymMap.Builder(true);

    // Lettura del file contenente i sinonimi
        Path synonymsFile = Paths.get("python_scripts/synonyms-french.txt");
        List<String> lines = null;
        try {
            lines = Files.readAllLines(synonymsFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Aggiunta dei sinonimi alla SynonymMap.Builder
        for (String line : lines) {
            String[] words = line.split(", ");
            for (int i = 1; i < words.length; i++) {
                builder.add(new CharsRef(words[i]), new CharsRef(words[0].toLowerCase()), true);
            }
        }

    // Creazione della SynonymMap
        SynonymMap synonymMap = null;
        try {
            synonymMap = builder.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/

        //tokens = new SynonymGraphFilter(tokens, synonymMap, true);


        switch (stemFilterType) {
            case EnglishMinimal:
                tokens = new EnglishMinimalStemFilter(tokens);
                break;

            case Porter:
                tokens = new PorterStemFilter(tokens);
                break;

            case K:
                tokens = new KStemFilter(tokens);
                break;

            case French:
                tokens = new FrenchLightStemFilter(tokens);
                break;

            /*case Lovins:
                tokens = new LovinsStemFilter(tokens);
                break;*/

        }

        if (nGramFilterSize != null) {
            tokens = new NGramTokenFilter(tokens, nGramFilterSize);
        }

        if (shingleFilterSize != null) {
            tokens = new ShingleFilter(tokens, shingleFilterSize);
        }

        if (useNLPFilter) {
            tokens = new OpenNLPPOSFilter(source, loadPosTaggerModel("en-pos-maxent.bin"));

            tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-location.bin"));

            tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-person.bin"));

            tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-organization.bin"));

            //tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-money.bin"));

            //tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-date.bin"));

            //tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-time.bin"));

            tokens = new TypeAsSynonymFilter(tokens, "<nlp>");

        }

        if (lemmatization) {

            tokens = new OpenNLPLemmatizerFilter(tokens, loadLemmatizerModel("en-lemmatizer.bin"));
        }

        if(frenchElisionFilter) {
            Character[] elisionsList = {'l', 'd', 's', 't', 'n', 'm'};
            CharArraySet elisionArray = new CharArraySet(elisionsList.length, true);
            elisionArray.addAll(Arrays.asList(elisionsList));
            tokens = new ElisionFilter(tokens, elisionArray);
        }

        return new TokenStreamComponents(source, tokens);

    }

    /**
     * Main method to try the analyzer with the different configurations
     *
     * @param args the arguments to be passed to the method.
     * @throws IOException if an I/O error occurs.
     */
    public static void main(String[] args) throws IOException {
        final String text = "100 - This text; is used $ to see (and test) what our Analyzer does, in order to improve it." +
                "% So, I think it's appropriate to add lot of noise to this";
        CloseAnalyzer closeAnalyzer = new CloseAnalyzer(TokenizerType.Standard, 2, 10, true, null, StemFilterType.EnglishMinimal, null, null, false, false, true);

        consumeTokenStream(closeAnalyzer, text);

    }

}
