package linqs.gaia.model.oc.ncc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.log.Log;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.ListUtils;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * Logistic regression implementation using the methods described in:
 * <p>
 * Carpenter, Bob.
 * Lazy Sparse Stochastic Gradient Descent for Regularized Multinomial Logistic Regression. 
 * http://lingpipe.files.wordpress.com/2008/04/lazysgdregression.pdf, 2008.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> lr-Learning rate of model.  Default is 0.01.
 * <LI> rc-Regularization constant. Default is 0.001.
 * <LI> maxIteration-Maximum number of iterations.  Default is 50.
 * <LI> epsilon-Epsilon of model.  Default is 0.001.
 * <LI> annealingRate-Annealing rate.  Default is 100.
 * <LI>includefeatures-The parameters is treated as a
 * comma delimited list of feature ids and/or regex pattern
 * for feature IDs in the form REGEX:&lt;pattern&gt;
 * (e.g., color,size,REGEX:\\d,length).  By default,
 * all features, except the target feature, is used.
 * <LI>excludefeatures-Same format as include features
 * but any matching feature id and/or regex pattern
 * is removed.
 * <LI>printmodel-If "yes", print the model.
 * </UL>
 * 
 * @author mbilgic
 * @author namatag
 */
public class LogisticRegression extends BaseVBClassifier implements VBClassifier {

	private static final long serialVersionUID = 1L;

	private double[][] weights;

	private double lr = 0.01;
	private double rc = 0.001;

	public static boolean debug=false;

	private double annealingRate=100;
	private int maxIteration=50;// To overfit, set to 2000
	private double epsilon=0.001;// To overfit, set to 0.00

	private List<String> actualfeatureids;
	private UnmodifiableList<String> targetcategories;
	
	private Schema schema = null;
	
	private void initialize() {
		this.lr = 0.01;
		if(this.hasParameter("lr")) {
			this.lr = this.getDoubleParameter("lr");
		}

		this.rc = 0.001;
		if(this.hasParameter("rc")) {
			this.rc = this.getDoubleParameter("rc");
		}

		this.annealingRate=100;
		if(this.hasParameter("annealingRate")) {
			this.annealingRate = this.getDoubleParameter("annealingRate");
		}

		this.maxIteration=50;
		if(this.hasParameter("maxIteration")) {
			this.maxIteration=this.getIntegerParameter("maxIteration");
		}

		this.epsilon=0.001;
		if(this.hasParameter("epsilon")) {
			this.epsilon = this.getDoubleParameter("epsilon");
		}
	}
	
	/**
	 * Get the weights learned by the logistic regression model
	 * 
	 * @return Logistic regression weights
	 */
	public double[][] getWeights() {
		return weights;
	}
	
	/**
	 * Set the weights used by the logistic regression model.
	 * This provides the initial values when learning the logistic regression
	 * function.  If you want to load and use a previously learned logistic
	 * regression method instead, use {@link #loadModel(String)}.
	 * 
	 * @param weights Logistic regression weights
	 */
	public void setWeights(double[][] weights) {
		if(this.weights!=null) {
			throw new InvalidStateException("Weights cannot be manually set once they are already learned");
		}
		
		this.weights = weights;
	}

