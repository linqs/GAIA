package linqs.gaia.model.util.plg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.BaseIterator;
import linqs.gaia.util.KeyedList;

/**
 * Create binary potential links for pairs of nodes which
 * belong to the same block, given some block feature ID
 * (i.e., the string value of some specified feature is the same for both).
 * While {@link AllPairwise} can perform the same thing,
 * this potential link generator can be faster at the cost
 * of pre-computing and storing the nodes in their blocks.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> blockfid-Feature to block by. Only two nodes with the same value
 * for this feature are returned as a possible edge.
 * <LI> nodeschemaid-Schema ID of the nodes to consider potential edges between
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> allowselflinks-If "yes", allow self links (i.e., allow node to link to self).  Default is "no".
 * <LI> allowduplinks-If "yes", allow duplicate links (i.e., create edges between two nodes
 * even if they are already adjacent given the specified edge schema id).  Default is "no".
 * </UL>
 * 
 * 
 * @author namatag
 *
 */
public class BlockedPairs extends BaseConfigurable implements PotentialLinkGenerator {
	private boolean initialize = true;
	private String blockfid;
	private String nodeschemaid;
	private boolean allowselflinks;
	private boolean allowduplinks;
	
	public Iterator<Edge> getLinksIteratively(Graph g, String edgeschemaid) {
		if(initialize) {
			this.initialize();
		}
		
		SchemaType type = g.getSchemaType(edgeschemaid);
		
		// Return existing edges matching some criterion
		if(type.equals(SchemaType.DIRECTED)) {
			// Check to see if the feature for the bin value is specified
			checkForBinFeature(g, nodeschemaid);
			return new DirectedIterator(g, edgeschemaid);
		} else if(type.equals(SchemaType.UNDIRECTED)) {
			// Check to see if the feature for the bin value is specified
			checkForBinFeature(g, nodeschemaid);
			return new UndirectedIterator(g, edgeschemaid);
		} else {
			throw new UnsupportedTypeException("Unsupported edge type: "+edgeschemaid+" is "+type);
		}
	}
	
	private void checkForBinFeature(Graph g, String schemaid) {
		if(this.blockfid == null){
			return;
		}

		Schema schema = g.getSchema(schemaid);
		if(!schema.hasFeature(this.blockfid)) {
			throw new ConfigurationException("Bin value not defined for schema: "
					+this.blockfid+" from "+schemaid);
		}
	}
	
	private class DirectedIterator extends BaseIterator<Edge> {
		private Graph g;
		private String edgeschemaid;
		private KeyedList<String,Node> bin2nodes = new KeyedList<String,Node>();
		private List<String> binkeys;
		private List<Node> currbin;
		private String currbinkey;
		private boolean done = false;
		private int n1index = 0;
		private int n2index = 0;
		
		public DirectedIterator(Graph g, String edgeschemaid) {
			this.g = g;
			this.edgeschemaid = edgeschemaid;
			
			Iterator<Node> nitr = this.g.getNodes(nodeschemaid);
			while(nitr.hasNext()) {
				Node n = nitr.next();
				String bin = n.getFeatureValue(blockfid).getStringValue();
				bin2nodes.addItem(bin, n);
			}
			
			binkeys = new ArrayList<String>(bin2nodes.getKeys());
			currbinkey = binkeys.remove(0);
			currbin = bin2nodes.remove(currbinkey);
			n1index = 0;
			n2index = 0;
		}
		
