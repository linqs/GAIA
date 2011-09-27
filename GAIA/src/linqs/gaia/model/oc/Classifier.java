package linqs.gaia.model.oc;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.Model;

/**
 * Interface for the most common specification of feature classification.
 * Namely, this is the interface for performing classification over
 * one over one target feature for a given schema.
 * 
 * @author namatag
 *
 */
public interface Classifier extends Model {
	/**
	 * Learn a classifier for the given target feature using the following
	 * training items.
	 * 
	 * @param trainitems Decorable items to train over
	 * @param targetschemaid Schema ID of object whose features we're classifying
	 * @param targetfeatureid Feature ID of feature to classify
	 */
	void learn(Iterable<? extends Decorable> trainitems, String targetschemaid, String targetfeatureid);
	
	/**
	 * Learn a classifier for the given target feature using the
	 * labeled instances from the given graph.
	 * 
	 * @param traingraph Graph to train over
	 * @param targetschemaid Schema ID of object whose features we're classifying
	 * @param targetfeatureid Feature ID of feature to classify
	 */
	void learn(Graph traingraph, String targetschemaid, String targetfeatureid);
	
	/**
	 * Predict features over the specified test items.
	 * 
	 * @param testitems Decorable items to test over
	 */
	void predict(Iterable<? extends Decorable> testitems);
	
	/**
	 * Predict features over the unlabeled items of the graph
	 * 
	 * @param testgraph Graph to predict feature over
	 */
	void predict(Graph testgraph);
}
