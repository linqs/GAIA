package linqs.gaia.graph.noise.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.global.Constants;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.noise.AttributeNoise;
import linqs.gaia.log.Log;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.KeyedList;

/**
 * Perform random, stratified snowball sampling given the
 * set of schema IDs and features.  The sampled items
 * will have the value of that feature removed.
 * 
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>schemaids-Comma delimited set of schema IDs whose value will be sampled.
 * There must be a one to one correspondence to the parameter "featureids".
 * Note: Does not support sampling multiple feature ids for the same schema id.
 * <LI>featureids-Comma delimited set of schema IDs whose value will be sampled.
 * There must be a one to one correspondence to the parameter "featureids".
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>pctremove-Percent of each value to remove.  Default is .1.
 * <LI>probrandom-Probability of randomly choosing an item, instead of using
 * snowball sampling.  Default is .25.
 * <LI>removeitem-If yes, remove the item with that value, instead of just removing the value.
 * Default is no.
 * <LI>verifiedfids-Comma delimited set of feature ids which contains
 * true for items whose value is verified and false otherwise.
 * There must be a one to one correspondence to the parameter "featureids".
 * <LI>seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class SnowballRemoveLabels extends AttributeNoise {

	@Override
	public void addNoise(Graph sampleg) {
		List<String> sids = Arrays.asList(this.getStringParameter("schemaids").split(","));
		List<String> fids = Arrays.asList(this.getStringParameter("featureids").split(","));
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		Random rand = new Random(seed);
		
		double pctremove = .1;
		if(this.hasParameter("pctremove")) {
			pctremove = this.getDoubleParameter("pctremove");
		}
		
		double probrandom = .25;
		if(this.hasParameter("probrandom")) {
			probrandom = this.getDoubleParameter("probrandom");
		}
		
		boolean removeitem = this.hasParameter("removeitem", "yes");
		List<String> verifiedfids = null;
		if(this.hasParameter("verifiedfids")) {
			verifiedfids = Arrays.asList(this.getStringParameter("verifiedfids").split(","));
		}
		
		KeyedList<String,GraphItem> label2gi = new KeyedList<String,GraphItem>();
		KeyedList<String,GraphItem> neighborlabel2gi = new KeyedList<String,GraphItem>();
		Map<GraphItem,String> gi2label = new HashMap<GraphItem,String>();
		KeyedCount<String> labelcount = new KeyedCount<String>();
		Set<GraphItem> processed = new HashSet<GraphItem>();
		Set<String> sidset = new HashSet<String>(sids);
		Map<String,String> sid2fid = new HashMap<String,String>();
		
		// Add graph items
		// Note: Assume sampling over only one fid per sid
		for(int i=0; i<sids.size(); i++) {
			String sid = sids.get(i);
			String fid = fids.get(i);
			Iterator<GraphItem> gitr = sampleg.getGraphItems(sid);
			while(gitr.hasNext()) {
				GraphItem gi = gitr.next();
				String label = gi.getFeatureValue(fid).getStringValue();
				String key = sid+"."+label;
				label2gi.addItem(key, gi);
				gi2label.put(gi, key);
				labelcount.increment(key);
			}
			
			sid2fid.put(sid, fid);
		}
		
		// Set number of each we need to remove
		List<String> keys = new ArrayList<String>(labelcount.getKeys());
		Collections.shuffle(keys, rand);
		for(String k:keys) {
			labelcount.setCount(k, (int) (labelcount.getCount(k)*pctremove));
		}
		
		KeyedCount<String> sampledgi = new KeyedCount<String>();
		int numrandom = 0;
		int numneighbors = 0;
		while(labelcount.totalCounted()!=0) {
			for(String k:keys) {
				if(labelcount.getCount(k)==0) {
					continue;
				}
				
				double coin = rand.nextDouble();
				GraphItem curritem = null;
				if(!neighborlabel2gi.hasKey(k) || neighborlabel2gi.getList(k).isEmpty() || coin<probrandom) {
					// Choose random node
					List<GraphItem> list = label2gi.getList(k);
					curritem = list.get(rand.nextInt(list.size()));
					numrandom++;
				} else {
					// Choose neighbor node
					List<GraphItem> list = neighborlabel2gi.getList(k);
					curritem = list.get(rand.nextInt(list.size()));
					numneighbors++;
				}
				
				// Remove label of item
				if(removeitem) {
					if(curritem instanceof Node) {
						((Node) curritem).removeIncidentEdges();
					}
					
					sampleg.removeGraphItem(curritem);
				} else {
					curritem.removeFeatureValue(sid2fid.get(curritem.getSchemaID()));
				}
				
				// Remove item from potential list
				label2gi.removeItem(k, curritem);
				if(neighborlabel2gi.hasKey(k) && neighborlabel2gi.getList(k).contains(curritem)) {
					neighborlabel2gi.removeItem(k, curritem);
				}
				processed.add(curritem);
				sampledgi.increment(k);
				
				// Add incident neighbors
				Iterator<GraphItem> gitr = curritem.getIncidentGraphItems();
				while(gitr.hasNext()) {
					GraphItem ngi = gitr.next();
					if(processed.contains(ngi) || !sidset.contains(ngi.getSchemaID())) {
						continue;
					}
					
					neighborlabel2gi.addItem(gi2label.get(ngi), ngi);
					processed.add(ngi);
				}
				
				// Add adjacent neighbors
				gitr = curritem.getAdjacentGraphItems();
				while(gitr.hasNext()) {
					GraphItem ngi = gitr.next();
					if(processed.contains(ngi) || !sidset.contains(ngi.getSchemaID())) {
						continue;
					}
					
					neighborlabel2gi.addItem(gi2label.get(ngi), ngi);
					processed.add(ngi);
				}
				
				// Decrement counter
				labelcount.decrement(k);
			}
		}
		
		// Add verified feature, if requested
		if(verifiedfids!=null) {
			for(int i=0; i<sids.size(); i++) {
				this.addVerifiedAttribute(sampleg, sids.get(i), fids.get(i), verifiedfids.get(i));
			}
		}
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Sampled items #random="+numrandom+" #neighbor="+numneighbors+":\n"+sampledgi);
		}
	}
	
	/**
	 * Set items whose value is known with the observed attribute as true
	 * and false otherwise
	 * 
	 * @param g Graph
	 * @param sid Schema ID
	 * @param fid Feature ID sampled
	 * @param verifiedfid Feature ID of verified attribute
	 */
	private void addVerifiedAttribute(Graph g, String sid, String fid, String verifiedfid) {
		Schema schema = g.getSchema(sid);
		if(!schema.hasFeature(verifiedfid)) {
			Feature f = new ExplicitCateg(Constants.FALSETRUE);
			schema.addFeature(verifiedfid, f);
			g.updateSchema(sid, schema);
		}
		
		CategValue truevalue = new CategValue(Constants.TRUE);
		CategValue falsevalue = new CategValue(Constants.FALSE);
		Iterator<GraphItem> gitr = g.getGraphItems(sid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			if(gi.hasFeatureValue(fid)) {
				gi.setFeatureValue(verifiedfid, truevalue);
			} else {
				gi.setFeatureValue(verifiedfid, falsevalue);
			}
		}
	}

	
	/**
	 * This operation is not supported for this attribute noise object.
	 */
	@Override
	public void addNoise(Decorable d) {
		throw new UnsupportedOperationException("Cannot snowball remove labels from a single item");
	}
}
