package linqs.gaia.prediction;

import java.util.Iterator;

/**
 * Base interface for the set of generated predictions from a model.
 * 
 * @author namatag
 *
 */
public interface PredictionGroup {
	/**
	 * Get set of predictions
	 * 
	 * @return Set of predictions
	 */
	Iterator<? extends Prediction> getAllPredictions();
	
	/**
	 * Remove the specified predictions from the graph.
	 * i.e., Remove the labels predicted in the graph and set them to unknown
	 */
	void removeAllPredictions();
	
	/**
	 * Return the number of predictions in this group
	 * 
	 * @return Number of predictions
	 */
	int numPredictions();
}
