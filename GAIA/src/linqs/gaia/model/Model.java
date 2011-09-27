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
