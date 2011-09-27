package linqs.gaia.model.oc.active;

import java.util.List;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.Model;
import linqs.gaia.model.oc.active.query.Query;

/**
 * Interface for active learning algorithms
 * 
 * @author namatag
 *
 */
public interface ActiveLearning extends Model {
	/**
	 * Initialize the active learning algorithm to create
	 * queries for items of the specified schema and feature
	 * 
	 * @param targetschemaid Schema ID
	 * @param targetfeatureid Feature ID
	 */
	void initialize(String targetschemaid, String targetfeatureid);
	
	/**
	 * Return an ordered list over the queries of the graph.
	 * The queries are ordered by the informativeness (i.e., score)
	 * of the query.
	 * 
	 * @param g Graph
	 * @param numqueries Number of queries to return
	 * @return Ordered list of items and features
	 */
	List<Query> getQueries(Graph g, int numqueries);
	
	/**
	 * Return an ordered list over the queries of the specified items.
	 * The queries are ordered by the informativeness (i.e., score)
	 * of the query.
	 * 
	 * @param testitems Items to apply active learning over
	 * @param numqueries Number of queries to return
	 * @return Ordered list of items and features
	 */
	List<Query> getQueries(Iterable<? extends Decorable> testitems, int numqueries);
}
