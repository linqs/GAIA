package linqs.gaia.feature.derived.neighbor;

import java.util.HashSet;
import java.util.Set;

import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.util.IteratorUtils;

/**
 * Given a node which is assumed to be an entity node, the neighbors of the entity
 * node is the set of reference nodes which share the specified edge
 * with the references of this entity.  For example, an entity node E1 has a
 * reference node R2 as a neighbor if E1 has at least one reference R1
 * (i.e., a directed "refers-to" edge exists from R1 to E1)
 * and R1 and R2 are adjacent given the specified edge type.
 * 
 * Required Parameters:
 * <UL>
 * <LI> referstosid-Schema ID of directed edge which goes from a reference node
 * to that nodes entity node.
 * <LI> refedgesid-Schema ID of edges between the references.
 * <LI>dirtype-If the connecting sid is a directed edge, this specifies
 * whether or not to include only sourceonly, targetonly, or all.
 * Default is all.
 * </UL>
 * 
 * @author namatag
 *
 */
public class RefCitedRefs extends Neighbor {
	private static final long serialVersionUID = 1L;
	
	private boolean initialize = true;
	private String referstosid = null;
	private String refedgesid = null;
	private int dirtype = 3;
	
	private void initialize() {
		initialize = false;
		
		referstosid = this.getStringParameter("referstosid");
		refedgesid = this.getStringParameter("refedgesid");
		
		if(this.hasParameter("dirtype")) {
			String dirtype = this.getCaseParameter("dirtype", new String[]{"sourceonly","targetonly","all"});
			
			if(dirtype.equals("sourceonly")) {
				this.dirtype = 1;
			} else if(dirtype.equals("targetonly")) {
				this.dirtype = 2;
			} else {
				this.dirtype = 3;
			}
		}
	}
	
	@Override
	protected Iterable<GraphItem> calcNeighbors(GraphItem gi) {
		if(initialize) {
			this.initialize();
		}
		
		Node entity = (Node) gi;
		
		// Add references to entity
		Set<Node> refs =  IteratorUtils.iterator2nodeset(entity.getAdjacentSources(referstosid));
		
		// Add cited references of this entities reference
		Set<GraphItem> citedrefs = new HashSet<GraphItem>();
		for(Node ref:refs) {
			if(dirtype==1) {
				citedrefs.addAll(IteratorUtils.iterator2nodeset(ref.getAdjacentSources(refedgesid)));
			} else if(dirtype==2) {
				citedrefs.addAll(IteratorUtils.iterator2nodeset(ref.getAdjacentTargets(refedgesid)));
			} else {
				citedrefs.addAll(IteratorUtils.iterator2nodeset(ref.getAdjacentGraphItems(refedgesid)));
			}
		}
		
		citedrefs.remove(gi);
		
		return citedrefs;
	}
}