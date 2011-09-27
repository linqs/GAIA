package linqs.gaia.model.lp;

import java.util.Arrays;
import java.util.List;

import linqs.gaia.feature.Feature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.Model;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;

/**
 * Interface for models which perform link prediction.
 * <p>
 * For all implementing interfaces, we define the problem
 * of link prediction predicting whether an edge exists
 * in the graph or not.  When training or predicting over a graph,
 * however, there are three types of edges to consider:
 * the set of edges that are known to exist (KE),
 * the set of edges that are known not to exists(KN),
 * and the set of edges which may or may not exist as (U)
 * (i.e., their existence is unknown).
 * <p>
 * There are four ways GAIA represents these three kinds of edges.
 * The first assumes that we know, for every possible edge,
 * whether or not that edge exist.  In this representation,
 * the set U is empty, all edges in 
 * the set KE are instantiated in the graph
 * (i.e., the graph object has an edge object to represent this edge)
 * and all edges in the set KN are not instantiated
 * (i.e., an instantiated edge is known to exist and
 * all possible edges not instantiated are known not to exist).
 * We call this representation the <b>KE+KN Representation</b>.
 * <p>
 * The second representation is much like the first.
 * As in the first representation, all edges which are instantiated
 * in the graph are known to exist (i.e., belong to KE).
 * However, in this representation,
 * those edges which are not instantiated are assumed to belong
 * to set U.  The set KN is empty. (i.e., either an edge is instantiated
 * and known to exist, or we don't know whether or not it exists).
 * We call this representation the <b>KE+U Representation</b>.
 * <p>
 * The third representation is to have all the edges that may exist
 * be in the graph.  The existence state of an edge is defined
 * by a specified categorical feature with the categories
 * defined by LinkPredictor.EXISTENCE.  In this feature, the value is set to
 * LinkPredictor.EXIST for all edges in E, LinkPredictor.NOTEXIST
 * for all edges in N, and is unknown for all edges in U.
 * We also note that as with all categorical features,
 * there is a probability distribution over the possible existence
 * values which can be used for other evaluation or inference.
 * We call this representation the <b>Feature Value (FV) Representation</b>.
 * <p>
 * The final representation is a variant of the third representation
 * but where all edges predicted not to exist are removed
 * from the graph. The feature value for existing and unknown
 * edges as specified in the third representation.  Given
 * the skew in the data though, this final option is useful
 * as a way to get probabilities for the positive cases without
 * the overhead of storing all the negative cases.
 * We call this representation the <b>Existing Only Feature Value (EOFV) Representation</b>.
 * <p>
 * All three versions can store probabilities of existence using
 * a categorical feature with the categories of LinkPredictor.EXISTENCE.
 * A limitation with the first two representations, however, are that
 * you can only store existence likelihood in the graph for the positive cases
 * as links that aren't instantiated, cannot have features.
 * <p>
 * Different learning or prediction methods assume the graph
 * is in one of these representations.  Read the documentation
 * on the graph, as well as the implementing classes, for additional
 * detail.
 * 
 * @author namatag
 *
 */
public interface LinkPredictor extends Model {
	public static final String EXIST = "EXIST";
	public static final String NOTEXIST = "NOTEXIST";
	public static final List<String> EXISTENCE = Arrays.asList(NOTEXIST, EXIST);
	public static final int NOTEEXISTINDEX = 0;
	public static final int EXISTINDEX = 1;
	public static final Feature EXISTENCEFEATURE = new ExplicitCateg(EXISTENCE);
	public static final CategValue EXISTVALUE = new CategValue(EXIST, new double[]{0,1});
	public static final CategValue NOTEXISTVALUE = new CategValue(NOTEXIST, new double[]{1,0});
	
	/**
	 * Learn over the edges in the graph.
	 * The graph, in this case, is assumed to have
	 * the feature value representation.
	 * 
	 * @param graph Graph to predict edge over
	 * @param knownedges Iterable collection of edges whose existence is known
	 * (either as known existing or known not existing)
	 * @param edgeschemaid Schema ID of edges to predict existence of
	 * @param existfeature Edge feature which stores the attribute specifying existence
	 */
	void learn(Graph graph, Iterable<Edge> knownedges, String edgeschemaid, String existfeature);
	
	/**
	 * Learn over the edges in the graph.
	 * The graph, in this case, is treated either
	 * as a KE+KN or KE+U representation.
	 * 
	 * @param graph Graph to predict edge over
	 * @param generator {@link PotentialLinkGenerator} class to use to specify
	 * which pairs of nodes are potentialy adjacent.
	 * Set to null if the link prediction model don't need negative examples.
	 * @param edgeschemaid Schema ID of edges to predict existence of
	 */
	void learn(Graph graph, PotentialLinkGenerator generator, String edgeschemaid);
	
	/**
	 * Predict existence of unknown edges over the graph.
	 * The input graph is assumed to
	 * be in the KE+U representation.
	 * Links will be added or removed from the graph
	 * such that the resulting graph is assumed
	 * to be in the KE+KN representation.
	 * 
	 * @param graph Graph to predict edge over
	 * @param unknownedges Iterable collection of edges that potentially exist
	 */
	void predict(Graph graph, Iterable<Edge> unknownedges);
	
	/**
	 * Predict existence of unknown edges over the graph.
	 * The input graph is assumed to
	 * be in the KE+U representation.
	 * Links will be added or removed from the graph
	 * such that the resulting graph is assumed
	 * to be in the KE+KN representation.
	 * 
	 * @param graph Graph to predict edge over
	 * @param generator {@link PotentialLinkGenerator} class to use to specify
	 * which pairs of nodes are potentialy adjacent
	 */
	void predict(Graph graph, PotentialLinkGenerator generator);
	
	/**
	 * Predict existence of unknown edges over the graph.
	 * The input graph is assumed to be
	 * in the feature value representation.
	 * All unknown edges will be added and remain
	 * in the graph with the existence feature
	 * set as appropriate, as defined in the FV representation.
	 * If, however, removenotexist is set to true the 
	 * resulting graph will be in the EOFV representation
	 * where all edges predicted not to exist
	 * are removed.
	 * 
	 * @param graph Graph to predict edge over
	 * @param unknownedges Iterable collection of edges that potentially exist
	 * @param removenotexist If true, those predicted not to exist are removed from the resulting graph.
	 * @param existfeature Edge feature which stores the attribute specifying existence
	 */
	void predict(Graph graph, Iterable<Edge> unknownedges, boolean removenotexist, String existfeature);
	
	/**
	 * Predict existence of unknown edges over the graph.
	 * The input graph is assumed to be
	 * in the feature value representation.
	 * All unknown edges will be added and remain
	 * in the graph with the existence feature
	 * set as appropriate, as defined in the FV representation.
	 * If, however, removenotexist is set to true the 
	 * resulting graph will be in the EOFV representation
	 * where all edges predicted not to exist
	 * are removed.
	 * 
	 * @param graph Graph to predict edge over
	 * @param generator {@link PotentialLinkGenerator} class to use to specify
	 * which pairs of nodes are potentialy adjacent
	 * @param removenotexist If true, those predicted not to exist are removed from the resulting graph.
	 * @param existfeature Edge feature which stores the attribute specifying existence
	 */
	void predict(Graph graph, PotentialLinkGenerator generator, boolean removenotexist, String existfeature);
}
