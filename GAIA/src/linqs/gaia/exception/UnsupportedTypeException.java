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
package linqs.gaia.exception;

/**
 * Exception to throw when a received object is of an unsupported type.
 * For example, passing in a numeric feature to a model which
 * doesn't handle numeric features should throw this exception.
 * 
 * @author namatag
 *
 */
public class UnsupportedTypeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UnsupportedTypeException() {
		super();
	}

	public UnsupportedTypeException(String msg) {
		super(msg);
	}
	
	public UnsupportedTypeException(Object o) {
		super("Unsupported Class Type: "+o+" of type "+o.getClass().getCanonicalName());
	}
}
