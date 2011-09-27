package linqs.gaia.graph.noise.edge;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.noise.Noise;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.model.er.ERUtils;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.util.IteratorUtils;

/**
 * Add spurious binary edges to the graph.  Unlike {@link AddRandomEdges},
 * this adds only binary edges and is simpler since it can be used without specifying a
 * potential link generator.
 * 
 * Required Parameters:
 * <UL>
 * <LI>edgeschemaid-Schema ID of edges to randomly remove
 * <LI>sourcesid-Required if the edges to add are directed.  This specifies the schema ID
 * of the source nodes.
 * <LI>targetsid-Required if the edges to add are directed.  This specifies the schema ID
 * of the target nodes.
 * <LI>nodesid-Required if the edges to add are undirected.  This specifies the schema ID
 * of the nodes.
 * </UL>
 * Optional Parameters:
 * <UL>
 * <LI>numrandom-Number of random edges to add.  Default is 0.
 * <LI>pctrandom-Percent of edges to add relative to the current number of
 * edges with the specified edge schema id.  Overrides "numrandom" parameter.
 * <LI>linkexistfid-Categorical feature specifying whether the link is real or randomly added.
 * If the feature doesn't already exist, it is added.  All original
 * edges, as well as those added, with the given edge schema id,
 * all have the value of LinkPredictor.EXIST.
 * <LI>setunknown-If linkexistfid is specified and this is set to yes,
 * we set the existence feature value of the added edges as unknown.
 * Default is to set them as LinkPredictor.EXIST.
 * <LI>setnotexist-If linkexistfid is specified and this is set to yes,
 * we set the existence feature value of the added edges as LinkPredictor.NOTEXIST.
 * Default is to set them as LinkPredictor.EXIST.
 * <LI>allowselflink-If "yes", allow addition of a self link.  Default is "no".
 * <LI>referstosid-If specified, checks to see if the entity a node of a potential
 * edge refers to is adjacent to the entity of the other node given an edge with
 * the schema id given by the parameter "entityedgesid".  If the entities are adjacent,
 * the random edge is not added.
 * <LI>entityedgesid-Schema ID of edge between entities, used when the "referstosid" parameter is defined.
 * <LI>seed-Random number generator seed to use.
 * </UL>
 * 
 * @author namatag
 *
 */
