package it.unipd.dei.se.searcher;

import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A query parser for queries. This parser is used to parse the queries in the CLEF(LongEval Lab).
 * @author CLOSE GROUP
 * @version 1.0
 */
public class ClefQueryParser extends TrecTopicsReader {

    private static final String newline = System.getProperty("line.separator");

    /** Default constructor for the class
     */
    public ClefQueryParser() {
        super();
    }

    @Override
    public QualityQuery[] readQueries(BufferedReader reader) throws IOException {
        ArrayList<QualityQuery> res = new ArrayList();

        try {
            while(null != this.read(reader, "<top>", (StringBuilder)null, false, false)) {
                HashMap<String, String> fields = new HashMap();
                StringBuilder sb = this.read(reader, "<num>", (StringBuilder)null, true, false);
                int h = sb.indexOf("</num>");
                String num = sb.substring(5, h).trim();
                sb = this.read(reader, "<title>", (StringBuilder)null, true, false);
                //int k = sb.indexOf(">");
                h = sb.indexOf("</title>");
                String title = sb.substring(7, h).trim();

                String line;


                sb.setLength(0);

                for(; (line = reader.readLine()) != null && !line.startsWith("</top>"); sb.append(line)) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                }

                System.out.println(title);

                //fields.put("num", num);
                fields.put("title", title);

                QualityQuery topic = new QualityQuery(num, fields);
                res.add(topic);
            }
        } finally {
            reader.close();
        }

        QualityQuery[] qq = (QualityQuery[])res.toArray(new QualityQuery[0]);
        Arrays.sort(qq);
        return qq;
    }


    private StringBuilder read(BufferedReader reader, String prefix, StringBuilder sb, boolean collectMatchLine, boolean collectAll) throws IOException {
        sb = sb == null ? new StringBuilder() : sb;
        String sep = "";

        while(true) {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }

            if (line.startsWith(prefix)) {
                if (collectMatchLine) {
                    sb.append(sep).append(line);
                    sep = newline;
                }

                return sb;
            }

            if (collectAll) {
                sb.append(sep).append(line);
                sep = newline;
            }
        }
    }

}
