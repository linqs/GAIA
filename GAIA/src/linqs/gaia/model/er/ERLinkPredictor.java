package linqs.gaia.model.er;

import java.io.File;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.model.lp.ScoreThreshold;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;

/**
 * Perform entity resolution by applying a
 * {@link LinkPredictor} model to predict
 * the existence of "co-reference" edges between
 * the references.
 * 
 * Optional Parameters:
 * <UL>
 * <LI>lpclass-Class of the {@link LinkPredictor} model to use,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * Default is {@link linqs.gaia.model.lp.ScoreThreshold}.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class ERLinkPredictor extends BaseConfigurable implements EntityResolution {
	private static final long serialVersionUID = 1L;

	private String edgeschemaid = null;
	private String refschemaid;
	private LinkPredictor lp = null;
	private boolean initialize = true;
	
	private void initialize(String refschemaid, String edgeschemaid) {
		this.initialize = false;
		this.edgeschemaid = edgeschemaid;
		this.refschemaid = refschemaid;
		
		// Load link predictor to use
		String lpclass = ScoreThreshold.class.getCanonicalName();
		if(this.hasParameter("lpclass")) {
			lpclass = this.getStringParameter("lpclass");
		}
		
		lp = (LinkPredictor) Dynamic.forConfigurableName(LinkPredictor.class, lpclass);
		lp.copyParameters(this);
	}

	public void learn(Graph graph, String refschemaid, String edgeschemaid, PotentialLinkGenerator generator) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
		
		lp.learn(graph, generator, edgeschemaid);
	}
	
	public void learn(Graph graph, String edgeschemaid, String entityschemaid, String refschemaid,
			String referstoschemaid, PotentialLinkGenerator generator) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
		
		Graph copygraph = graph.copy("gaiatmp-"+graph.getID().getObjID());
		ERUtils.addCoRefFromRefersToEdges(copygraph, entityschemaid, referstoschemaid, edgeschemaid);
		this.learn(copygraph, refschemaid, edgeschemaid, generator);
		
		copygraph.destroy();
	}
	
	public void learn(Graph graph, Iterable<Edge> knownedges,
			String edgeschemaid, String refschemaid, String existfeature) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
		
		lp.learn(graph, knownedges, edgeschemaid, existfeature);
	}
	
	public void predictAsLink(Graph graph, PotentialLinkGenerator generator) {
		// Call link prediction code over the generated edges
		lp.predict(graph, generator);
	}
	
	public void predictAsLink(Graph graph, PotentialLinkGenerator generator, boolean removenotexist, String existfeature) {
		// Call link prediction code over the generated edges
		lp.predict(graph, generator, removenotexist, existfeature);
	}
	
	public void predictAsLink(Graph graph, Iterable<Edge> unknownedges,
			boolean removenotexist, String existfeature) {
		lp.predict(graph, unknownedges, removenotexist, existfeature);
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
		
		String edgeschemaid = this.getStringParameter("saved-edgeschemaid");
		this.initialize(refschemaid, edgeschemaid);
		
		this.lp.loadModel(directory+File.separator+"lpmodel");
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-edgeschemaid", this.edgeschemaid);
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
		
		this.lp.saveModel(directory+File.separator+"lpmodel");
	}
}
