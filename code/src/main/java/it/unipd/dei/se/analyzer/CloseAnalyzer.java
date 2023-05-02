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
import org.apache.lucene.analysis.fr.FrenchMinimalStemFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.miscellaneous.TypeAsSynonymFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPLemmatizerFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizerFactory;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static it.unipd.dei.se.analyzer.AnalyzerUtil.*;

public class CloseAnalyzer extends Analyzer {
    /**
     * The class loader of this class. Needed for reading files from the {@code resource} directory.
     */
    private static final ClassLoader CL = CloseAnalyzer.class.getClassLoader();


    public enum TokenizerType
    {
        Whitespace,
        Letter,
        Standard,


    }

    public enum StemFilterType
    {
        EnglishMinimal,
        Porter,
        K,
        French,
        FrenchMinimal
        //Lovins
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
     */
    public CloseAnalyzer(TokenizerType tokenizerType, int minLength, int maxLength,
                         boolean isEnglishPossessiveFilter, String stopFilterListName, StemFilterType stemFilterType,
                         Integer nGramFilterSize, Integer shingleFilterSize, boolean useNLPFilter, boolean lemmatization, boolean frenchElisionFilter)
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

        if (minLength != null && maxLength != null) {
            tokens = new LengthFilter(tokens, minLength, maxLength);
        }

        if (isEnglishPossessiveFilter) {
            tokens = new EnglishPossessiveFilter(tokens);
        }

        if (stopFilterListName != null) {
            tokens = new StopFilter(tokens, loadStopList(stopFilterListName));
        }

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

            case FrenchMinimal:
                tokens = new FrenchMinimalStemFilter(tokens);
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

            //tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-location.bin"));

            //tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-person.bin"));

            //tokens = new OpenNLPNERFilter(tokens, loadLNerTaggerModel("en-ner-organization.bin"));

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

    public static void main(String[] args) throws IOException {
        final String text = "100 - This text; is used $ to see (and test) what our Analyzer does, in order to improve it." +
                "% So, I think it's appropriate to add lot of noise to this";
        CloseAnalyzer closeAnalyzer = new CloseAnalyzer(TokenizerType.Standard, 2, 10, true, null, StemFilterType.EnglishMinimal, null, null, false, false);

        consumeTokenStream(closeAnalyzer, text);

    }

}
