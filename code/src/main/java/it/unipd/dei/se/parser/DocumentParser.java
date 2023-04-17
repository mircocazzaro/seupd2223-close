package it.unipd.dei.se.parser;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.Iterator;
import java.lang.reflect.Type;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.Spliterators;

/**
 * @author CLOSE GROUP
 * @version 1.0
 * <p>
 * A parser for documents.
 */

public abstract class DocumentParser {

    /**
     * Create a Json Iterator from a JsonReader.
     *
     * @param gson       the Gson object to use for deserialization.
     * @param objectType the type of the objects to deserialize.
     * @param reader     the reader to read from.
     */
    private record JsonIterator<T>(Gson gson, Type objectType, JsonReader reader) implements Iterator<T> {

        /**
         * Returns true if the iteration has more elements.
         *
         * @return true if the iteration has more elements
         */
        @Override
        public boolean hasNext() {
            try {
                return reader.hasNext();
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         */
        @Override
        public T next() {
            return gson.fromJson(reader, objectType);
        }
    }


    /**
     * Reads a JSON file and returns a stream of objects.
     *
     * @param gson       the Gson object to use for deserialization.
     * @param objectType the type of the objects to deserialize.
     * @param in         the reader to read from.
     * @param <J>        the type of the objects to deserialize.
     * @return a stream of objects.
     * @throws IOException if an error occurs while reading the file.
     */
    public static <J> Stream<J> readJsonFromFile(Gson gson, Type objectType, final Reader in) throws IOException {
        // Create a JsonReader from the file
        JsonReader reader = new JsonReader(new BufferedReader(in));

        reader.beginArray();

        // If the file is empty, return an empty stream
        if (!reader.hasNext()) {
            return Stream.empty();
        }

        // Otherwise, return a stream of objects
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new JsonIterator<J>(gson, objectType, reader), // The JsonIterator
                        0 // The spliterator is not ordered
                ),
                false // The stream is not parallel
        ).onClose(() -> {
            // Close the reader when the stream is closed
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Creates a new {@code DocumentParser}.
     * <p>
     * It assumes the {@code DocumentParser} has a single-parameter constructor which takes a {@code Reader} as input.
     *
     * @param cls the class of the document parser to be instantiated.
     * @param in  the reader to the document(s) to be parsed.
     * @return a new instance of {@code DocumentParser} for the given class.
     * @throws NullPointerException  if {@code cls} and/or {@code in} are {@code null}.
     * @throws IllegalStateException if something goes wrong in instantiating the class.
     */
    public static Stream<ParsedDocument> create(Class<? extends DocumentParser> cls, Reader in) {

        if (cls == null) {
            throw new NullPointerException("Document parser class cannot be null.");
        }

        if (in == null) {
            throw new NullPointerException("Reader cannot be null.");
        }

        try {
            return cls.getConstructor().newInstance().getDocumentStream(in);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Unable to instantiate document parser %s.", cls.getName()), e);
        }

    }

    /**
     * Returns a stream of parsed documents.
     *
     * @param in the reader to the document(s) to be parsed.
     * @return a stream of parsed documents.
     * @throws IOException if something goes wrong in parsing the document(s).
     */
    protected abstract Stream<ParsedDocument> getDocumentStream(final Reader in) throws IOException;
}
