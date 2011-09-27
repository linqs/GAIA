package linqs.gaia.graph.generator.decorator;

import java.util.Iterator;
import java.util.Random;

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

/**
 * Generate attributes based on labels (explicit categorical features).
 * The attributes are generated using a naive bayes assumption.
 * The decorator works as follows.  For each label,
 * there are numwordsperlabel attributes generated so
 * that the total number of generated attributes will be
 * (numlabels*numwordsperlabel).  A subset of attributes
 * are assigned to each label.  Thus, for a given object
 * with a label value of L, that subset
 * of attributes have a value of 1 with the probability
 * defined by probsuccessprimary and 0 otherwise for
 * a given object with that label.  For the other
 * subsets of attributes corresponding to other labels,
 * we assign those values to 1 with a probability defined
 * by probsuccessprimary and 0 otherwise.
 * <p>
 * The decorator is based on a decorator described in:
 * <p>
 * Mustafa Bilgic and Lise Getoor.  Effective Label Acquisition for Collective Classification.
 * ACM SIGKDD International Conference on Knowledge Discovery and Data Mining, page 43--51 - 2008.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of graph items to set attributes for
 * <LI> targetfeatureid-Feature id of the label feature to add.  Default is "label".
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> probsuccessprimary-This is the probability of setting the values based
 * on the correct label.  This should be above .5 where
 * .5 is random and 1 is perfect.  Default is .65.
 * <LI> probsuccesssecondary-This is the probability of setting the values
 * based on an incorrect label.  This should be below .5 where .5
 * is random and 0 is perfect. Default is .4.
 * <LI> numwordsperlabel-Number of word to generate per label. Default is 1.
 * <LI> attrprefix-Prefix to use in the feature name.  Default is "w".
 * <LI> seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author mbilgic
 * @author namatag
 *
 */
public class NaiveBayesAttributes extends BaseConfigurable implements Decorator {
	protected String schemaid;
	protected String targetfeatureid;
	private String attrprefix = "w";
	protected int numlabels = 2;
	protected int numwordsperlabel = 1;
	private double probsuccessprimary = .65;
	private double probsuccesssecondary = .4;
	private int seed = 0;
	
	public void decorate(Graph g) {
		// Set parameters
		this.schemaid = this.getStringParameter("schemaid");
		this.targetfeatureid = this.getStringParameter("targetfeatureid");
		
		if(this.hasParameter("attrprefix")) {
			this.attrprefix = this.getStringParameter("attrprefix");
		}
		
		if(this.hasParameter("numwordsperlabel")) {
			this.numwordsperlabel = (int) this.getDoubleParameter("numwordsperlabel");
		}
		
		if(this.hasParameter("probsuccessprimary")) {
			this.probsuccessprimary = this.getDoubleParameter("probsuccessprimary");
		}
		
		if(this.hasParameter("probsuccesssecondary")) {
			this.probsuccesssecondary = this.getDoubleParameter("probsuccesssecondary");
		}
		
		if(this.hasParameter("seed")) {
			this.seed = (int) this.getDoubleParameter("seed");
		}
		Random rand = new Random(this.seed);
		
		// Get the label feature
		Schema schema = g.getSchema(schemaid);
		Feature f = schema.getFeature(targetfeatureid);
		if(!(f instanceof ExplicitCateg)) {
			throw new ConfigurationException("Unsupported feature type: "
					+f.getClass().getCanonicalName());
		}
		UnmodifiableList<String> cats = ((CategFeature) f).getAllCategories();
		numlabels = cats.size();
		
		// Update schema to support new attributes
		int totalwords = (int) (numlabels*numwordsperlabel);
		for(int i=0;i<totalwords;i++){
			// Add numeric features for the different words to add
			schema.addFeature(attrprefix+i, new ExplicitNum(new NumValue(0.0)));
		}
		g.updateSchema(schemaid, schema);
		
		// Go over all graph items, with the given schema, and add attributes
		Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fvalue = gi.getFeatureValue(targetfeatureid);
			if(fvalue.equals(FeatureValue.UNKNOWN_VALUE)) {
				throw new ConfigurationException("All labels must be known: "+
						gi+"."+targetfeatureid+"="+fvalue);
			}
			
			int labelindex = cats.indexOf(((CategValue) fvalue).getCategory());
			genAttributesNaiveBayes(gi, labelindex, rand);
		}
	}
	
	/**
	 * Generate attributes using naive bayes
	 * 
	 * @param gi Graph Item to generate attribute for
	 * @param c Label index
	 * @param rand Random number generator
	 */
	private void genAttributesNaiveBayes(GraphItem gi, int c, Random rand) {
		int totalWords = numlabels*numwordsperlabel;
		int[] wordCounts = new int[totalWords];

		int beginIndex = c*numwordsperlabel;
		int endIndex = c*numwordsperlabel + numwordsperlabel - 1;

		for(int i=0;i<totalWords;i++){
			double r = rand.nextDouble();
			if(i>=beginIndex && i<=endIndex) {
				if(r<probsuccessprimary){
					wordCounts[i]++;
				}
			} else {
				if(r<probsuccesssecondary){
					wordCounts[i]++;
				}
			}
		}
		
		for(int i=0;i<totalWords;i++) {
			gi.setFeatureValue(attrprefix+i, new NumValue(0.0 + wordCounts[i]));
		}
	}
}
