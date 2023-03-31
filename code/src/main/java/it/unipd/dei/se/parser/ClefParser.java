package it.unipd.dei.se.parser;

import java.io.BufferedReader;
import java.io.Reader;


public class ClefParser extends it.unipd.dei.se.parser.DocumentParser {

    /**
     * The size of the buffer for the body element.
     */
    private static final int BODY_SIZE = 1024 * 8;

    /**
     * The currently parsed document
     */
    private final it.unipd.dei.se.parser.ParsedDocument document = null;

    public ClefParser(final Reader in) {
        super(new BufferedReader(in));
    }


    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    protected final it.unipd.dei.se.parser.ParsedDocument parse() {
        return document;
    }

}
