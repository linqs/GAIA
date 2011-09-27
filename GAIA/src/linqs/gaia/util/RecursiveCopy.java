package linqs.gaia.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Utilities to make recursive copies of serialized objects easier.
 * 
 * @author namatag
 *
 */
public class RecursiveCopy {
	/**
	 * Perform a deep recursive clone of the object using Java Serialization
	 * as described in:
	 * 
	 * http://javatechniques.com/blog/faster-deep-copies-of-java-objects/
	 * <p>
	 * Note: This procedure can be very slow.  Avoid running this function.
	 * 
	 * @return Recursive copy of this model
	 */
	public static Object createCopy(Object oldobj) {
		// Code adapted from:
		// http://javatechniques.com/blog/faster-deep-copies-of-java-objects/
		Object obj = null;
        try {
            // Write the object out to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(oldobj);
            out.flush();
            out.close();

            // Make an input stream from the byte array and read
            // a copy of the object back in.
            ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()));
            obj = in.readObject();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        
        return obj;
	}
}
