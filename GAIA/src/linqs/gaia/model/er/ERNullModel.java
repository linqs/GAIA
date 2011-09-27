package linqs.gaia.model.er;

import java.io.File;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.util.FileIO;

/**
 * This is a null ER model.  When run, it makes no predictions.
 * For use mainly for testing, as well as to establish baselines.
 * 
 * @author namatag
 *
 */
public class ERNullModel extends BaseConfigurable implements EntityResolution {
	private static final long serialVersionUID = 1L;
	private String edgeschemaid;
	private String refschemaid;

	public void learn(Graph graph, String refschemaid, String edgeschemaid,
			PotentialLinkGenerator generator) {
		this.edgeschemaid = edgeschemaid;
		this.refschemaid = refschemaid;
	}
	
	public void learn(Graph graph, String edgeschemaid, String entityschemaid, String refschemaid,
			String referstoschemaid, PotentialLinkGenerator generator) {
		this.edgeschemaid = edgeschemaid;
		this.refschemaid = refschemaid;
	}
	
	public void learn(Graph graph, Iterable<Edge> knownedges,
			String edgeschemaid, String refschemaid, String existfeature) {
		this.edgeschemaid = edgeschemaid;
		this.refschemaid = refschemaid;
	}

	public void predictAsLink(Graph graph, PotentialLinkGenerator generator) {
		// Add co-reference edge schema, but do nothing else
		if(!graph.hasSchema(this.edgeschemaid)) {
			graph.addSchema(this.edgeschemaid, new Schema(SchemaType.UNDIRECTED));
		}
	}
	
	public void predictAsLink(Graph graph, PotentialLinkGenerator generator, boolean removenotexist, String existfeature) {
		// Add co-reference edge schema and feature, but do nothing else
		
		// Add schema, if not already defined
		if(!graph.hasSchema(this.edgeschemaid)) {
			graph.addSchema(this.edgeschemaid, new Schema(SchemaType.UNDIRECTED));
		}
		
		// Add existence feature, if not already defined
		Schema schema = graph.getSchema(this.edgeschemaid);
		if(!schema.hasFeature(existfeature)) {
			schema.addFeature(existfeature, LinkPredictor.EXISTENCEFEATURE);
			graph.updateSchema(this.edgeschemaid, schema);
		}
	}
	
	public void predictAsLink(Graph graph, Iterable<Edge> unknownedges,
			boolean removenotexist, String existfeature) {
		// Add co-reference edge schema and feature, but do nothing else
		
		// Add schema, if not already defined
		if(!graph.hasSchema(this.edgeschemaid)) {
			graph.addSchema(this.edgeschemaid, new Schema(SchemaType.UNDIRECTED));
		}
		
		// Add existence feature, if not already defined
		Schema schema = graph.getSchema(this.edgeschemaid);
		if(!schema.hasFeature(existfeature)) {
			schema.addFeature(existfeature, LinkPredictor.EXISTENCEFEATURE);
			graph.updateSchema(this.edgeschemaid, schema);
		}
	}
	
	public void predictAsNode(Graph graph, PotentialLinkGenerator generator,
			String entitysid, String referstosid) {
		this.predictAsLink(graph, generator);
		ERUtils.addEntityNodesFromCoRef(graph, graph, edgeschemaid, entitysid, refschemaid, referstosid, null, true);
	}
	
	public void predictAsNode(Graph refgraph, Graph entitygraph, PotentialLinkGenerator generator,
			String entitysid, String reffeatureid) {
		this.predictAsLink(refgraph, generator);
		ERUtils.addEntityNodesFromCoRef(refgraph, entitygraph, edgeschemaid, entitysid, refschemaid, null, reffeatureid, true);
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		this.edgeschemaid = this.getStringParameter("saved-edgeschemaid");
		this.refschemaid = this.getStringParameter("saved-refschemaid");
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-edgeschemaid", this.edgeschemaid);
		this.setParameter("saved-refschemaid", this.refschemaid);
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}
	
}
