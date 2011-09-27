package linqs.gaia.model.oc.ncc;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.registry.GraphRegistry;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.identifiable.Identifiable;
import linqs.gaia.model.er.ERUtils;
import linqs.gaia.model.er.merger.MergeUtils;
import linqs.gaia.model.oc.Classifier;
import linqs.gaia.model.oc.OCUtils;
import linqs.gaia.util.KeyedCount;

/**
 * This is an oracle model for OC. This oracle assumes that the
 * correct label has been saved, with the id of the object and given sdkey, in the
 * system data.  The model then sets the label to whatever
 * was specified in the system data with the probability of 1
 * for the true value and 0 otherwise.  An exception
 * is thrown whenever a decorable item is encountered where
 * the system data does not have a value specified for that
 * ID and system data key pair.
 * <p>
 * Note:
 * <UL>
 * <LI> Assumes the Decorable item is either a graph or graph item.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> ocsdkey - System data key to use such that the id of the object
 * and the system data key pair in the system data will return
 * the saved true label for the object.
 * Either outputgraphid or ersdkey must be specified.
 * <LI> mergedidsfid - If specified, the decorable items maybe the
 * result of merging nodes (i.e., from entity resolution).  In this case,
 * set the label to the most common label from all the merged entities.
 * <LI> outputgraphid-String representation of the output graph id where
 * the reference nodes are specified with refers-to edges to their entities.
 * If specified, a graph with this graph ID must be registered in {@link GraphRegistry} or
 * an exception will be thrown.
 * Either outputgraphid or ocsdkey must be specified.
 * <LI> referstosid-The schema id of the "refers-to" in output graph.
 * Only used when outputgraphid is specified.
 * </UL>
 * 
 * @author namatag
 *
 */
public class OCOracle extends BaseConfigurable implements Classifier {
	private static final long serialVersionUID = 1L;
	
	private boolean initialize = true;
	private String mergedidsfid = null;
	private GraphID outputgraphid;
	private String referstosid = null;
	
	private String ocsdkey = null;
	private String targetschemaid = null;
	private String targetfeatureid = null;
	
	public void initialize(String targetschemaid, String targetfeatureid) {
		initialize=false;
		
		if(this.hasParameter("ocsdkey")) {
			ocsdkey = this.getStringParameter("ocsdkey");
		}
		
		if(this.hasParameter("outputgraphid")) {
			this.outputgraphid = GraphID.parseGraphID(this.getStringParameter("outputgraphid"));
		}
		
		if(this.hasParameter("referstosid")) {
			this.referstosid = this.getStringParameter("referstosid");
		}
		
		if(this.hasParameter("mergedidsfid")) {
			mergedidsfid = this.getStringParameter("mergedidsfid");
		}
		
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
	}
	
	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid, String targetfeatureid) {
		if(initialize) {
			this.initialize(targetschemaid, targetfeatureid);
		}
	}

	public void learn(Graph traingraph, String targetschemaid,
			String targetfeatureid) {
		if(initialize) {
			this.initialize(targetschemaid, targetfeatureid);
		}
	}
	
	public void predict(Iterable<? extends Decorable> testitems) {
		Graph outputgraph = null;
		if(outputgraphid!=null) {
			outputgraph = GraphRegistry.getGraph(outputgraphid);
		}
		
		for(Decorable d:testitems) {
			Graph g = this.getGraph(d);
			String label = null;
			KeyedCount<String> labels = new KeyedCount<String>();
			
			// Count the labels and keep the highest
			// Note:  This is to handle case where references
			// may have been incorrectly merged.
			Set<ID> refs = this.getReferences(d);
			for(ID ref:refs) {
				String currlabel = this.getLabel(g, outputgraph, ref);
				labels.increment(currlabel);
			}
			
			// Set the feature value to whatever was the most common value
			// specified in the system data
			label = labels.highestCountKey();
			d.setFeatureValue(this.targetfeatureid, new CategValue(label));
		}
	}
	
	private Set<ID> getReferences(Decorable d) {
		Set<ID> refids = new HashSet<ID>();
		
		// Support the two ways a new node may be added
		// 1) As a merge of reference nodes
		// 2) As a new node linke to its references by a refers-to edge
		if(mergedidsfid!=null) {
			// Get the ids of references merged
			refids.addAll(MergeUtils.getMergeIDs((Node) d, mergedidsfid));
		} else if(referstosid!=null) {
			// Get the ids of references with refers-to edge to this entity
			Iterator<Node> itr = ((Node) d).getAdjacentSources(referstosid);
			while(itr.hasNext()) {
				refids.add(itr.next().getID());
			}
		}
		
		if(refids.isEmpty()) {
			// No references so it must be an entity
			ID id = ((Identifiable<?>) d).getID();
			refids.add(id);
		}
		
		return refids;
	}
	
	private String getLabel(Graph graph, Graph outputgraph, ID id) {
		String label = null;
		
		if(ocsdkey!=null) {
			label = graph.getSystemData(id, ocsdkey);
			if(label == null) {
				throw new InvalidStateException("Undefined label for "
						+id+" for System Data Key "+ocsdkey);
			}
		} else if(outputgraph!=null) {
			// Get equivalent node
			Node ref = (Node) outputgraph.getEquivalentGraphItem((GraphItemID) id);
			
			if(ref==null) {
				throw new InvalidStateException("No Equivalent item for "+id+" in "+outputgraph);
			}
			
			// Get label for entity of that node
			Node entity = ERUtils.getRefersToEntity(ref, referstosid);
			label = entity.getFeatureValue(targetfeatureid).getStringValue();
		} else {
			throw new ConfigurationException("Parameters ocsdkey or outputgraphid must be defined");
		}
		
		return label;
	}
	
	private Graph getGraph(Decorable d) {
		if(d instanceof GraphItem) {
			return ((GraphItem) d).getGraph();
		} else if(d instanceof Graph) {
			return ((Graph) d);
		} else {
			throw new UnsupportedTypeException("UnsupportedType: "
					+d.getClass().getCanonicalName());
		}
	}

	public void predict(Graph testgraph) {
		Iterable<? extends Decorable> testitems = OCUtils.getItemsByFeature(testgraph,
				targetschemaid, targetfeatureid, true);
		this.predict(testitems);
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		String targetschemaid = this.getStringParameter("saved-targetschemaid");
		String targetfeatureid = this.getStringParameter("saved-targetfeatureid");
		this.initialize(targetschemaid, targetfeatureid);
	}

	public void saveModel(String directory) {
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-targetschemaid", this.targetschemaid);
		this.setParameter("saved-targetfeatureid", this.targetfeatureid);
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}
}
