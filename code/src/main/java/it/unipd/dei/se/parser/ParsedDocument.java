package it.unipd.dei.se.parser;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author CLOSE GROUP
 * @version 1.0
 * <p>
 * A parsed document.
 */

public record ParsedDocument(String id, String body) {

    /**
     * The fields of a parsed document.
     * ID: the document identifier.
     * BODY: the document body.
     */
    public final static class Fields {
        public static final String ID = "id";
        public static final String BODY = "contents";
    }

    /**
     * Creates a new parsed document.
     *
     * @param id  the document identifier.
     * @param body the document body.
     */
    public ParsedDocument(final String id, final String body) {
        /*
         * Check the document identifier.
         * It cannot be null or empty.
         */
        if (id == null) {
            throw new NullPointerException("Document identifier cannot be null.");
        }

        if (id.isEmpty()) {
            throw new IllegalStateException("Document identifier cannot be empty.");
        }

        this.id = id;

        /*
         * Check the document body.
         * It cannot be null or empty.
         */
        if (body == null) {
            throw new NullPointerException("Document body cannot be null.");
        }

        if (body.isEmpty()) {
            throw new IllegalStateException("Document body cannot be empty.");
        }

        this.body = body;
    }

    /**
     * Override the toString method. It returns a string representation of the document.
     *
     * @return the string representation of the document.
     */
    @Override
    public final String toString() {
        ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("identifier", id)
                .append("body", body);

        return tsb.toString();
    }

    /**
     * Override the equals' method. It returns true if the two documents have the same identifier.
     *
     * @param o the object to compare.
     * @return true if the two documents have the same identifier.
     */
    @Override
    public final boolean equals(Object o) {
        return (this == o) || ((o instanceof ParsedDocument) && id.equals(((ParsedDocument) o).id));
    }

    /**
     * Override the hashCode method. It returns the hash code of the document identifier.
     *
     * @return the hash code of the document identifier.
     */
    @Override
    public final int hashCode() {
        return 37 * id.hashCode();
    }

}
