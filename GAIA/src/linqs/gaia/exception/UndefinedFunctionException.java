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
 * Exception to throw whenever a function, which was not defined for a
 * variety of reasons (i.e., Java class is incomplete), is called.
 * These should be avoided as much as possible and should not be included
 * in any official releases of the code.  However, if a case does
 * arise where the code needs to be released with a function undefined,
 * make sure to include this to make sure that the condition
 * is known.
 * 
 * @author namatag
 *
 */
public class UndefinedFunctionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UndefinedFunctionException() {
		super();
	}

	public UndefinedFunctionException(String msg) {
		super(msg);
	}
}
