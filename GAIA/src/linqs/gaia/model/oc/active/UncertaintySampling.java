package linqs.gaia.model.oc.active;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.BaseModel;
import linqs.gaia.model.oc.active.query.Query;
import linqs.gaia.util.Entropy;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.TopK;
import linqs.gaia.util.WeightedSampler;

/**
 * Implementation of active learning by querying the instances
 * with the highest enthropy.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> weightedsampling-If "yes", select instances by doing a random weighted sampling.
 * Otherwise, directly select the instances with the highest entropy.  Default is "no".
 * <LI> seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class UncertaintySampling extends BaseModel implements ActiveLearning {
	private static final long serialVersionUID = 1L;
	private String targetschemaid;
	private String targetfeatureid;
	private boolean weightedsampling = false;
	private Random rand = null;
	
	public void initialize(String targetschemaid, String targetfeatureid) {
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		this.weightedsampling = this.getYesNoParameter("weightedsampling","no");
		if(this.weightedsampling) {
			int seed = this.getIntegerParameter("seed",0);
			rand = new Random(seed);
		}
	}

	public List<Query> getQueries(Graph g, int numqueries) {
		return this.getQueries(g.getIterableGraphItems(targetschemaid), numqueries);
	}
	
	public List<Query> getQueries(Iterable<? extends Decorable> testitems,
			int numqueries) {
		if(this.weightedsampling) {
			return this.getWeightedSamplingQueries(testitems, numqueries);
		} else {
			return this.getTopKQueries(testitems, numqueries);
		}
	}
	
	private List<Query> getWeightedSamplingQueries(Iterable<? extends Decorable> testitems,
			int numqueries) {
		List<Decorable> allitems = new ArrayList<Decorable>();
		for(Decorable d:testitems) {
			allitems.add(d);
		}
		
		List<Double> weights = new ArrayList<Double>(allitems.size());
		for(Decorable d:allitems) {
			FeatureValue fv = d.getFeatureValue(targetfeatureid);
			if(!(fv instanceof CategValue)) {
				throw new UnsupportedTypeException("Only categorical values supported: "
						+fv.getClass().getCanonicalName());
			}
			
			CategValue cv = (CategValue) fv;
			// Order by highest enthropy first
			double enthropy = Entropy.computeEntropy(cv.getProbs());
			weights.add(enthropy);
		}
		
		List<Object> sampled = WeightedSampler.performWeightedSampling(allitems, weights, numqueries, false, rand);
		List<Query> queries = new ArrayList<Query>();
		for(Object o:sampled) {
			queries.add(new Query((Decorable) o, targetfeatureid));
		}
		
		return queries;
	}
	
	private List<Query> getTopKQueries(Iterable<? extends Decorable> testitems,
			int numqueries) {
		TopK<Decorable> topk = new TopK<Decorable>(numqueries,false,false);
		for(Decorable d:testitems) {
			FeatureValue fv = d.getFeatureValue(targetfeatureid);
			if(!(fv instanceof CategValue)) {
				throw new UnsupportedTypeException("Only categorical values supported: "
						+fv.getClass().getCanonicalName());
			}
			
			CategValue cv = (CategValue) fv;
			// Order by highest enthropy first
			double enthropy = Entropy.computeEntropy(cv.getProbs());
			topk.add(enthropy, d);
		}
		
		List<Query> queries = new ArrayList<Query>();
		List<SimplePair<Decorable,Double>> topqueries = topk.getTopKSortedWithScores();
		for(SimplePair<Decorable,Double> pair:topqueries) {
			queries.add(new Query(pair.getFirst(), targetfeatureid, pair.getSecond()));
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
