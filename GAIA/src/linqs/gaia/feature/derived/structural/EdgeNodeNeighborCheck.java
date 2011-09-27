package linqs.gaia.feature.derived.structural;

import java.util.Iterator;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedCateg;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.global.Constants;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.UnmodifiableList;

/**
 * Valid only for binary edges, this feature looks at
 * the two nodes adjacent to the edge and checks
 * to see whether they are neighbors, given some neighbor criterion.
 * This feature can capture whether the nodes are incident
 * to each other via another edge type or if they are transitively co-referent.
 * 
 * Required Parameters:
 * <UL>
 * <LI>neighborclass-Neighbor class to use for the node.
 * </UL
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>existfid-If specified, the feature first changes the feature
 * specified by this ID to non-existent.  This allows the neighbor computation
 * to occur regardless of whether or not this edge is predicted to exist or not. 
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeNodeNeighborCheck extends DerivedCateg {
	UnmodifiableList<String> categs = new UnmodifiableList<String>(Constants.FALSETRUE);
	private boolean initialize = true;
	private Neighbor neighbor = null;
	private String existfid = null;
	
	private static CategValue truevalue = new CategValue(Constants.TRUE, new double[]{0,1});
	private static CategValue falsevalue = new CategValue(Constants.FALSE, new double[]{1,0});
	
	private void initialize() {
		initialize=false;
		String neighborclass = this.getStringParameter("neighborclass");
		neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass, this);
		
		if(this.hasParameter("existfid")) {
			existfid = this.getStringParameter("existfid");
		}
	}
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		if(initialize) {
			this.initialize();
		}
		
		// Only supports edges
		if(!(di instanceof Edge)) {
			throw new UnsupportedTypeException("Feature only defined for edges: "+di);
		}
		
		// Only supports binary edges
		if(((Edge) di).numNodes()!=2) {
			throw new UnsupportedTypeException("Feature only defined for binary edges: "+di
					+" has #nodes="+((Edge) di).numNodes());
		}
		
		// Handle case where feature is affected by the existence of this edge
		FeatureValue existval = null;
		if(existfid!=null) {
			existval = di.getFeatureValue(existfid);
			di.setFeatureValue(existfid, LinkPredictor.NOTEXISTVALUE);
		}
		
		Node n1 = null;
		Node n2 = null;
		if(di instanceof DirectedEdge) {
			n1 = ((DirectedEdge) di).getSourceNodes().next();
			n2 = ((DirectedEdge) di).getTargetNodes().next();
		} else if(di instanceof UndirectedEdge) {
			Iterator<Node> nitr = ((UndirectedEdge) di).getAllNodes();
			n1 = nitr.next();
			n2 = nitr.next();
		} else {
			throw new UnsupportedTypeException("Unsupported Edge Type: "+di);
		}
		
		FeatureValue returnval = falsevalue;
		Iterable<GraphItem> n1neighbors = neighbor.getNeighbors(n1);
		for(GraphItem gi:n1neighbors) {
			if(gi.equals(n2)) {
				returnval = truevalue;
			}
		}
		
		// Do opposite direction for undirected edges
		// to handle case where the neighbor definition may not be symmetric
		if(di instanceof UndirectedEdge && !returnval.equals(truevalue)) {
			Iterable<GraphItem> n2neighbors = neighbor.getNeighbors(n2);
			for(GraphItem gi:n2neighbors) {
				if(gi.equals(n1)) {
					returnval = truevalue;
				}
			}
		}
		
		// Return previous state
		if(existfid!=null) {
			di.setFeatureValue(existfid, existval);
		}
		
		return returnval;
	}

	@Override
	public UnmodifiableList<String> getAllCategories() {
		return categs;
	}
}
