package linqs.gaia.model.oc.ncc;

import java.util.List;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.oc.Classifier;
import linqs.gaia.util.UnmodifiableList;

/**
 * General interface for vector based qualifiers.  These
 * are classifiers which rely mainly on a defining a target feature
 * and a set of features we can use to predict that target feature.
 * 
 * @see linqs.gaia.model.oc.ncc.BaseVBClassifier
 * 
 * @author namatag
 *
 */
public interface VBClassifier extends Classifier {
	/**
	 * Learn a model for the vector based classifier
	 * 
	 * @param trainitems Items to train over
	 * @param targetschemaid Schema ID of object whose features we're classifying
	 * @param targetfeatureid Feature ID of the feature we want to classify
	 * @param featureids List of feature ids of features to use in the classifier
	 */
	public void learn(Iterable<? extends Decorable> trainitems, String targetschemaid,
			String targetfeatureid, List<String> featureids);
	
	/**
	 * Learn a model for the vector based classifier
	 * 
	 * @param graph Graph where all items with the specified targetschemaid is used to train over
	 * @param targetschemaid Schema ID of object whose features we're classifying
	 * @param targetfeatureid Feature ID of the feature we want to classify
	 * @param featureids List of feature ids of features to use in the classifier
	 */
	public void learn(Graph graph, String targetschemaid,
			String targetfeatureid, List<String> featureids);
	
	/**
	 * Return the list of feature IDs used in the vector based classifier
	 * 
	 * @return Feature IDs of features used in classifier
	 */
	public UnmodifiableList<String> getFeatureIDs();

	/**
	 * Predict the feature over the given test item
	 * 
	 * @param testitem Item to test over
	 * 
	 * @return Predicted value
	 */
	public FeatureValue predict(Decorable testitem);
}
