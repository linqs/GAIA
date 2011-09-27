package linqs.gaia.model.util.plg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.model.util.plg.PLGUtils;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableSet;

/**
 * Loads edges defined by a pair of nodes given in a text file
 * where the text file has tab delimited lines of the form:
 * <p>
 * node1id\tnode2id[\t&lt;EXIST|NOTEXIST&gt;]
 * <p>
 * where node1id and node2id are string representations of the graph ids
 * and EXIST or NOTEXIST indicates whether an edge of the given type
 * exists between them.  For directed edges, node1id is used as the
 * source node and node2id is the target nodes.
 * <p>
 * Note:
 * <UL>
 * <LI> Supports only binary edges.
 * </UL>
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>edgesfile-File which contains the pairs of node ids (using the format used by toString)
 * of nodes to generate links for.  The pairs are tab delimited.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> allowselflink-If yes, self links will be allowed.  Otherwise, all
 * self edges which result in a self link are excluded.  Default is no.
 * <LI> maxnum-Maximum number of edges to propose.  Used for debugging
 * code on a small subset of the edges provided in the file.
 * The default is propose all edges in the file.
 * <LI> mergedidsfid-If specified, the nodes in the file may
 * have exists in the graph merged with other nodes (i.e., when applying entity resolution).
 * This feature, for a given node, must contain a MultiIDFeature with the ids of the nodes
 * merged to create that node.
 * <LI> type-Parameter which can be either positiveonly, negativeonly, or both.
 * If positiveonly, only edges which are labeled as existing in the file are returned.
 * If negative only, only edges which are labels ad non-existing in the file are returned.
 * If both, all edges are return.  Default is both.
 * </UL>
 * 
 * @author namatag
 *
 */
public class SavedEdges extends BaseConfigurable implements PotentialLinkGenerator {

	public void addAllLinks(Graph g, String edgeschemaid, String existfeature, boolean setnotexist) {
		PLGUtils.addAllLinks(g, edgeschemaid, existfeature,
				this.getLinksIteratively(g, edgeschemaid), setnotexist);
	}

	public Iterator<Edge> getLinksIteratively(Graph g, String edgeschemaid) {		
		Schema schema = g.getSchema(edgeschemaid);
		
		String edgesfile = this.getStringParameter("edgesfile");
		
		String mergedidsfid = null;
		if(this.hasParameter("mergedidsfid")) {
			mergedidsfid = this.getStringParameter("mergedidsfid");
		}
		
		int maxnum = -1;
		if(this.hasParameter("maxnum")) {
			maxnum = this.getIntegerParameter("maxnum");
		}
		
		boolean allowselflink = false;
		allowselflink = this.hasYesNoParameter("allowselflink", "yes");
		
		String type = "both";
		if(this.hasParameter("type")) {
			type = this.getCaseParameter("type", new String[]{"positiveonly","negativeonly","both"});
		}
		
		if(schema.getType().equals(SchemaType.UNDIRECTED)) {
			return new UndirectedIterator(g, edgeschemaid, edgesfile, mergedidsfid, type, maxnum, allowselflink);
		} else if(schema.getType().equals(SchemaType.DIRECTED)) {
			return new DirectedIterator(g, edgeschemaid, edgesfile, mergedidsfid, type, maxnum, allowselflink);
		} else {
			throw new UnsupportedTypeException("Unsupported Edge Type: "+schema);
		}
	}
	
	public void addAllLinks(Graph g, String edgeschemaid) {
		Iterator<Edge> eitr = this.getLinksIteratively(g, edgeschemaid);
		while(eitr.hasNext()) {
			eitr.next();
		}
	}
	
	public abstract class SEIterator implements Iterator<Edge> {
		private Graph g = null;
		private String edgeschemaid = null;
		private String mergedidsfid = null;
		private SimplePair<Node, Node> nextpair = null;
		private BufferedReader input =  null;
		private Map<GraphItemID, GraphItemID> origid2mergedid = new HashMap<GraphItemID,GraphItemID>();
		private String type;
		private Set<String> nodesids = new HashSet<String>();
		private int maxnum = -1;
		private int numproposed = 0;
		private boolean allowselflink = false;
		