	@Override
	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid, String targetfeatureid, List<String> featureids) {
		this.initialize();

		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		this.featureids = featureids;
		
		// Check to prevent information leak
		if(featureids.contains(targetfeatureid)) {
			throw new InvalidStateException("Target feature id cannot be part"
					+" of the model feature ids: "+targetfeatureid+" in "+featureids);
		}

		Iterator<? extends Decorable> ditr = trainitems.iterator();
		if(!ditr.hasNext()) {
			throw new InvalidStateException("No training items provided");
		}

		// All training items are assumed to have the same exact type
		// of features for all featureids specified
		Decorable sampledi = ditr.next();
		this.schema = sampledi.getSchema();
		this.genActualFeatureIDs(schema);

		Feature targetfeature = schema.getFeature(targetfeatureid);

		if(!(targetfeature instanceof CategFeature)) {
			throw new InvalidStateException("Model only defined for predicting"
					+" categorical valued features: "+targetfeatureid+" is of type "
					+targetfeature.getClass().getCanonicalName());
		}

		CategFeature ctargetfeature = (CategFeature) targetfeature;
		this.targetcategories = ctargetfeature.getAllCategories();
		String[] categories = (String[]) this.targetcategories.toArray(new String[0]);
		
		int numCats = categories.length;
		int featureVSize = actualfeatureids.size();

		// Create a set of weights, one for each class.
		// Each weight set is the number of features + 1 (for the prior).
		if(weights==null) {
			weights = new double[numCats-1][featureVSize+1];
		}
		
		if(weights.length!=(numCats-1) || weights[0].length!=(featureVSize+1)) {
			throw new InvalidStateException("Specified weights not of the appropriate dimensions");
		}

		double newLr=lr;
		double ollh=-Double.MAX_VALUE;
		double llh=0;
		int e=0;

		// Until the maximum number of iterations is reached
		for(e=0;e<maxIteration;e++){
			newLr = lr/(1+e/annealingRate);

			// Go over all train items
			ditr = trainitems.iterator();
			while(ditr.hasNext()) {
				Decorable di = ditr.next();
				double[] inst = this.convertToInst(di, true);

				// For all the categories
				for(int c=0;c<numCats-1;c++){
					double prob=calcProb(inst, c);

					for(int f=0;f<=featureVSize;f++){
						double classVal = -1;

						int classIndex = (int) inst[inst.length-1];
						if(classIndex == c) {
							classVal=1;
						} else {
							classVal=0;
						}

						double error = -1;
						if(f==0) {
							error = 1*(classVal-prob);
						} else {
							error = inst[f-1]*(classVal-prob);
						}

						weights[c][f] = weights[c][f]+newLr*error - newLr*rc*weights[c][f];
					}
				}
			}

			llh = computeLogLikelihood(trainitems);

			Log.DEBUG("Iterator: "+e+"\tLLH: "+llh+"\tLR:\t"+newLr);
			double rdiff = relDiff(llh,ollh);	
			if(rdiff<epsilon && e>10){
				break;
			}

			ollh=llh;
		}

		// Go over all train items
		int trainitemssize = IteratorUtils.numIterable(trainitems);
		double aveProb = Math.pow(Math.E, llh/trainitemssize);

		if(Log.SHOWDEBUG) {
			StringBuffer buf = new StringBuffer();
			buf.append("LRaveProb:\t"+aveProb);

			double[] priors = computePseudoPriors();
			buf.append("Priors: [ ");
			for(double p: priors){
				buf.append(p+" ");
			}
			buf.append("]");

			Log.DEBUG(buf.toString());
		}

		if(aveProb<0.75){
			Log.WARN("LR: probably not enough learning (or difficult dataset)");
			Log.WARN("Used "+e+" of "+this.maxIteration+" iterations.");
			Log.WARN("Ave prob:\t"+aveProb);
			Log.WARN("Try adjusting the learning rate.");
		}

		// Print Model, if requested
		if(this.hasYesNoParameter("printmodel", "yes")) {
			Log.INFO("Learned Model:\n"+this.toString());
		}
	}

	/**
	 * Convert the decorable item to an array of doubles
	 * depending on the features requested.
	 * 
	 * @param di Decorable Item
	 * @return Array of doubles
	 */
	private double[] convertToInst(Decorable di, boolean includetarget) {
		double[] inst = new double[actualfeatureids.size()+1];
		
		int index = 0;
		for(String fid:this.featureids) {
			Feature f = schema.getFeature(fid);
			
			if(!(f instanceof CompositeFeature)) {
				index = this.setInstanceValue(inst, index, f, di.getFeatureValue(fid));
			} else {
				// Handle Composite features
				CompositeFeature cvf = (CompositeFeature) f;
				UnmodifiableList<SimplePair<String, CVFeature>> mvfeatures = cvf.getFeatures();
				
				CompositeValue multifval = (CompositeValue) di.getFeatureValue(fid);
				UnmodifiableList<FeatureValue> allmfvals = multifval.getFeatureValues();
				
				for(int i=0; i< mvfeatures.size(); i++) {
					SimplePair<String, CVFeature> sp = mvfeatures.get(i);
					Feature currf = sp.getSecond();
					index = this.setInstanceValue(inst, index, currf, allmfvals.get(i));
				}
			}
		}

		// Set the last value to the index of the category
		if(includetarget) {
			String cat = ((CategValue) di.getFeatureValue(targetfeatureid)).getCategory();
			inst[inst.length-1] = this.targetcategories.indexOf(cat);
		}

		return inst;
	}
	
	public int setInstanceValue(double[] inst, int index, Feature f, FeatureValue fval) {
		if(f instanceof NumFeature) {
			inst[index] = ((NumValue) fval).getNumber();
			index++;
		} else if(f instanceof StringFeature) {
			inst[index] = Double.parseDouble(
					((StringValue) fval).getString());
			index++;
		} else if(f instanceof CategFeature) {
			String[] cfcats = (String[])
				((CategFeature) f).getAllCategories().toArray(new String[0]);
			String cat = ((CategValue) fval).getCategory();
			for(String cfcat:cfcats) {
				inst[index] = cat.equals(cfcat) ? 1 : 0;
				index++;
			}
		} else {
			throw new UnsupportedTypeException("Unsupported feature type: "+
					f.getClass().getCanonicalName());
		}
		
		return index;
	}

	/**
	 * Generate the actual list of features used.
	 * This may vary from the feature ids original given
	 * so that we can handle explicitly non-numeric features.
	 * 
	 * @param schema
	 */
	private void genActualFeatureIDs(Schema schema) {
		actualfeatureids = new ArrayList<String>();
		
		for(String fid:featureids) {
			Feature f = schema.getFeature(fid);
			
			if(!(f instanceof CompositeFeature)) {
				this.handleFeature(fid, f);
			} else {
				// Handle Composite features
				CompositeFeature cvf = (CompositeFeature) f;
				UnmodifiableList<SimplePair<String, CVFeature>> mvfeatures = cvf.getFeatures();
				for(SimplePair<String, CVFeature> sp: mvfeatures) {
					String newname = fid+":"+sp.getFirst();
					Feature currf = sp.getSecond();
					this.handleFeature(newname, currf);
				}
			}
		}
	}
	
	public void handleFeature(String fid, Feature f) {
		if(f instanceof NumFeature){
			actualfeatureids.add(fid);
		} else if(f instanceof StringFeature){
			actualfeatureids.add(fid);
		} else if(f instanceof CategFeature) {
			CategFeature cf = (CategFeature) f;
			UnmodifiableList<String> cfcats = cf.getAllCategories();

			// Add a different feature name for each possible category value
			// Categorical features are binarized where
			// for each category, we have -1 if its the same and 1 if not.
			for(String cfcat:cfcats) {
				actualfeatureids.add(fid+"="+cfcat);
			}
		} else {
			throw new UnsupportedTypeException("Unsupported feature type: "
					+f.getClass().getCanonicalName());
		}
	}

	/**
	 * Compute pseudo priors
	 */
	private double[] computePseudoPriors() {
		int numClasses = this.weights.length+1;

		double[] priors = new double[numClasses];

		double sum=0;

		for(int i=0;i<numClasses-1;i++){
			priors[i] = Math.exp(weights[i][0]);
			sum += priors[i];
		}

		priors[numClasses-1] = 1;
		sum += 1;

		for(int i=0;i<numClasses;i++){
			priors[i] /= sum;
		}


		return priors;
	}

	/**
	 * Calculate relative difference
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private double relDiff(double x, double y) {		
		return ((Math.abs(x-y))/(Math.abs(x)+Math.abs(y)));
	}

	/**
	 * Compute log likelihood of all predictions
	 * 
	 * @param data
	 * @return Log likelihood
	 */
	private double computeLogLikelihood(Iterable<? extends Decorable> trainitems) {
		double logSum=0;

		Iterator<? extends Decorable> ditr = trainitems.iterator();
		while(ditr.hasNext()) {
			Decorable di = ditr.next();
			double[] inst = this.convertToInst(di, true);
			int classIndex = (int) inst[inst.length-1];
			double prob = calcProb(inst,classIndex);
			logSum += Math.log(prob);			
		}

		return logSum;
	}

	/**
	 * Calculates the probability of instance's class being c,
	 * i.e., P(inst.class=c|inst,weights).
	 * 
	 * @param inst
	 * @param c
	 * @return
	 */
	private double calcProb(double[] inst, int c) {
		int numCats = weights.length+1;

		double numerator=0, denominator=1;

		for(int j=0;j<numCats-1;j++){
			denominator +=  Math.exp(weights[j][0]+dotProduct(j,inst));
		}

		if(c != numCats-1){
			numerator = Math.exp(weights[c][0]+dotProduct(c, inst));
		}
		else{
			numerator = 1;
		}

		if(Double.isInfinite(denominator)){
			if(Double.isInfinite(numerator))
				return 1;
			else
				return 0;
		}

		if(Double.isNaN(denominator) 
				|| Double.isNaN(numerator) 
				|| Double.isInfinite(denominator) 
				|| Double.isInfinite(numerator)){
			throw new InvalidStateException("Invalid numerator or denominator: "
					+numerator+"/"+denominator);
		}

		return numerator/denominator;
	}

	/**
	 * Compute the dot product for the given instance
	 * 
	 * @param c
	 * @param inst
	 * @return Dot product
	 */
	private double dotProduct(int c, double[] inst) {
		double sum = 0;

		for(int i=1;i<inst.length;i++){ // assumes that the last one is the class feature
			double fvalue = inst[i-1];
			sum += weights[c][i]*fvalue;
		}

		if(Double.isInfinite(sum) || Double.isNaN(sum)){
			Log.WARN("Infinite/NAN sum");
		}

		return sum;
	}

	@Override
	public FeatureValue predict(Decorable testitem) {
		// Added in case the model was loaded from file
		if(this.schema == null) {
			this.schema = testitem.getSchema();
		}
		
		double[] inst = this.convertToInst(testitem, false);

		int numCats = weights.length+1;
		double[] probs = new double[numCats];

		for(int i=0;i<numCats;i++){
			probs[i] = calcProb(inst, i);
		}

		int catindex = ArrayUtils.maxValueIndex(probs);

		return new CategValue(this.targetcategories.get(catindex), probs);
	}

	/**
	 * Create string representation of model, including the weights
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Logistic Regression:\n");
		
		for(int i=0;i<weights.length;i++){
			buf.append("LR Weights: CLASS="+this.targetcategories.get(i)+"\n");
			for(int j=0;j < weights[i].length;j++){
				if(i!=0 && j!=0) {
					buf.append(" ");
				}
				
				if(j==0) {
					buf.append("PRIOR"+"\t"+weights[i][j]+"\n");
				} else {
					buf.append(actualfeatureids.get(j-1)+"\t"+weights[i][j]+"\n");
				}
			}
			
			buf.append("\n");
		}		

		return buf.toString();
	}

	@Override
	public void saveVBOC(String directory) {
		// Save the local features
		this.setParameter("saved-targetcategories",
				IteratorUtils.iterator2string(this.targetcategories.iterator(), ","));
		this.setParameter("saved-actualfeatureids",
				ListUtils.list2string(this.actualfeatureids, ","));
		
		this.setParameter("saved-weight-i", ""+this.weights.length);
		this.setParameter("saved-weight-j", ""+this.weights[0].length);
		
		String weightstring = null;
		for(int i=0;i<this.weights.length;i++){
			for(int j=0;j < this.weights[i].length;j++){
				if(weightstring==null) {
					weightstring = "";
				} else {
					weightstring += ",";
				}
				
				weightstring += this.weights[i][j];
			}
		}
		
		this.setParameter("saved-weights", weightstring);
	}

	@Override
	public void loadVBOC(String directory) {
		this.targetcategories = new UnmodifiableList<String>(Arrays.asList(
				this.getStringParameter("saved-targetcategories").split(",")));
		
		this.actualfeatureids = Arrays.asList(
				this.getStringParameter("saved-actualfeatureids").split(","));
		
		int isize =  this.getIntegerParameter("saved-weight-i");
		int jsize =  this.getIntegerParameter("saved-weight-j");
		this.weights = new double[isize][jsize];
		int expectedsize = isize * jsize;
		
		String[] weightstring = this.getStringParameter("saved-weights").split(",");
		if(weightstring.length != expectedsize) {
			throw new ConfigurationException("Invalid Weight String: Length="
					+weightstring.length+" expected "+expectedsize);
		}
		int index = 0;
		for(int i=0; i<isize; i++) {
			for(int j=0; j<jsize; j++) {
				this.weights[i][j]=Double.parseDouble(weightstring[index]);
				index++;
			}
		}
		
		// Remove parameters to save space
		this.removeParameter("saved-weights");
		
		this.initialize();
	}
}

