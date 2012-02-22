package linqs.gaia.graph.generator.decorator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.generator.decorator.Decorator;
import linqs.gaia.log.Log;
import linqs.gaia.util.KeyedCount;

/**
 * Decorator to add a randomly assigned numeric feature to use as a bin
 * value and/or a numeric feature for use with similarity in entity resolution.
 * The numeric generation method was taken from the synthetic data generator
 * description at http://www.cs.umd.edu/projects/linqs/projects/er/index.html.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>nodeschemaid-Schema ID of nodes to add features for use with ER to
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>binfeatureid-Feature ID for a numeric valued attribute to use as a bin value.
 * If set, this explicit valued string feature will contain a value for use in "binning"
 * where only those nodes with the same bin value are considered as those whose entities
 * maybe the same.  (i.e., when resolving names, you can use the first initial of the
 * last name to reduce the number of pairwise comparison you have to make).
 * <LI>numbins-This is the number of bins to use with bin feature id is set.  Default is 5.
 * <LI>erfeatureid-Feature ID to use as an ER feature.  When set,
 * this numeric feature is created and set to an unused number between 0 and erfeaturerange,
 * or, given some ambiguity probability, to a number previously assigned in the same range.
 * <LI>erfeaturerange-The value of erfeatureid will be between 0 (inclusive) and this number (exclusive).
 * Default is 10000.
 * <LI>featureasbin-If yes, assign the bin feature to the ER feature's value.
 * If not, the bin will be randomly assigned using numbins.
 * <LI>ambprob-Probability of ambiguity for ER feature.  Default is .2.
 * </UL>
 * 
 * @author namatag
 *
 */
public class BhattacharyaTKDD07Value extends BaseConfigurable implements Decorator{

	public void decorate(Graph g) {
		String nodeschemaid = this.getStringParameter("nodeschemaid");
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		
		boolean featureasbin = false;
		if(this.hasParameter("featureasbin")) {
			featureasbin = this.getYesNoParameter("featureasbin");
		}
		
		Random rand = new Random(seed);
		
		/*************************************/
		// Initialize for bin feature id
		String binfeatureid = null;
		int numbins = 5;
		if(this.hasParameter("binfeatureid")) {
			binfeatureid = this.getStringParameter("binfeatureid");
			
			if(this.hasParameter("numbins")) {
				numbins = (int) this.getDoubleParameter("numbins");
			}
			
			// Update schema with the new bin value
			Schema schema = g.getSchema(nodeschemaid);
			if(schema.hasFeature(binfeatureid)) {
				throw new ConfigurationException("Bin feature already defined: "
						+binfeatureid+" in "+nodeschemaid);
			} else {
				schema.addFeature(binfeatureid, new ExplicitNum());
			}
			
			g.updateSchema(nodeschemaid, schema);
		}
		
		/*************************************/
		
		String erfeatureid = null;
		double ambprob = .25;
		double erfeaturerange = 10000;
		List<Double> availableervals = null;
		List<Double> usedervals = null;
		
		// Initialize for er feature value
		if(this.hasParameter("erfeatureid")) {
			// Initialize for ER feature id
			if(this.hasParameter("erfeaturerange")) {
				erfeaturerange = this.getDoubleParameter("erfeaturerange");
			}
			
			erfeatureid = this.getStringParameter("erfeatureid");
			
			if(this.hasParameter("ambprob")) {
				ambprob = this.getDoubleParameter("ambprob");
			}
			
			// Set feature value lists
			availableervals = new ArrayList<Double>((int) erfeaturerange);
			for(double i=0; i<erfeaturerange; i++) {
				availableervals.add(i);
			}
			
			usedervals = new ArrayList<Double>((int) erfeaturerange);
			
			// Update schema
			Schema schema = g.getSchema(nodeschemaid);
			if(schema.hasFeature(erfeatureid)) {
				throw new ConfigurationException("Bin feature already defined: "
						+erfeatureid+" in "+nodeschemaid);
			} else {
				schema.addFeature(erfeatureid, new ExplicitNum());
			}
			
			g.updateSchema(nodeschemaid, schema);
		}
		
		/*************************************/
		
		// Iterate over all the notes and set as requested
		KeyedCount<String> bincount = new KeyedCount<String>();
		Iterator<Node> nitr = g.getNodes(nodeschemaid);
		while(nitr.hasNext()) {
			Node n = nitr.next();
			Double eid = null;
			
			// Set er feature value
			if(erfeatureid != null) {
				double prob = rand.nextDouble();
				
				if(!availableervals.isEmpty() && (usedervals.isEmpty() || prob > ambprob)) {
					// Otherwise, set value to the next available in the counter
					eid = availableervals.get(rand.nextInt(availableervals.size()));
					availableervals.remove(eid);
					usedervals.add(eid);
				} else {
					// Given some probability, select a mean value previously given to
					// another node
					eid = usedervals.get(rand.nextInt(usedervals.size()));
				}
				
				eid = eid * 10;
				bincount.increment(""+eid);
				n.setFeatureValue(erfeatureid, new NumValue(eid));
			}
			
			// Set bin value
			if(binfeatureid != null) {
				if(featureasbin) {
					// Assign it to the value the feature id was assigned to
					if(eid==null) {
						throw new ConfigurationException("Feature must be assigned for using feature as bin");
					}
					
					n.setFeatureValue(binfeatureid, new NumValue(eid));
				} else {
					// Randomly assign one
					n.setFeatureValue(binfeatureid, new NumValue(0.0+rand.nextInt(numbins)));
				}
			}
		}
		
		Log.DEBUG("Number of bins: "+bincount.numKeys()
				+" Number of entries: "+bincount.numKeys()
				+" Number per bin: "+bincount.toString("=", ","));
	}
}
