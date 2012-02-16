package linqs.gaia.graph.generator.decorator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.distribution.BinomialDistributionImpl;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.UnmodifiableList;
import linqs.gaia.util.WeightedSampler;

/**
 * Generate attributes based on labels (explicit numerical features).
 * The decorator works by instantiating a specified number of numerical features.
 * Then, for each item, we generate some specified number features to have a value of 1.0
 * where, with some probability, the feature
 * is chosen uniformly at random, and otherwise the id for the feature
 * is drawn from a binomial distribution.
 * <p>
 * The decorator is based on a decorator described in:
 * <p>
 * Prithviraj Sen and Lise Getoor.  Link-based Classification.
 * Technical Report CS-TR-4858, University of Maryland, 2007.
 * <p>
 * 
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of graph items to set attributes for
 * <LI> targetfeatureid-Feature id of the label feature to add.  Default is "label".
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> attrnoise-Probability of drawing an attribute uniformly at random instead
 * of using the binomial distribution.
 * <LI> attrprefix-Prefix to use in the feature name.  Default is "w".
 * <LI> numattributes-Total number of attributes to add.  Default is 100.
 * <LI> numperitem-Number of non-zero valued attributes per item.  Default is 10.
 * <LI> seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class SenTR07Attributes extends BaseConfigurable implements Decorator {	
	public void decorate(Graph g) {
		String schemaid=null;
		String targetfeatureid=null;
		String attrprefix="w";
		int numattributes=100;
		int numperitem=10;
		double attrnoise=0;
		
		// Set parameters
		schemaid = this.getStringParameter("schemaid");
		targetfeatureid = this.getStringParameter("targetfeatureid");
		
		if(this.hasParameter("attrprefix")) {
			attrprefix = this.getStringParameter("attrprefix");
		}
		
		if(this.hasParameter("numattributes")) {
			numattributes = this.getIntegerParameter("numattributes");
		}
		
		if(this.hasParameter("numperitem")) {
			numperitem = this.getIntegerParameter("numperitem");
		}
		
		if(this.hasParameter("attrnoise")) {
			attrnoise = this.getDoubleParameter("attrnoise");
		}
		
		int seed=0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		Random rand = new Random(seed);
		
		// Get the label feature
		Schema schema = g.getSchema(schemaid);
		Feature f = schema.getFeature(targetfeatureid);
		if(!(f instanceof ExplicitCateg)) {
			throw new ConfigurationException("Unsupported feature type: "
					+f.getClass().getCanonicalName());
		}
		UnmodifiableList<String> cats = ((CategFeature) f).getAllCategories();
		int numLabels = cats.size();
		
		// Update schema to support new attributes
		for(int i=0;i<numattributes;i++){
			// Add numeric features for the different words to add
			schema.addFeature(attrprefix+i, new ExplicitNum(new NumValue(0.0)));
		}
		g.updateSchema(schemaid, schema);
		
		// Compute the object and weight list
		List<Integer> indices = new ArrayList<Integer>();
		for(int i=0; i<numattributes; i++) {
			indices.add(i);
		}
		
		// Generate the binomial distributions for each label
		List<List<Double>> probslist = new ArrayList<List<Double>>();
		for(int l=0; l<numLabels; l++) {
			double p = (1 + l)/(1 + numLabels);
			BinomialDistributionImpl tests = new BinomialDistributionImpl(numattributes, p);
			
			List<Double> probs = new ArrayList<Double>();
			for(int i=0; i<numattributes; i++) {
				probs.add(tests.probability(i));
			}
			
			probslist.add(probs);
		}
		
		// Go over all graph items, with the given schema, and add attributes
		NumValue value1 = new NumValue(1);
		Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fvalue = gi.getFeatureValue(targetfeatureid);
			if(fvalue.equals(FeatureValue.UNKNOWN_VALUE)) {
				throw new ConfigurationException("All labels must be known: "+
						gi+"."+targetfeatureid+"="+fvalue);
			}
			
			int labelindex = cats.indexOf(((CategValue) fvalue).getCategory());
			List<Double> probs = probslist.get(labelindex);
			
			for(int i=0; i<numperitem; i++) {
				List<Object> samples = null;
				if(rand.nextDouble()<attrnoise) {
					samples = new ArrayList<Object>(1);
					samples.add(rand.nextInt(numattributes));
				} else {
					samples = WeightedSampler.performWeightedSampling(indices, probs,
							1, false, rand);
				}
				
				for(Object o:samples) {
					Integer index = (Integer) o;
					gi.setFeatureValue(attrprefix+index, value1);
				}
			}
		}
	}
}
