package linqs.gaia.sampler.decorable;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.log.Log;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.KeyedList;
import linqs.gaia.util.ListUtils;

/**
 * Does stratified sampling over some Decorable item and a Feature for that item.
 * <p>
 * Note:  The implementation currently stores the splits in memory.  The implementation
 * can be changed to store the splits elsewhere, i.e. from a file, by making a custom
 * iterable object.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>targetfid-Feature id of feature to stratify over
 * <LI>numsubsets-Number of subsets to generate
 * <LI>pctsample-This parameter is required only if disjoint parameter is set to no.
 * The parameter value, [0,1], specifies the percentage of items, from each class,
 * to randomly assign to a given subset.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>disjoint-If yes, the returned subsets are disjoint splits where the items
 * are evenly distributed among the splits.  If no, the subsets contain
 * a random stratified sampling of the full set of items where the number of
 * items in each subset is defined by the pctsample parameter.  Default is yes.
 * <LI>ignoreunlabeled-If yes, when an unlabeled instance is encountered,
 * just skip the instance.  If no and an unlabeled instance is encountered,
 * throw an exception.  Default is no.  Note: When ignoring unlabeled cases,
 * the unlabeled cases are completely ignored and never returned
 * as part of a subset by the sampler.
 * <LI>seed-Random generator seed for sampling.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class StratifiedFeatureSampler extends DecorableSampler {
	private Random rand = null;
	
	public void generateSampling(Iterator<? extends Decorable> items) {
		String targetfeatureID = this.getStringParameter("targetfid");
		this.numsubsets = (int) this.getDoubleParameter("numsubsets");
		boolean ignoreunlabeled = this.getYesNoParameter("ignoreunlabeled", "no");
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		rand = new Random(seed);
		
		this.subsets.clear();
		for(int i=0; i<this.numsubsets; i++){
			this.subsets.add(new LinkedList<Decorable>());
		}
		
		// Assign items based on target value to a list
		KeyedList<String,Decorable> kl = new KeyedList<String,Decorable>();
		boolean checked = false;
		while(items.hasNext()) {
			Decorable di = items.next();
			if(!checked) {
				Feature f = di.getSchema().getFeature(targetfeatureID);
				if(!(f instanceof CategFeature)){
					throw new UnsupportedTypeException("Categorical feature expected and got: "
							+f.getClass().getCanonicalName());
				}
				
				checked = true;
			}
			
			FeatureValue fv = di.getFeatureValue(targetfeatureID);
			if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				if(ignoreunlabeled) {
					Log.WARN("Not adding item to split.  Feature value unknown for: "+di);
					continue;
				} else {
					throw new InvalidStateException("Unable to perform stratified sampling" +
							" when unlabeled instances are encountered: "+di);
				}
			}
			
			CategValue cvalue = (CategValue) fv;
			kl.addItem(cvalue.getCategory(), di);
			
			// Store list of all items
			this.allitems.add(di);
		}
		
		boolean disjoint = this.getYesNoParameter("disjoint", "yes");
		if(disjoint) {
			// Assign items into disjoint subsets
			
			// Randomly assign GraphItem in each list to one of the splits
			// with equal probability.
			Set<String> keys = new TreeSet<String>();
			keys.addAll(kl.getKeys());
			for(String key:keys){
				List<Decorable> shuffledkgitems = new LinkedList<Decorable>(kl.getList(key));
				Collections.shuffle(shuffledkgitems, rand);
				
				// Note: I shuffle the indices so that there is not bias
				// introduced by a non-random ordering where earlier subsets
				// may have more instances than latter ones.
				List<Integer> shuffledindices = ListUtils.shuffledIndices(subsets, rand);
				
				int siindex = 0;
				while(!shuffledkgitems.isEmpty()){
					int splitindex = shuffledindices.get(siindex);
					this.subsets.get(splitindex).add((GraphItem) shuffledkgitems.get(0));
					shuffledkgitems.remove(0);
					siindex = (siindex+1) % this.numsubsets;
				}
			}
		} else {
			// Assign a random subset of the items from each class to each subset
			// allowing for the same item to appear in multiple subsets
			double pctsample = this.getDoubleParameter("pctsample");
			Set<String> keys = new TreeSet<String>();
			keys.addAll(kl.getKeys());
			
			// Sample nodes where the subsets are not necessarily disjoint
			for(int i=0; i<this.numsubsets; i++) {
				for(String key:keys){
					List<Decorable> shuffledkgitems = new LinkedList<Decorable>(kl.getList(key));
					Collections.shuffle(shuffledkgitems, rand);
					
					// Note: This guarantees at least one item per subset
					double numsamples = shuffledkgitems.size() * pctsample;
					for(int s=0; s<numsamples; s++) {
						this.subsets.get(i).add(shuffledkgitems.get(s));
					}
				}
			}
		}
		
		// Show statistics per split
		if(Log.SHOWDEBUG) {
			for(int i=0; i<this.numsubsets;i++){
				Iterable<? extends Decorable> currsplit = this.getSubset(i);
				KeyedCount<String> splitkc = new KeyedCount<String>();
				for(Decorable di:currsplit) {
					CategValue cvalue = (CategValue) di.getFeatureValue(targetfeatureID);
					splitkc.increment(cvalue.getCategory());
				}
				
				Log.DEBUG("Split "+i+" (# items in split="+splitkc.totalCounted()+"):\n"+splitkc);
			}
		}
	}
}
