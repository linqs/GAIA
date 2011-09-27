package linqs.gaia.model.gd;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.graph.converter.jung.JungConverter;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.model.BaseModel;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;

/**
 * A wrapper of the JUNG WeakComponentClusterer which
 * finds all weak components in a graph as sets of vertex sets.
 * A weak component is defined as a maximal subgraph in which all
 * pairs of vertices in the subgraph are reachable from one another
 * in the underlying undirected subgraph. Running time:
 * O(|V| + |E|) where |V| is the number of vertices and |E| is the number of edges. 
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> jcclass-JUNG graph converter, ({@link JungConverter}),
 * instantiated using in {@link Dynamic#forConfigurableName},
 * to use in converting the GAIA graph to a JUNG graph.
 * Default is to use a JUNG graph converter with default settings.
 * <LI> numedgestoremove-Number of edges to remove in the algorithm.
 * If not specified, 10 percent of the edges are removed.
 * </UL>
 * 
 * @author namatag
 *
 */
public class WeakComponent extends BaseModel implements GroupDetection {
	private static final long serialVersionUID = 1L;
	
	private boolean initialize = true;
	private void initialize() {
		
		initialize = false;
	}
	
	public void learn(Graph graph, String nodeschemaid, String groupschemaid,
			String memberofschemaid) {
		if(initialize) {
			this.initialize();
		}
	}

	public void learn(Graph graph, String nodeschemaid, String groupschemaid) {
		if(initialize) {
			this.initialize();
		}
	}

	public void predictAsEdge(Graph graph, String groupschemaid) {
		if(initialize) {
			this.initialize();
		}
		
		if(!graph.hasSchema(groupschemaid)) {
			graph.addSchema(groupschemaid, new Schema(SchemaType.UNDIRECTED));
		}
		
		String jcclass = this.getStringParameter("jcclass",JungConverter.class.getCanonicalName());
		JungConverter jc = (JungConverter) Dynamic.forConfigurableName(JungConverter.class, jcclass, this);
		
		edu.uci.ics.jung.graph.Graph<Object,Object> jungg = jc.exportGraph(graph);
		WeakComponentClusterer<Object,Object> ebc = new WeakComponentClusterer<Object,Object>();
		Set<Set<Object>> clusters = ebc.transform(jungg);
		
		for(Set<Object> c:clusters) {
			UndirectedEdge e = null;
			for(Object id: c) {
				Node n = graph.getNode(GraphItemID.parseGraphItemID((String) id));
				if(e==null) {
					e = graph.addUndirectedEdge(GraphItemID.generateGraphItemID(graph, groupschemaid), n);
				} else {
					e.addNode(n);
				}
			}
		}
		
		// Put nodes, not in a group, into their own group
		Iterator<Node> nitr = graph.getNodes();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			if(n.numIncidentGraphItems(groupschemaid)==0) {
				graph.addUndirectedEdge(GraphItemID.generateGraphItemID(graph, groupschemaid), n);
			}
		}
	}

	public void predictAsNode(Graph graph, String groupschemaid,
			String memberofschemaid) {
		if(initialize) {
			this.initialize();
		}
		
		if(!graph.hasSchema(groupschemaid)) {
			graph.addSchema(groupschemaid, new Schema(SchemaType.NODE));
		}
		
		if(!graph.hasSchema(memberofschemaid)) {
			graph.addSchema(memberofschemaid, new Schema(SchemaType.DIRECTED));
		}
		
		JungConverter converter = new JungConverter();
		edu.uci.ics.jung.graph.Graph<Object,Object> jungg = converter.exportGraph(graph);
		WeakComponentClusterer<Object,Object> ebc = new WeakComponentClusterer<Object,Object>();
		Set<Set<Object>> clusters = ebc.transform(jungg);
		
		for(Set<Object> c:clusters) {
			Node group = graph.addNode(GraphItemID.generateGraphItemID(graph, groupschemaid));
			for(Object id: c) {
				Node n = graph.getNode(GraphItemID.parseGraphItemID((String) id));
				graph.addDirectedEdge(GraphItemID.generateGraphItemID(graph, memberofschemaid), n, group);
			}
		}
		
		// Put nodes, not in a group, into their own group
		Iterator<Node> nitr = graph.getNodes();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			if(!n.getSchemaID().equals(groupschemaid) && n.numIncidentGraphItems(memberofschemaid)==0) {
				Node group = graph.addNode(GraphItemID.generateGraphItemID(graph, groupschemaid));
				graph.addDirectedEdge(GraphItemID.generateGraphItemID(graph, memberofschemaid), n, group);
			}
		}
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		this.initialize();
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}
}
