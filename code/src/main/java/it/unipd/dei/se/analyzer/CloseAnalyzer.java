package it.unipd.dei.se.analyzer;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import static it.unipd.dei.se.analyzer.AnalyzerUtil.loadStopList;

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



    /**
     * Creates a new instance of the analyzer.
     */
    public CloseAnalyzer(TokenizerType tokenizerType, int minLength, int maxLength,
                          boolean isEnglishPossessiveFilter, String stopFilterListName, StemFilterType stemFilterType,
                          Integer nGramFilterSize, Integer shingleFilterSize)
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

    }



    @Override
    protected TokenStreamComponents createComponents(String s) {
        Tokenizer source = null;
        TokenStream tokens = null;

        switch(tokenizerType)
        {
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

        if(minLength != null && maxLength != null)
        {
            tokens = new LengthFilter(tokens, minLength, maxLength);
        }

        if(isEnglishPossessiveFilter)
        {
            tokens = new EnglishPossessiveFilter(tokens);
        }

        if(stopFilterListName != null)
        {
            tokens = new StopFilter(tokens, loadStopList(stopFilterListName));
        }

        switch(stemFilterType)
        {
            case EnglishMinimal:
                tokens = new EnglishMinimalStemFilter(tokens);
                break;

            case Porter:
                tokens = new PorterStemFilter(tokens);
                break;

            case K:
                tokens = new KStemFilter(tokens);
                break;

            /*case Lovins:
                tokens = new LovinsStemFilter(tokens);
                break;*/
        }

        if(nGramFilterSize != null)
        {
            tokens = new NGramTokenFilter(tokens, nGramFilterSize);
        }

        if(shingleFilterSize != null)
        {
            tokens = new ShingleFilter(tokens, shingleFilterSize);
        }

        return new TokenStreamComponents(source, tokens);
    }


}
