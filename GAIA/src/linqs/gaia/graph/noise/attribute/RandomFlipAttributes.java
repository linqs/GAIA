package linqs.gaia.graph.noise.attribute;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.noise.AttributeNoise;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.UnmodifiableList;

/**
 * Randomly changes the value of a graph items to another.
 * This noise generator has been implemented for both explicit
 * categorical features, as well as numeric features which are binary (i.e., 0.0 or 1.0 values).
 * 
 * Required Parameters:
 * <UL>
 * <LI>schemaid-Schema id of schema and graph items whose categorical values will be flipped.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>includefeatures-The parameters is treated as a
 * comma delimited list of feature ids and/or regex pattern
 * for feature IDs in the form REGEX:&lt;pattern&gt;
 * (e.g., color,size,REGEX:\\d,length).
 * <LI>excludefeatures-Same format as include features
 * but any matching feature id and/or regex pattern
 * is removed.
 * <LI>sparsevalue-If specified, it means that the attribute value is sparse
 * and that a majority of the attributes belong to some value (i.e., 0 meaning a word is not in a document).
 * We may not want to flip those since it will alter this skew.
 * Instead, we may only want to flip when
 * we encounter one of the sparse values (i.e., 1 meaning a word is in a document).
 * This parameter defines the string representation of the sparse value for
 * which we want to do this (i.e., 1).
 * <LI>changevalue-If yes, always set the attribute to a new value
 * (i.e., do not allow it to remain the same value).  Default is false.
 * <LI>probflip-Probability of flipping an attribute.  Default is .25.
 * <LI>seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class RandomFlipAttributes extends AttributeNoise {
	private boolean initialize = true;
	private String schemaid;
	private double probflip;
	private String sparsevalue;
	private boolean changevalue;
	private Random rand;
	private List<String> fids = null;
	
	private void initialize() {
		initialize = false;
		
		// Get parameters
		schemaid = this.getStringParameter("schemaid");
		probflip = .25;
		if(this.hasParameter("probflip")) {
			probflip = this.getDoubleParameter("probflip");
		}
		
		sparsevalue = null;
		if(this.hasParameter("sparsevalue")) {
			sparsevalue = this.getStringParameter("sparsevalue");
		}
		
		changevalue = false;
		if(this.hasParameter("changevalue")) {
			changevalue = this.hasYesNoParameter("changevalue", "yes");
		}
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		
		rand = new Random(seed);
	}
	
	@Override
	public void addNoise(Graph g) {
		if(initialize) {
			this.initialize();
		}
		
		Schema schema = g.getSchema(schemaid);
		
		// Load feature ids, if needed
		if(fids == null) {
			fids = FeatureUtils.parseFeatureList(this,
					schema, IteratorUtils.iterator2stringlist(schema.getFeatureIDs()));
		}
		
		for(String fid:fids) {
			Feature f = schema.getFeature(fid);
			
			// Flip attributes
			if(f instanceof ExplicitCateg) {
				// Handle explicit categorical features
				UnmodifiableList<String> cats = ((ExplicitCateg) f).getAllCategories();
				int catsize = cats.size();
				
				// Iterate over graph all graph items with the given schema
				Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
				while(gitr.hasNext()) {
					GraphItem gi = gitr.next();
					
					String oldvalue = gi.getFeatureValue(fid).getStringValue();
					if(sparsevalue!=null && !oldvalue.equals(sparsevalue)) {
						// Only flip the non-sparse values
						// Done so for sparse sets, the common value doesn't become
						// too common.
						continue;
					}
					
					// Only flip given some probability
					if(rand.nextDouble() > probflip) {
						continue;
					}
					
					// Randomly select a category
					int index = rand.nextInt(catsize);
					if(changevalue && catsize > 1) {
						while(oldvalue.equals(cats.get(index))) {
							index = rand.nextInt(catsize);
						}
					}
					
					gi.setFeatureValue(fid, new CategValue(cats.get(index)));
				}
			} else if(f instanceof ExplicitNum) {
				// Iterate over graph all graph items with the given schema
				Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
				while(gitr.hasNext()) {
					GraphItem gi = gitr.next();
					
					double oldvalue = ((NumValue) gi.getFeatureValue(fid)).getNumber();
					if(oldvalue != 0.0 && oldvalue != 1.0) {
						throw new UnsupportedTypeException("Numeric values must be either 0 or 1: "
								+gi+"."+fid+"="+oldvalue);
					}
					
					if(sparsevalue!=null && oldvalue!=Double.parseDouble(sparsevalue)) {
						// Only flip the non-sparse values
						// Done so for sparse sets, the common value doesn't become
						// too common.
						continue;
					}
					
					// Only flip given some probability
					if(rand.nextDouble() > probflip) {
						continue;
					}
					
					double value = 0;
					if(changevalue && oldvalue==value) {
						value = (oldvalue + 1) % 2;
					} else {
						// Randomly select a category
						value = rand.nextInt(2);
					}
					
					gi.setFeatureValue(fid, new NumValue(value));
				}
			} else {
				throw new UnsupportedTypeException("Unsupported Type: "+f.getClass().getCanonicalName());
			}
		}
	}

	@Override
	public void addNoise(Decorable d) {
		if(initialize) {
			this.initialize();
		}
		
		Schema schema = d.getSchema();
		
		// Load feature ids, if needed
		if(fids == null) {
			fids = FeatureUtils.parseFeatureList(this,
					schema, IteratorUtils.iterator2stringlist(schema.getFeatureIDs()));
		}
		
		for(String fid:fids) {
			Feature f = schema.getFeature(fid);
			
			// Flip attributes
			if(f instanceof ExplicitCateg) {
				// Handle explicit categorical features
				UnmodifiableList<String> cats = ((ExplicitCateg) f).getAllCategories();
				int catsize = cats.size();
				
				String oldvalue = d.getFeatureValue(fid).getStringValue();
				if(sparsevalue!=null && !oldvalue.equals(sparsevalue)) {
					// Only flip the non-sparse values
					// Done so for sparse sets, the common value doesn't become
					// too common.
					continue;
				}
				
				// Only flip given some probability
				if(rand.nextDouble() > probflip) {
					continue;
				}
				
				// Randomly select a category
				int index = rand.nextInt(catsize);
				if(changevalue && catsize > 1) {
					while(oldvalue.equals(cats.get(index))) {
						index = rand.nextInt(catsize);
					}
				}
				
				d.setFeatureValue(fid, new CategValue(cats.get(index)));
			} else if(f instanceof ExplicitNum) {
				double oldvalue = ((NumValue) d.getFeatureValue(fid)).getNumber();
				if(oldvalue != 0.0 && oldvalue != 1.0) {
					throw new UnsupportedTypeException("Numeric values must be either 0 or 1: "
							+d+"."+fid+"="+oldvalue);
				}
				
				if(sparsevalue!=null && oldvalue!=Double.parseDouble(sparsevalue)) {
					// Only flip the non-sparse values
					// Done so for sparse sets, the common value doesn't become
					// too common.
					continue;
				}
				
				// Only flip given some probability
				if(rand.nextDouble() > probflip) {
					continue;
				}
				
				double value = 0;
				if(changevalue && oldvalue==value) {
					value = (oldvalue + 1) % 2;
				} else {
					// Randomly select a category
					value = rand.nextInt(2);
				}
				
				d.setFeatureValue(fid, new NumValue(value));
			} else {
				throw new UnsupportedTypeException("Unsupported Type: "+f.getClass().getCanonicalName());
			}
		}
	}
}
