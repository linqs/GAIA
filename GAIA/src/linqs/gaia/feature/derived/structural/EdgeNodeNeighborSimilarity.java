package linqs.gaia.feature.derived.structural;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.similarity.NormalizedSetSimilarity;
import linqs.gaia.similarity.set.CommonNeighbor;
import linqs.gaia.util.Dynamic;

/**
 * Given a binary edge, consider the two nodes adjacent to that edge,
 * return the specified set similarity of the neighbors of those two nodes.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-Neighbor class to use for the node.
 * Default is to use {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
 * <LI> normalize-If yes, return the normalized similarity.
 * Default is to use the unnormalized similarity.
 * <LI> nssclass-Normalized set similarity class to mention.
 * Default is {@link linqs.gaia.similarity.set.CommonNeighbor}.
 * <LI> existfid-If specified, the edge existence, specified by this feature,
 * is set to non existing prior to computing the neighbors.
 * </UL>
 * @author namatag
 *
 */
public class EdgeNodeNeighborSimilarity extends DerivedNum {
	private boolean initialize = true;
	private Neighbor neighbor = null;
	private boolean normalize = true;
	private NormalizedSetSimilarity nss = null;
	private String existfid = null;
	
	private void initialize() {
		initialize = false;
		
		String neighborclass = Adjacent.class.getCanonicalName();
		if(this.hasParameter("neighborclass")) {
			neighborclass = this.getStringParameter("neighborclass");
		}
		neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass, this);
		
		if(this.hasParameter("normalize")) {
			normalize = this.getYesNoParameter("normalize");
		}
		
		String nssclass = CommonNeighbor.class.getCanonicalName();
		if(this.hasParameter("nssclass")) {
			nssclass = this.getStringParameter("nssclass");
		}
		nss = (NormalizedSetSimilarity)
			Dynamic.forConfigurableName(NormalizedSetSimilarity.class, nssclass, this);
		
		if(this.hasParameter("existfid")) {
			existfid = this.getStringParameter("existfid");
		}
	}
	
	/**
	 * Set neighbor class to use
	 * 
	 * @param neighbor Neighbor class to use
	 */
	public void setNeighbor(Neighbor neighbor) {
		this.initialize();
		this.neighbor = neighbor;
	}
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		if(initialize) {
			this.initialize();
		}
		
		if(!(di instanceof Edge)) {
			throw new UnsupportedTypeException("Feature only valid for edges: "+
					di.getClass().getCanonicalName());
		}
		
		Edge e = (Edge) di;
		if(e.numNodes()!=2) {
			throw new UnsupportedTypeException("Only binary edges supported: "+
					e.numNodes());
		}
		
		// Handle case where feature is affected by the existence of this edge
		FeatureValue existval = null;
		if(existfid!=null) {
			existval = di.getFeatureValue(existfid);
			di.setFeatureValue(existfid, LinkPredictor.NOTEXISTVALUE);
		}
		
		// Get nodes
		Iterator<Node> nitr = e.getAllNodes();
		Node n1 = nitr.next();
		Node n2 = nitr.next();
		
		// Get neighbors for nodes
		Set<GraphItem> set1 = new HashSet<GraphItem>();
		Iterable<GraphItem> neighbors = neighbor.getNeighbors(n1);
		for(GraphItem gi:neighbors) {
			set1.add(gi);
		}
		
		Set<GraphItem> set2 = new HashSet<GraphItem>();
		neighbors = neighbor.getNeighbors(n2);
		for(GraphItem gi:neighbors) {
			set2.add(gi);
		}
		
		// Return previous state
		if(existfid!=null) {
			di.setFeatureValue(existfid, existval);
		}
		
		if(normalize) {
			return new NumValue(nss.getNormalizedSimilarity(set1, set2));
		} else {
			return new NumValue(nss.getSimilarity(set1, set2));
		}
	}
}
