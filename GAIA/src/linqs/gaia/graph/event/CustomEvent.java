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
package linqs.gaia.graph.event;

/**
 * For use to call a custom event with a single string
 * message.
 * 
 * @author namatag
 *
 */
public class CustomEvent implements GraphEvent {
	private String message;
	
	/**
	 * Constructor
	 * 
	 * @param message Message for custom event
	 */
	public CustomEvent(String message) {
		this.message = message;
	}
	
	/**
	 * Return the message
	 * 
	 * @return Message for event
	 */
	public String getMessage() {
		return this.message;
	}
}
