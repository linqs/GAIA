package linqs.gaia.graph.noise.edge;

import java.util.Iterator;
import java.util.Random;

import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.noise.Noise;
import linqs.gaia.log.Log;
import linqs.gaia.model.lp.LinkPredictor;

/**
 * Randomly remove edges of a given schema id from the graph
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> One of the following must be specified:
 * <UL>
 * <LI>edgeschemaid-Schema ID of edges to randomly remove
 * <LI>edgeschemaids-Comma delimited list of schema IDs of edges to randomly remove
 * </UL>
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>linkexistfid-Categorical feature specifying whether the link is real or randomly added.
 * If the feature doesn't already exist, it is added.  All original
 * edges with the given edge schema id all have the value of LinkPredictor.EXIST and,
 * if removed, have a value of LinkPredictor.NOTEXIST.
 * <LI>setunknown-If linkexistfid is specified and this is set to yes,
 * we set the existence feature value of the removed edges as unknown.
 * Default is to set them as LinkPredictor.NOTEXIST.
 * <LI>probremove-Probability of removing and edge.  Default is .25.
 * <LI>numremovedparam-Key value for Java system parameter which will
 * contain the number of edges removed.  For use with later noise generating steps.
 * If multiple edge types are removed, we create a different java parameter for
 * each edge schema id of using the edge schema id as a prefix (e.g., cites-numremoved).
 * <LI>seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class RemoveRandomEdges extends Noise {
	private boolean initialize = true;
	private boolean setunknown = false;
	private double probremove = .25;
	private boolean ismultiplesids = false;
	private String propname = null;
	private String linkexistfid = null;
	private Random rand = null;
	
	private void initialize() {
		initialize = false;
		
		setunknown = this.hasYesNoParameter("setunknown","yes");
		
		probremove = .25;
		if(this.hasParameter("probremove")) {
			probremove = this.getDoubleParameter("probremove");
		}
		
		if(this.hasParameter("numremovedparam")) {
			propname = this.getStringParameter("numremovedparam");
		}
		
		if(this.hasParameter("linkexistfid")) {
			linkexistfid = this.getStringParameter("linkexistfid");
		}
			
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		
		rand = new Random(seed);
	}
	
	@Override
	public void addNoise(Graph g) {
		if(this.hasParameter("edgeschemaids")) {
			ismultiplesids = true;
			
			// Handle multiple schema IDs being specified
			String[] esids = this.getStringParameter("edgeschemaids").split(",");
			for(String esid:esids) {
				this.addNoise(g, esid);
			}
		} else {
			// Get parameters
			String edgeschemaid = this.getStringParameter("edgeschemaid");
			this.addNoise(g, edgeschemaid);
		}
	}
	
	private void addNoise(Graph g, String edgeschemaid) {
		if(initialize) {
			this.initialize();
		}
		
		// Get link existence feature, if defined
		if(linkexistfid!=null) {
			Schema schema = g.getSchema(edgeschemaid);
			if(!schema.hasFeature(linkexistfid)) {
				schema.addFeature(linkexistfid, LinkPredictor.EXISTENCEFEATURE);
				g.updateSchema(edgeschemaid, schema);
				
				// Set current edges as existing
				Iterator<GraphItem> gitr = g.getGraphItems(edgeschemaid);
				while(gitr.hasNext()) {
					GraphItem gi = gitr.next();
					gi.setFeatureValue(linkexistfid, LinkPredictor.EXISTVALUE);
				}
			}
		}
		
		// Iterate over all edges with the given schema
		int numremoved = 0;
		Iterator<Edge> eitr = g.getEdges(edgeschemaid);
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			// Only remove given some probability
			if(rand.nextDouble() > probremove) {
				continue;
			}
			
			if(linkexistfid==null) {
				g.removeEdge(e);
			} else {
				if(!setunknown) {
					e.setFeatureValue(linkexistfid, LinkPredictor.NOTEXISTVALUE);
				}
			}
			
			numremoved++;
		}
		
		if(propname!=null) {
			String prefix = ismultiplesids ? edgeschemaid+"-" : "";
			System.setProperty(propname, prefix+numremoved);
		}
		
		Log.DEBUG("Removed edges: "+numremoved);
	}
}
