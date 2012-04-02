package linqs.gaia.similarity.node;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.graph.Node;
import linqs.gaia.similarity.NormalizedNodeSimilarity;
import linqs.gaia.similarity.NormalizedSetSimilarity;
import linqs.gaia.similarity.SetSimilarity;
import linqs.gaia.similarity.set.CommonNeighbor;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.IteratorUtils;

/**
 * Node similarity function where given a summary feature (which is assumed to be string
 * valued with a comma delimited list of items) compute the set similarity of the
 * two sets of string values given the particular pair of nodes.
 * <p>
 * Note: This is an optimization designed for use when taking the set similarity
 * of sparse features (compactly captures in the summary feature) and neighborhood similarity.
 * The same functionality can be less efficiently accomplished using {@link NaiveRelationalSim} and
 * {@link linqs.gaia.similarity.string.CharacterSetSimilarity}.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> featureid-Feature ID of summary feature
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> setsimclass-Class of set similarity measure to use.  Default is {@link CommonNeighbor}.
 * <LI> neighborclass-Class of neighbors to compute.  If specified, compute weighted sum of the
 * neighbor similarity to the set similarity of items in the feature as follows:
 * value = (1.0 - alpha)*(summarysimilarity) + alpha*(neighborhoodsimilarity).
 * Default is not to use neighborhood similarity.
 * <LI> alpha-Alpha to use in weighted combination.  Default is .5.
 * </UL>
 * 
 * @author namatag
 *
 */
public class NodeSummarySetSimilarity extends BaseConfigurable 
	implements NormalizedNodeSimilarity {
	private static final long serialVersionUID = 1L;
	private boolean initialize = true;
	private String featureid = null;
	private SetSimilarity setsim = null;
	private Neighbor neighbor = null;
	private double alpha = .5;
	
	private void initialize() {
		initialize = false;
		this.featureid = this.getStringParameter("featureid");
		
		String setsimclass = this.getStringParameter("setsimclass",
				CommonNeighbor.class.getCanonicalName());
		setsim = (SetSimilarity) Dynamic.forConfigurableName(SetSimilarity.class, setsimclass, this);
		
		// Initialize neighbor information
		if(this.hasParameter("neighborclass")) {
			String neighborclass = this.getStringParameter("neighborclass");
			alpha = this.getDoubleParameter("alpha");
			this.neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass);
			this.neighbor.copyParameters(this);
		}
	}

	private double getSimilarity(Node item1, Node item2, boolean normalize) {
		if(initialize) {
			initialize();
		}
		
		// If either value is unknown, return 0 similarity
		if(!item1.hasFeatureValue(featureid) || !item2.hasFeatureValue(featureid)) {
			return 0;
		}
		
		String[] string1 = item1.getFeatureValue(featureid).getStringValue().split(",");
		Set<String> string1set = new HashSet<String>(Arrays.asList(string1));
		
		String[] string2 = item2.getFeatureValue(featureid).getStringValue().split(",");
		Set<String> string2set = new HashSet<String>(Arrays.asList(string2));
		
		double finalsim = 0;
		double sssim = normalize ?
			((NormalizedSetSimilarity) setsim).getNormalizedSimilarity(string1set, string2set) :
			setsim.getSimilarity(string1set, string2set);
		
		if(this.neighbor!=null) {
			Set<Node> n1nset = IteratorUtils.iterator2nodeset(neighbor.getNeighbors(item1).iterator());
			Set<Node> n2nset = IteratorUtils.iterator2nodeset(neighbor.getNeighbors(item2).iterator());
			
			double nsim = normalize ?
				((NormalizedSetSimilarity) setsim).getNormalizedSimilarity(string1set, string2set) :
				setsim.getSimilarity(n1nset, n2nset);
			
			finalsim = ((1.0-alpha)*sssim) + (alpha * nsim);
		} else {
			finalsim = sssim;
		}
		
		// Take the set similarity
		return finalsim;
	}
	
	public double getSimilarity(Node item1, Node item2) {
		return this.getSimilarity(item1, item2, false);
	}

	public double getNormalizedSimilarity(Node item1, Node item2) {
		return this.getSimilarity(item1, item2, true);
	}
}
