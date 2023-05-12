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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A parsed document.
 * @author CLOSE GROUP
 * @version 1.0
 * @param id the document identifier
 * @param body the document body
 */

public record ParsedEmbeddedDocument(String id, float[] body) {



    /**
     * The fields of a parsed document.
    */
    public final static class Fields {
        /** Default constructor for the class
         */
        public Fields(){
            super();
        }

        /** ID: the document identifier.
         */
        public static final String ID = "id";
        /** BODY: the document body.
         */
        public static final String BODY = "contents";
        /** EMB_BODY: the document body for the embedded model.
         */
        public static final String EMB_BODY = "emb_contents";
    }

    /**
     * Creates a new parsed document.
     *
     * @param id   the document identifier.
     * @param body the document body.
     */
    public ParsedEmbeddedDocument(final String id, final float[] body) {
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

        if (body.length == 0) {
            throw new IllegalStateException("Document body cannot be empty.");
        }

        this.body = body;
    }

    /**
     * Returns the unique document identifier.
     *
     * @return the unique document identifier.
     */
    public String getIdentifier() {
        return id;
    }

    /**
     * Returns the body of the document.
     *
     * @return the body of the document.
     */
    public float[] getBody() {
        return body;
    }

    /**
     * Override the toString method. It returns a string representation of the document.
     *
     * @return the string representation of the document.
     */
    @Override
    public String toString() {
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
    public boolean equals(Object o) {
        return (this == o) || ((o instanceof ParsedEmbeddedDocument) && id.equals(((ParsedEmbeddedDocument) o).id));
    }

    /**
     * Override the hashCode method. It returns the hash code of the document identifier.
     *
     * @return the hash code of the document identifier.
     */
    @Override
    public int hashCode() {
        return 37 * id.hashCode();
    }

}
