package linqs.gaia.model.oc.active;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.converter.jung.JungConverter;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.log.Log;
import linqs.gaia.model.BaseModel;
import linqs.gaia.model.oc.active.query.Query;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.MinMax;
import linqs.gaia.util.TopK;

/**
 * Return the highest betweenness nodes among the specified items.
 * This active learning measure is topology based and is only
 * valid for nodes in the same graph.  An exception is thrown
 * if nodes are not provided or nodes from multiple graphs are specified.
 * This implementation requires the JUNG library.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> jcclass-JUNG converter class ({@link JungConverter}) to use to convert the graph
 * to a JUNG graph from which betweenness is computed,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> cachebetweeness-If "yes", cache the betweenness value for nodes as they are computed.
 * This is useful in cases where you are calling betweenness centrality on
 * a static graph repeatedly.  Default is "no".
 * </UL>
 * 
 * @author namatag
 *
 */
public class BetweennessSampling extends BaseModel implements ActiveLearning {
	private static final long serialVersionUID = 1L;
	private String targetschemaid;
	private String targetfeatureid;
	private boolean cachebetweeness = false;
	private Map<GraphItemID,Double> node2bet = null;
	
	public void initialize(String targetschemaid, String targetfeatureid) {
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		
		cachebetweeness = this.getYesNoParameter("cachebetweeness","no");
	}

	public List<Query> getQueries(Graph g, int numqueries) {
		return this.getQueries(g.getIterableGraphItems(targetschemaid), numqueries);
	}

	public List<Query> getQueries(Iterable<? extends Decorable> testitems,
			int numqueries) {
		Set<Graph> graphs = new HashSet<Graph>();
		Set<Decorable> testitemset = new HashSet<Decorable>();
		boolean foundall = true;
		for(Decorable d:testitems) {
			graphs.add(((GraphItem) d).getGraph());
			testitemset.add(d);
			
			if(cachebetweeness && foundall && !this.node2bet.containsKey(d)) {
				foundall = false;
			}
		}
		
		// Assume that the testitems are all graph items
		// from the same graph
		if(graphs.size()>1) {
			throw new UnsupportedTypeException("All items must be from the same graph: "+
					graphs);
		}
		
		Graph g = graphs.iterator().next();
		Map<GraphItemID,Double> localnode2bet = new HashMap<GraphItemID,Double>();
		if(foundall) {
			localnode2bet = node2bet;
		} else {
			localnode2bet = new HashMap<GraphItemID,Double>();
			
			String jcclass = this.getStringParameter("jcclass");
			JungConverter jc = (JungConverter) Dynamic.forConfigurableName(JungConverter.class, jcclass, this);
			edu.uci.ics.jung.graph.Graph<Object,Object> jungg = jc.exportGraph(g);
			
			// For betweeness
			@SuppressWarnings({ "unchecked", "rawtypes" })
			BetweennessCentrality BCranker = new BetweennessCentrality(jungg);
			BCranker.setRemoveRankScoresOnFinalize(false);
			BCranker.evaluate();
			
			MinMax mm = new MinMax();
			Collection<Object> vertices = jungg.getVertices();
			Iterator<Object> itr = vertices.iterator();
			while(itr.hasNext()) {
				Object o = itr.next();
				Node n = g.getNode(GraphItemID.parseGraphItemID(o.toString()));
				
				// Skip nodes alread in the candidate list or surveyed
				if(!testitemset.contains(n)) {
					continue;
				}
				
				@SuppressWarnings("unchecked")
				double score = BCranker.getVertexRankScore(o);
				
				mm.addValue(score);
				localnode2bet.put(n.getID(), score);
			}
			
			if(Log.SHOWDEBUG) {
				Log.DEBUG("Betweenness value statistic: "+mm);
			}
			
			if(cachebetweeness && node2bet==null) {
				node2bet = localnode2bet;
			}
		}
		
		TopK<Decorable> topk = new TopK<Decorable>(numqueries);
		for(Decorable d:testitems) {
			double score = localnode2bet.get(((GraphItem) d).getID());
			topk.add(score, d);
		}
		
		Set<Decorable> sampled = topk.getTopK();
		List<Query> queries = new ArrayList<Query>();
		for(Decorable d:sampled) {
			queries.add(new Query(d, 
					this.targetfeatureid, 
					localnode2bet.get(((GraphItem) d).getID())));
		}
		
		return queries;
	}
	
	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-targetschemaid", this.targetschemaid);
		this.setParameter("saved-targetfeatureid", this.targetfeatureid);
		
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
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
}
