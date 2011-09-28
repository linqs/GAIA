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
 * Exception to throw when attempting to perform an operation
 * that is invalid.  For example, trying to remove an edge
 * which doesn't exist in the graph should throw this exception.
 * 
 * @see UnsupportedTypeException
 * @see InvalidAssignmentException
 * 
 * @author namatag
 *
 */
public class InvalidOperationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidOperationException() {
		super();
	}

	public InvalidOperationException(String msg) {
		super(msg);
	}
}
