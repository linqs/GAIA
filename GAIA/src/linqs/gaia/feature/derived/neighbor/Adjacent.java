package linqs.gaia.feature.derived.neighbor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.log.Log;

/**
 * Return all unique graph items adjacent to a specified item.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>connectingsid-Schema ID of the connecting graph item.
 * For a node, this is the schema ID of the incident edges and
 * for edges, this is the schema ID of the incident nodes.
 * If this is specified, only items connected to this item
 * by something of this schema ID will be counted.
 * <LI>connectingfeature-Parameter in the form of fname:fvalue.
 * Only connecting graph items which have the specified feature value
 * will be considered.  If the feature is not specified in the schema
 * of the connecting schemaid, all connecting graph items will be used.
 * <LI>dirtype-If the connecting sid is a directed edge, this specifies the
 * types of nodes to include.  Options include:
 * <UL>
 * <LI>sourceonly-Return only nodes which are adjacent as a source to a common incident edge
 * <LI>targetonly-Return only nodes which are adjacent as a target to a common incident edge
 * <LI>all-Return all adjecent nodes (Default)
 * </UL>
 * </UL>
 * 
 * @author namatag
 *
 */
public class Adjacent extends Neighbor {
	private static final long serialVersionUID = 1L;
	private boolean initialize = true;
	private int dirtype = 3;
	private String connectingsid = null;
	private boolean ignoreconnvalue = false;
	private String fname = null;
	private String fvalue = null;
	
	private void initialize(GraphItem gi) {
		initialize = false;
		
		if(this.hasParameter("connectingsid")){
			connectingsid = this.getStringParameter("connectingsid");
			
			if(!gi.getGraph().hasSchema(connectingsid)) {
				throw new ConfigurationException("Invalid connecting schema id: "+connectingsid);
			}
		}
		
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
		
		if(this.hasParameter("connectingfeature")){
			String fparts[] = this.getStringParameter("connectingfeature").split(":");
			if(fparts.length!=2) {
				throw new ConfigurationException("Invalid connecting feature declarationg:"
						+" Size="+fparts.length);
			}
			
			fname = fparts[0].intern();
			fvalue = fparts[1].intern();
		}
		
		ignoreconnvalue = false;
		if(connectingsid!=null && fname!=null) {
			Schema schema = gi.getGraph().getSchema(connectingsid);
			if(!schema.hasFeature(fname)) {
				ignoreconnvalue = true;
				
				throw new InvalidStateException("Connecting feature not defined: "+fname);
			}
		}
	}
	
	@Override
	public Iterable<GraphItem> calcNeighbors(GraphItem gi) {
		if(initialize) {
			this.initialize(gi);
		}
		
		Set<GraphItem> deps = new HashSet<GraphItem>();
		if(gi instanceof Node){
			// Handle nodes
			Node n = (Node) gi;
			
			// Specify certain types of edges
			Iterator<? extends Edge> eitr = null;
			if(dirtype == 1) {
				eitr = (connectingsid == null) 
					? n.getEdgesWhereTarget() : n.getEdgesWhereTarget(connectingsid);
			} else if(dirtype == 2) {
				eitr = (connectingsid == null)
					? n.getEdgesWhereSource() : n.getEdgesWhereSource(connectingsid);
			} else {
				eitr = (connectingsid == null)
					? n.getAllEdges() : n.getAllEdges(connectingsid);
			}
			
			// Iterate over all edges
			while(eitr.hasNext()){
				Edge e = eitr.next();
				
				if(!ignoreconnvalue && fname != null) {
					FeatureValue f = e.getFeatureValue(fname);
					if(!f.getStringValue().equals(fvalue) || f.equals(FeatureValue.UNKNOWN_VALUE)){
						continue;
					}
				}
				
				Iterator<Node> nitr = e.getAllNodes();
				while(nitr.hasNext()) {
					deps.add(nitr.next());
				}
			}
		} else if(gi instanceof Edge){
			// Handle edges
			Edge rel = (Edge) gi;
			Iterator<Node> nitr = rel.getAllNodes();
			while(nitr.hasNext()) {
				Node n = nitr.next();
				
				if(connectingsid != null && !n.getSchemaID().equals(connectingsid)){
					continue;
				}
				
				if(fname != null) {
					if(n.getSchema().hasFeature(fname)) {
						FeatureValue f = n.getFeatureValue(fname);
						
						if(f.equals(FeatureValue.UNKNOWN_VALUE)
								|| !f.getStringValue().equals(fvalue)){
							continue;
						}
					} else {
						Log.WARN("Connecting feature not defined: "+fname);
					}
				}
				
				Iterator<Edge> eitr = n.getAllEdges();
				while(eitr.hasNext()) {
					deps.add(eitr.next());
				}
			}
		} else {
			throw new UnsupportedTypeException("Unsupported type: "+gi.getClass().getCanonicalName());
		}
		
		// Do not include the data item itself
		deps.removeAll(Arrays.asList(new GraphItem[]{gi}));
		
		return deps;
	}
}
