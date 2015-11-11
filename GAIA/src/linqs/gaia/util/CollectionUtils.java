package linqs.gaia.util;

import java.util.Collection;
import java.util.Iterator;

public class CollectionUtils {
    /**
     * Return String representation of collection
     * 
     * @param c
     *            Collection
     * @param delimiter
     *            Delimiter to use between items
     * @return String representation
     */
    public static String collection2string(Collection<?> c, String delimiter) {
        if (c == null) {
            return null;
        } else if (c.isEmpty()) {
            return "";
        }

        Iterator<?> itr = c.iterator();
        StringBuffer output = new StringBuffer();
        while (itr.hasNext()) {
            if (output.length() != 0) {
                output.append(delimiter);
            }

            output.append(itr.next());
        }

        return output.toString();
    }

    /**
     * Return String representation of collection
     * 
     * @param c
     *            Collection
     * @param delimiter
     *            Delimiter to use between items
     * @return String representation
     */
    public static String collection2string(Collection<?> c, String delimiter, int max) {
        if (c == null) {
            return null;
        } else if (c.isEmpty()) {
            return "";
        }

        int counter = 0;
        Iterator<?> itr = c.iterator();
        StringBuffer output = new StringBuffer();
        while (itr.hasNext()) {
            counter++;
            if (counter > max) {
                break;
            }

            if (output.length() != 0) {
                output.append(delimiter);
            }

            output.append(itr.next());
        }

        return output.toString();
    }
}
