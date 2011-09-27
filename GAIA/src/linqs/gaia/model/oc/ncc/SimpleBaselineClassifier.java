package linqs.gaia.model.oc.ncc;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.log.Log;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.ListUtils;

/**
 * Simple baseline class distribution based classification algorithms.
 * 
 * Optional Parameters:
 * <UL>
 * <LI>baseline-Baseline algorithm to use.  Options include:
 *   <UL>
 *   <LI>purelyrandom Assign labels randomly
 *   <LI>stratified Assign labels randomly but following the learned class distribution
 *   <LI>majority Assign the learned majority label.  Default value.
 *   <LI>minority Assign the learned minority label
 *   <LI>select Assign the label specified by select
 *      <UL>
 *         <LI> selectlabel-Required label to use when baseline is set to select
 *      </UL>
 *   </UL>
 * <LI>seed-Random generator seed
 * </UL>
 *
 * @author namatag
 *
 */
public class SimpleBaselineClassifier extends BaseVBClassifier implements VBClassifier {
	private static final long serialVersionUID = 1L;
	
	private KeyedCount<String> classcount = null;
	private List<String> categories = null;
	private double distribution[] = null;
	private Random rand = null;
	private int majorityindex;
	private int minorityindex;
	private int selectlabelindex=-1;
	private String type = null;
	private boolean initialize = true;
	
	@Override
	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid, String targetfeatureid, List<String> featureids) {
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		
		Iterator<? extends Decorable> itr = trainitems.iterator();
		// If there are training instances
		if(itr.hasNext()) {
			classcount = new KeyedCount<String>();
			
			while(itr.hasNext()) {
				Decorable d = itr.next();
				
				// Get categories, if needed
				if(categories == null) {
					Feature f = d.getSchema().getFeature(targetfeatureid);
					
					if(!(f instanceof CategFeature)) {
						throw new UnsupportedTypeException("Only categorical features supported: "
								+f.getClass().getCanonicalName());
					}
					
					CategFeature cf = (CategFeature) f;
					categories = IteratorUtils.iterator2stringlist(cf.getAllCategories().iterator());
				}
				
				// Get class distribution
				CategValue cval = (CategValue) d.getFeatureValue(targetfeatureid);
				classcount.increment(cval.getCategory());
			}
		}
		
		if(initialize) {
			this.initialize();
		}
	}
	
	private void initialize() {
		this.initialize = false;
		
		type = this.getStringParameter("baseline","majority");
		
		// Specify type
		if(type.equals("select")) {
			if(categories!=null) {
				String selectlabel = this.getStringParameter("selectlabel");
				this.selectlabelindex = this.categories.indexOf(selectlabel);
				
				if(this.selectlabelindex == -1) {
					throw new ConfigurationException(
							"Invalid select label: "+selectlabel
							+ " Valid are: "+ListUtils.list2string(this.categories, ","));
				}
			}
		} else if(type.equals("purelyrandom") || type.equals("stratified")) { 
			int seed = 0;
			if(this.hasParameter("seed")) {
				seed = (int) this.getDoubleParameter("seed");
			}
			
			rand = new Random(seed);
		} else {
			double majval = Double.NEGATIVE_INFINITY;
			double minval = Double.POSITIVE_INFINITY;
			distribution = new double[categories.size()];
			for(int i=0; i<categories.size(); i++) {
				double percent = this.classcount.getPercent(categories.get(i));
				distribution[i] = percent;
				
				if(majval < percent) {
					majval = percent;
					majorityindex = i;
				}
				
				if(minval > percent) {
					minval = percent;
					minorityindex = i;
				}
			}
			
			Log.DEBUG("Class Distribution:\n"+this.classcount);
		}
	}

	@Override
	public FeatureValue predict(Decorable testitem) {
		// Load categories, if not already loaded
		if(categories == null) {
			Feature f = testitem.getSchema().getFeature(targetfeatureid);
			
			if(!(f instanceof CategFeature)) {
				throw new UnsupportedTypeException("Only categorical features supported: "
						+f.getClass().getCanonicalName());
			}
			
			CategFeature cf = (CategFeature) f;
			categories = IteratorUtils.iterator2stringlist(cf.getAllCategories().iterator());
			
			// Get the index of the select label, if appropriate
			if(this.selectlabelindex==-1) {
				String selectlabel = this.getStringParameter("selectlabel");
				this.selectlabelindex = this.categories.indexOf(selectlabel);
				
				if(this.selectlabelindex == -1) {
					throw new ConfigurationException(
							"Invalid select label: "+selectlabel
							+ " Valid are: "+ListUtils.list2string(this.categories, ","));
				}
			}
		}
		
		CategValue cvalue = null;
		int index = -1;
		if(type.equals("purelyrandom")) {
			// Randomly select class
			index = rand.nextInt(categories.size());
			cvalue = new CategValue(categories.get(index));
		} else if(type.equals("stratified")) {
			// Choose a class following the distribution
			// from the training set
			double flip = rand.nextDouble();
			double upper = 0;
			index = distribution.length-1;
			for(int i=0; i<distribution.length; i++) {
				upper += distribution[i];
				if(flip < upper) {
					index = i;
					break;
				}
			}
			
			cvalue = new CategValue(categories.get(index), this.distribution);
		} else if(type.equals("majority")) {
			// Return majority class
			index = this.majorityindex;
			cvalue = new CategValue(categories.get(index));
		} else if(type.equals("minority")) {
			// Return minority class
			index = this.minorityindex;
			cvalue = new CategValue(categories.get(index));
		} else if(type.equals("select")) {
			// Return select class
			index = this.selectlabelindex;
			cvalue = new CategValue(categories.get(index));
		} else {
			throw new ConfigurationException("Parameter baseline undefined for: "+type);
		}
		
		return cvalue;
	}
	
	@Override
	public void loadVBOC(String directory) {
		// Load categories if needed
		if(this.hasParameter("saved-categories")) {
			this.categories = Arrays.asList(this.getStringParameter("saved-categories").split(","));
		}
		
		// Load class distributions from training data
		if(this.hasParameter("saved-keyedcount")) {
			classcount = new KeyedCount<String>();
			
			String skc = this.getStringParameter("saved-keyedcount");
			String counts[] = skc.split(",");
			for(String count:counts) {
				String cpair[] = count.split("=");
				classcount.setCount(cpair[0], Integer.parseInt(cpair[1]));
			}
		}
	}

	@Override
	public void saveVBOC(String directory) {
		// Save categories and class count
		if(this.categories!=null) {
			this.setParameter("saved-categories", ListUtils.list2string(this.categories,","));
		}
		
		// Save class distributions from training data
		if(this.classcount!=null) {
			this.setParameter("saved-keyedcount", this.classcount.toString("=",","));
		}
	}
}
