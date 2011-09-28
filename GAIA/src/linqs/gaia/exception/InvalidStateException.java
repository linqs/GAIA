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
 * Exception to throw when the graph, or any other object,
 * is in a state that should not be valid given the design
 * of the code.  For example, finding an edge not adjacent
 * to any node should throw this exception.
 * 
 * @author namatag
 *
 */
public class InvalidStateException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidStateException() {
		super();
	}

	public InvalidStateException(String msg) {
		super(msg);
	}
}