		@Override
		public Edge getNext() {
			Edge nextedge = null;
			while(true) {
				boolean isvalid = true;
				
				// Iterator has reached its end
				if(done) {
					return null;
				}
				
				// Check pair
				Node n1 = currbin.get(n1index);
				Node n2 = currbin.get(n2index);
				
				// Check pair
				if(!allowselflinks && n1index==n2index) {
					isvalid = false;
				} else if(!allowduplinks && n1.isAdjacentTarget(n2, edgeschemaid)) {
					isvalid = false;
				}
				
				// Update index
				n2index++;
				if(n2index==currbin.size()) {
					n1index++;
					n2index=0;
					if(n1index==currbin.size()) {
						if(binkeys.isEmpty()) {
							currbin = null;
							done=true;
						} else {
							currbinkey = binkeys.remove(0);
							currbin = bin2nodes.remove(currbinkey);
							n1index = 0;
							n2index = 0;
						}
					}
				}
				
				// Add edge
				if(isvalid) {
					nextedge = g.addDirectedEdge(GraphItemID.generateGraphItemID(g, edgeschemaid),
						n1, n2);
					
					break;
				}
			}
			
			return nextedge;
		}
	}
	
	private class UndirectedIterator extends BaseIterator<Edge> {
		private Graph g;
		private String edgeschemaid;
		private KeyedList<String,Node> bin2nodes = new KeyedList<String,Node>();
		private List<String> binkeys;
		private List<Node> currbin;
		private String currbinkey;
		private boolean done = false;
		private int n1index = 0;
		private int n2index = 0;
		
		public UndirectedIterator(Graph g, String edgeschemaid) {
			this.g = g;
			this.edgeschemaid = edgeschemaid;
			
			Iterator<Node> nitr = this.g.getNodes(nodeschemaid);
			while(nitr.hasNext()) {
				Node n = nitr.next();
				String bin = n.getFeatureValue(blockfid).getStringValue();
				bin2nodes.addItem(bin, n);
			}
			
			binkeys = new ArrayList<String>(bin2nodes.getKeys());
			currbinkey = binkeys.remove(0);
			currbin = bin2nodes.remove(currbinkey);
			n1index = 0;
			n2index = n1index;
		}
		
		@Override
		public Edge getNext() {
			Edge nextedge = null;
			while(true) {
				boolean isvalid = true;
				
				// Iterator has reached its end
				if(done) {
					return null;
				}
				
				Node n1 = currbin.get(n1index);
				Node n2 = currbin.get(n2index);
				
				// Check pair
				if(!allowselflinks && n1index==n2index) {
					isvalid = false;
				} else if(!allowduplinks && n1.isAdjacent(n2, edgeschemaid)) {
					isvalid = false;
				}
				
				// Update index
				n2index++;
				if(n2index==currbin.size()) {
					n1index++;
					n2index=n1index;
					
					// Stop on last element
					if(n1index==currbin.size()) {
						if(binkeys.isEmpty()) {
							currbin = null;
							done=true;
						} else {
							currbinkey = binkeys.remove(0);
							currbin = bin2nodes.remove(currbinkey);
							n1index = 0;
							n2index = 0;
						}
					}
				}
				
				// Add edge
				if(isvalid) {
					if(allowselflinks && n1.equals(n2)) {
						nextedge = g.addUndirectedEdge(
								GraphItemID.generateGraphItemID(g, edgeschemaid),
								n1);
					} else {
						nextedge = g.addUndirectedEdge(
								GraphItemID.generateGraphItemID(g, edgeschemaid),
								n1, n2);
					}
					
					break;
				}
			}
			
			return nextedge;
		}
	}
	
	private void initialize() {
		initialize = false;
		
		// Get blocking feature
		this.blockfid = this.getStringParameter("blockfid");
		
		// Allow selflinks
		this.allowselflinks = this.getYesNoParameter("allowselflinks", "no");
		
		// Allow allowduplinks
		this.allowduplinks = this.getYesNoParameter("allowduplinks", "no");
		
		// Get node schema id
		nodeschemaid = this.getStringParameter("nodeschemaid");
	}

	public void addAllLinks(Graph g, String edgeschemaid) {
		PLGUtils.addAllLinks(g, edgeschemaid, this.getLinksIteratively(g, edgeschemaid));
	}

	public void addAllLinks(Graph g, String edgeschemaid, String existfeature,
			boolean setasnotexist) {
		PLGUtils.addAllLinks(g, edgeschemaid, existfeature,
				this.getLinksIteratively(g, edgeschemaid), setasnotexist);
	}
}
