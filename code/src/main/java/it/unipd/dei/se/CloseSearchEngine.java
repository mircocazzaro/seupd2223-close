package it.unipd.dei.se;

import java.util.Arrays;
import java.util.Collections;
public class CloseSearchEngine {
    public static void main(String[] args) throws Exception {
        if (args.length != 3 || Collections.frequency(Arrays.asList(args), null) > 0) {
            throw new IllegalArgumentException(
                    "Usage: java -jar close-1.00-jar-with-dependencies.jar <collection path> <topic path> <index path>"
            );
        }
        final String collectionPath = args[0];
        final String topicPath = args[1];
        final String indexPath = args[2];

    }
}
