/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
