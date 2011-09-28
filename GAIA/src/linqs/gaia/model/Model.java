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
package linqs.gaia.model;

import java.io.Serializable;

import linqs.gaia.configurable.Configurable;

public interface Model extends Configurable, Serializable {
  /**
   * Save the learned model in the provided directory
   * 
   * @param directory Directory to save model to
   */
  void saveModel(String directory);

  /**
   * Load the learned model from the provided directory
   * 
   * @param directory Directory to load model from
   */
  void loadModel(String directory);

  /**
   * Creates a human readable version of the learned model (if applicable)
   * 
   * @return Human readable version of the learned model, if applicable.
   */
  String toString();
}
