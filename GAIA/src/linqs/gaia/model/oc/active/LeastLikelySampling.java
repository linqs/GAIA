package linqs.gaia.model.oc.active;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.log.Log;
import linqs.gaia.model.BaseModel;
import linqs.gaia.model.oc.active.query.Query;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.TopK;
import linqs.gaia.util.WeightedSampler;

/**
 * Implementation of active learning by querying the instances
 * with the least confidence (i.e., least probability for the most likely assignment).
 * 
 * @author namatag
 *
 */
public class LeastLikelySampling extends BaseModel implements ActiveLearning {
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
			Log.DEBUG("Initalized for weighted sampling");
			int seed = this.getIntegerParameter("seed",0);
			rand = new Random(seed);
		}
	}

	public List<Query> getQueries(Graph g, int numqueries) {
		return this.getQueries(g.getIterableGraphItems(targetschemaid), numqueries);
	}

	public List<Query> getQueries(Iterable<? extends Decorable> testitems,
			int numqueries) {
		TopK<Decorable> topk = null;
		List<Decorable> objects = null;
		List<Double> weights = null;
		if(weightedsampling) {
			objects = new ArrayList<Decorable>();
			weights = new ArrayList<Double>();
		} else {
			topk = new TopK<Decorable>(numqueries,false,false);
		}
		
		for(Decorable d:testitems) {
			FeatureValue fv = d.getFeatureValue(targetfeatureid);
			if(!(fv instanceof CategValue)) {
				throw new UnsupportedTypeException("Only categorical values supported: "
						+fv.getClass().getCanonicalName());
			}
			
			CategValue cv = (CategValue) fv;
			// Order by highest enthropy first
			// Note: Modify score so that the most information has the highest score
			double score = 1.0 - ArrayUtils.maxValue(cv.getProbs());
			// Ensure that the score is non-zero and positive
			score += .0001;
			if(score<0 || score>1.0001 || Double.isNaN(score) || Double.isInfinite(score)) {
				throw new InvalidStateException("Probability should be between 0 and 1, inclusive: "+
						Arrays.toString(cv.getProbs()));
			}
			
			if(weightedsampling) {
				objects.add(d);
				weights.add(score);
			} else {
				topk.add(score, d);
			}
		}
		
		List<Query> queries = new ArrayList<Query>();
		if(weightedsampling) {
			Log.DEBUG("Performing weighted sampling");
			List<Object> wsamples = WeightedSampler.performWeightedSampling(objects, weights, numqueries, false, rand);
			for(Object o:wsamples) {
				// Confidence is not specified since its a sampling
				queries.add(new Query((Decorable) o, targetfeatureid));
			}
		} else {
			List<SimplePair<Decorable,Double>> topqueries = topk.getTopKSortedWithScores();
			for(SimplePair<Decorable,Double> pair:topqueries) {
				queries.add(new Query(pair.getFirst(),targetfeatureid,pair.getSecond()));
			}
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
