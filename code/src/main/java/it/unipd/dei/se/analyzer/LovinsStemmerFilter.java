package it.unipd.dei.se.analyzer;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.tartarus.snowball.ext.LovinsStemmer;

import java.io.IOException;

/**
 * A custom stemming filter using LovinsStemmer
 * @author CLOSE GROUP
 * @version 1.0
 */
public class LovinsStemmerFilter extends TokenFilter {

    private LovinsStemmer stemmer;
    private CharTermAttribute termAtt;

    /**
     * Method to create the LovinsStemmerFilter.
     * @param input the tokens to be stemmed.
     */
    public LovinsStemmerFilter(TokenStream input) {
        super(input);
        stemmer = new LovinsStemmer();
        termAtt = addAttribute(CharTermAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            char[] term = termAtt.buffer();
            int len = termAtt.length();
            stemmer.setCurrent(term, len);
            if (stemmer.stem()) {
                char[] stem = stemmer.getCurrentBuffer();
                int stemLen = stemmer.getCurrentBufferLength();
                termAtt.copyBuffer(stem, 0, stemLen);
            }
            return true;
        } else {
            return false;
        }
    }
}