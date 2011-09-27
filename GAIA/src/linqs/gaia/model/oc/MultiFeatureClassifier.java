package linqs.gaia.model.oc;

import java.util.List;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.Model;

/**
 * Interface for all object classification models that can handle
 * classification of multiple target features at the same time.
 * The target features can both be on items with the same schema
 * or among items of different schemas.  For example, when
 * working with a movie dataset, you may want to predict both
 * the genre of the movie, as well as the movie's rating.
 * 
 * @author namatag
 *
 */
public interface MultiFeatureClassifier extends Model {
	/**
	 * Learn a classifier for the given target feature using the
	 * labeled instances from the given graph.
	 * 
	 * @param traingraph Graph to train over
	 * @param targetschemaids Schema IDs of object whose features we're classifying
	 * @param targetfeatureids Feature IDs of feature to classify
	 */
	void learn(Graph traingraph, List<String> targetschemaids, List<String> targetfeatureids);
	
	/**
	 * Learn a classifier for the given target feature using the
	 * labeled instances from the given graph.
	 * Note:  All three lists should be of the same length.
	 * 
	 * @param trainitems List of iterables over objects to train over
	 * @param targetschemaids Schema IDs of object whose features we're classifying
	 * @param targetfeatureids Feature IDs of feature to classify
	 */
	void learn(List<Iterable<? extends Decorable>> trainitems,
			List<String> targetschemaids, List<String> targetfeatureids);
	
	/**
	 * Predict features over the unlabeled items of the graph
	 * 
	 * @param testgraph Graph to predict feature over
	 */
	void predict(Graph testgraph);
	
	/**
	 * Predict features over the iterable items given in the list.
	 * The list size must correrspond to the list size of the
	 * schema ids and features to classify.
	 * 
	 * @param testitems List of iterable items to predict features over
	 */
	void predict(List<Iterable<? extends Decorable>> testitems);
}