		public SEIterator(Graph g, String edgeschemaid,
				String edgesfile, String mergedidsfid,
				String type, int maxnum, boolean allowselflink) {
			try {
				this.input =  new BufferedReader(new FileReader(edgesfile));
				this.g = g;
				this.edgeschemaid = edgeschemaid;
				this.mergedidsfid = mergedidsfid;
				this.type = type;
				this.maxnum = maxnum;
				this.allowselflink = allowselflink;
				
				this.setNextPair();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public boolean hasNext() {
			boolean hasnext = nextpair != null;
			
			if(!hasnext) {
				this.g=null;
				this.origid2mergedid = null;
				try {
					this.input.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
			return hasnext;
		}
		
		/**
		 * Create the appropriate edge with the same endpoints
		 * 
		 * @param g Graph
		 * @param edgeschemaid Edge schema ID
		 * @param n1 Node 1 of the file
		 * @param n2 Node 2 of the file
		 * @return Edge
		 */
		protected abstract Edge createEdge(Graph g, String edgeschemaid, Node n1, Node n2);

		public Edge next() {
			if(nextpair == null) {
				return null;
			}
			
			SimplePair<Node,Node> lastpair = nextpair;
			this.setNextPair();
			
			Edge e = this.createEdge(g, edgeschemaid,
					lastpair.getFirst(),
					lastpair.getSecond());
			
			this.numproposed++;
			
			return e;
		}
		
		/**
		 * Return true if the graph already has an edge with the
		 * same schema and endpoint nodes.
		 * 
		 * @param n1 Node 1 in file
		 * @param n2 Node 2 in file
		 * @param edgeschemaid Schema ID of edge
		 * @return True if an edge exists with the same schema and endpoints.  False otherwise.
		 */
		protected abstract boolean hasEdgeWithSameNodes(Node n1, Node n2, String edgeschemaid);
		
		public void setNextPair() {
			try {
				if(maxnum>0 && this.numproposed>maxnum) {
					nextpair=null;
					return;
				}
				
				nextpair = null;
				String line = input.readLine();
				while(line != null) {
					String[] parts = line.split("\t");
					
					if(parts.length!=2 && parts.length!=3) {
						throw new InvalidStateException("Invalid line encountered: "+line);
					}
					
					GraphItemID fileid1 = GraphItemID.parseGraphItemID(parts[0]);
					GraphItemID fileid2 = GraphItemID.parseGraphItemID(parts[1]);
					fileid1 = fileid1.copyWithoutGraphID();
					fileid2 = fileid2.copyWithoutGraphID();
					
					if(parts.length==3) {
						String existence = parts[2];
						
						if(!type.equals("both")) {
							if(existence.equals(LinkPredictor.EXIST) && type.equals("negativeonly")) {
								continue;
							} else if(existence.equals(LinkPredictor.NOTEXIST) && type.equals("positiveonly")) {
								continue;
							}
						}
					}
					
					GraphItemID currid1 = fileid1;
					GraphItemID currid2 = fileid2;
					
					if(mergedidsfid!=null) {
						
						String sid1 = fileid1.getSchemaID();
						String sid2 = fileid2.getSchemaID();
						
						if(!nodesids.contains(sid1)) {
							initializeSID(sid1);
						}
						
						if(!nodesids.contains(sid2)) {
							initializeSID(sid2);
						}
						
						currid1 = this.origid2mergedid.get(fileid1);
						currid2 = this.origid2mergedid.get(fileid2);
					}
					
					if(currid1==null || !g.hasNode(currid1)) {
						throw new InvalidStateException("Cannot find corresponding for: "
								+fileid1);
					}
					
					if(currid2==null || !g.hasNode(currid2)) {
						throw new InvalidStateException("Cannot find corresponding for: "
								+fileid2);
					}
					
					Node n1 = g.getNode(currid1);
					Node n2 = g.getNode(currid2);
					
					// Don't allow self loops and don't predict link between nodes
					// who already share a link of the given schema.
					if((!allowselflink && n1.equals(n2)) || this.hasEdgeWithSameNodes(n1, n2, edgeschemaid)) {
						// Do not add self loops, unless requested.
						// Also, do not add a duplicate edge with the same endpoints.
					} else {
						nextpair = new SimplePair<Node,Node>(n1,n2);
						break;
					}
					
					line = input.readLine();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		private void initializeSID(String sid) {
			Schema schema = g.getSchema(sid);
			Feature f = schema.getFeature(this.mergedidsfid);
			if(!(f instanceof MultiIDFeature)) {
				throw new ConfigurationException("Invalid feature type: "
						+f.getClass().getCanonicalName());
			}
			
			if(schema.hasFeature(this.mergedidsfid)) {
				Iterator<Node> nitr = g.getNodes(sid);
				while(nitr.hasNext()) {
					Node n = nitr.next();
					FeatureValue fv = n.getFeatureValue(this.mergedidsfid);
					
					if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
						this.origid2mergedid.put(
								n.getID().copyWithoutGraphID(),
								n.getID().copyWithoutGraphID());
					} else {
						MultiIDValue mids = (MultiIDValue) fv;
						UnmodifiableSet<ID> ids = mids.getIDs();
						for(ID id:ids) {
							this.origid2mergedid.put(
									((GraphItemID) id).copyWithoutGraphID(),
									(n.getID()).copyWithoutGraphID());
						}
					}
				}
			}
		}

		public void remove() {
			throw new InvalidStateException("Remove feature unsupported");
		}
	}
	
	public class UndirectedIterator extends SEIterator {
		public UndirectedIterator(Graph g, String edgeschemaid,
				String edgesfile, String mergedidsfid, String type, int maxnum,
				boolean allowselflink) {
			super(g, edgeschemaid, edgesfile, mergedidsfid, type, maxnum, allowselflink);
		}

		@Override
		protected Edge createEdge(Graph g, String edgeschemaid, Node n1, Node n2) {
			return g.addUndirectedEdge(
					GraphItemID.generateGraphItemID(g, edgeschemaid),
					n1,
					n2);
		}

		@Override
		protected boolean hasEdgeWithSameNodes(Node n1, Node n2, String edgeschemaid) {
			return n1.isAdjacent(n2, edgeschemaid);
		}
	}
	
	public class DirectedIterator extends SEIterator {
		public DirectedIterator(Graph g, String edgeschemaid, String edgesfile,
				String mergedidsfid, String type, int maxnum,
				boolean allowselflink) {
			super(g, edgeschemaid, edgesfile, mergedidsfid, type, maxnum, allowselflink);
		}

		@Override
		protected Edge createEdge(Graph g, String edgeschemaid, Node n1, Node n2) {
			return g.addDirectedEdge(GraphItemID.generateGraphItemID(g, edgeschemaid),
					n1,
					n2);
		}

		@Override
		protected boolean hasEdgeWithSameNodes(Node n1, Node n2, String edgeschemaid) {
			return n1.isAdjacentTarget(n2, edgeschemaid);
		}
	}
}
