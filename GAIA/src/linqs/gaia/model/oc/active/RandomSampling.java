package linqs.gaia.model.oc.active;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.BaseModel;
import linqs.gaia.model.oc.active.query.Query;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.ListUtils;

/**
 * Implementation of active learning baseline which selects the instances randomly.
 * 
 * @author namatag
 *
 */
public class RandomSampling extends BaseModel implements ActiveLearning {
	private static final long serialVersionUID = 1L;
	private String targetschemaid;
	private String targetfeatureid;
	private Random rand;
	
	public void initialize(String targetschemaid, String targetfeatureid) {
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		int seed = this.getIntegerParameter("seed",0);
		rand = new Random(seed);
	}

	public List<Query> getQueries(Graph g, int numqueries) {
		return this.getQueries(g.getIterableGraphItems(targetschemaid), numqueries);
	}

	public List<Query> getQueries(Iterable<? extends Decorable> testitems,
			int numqueries) {
		List<Decorable> dlist = new ArrayList<Decorable>();
		for(Decorable d:testitems) {
			dlist.add(d);
		}
		
		// Score is set uniform for all items
		double score = 1.0/dlist.size();
		
		List<?> randk = ListUtils.pickKAtRandom(dlist, numqueries, rand);
		List<Query> queries = new ArrayList<Query>();
		for(Object d:randk) {
			queries.add(new Query((Decorable) d, this.targetfeatureid, score));
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