public class AddRandomBinaryEdges extends Noise {
	@Override
	public void addNoise(Graph g) {
		// Assume schema is already specified for this edge
		String edgeschemaid = this.getStringParameter("edgeschemaid");
		
		String linkexistfid = null;
		if(this.hasParameter("linkexistfid")) {
			linkexistfid = this.getStringParameter("linkexistfid");
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
		
		// Random generator seed
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = this.getIntegerParameter("seed");
		}
		Random rand = new Random(seed);
		
		// Number of random edges to add
		int numrandom = 0;
		if(this.hasParameter("numrandom")) {
			numrandom = this.getIntegerParameter("numrandom");
		}
		
		if(this.hasParameter("pctrandom")) {
			double pctrandom = this.getDoubleParameter("pctrandom");
			numrandom = (int) ((double) g.numGraphItems(edgeschemaid) * pctrandom);
		}
		
		if(this.hasParameter("numaddparam")) {
			numrandom = Integer.parseInt(
					System.getProperty(this.getStringParameter("numaddparam")));
		}
		
		// Get schema ID for nodes, for both directed and undirected edges
		String sourcesid = null;
		if(this.hasParameter("sourcesid")) {
			sourcesid = this.getStringParameter("sourcesid");
		}
		
		String targetsid = null;
		if(this.hasParameter("targetsid")) {
			targetsid = this.getStringParameter("targetsid");
		}
		
		String nodesid = null;
		if(this.hasParameter("nodesid")) {
			nodesid = this.getStringParameter("nodesid");
		}
		
		// Handle case where adding noisy reference edges
		// Used to verify added edge really does not exist
		String referstosid = null;
		if(this.hasParameter("referstosid")) {
			referstosid = this.getStringParameter("referstosid");
		}
		
		String entityedgesid = null;
		boolean isdirected = true;
		if(this.hasParameter("entityedgesid")) {
			entityedgesid = this.getStringParameter("entityedgesid");
			SchemaType type = g.getSchemaType(entityedgesid);
			if(type.equals(SchemaType.DIRECTED)) {
				isdirected = true;
			} else if(type.equals(SchemaType.UNDIRECTED)) {
				isdirected = false;
			} else {
				throw new UnsupportedTypeException("Unsupported schema type for entity edge: "
						+entityedgesid+" is of type "+type);
			}
		}
		
		boolean setunknown = this.hasYesNoParameter("setunknown","yes");
		boolean setnotexist = this.hasYesNoParameter("setnotexist","yes");
		boolean allowselflink = this.hasYesNoParameter("allowselflink", "yes");
		
		Schema schema = g.getSchema(edgeschemaid);
		if(schema.getType().equals(SchemaType.DIRECTED)) {
			List<Node> sources = IteratorUtils.iterator2nodelist(g.getNodes(sourcesid));
			List<Node> targets = IteratorUtils.iterator2nodelist(g.getNodes(targetsid));
			
			int counter = 0;
			while(counter < numrandom) {
				int sindex = rand.nextInt(sources.size());
				int tindex = rand.nextInt(targets.size());
				
				// Check to see if self links are allowed
				if(!allowselflink && sindex == tindex) {
					continue;
				}
				
				// Check to see if link already exists
				Node s = sources.get(sindex);
				Node t = targets.get(tindex);
				if(s.isAdjacentTarget(t, edgeschemaid)) {
					continue;
				}
				
				// Check to see if link exists given an entity
				if(referstosid!=null) {
					Node se = ERUtils.getRefersToEntity(s, referstosid);
					Node te = ERUtils.getRefersToEntity(t, referstosid);
					
					if(isdirected) {
						if(se.isAdjacentTarget(te, entityedgesid)) {
							continue;
						}
					} else {
						if(se.isAdjacent(te, entityedgesid)) {
							continue;
						}
					}
				}
				
				// Add link
				Edge e = g.addDirectedEdge(GraphItemID.generateGraphItemID(g, edgeschemaid, ""), s, t);
				
				// Set feature as existing, if requested,
				// or leave as unknown if setunknown is yes
				if(linkexistfid!=null && !setunknown) {
					if(setnotexist) {
						e.setFeatureValue(linkexistfid, LinkPredictor.NOTEXISTVALUE);
					} else if(!setunknown) {
						e.setFeatureValue(linkexistfid, LinkPredictor.EXISTVALUE);
					}
				}
				
				counter++;
			}
		} else if(schema.getType().equals(SchemaType.UNDIRECTED)) {
			List<Node> n1nodes = IteratorUtils.iterator2nodelist(g.getNodes(nodesid));
			List<Node> n2nodes = n1nodes;
			
			int counter = 0;
			while(counter < numrandom) {
				int n1index = rand.nextInt(n1nodes.size());
				int n2index = rand.nextInt(n2nodes.size());
				
				// Check to see if self links are allowed
				if(!allowselflink && n1index == n2index) {
					continue;
				}
				
				// Check to see if link already exists
				Node n1 = n1nodes.get(n1index);
				Node n2 = n2nodes.get(n2index);
				if(n1.isAdjacent(n2, edgeschemaid)) {
					continue;
				}
				
				// Check to see if link exists given an entity
				// If it does, do not add it
				if(referstosid!=null) {
					Node se = ERUtils.getRefersToEntity(n1, referstosid);
					Node te = ERUtils.getRefersToEntity(n2, referstosid);
					
					if(isdirected) {
						if(se.isAdjacentTarget(te, entityedgesid)) {
							continue;
						}
					} else {
						if(se.isAdjacent(te, entityedgesid)) {
							continue;
						}
					}
				}
				
				// Add link
				Edge e = g.addUndirectedEdge(GraphItemID.generateGraphItemID(g, edgeschemaid, ""), n1, n2);
				
				// Set feature as existing, if requested,
				// or leave as unknown if setunknown is yes
				if(linkexistfid!=null && !setunknown) {
					if(setnotexist) {
						e.setFeatureValue(linkexistfid, LinkPredictor.NOTEXISTVALUE);
					} else if(!setunknown) {
						e.setFeatureValue(linkexistfid, LinkPredictor.EXISTVALUE);
					}
				}
				
				counter++;
			}
		} else {
			throw new UnsupportedTypeException("Unsupported schema type for edge: "
					+edgeschemaid+" is of type "+schema.getType());
		}
	}
}
