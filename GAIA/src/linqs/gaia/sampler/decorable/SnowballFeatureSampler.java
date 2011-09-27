package linqs.gaia.sampler.decorable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.log.Log;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.KeyedSet;

/**
 * Perform stratified snowball sampling over some Decorable items
 * (must be Graph Items) and a Feature for that item.
 * This sampling is designed to place neighboring items (i.e., adjacent items)
 * in the same split.
 * <p>
 * The sampling works by first randomly choosing a feature value and one of the Decorable items
 * with that value for each of the splits.  In the next iteration, we select
 * another Decorable item for the next feature value for each split.  The Decorable item,
 * for a given probability, is randomly chosen from the set of
 * adjacent Decorable items which have the feature value.  Otherwise, it is randomly chosen
 * from the set of Decorable items (unassigned to a split) with the given feature value.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> targetfid-Name of feature to stratify sampling over.
 * <LI> numsubsets-Number of subsets to generate.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> incidentsid-If defined, only those adjacent given an incident
 * graph item of this schema id will be considered neighbors.
 * <LI> probneighbor-Probability of adding neighbors to a split instead of random.
 * Default is .75.
 * <LI> seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class SnowballFeatureSampler extends DecorableSampler {
	private double probneighbor;
	private List<KeyedSet<String,Decorable>> splitneighbors;
	private KeyedSet<String,Decorable> unassignedblocks;
	private List<Decorable> unassigned;
	private String targetfid;
	private String incidentsid;
	private HashMap<String,Integer> idealkeycount = new HashMap<String,Integer>();
	private KeyedCount<String> blockkeycount = new KeyedCount<String>();
	private Set<String> supportedsids = new HashSet<String>();
	private Random rand = null;
	
	public void generateSampling(Iterator<? extends Decorable> gitems) {
		this.targetfid = this.getStringParameter("targetfid");
		this.numsubsets = (int) this.getDoubleParameter("numsubsets");
		
		this.probneighbor = .75;
		if(this.hasParameter("probneighbor")) {
			this.probneighbor = this.getDoubleParameter("probneighbor");
		}
		
		if(this.hasParameter("incidentsid")) {
			this.incidentsid = this.getStringParameter("incidentsid");
		}
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		rand = new Random(seed);
		
		// Store the neighbors of the current set by their target value
		splitneighbors = new ArrayList<KeyedSet<String,Decorable>>();
		
		// Initialize splits
		this.subsets = new LinkedList<List<Decorable>>();
		for(int i=0; i<this.numsubsets; i++){
			this.subsets.add(new LinkedList<Decorable>());
			splitneighbors.add(new KeyedSet<String,Decorable>());
		}
		
		// Assign items based on target value to a list
		unassigned = new ArrayList<Decorable>();
		unassignedblocks = new KeyedSet<String,Decorable>();
		
		while(gitems.hasNext()) {
			GraphItem gi = (GraphItem) gitems.next();
			
			// Verify that the graph item has a feature with the given feature
			// defined.
			Feature f = gi.getSchema().getFeature(targetfid);
			if(!(f instanceof CategFeature)){
				throw new ConfigurationException("Categorical feature expected and got: "
						+f.getClass().getCanonicalName());
			}
			
			supportedsids.add(gi.getSchemaID());
			unassigned.add(gi);
			unassignedblocks.addItem(((CategValue) gi.getFeatureValue(targetfid)).getCategory(), gi);
			
			// Store list of all items
			this.allitems.add(gi);
		}
		
		// Maintain what the ideal stratified distribution count would be
		List<String> keys = new ArrayList<String>();
		keys.addAll(unassignedblocks.getKeys());
		for(String key:keys){
			this.idealkeycount.put(key, (unassignedblocks.getSet(key).size()/this.numsubsets)+1);
		}
		
		int idealsize = unassigned.size() / this.numsubsets;
		idealsize++;
		Log.DEBUG("Number of Decorable Items: "+unassigned.size());
		
		// Added so we randomly reorder which split gets to go first
		// so the first split doesn't always get first pick for all labels
		List<Integer> splitorder = new ArrayList<Integer>(this.numsubsets);
		for(int i=0; i<this.numsubsets; i++) {
			splitorder.add(i);
		}
		
		while(!unassigned.isEmpty()){
			for(String key:keys){
				Collections.shuffle(splitorder, rand);
				for(int oi=0; oi<this.numsubsets; oi++){
					if(unassigned.isEmpty()){
						break;
					}
					
					// Select which split to process
					int i = splitorder.get(oi);
					
					if(this.subsets.get(i).size() > idealsize){
						Log.DEBUG("Split i="+i+" is too large: Size="+this.subsets.get(i).size()
								+" from ideal="+idealsize
								+" with number remaining unassigned="+unassigned.size());
						continue;
					}
					
					// If the split has a sufficient number with this label, skip.
					if(this.blockkeycount.getCount(i+"-"+key) > this.idealkeycount.get(key)){
						continue;
					}
					
					Decorable newadd = null;
					
					// With some probability, add randomly from the unassigned block
					if(rand.nextDouble() > this.probneighbor
							&& unassignedblocks.hasKey(key)
							&& !unassignedblocks.getSet(key).isEmpty()
					) {
						// Add a random node
						newadd = this.getRandomFromSet(unassignedblocks.getSet(key));
					}
					
					// With some probability,
					// add a neighbor from right bin from set of neighbors
					if(newadd==null && splitneighbors.get(i).hasKey(key)
						&& !splitneighbors.get(i).getSet(key).isEmpty()){
						newadd = this.getRandomFromSet(splitneighbors.get(i).getSet(key));
					}
					
					// If no neighbors available from block, add randomly from block
					if(newadd==null && unassignedblocks.hasKey(key)
							&& !unassignedblocks.getSet(key).isEmpty()) {
						// Add a random node
						newadd = this.getRandomFromSet(unassignedblocks.getSet(key));
					}
					
					// If unable to set from block, add from set of all neighbors
					if(newadd==null){
						List<Decorable> allneighbors = new ArrayList<Decorable>();
						for(String nkey:keys){
							if(splitneighbors.get(i).hasKey(nkey)){
								allneighbors.addAll(splitneighbors.get(i).getSet(nkey));
							}
						}
						
						if(!allneighbors.isEmpty()) {
							newadd = allneighbors.get(rand.nextInt(allneighbors.size()));
						}
					}
					
					// If none available from block or neighbors, just randomly add from list
					if(newadd==null){
						newadd = unassigned.get(rand.nextInt(unassigned.size()));
					}
					
					if(newadd==null) {
						throw new InvalidStateException("Value should have been assigned");
					}
					
					this.addToSplit(i, newadd);
				}
			}
		}
		
		// Show statistics per split
		if(Log.SHOWDEBUG) {
			for(int i=0; i<this.numsubsets;i++){
				Collection<Decorable> currsplit = this.subsets.get(i);
				KeyedCount<String> splitkc = new KeyedCount<String>();
				for(Object o: currsplit){
					Decorable di = (Decorable) o;
					splitkc.increment(di.getFeatureValue(targetfid).getStringValue());
				}
				
				Log.DEBUG("Split "+i+": Num nodes="+currsplit.size()+"\n"+splitkc);
			}
		}
	}
	
	private Decorable getRandomFromSet(Set<Decorable> set) {
		Object[] array = set.toArray();
		return (Decorable) array[rand.nextInt(array.length)];
	}
	
	private void addToSplit(int i, Decorable newadd) {
		if(!unassigned.contains(newadd)) {
			throw new InvalidStateException("Adding something previously added: "+newadd);
		}
		
		String newaddcat = ((CategValue) newadd.getFeatureValue(targetfid)).getCategory();
		blockkeycount.increment(i+"-"+newaddcat);
		
		// Remove added item from all
		unassignedblocks.removeItem(newaddcat, newadd);
		unassigned.remove(newadd);
		for(int j=0; j<this.numsubsets; j++){
			// Remove added item from neighbors, if applicable
			if(splitneighbors.get(j).hasKey(newaddcat)) {
				splitneighbors.get(j).removeItem(newaddcat, newadd);
			}
		}
		
		// Add to subset
		subsets.get(i).add(newadd);
		
		// Add unassigned neighbors to splitneighbors
		List<GraphItem> newaddneighbors = this.getNeighbors((GraphItem) newadd);
		
		// Shuffle the neighbors so we don't always add the same first neighbor
		Collections.shuffle(newaddneighbors, rand);
		
		// Check to see if this item doesn't have a neighbor in the split
		Collection<GraphItem> newaddsneighbors = new HashSet<GraphItem>();
		newaddsneighbors.addAll(newaddneighbors);
		newaddsneighbors.retainAll(this.subsets.get(i));
		boolean addneighbor = newaddsneighbors.isEmpty();
		
		for(GraphItem newn:newaddneighbors){
			// Don't process neighbors already assigned
			if(!unassigned.contains(newn)) {
				continue;
			}
			
			Collection<GraphItem> newnneighbors = this.getNeighbors((GraphItem) newn);
			newnneighbors.retainAll(this.unassigned);
			
			// Ensure all nodes have at least one neighbor (if possible) per subset by:
			// -Adding neighbor if this neighbor has no other neighbors left in the unassigned list
			// -Adding the first neighbor if no previous neighbor was added for this node in the subset
			if(newnneighbors.size()==0 || addneighbor){
				this.addToSplit(i, newn);
				addneighbor=false;
			} else {
				// Keep track of graph items who are neighbors of nodes in this split
				// (which are not in the split)
				splitneighbors.get(i).addItem(
					((CategValue) newn.getFeatureValue(targetfid)).getCategory(), newn);
			}
		}
	}
	
	private List<GraphItem> getNeighbors(GraphItem gi) {
		List<GraphItem> neighbors = new ArrayList<GraphItem>();
		
		Iterator<GraphItem> nitr = null;
		if(incidentsid==null) {
			nitr = gi.getAdjacentGraphItems();
		} else {
			nitr = gi.getAdjacentGraphItems(incidentsid);
		}
		
		// Only neighbors with the target schemas are considered
		while(nitr.hasNext()) {
			GraphItem currgi = nitr.next();
			if(this.supportedsids.contains(currgi.getSchemaID())) {
				neighbors.add(currgi);
			}
		}
		
		return neighbors;
	}
}
