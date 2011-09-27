package linqs.gaia.model.oc.ncc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import libsvm.*;

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
import linqs.gaia.util.SimpleTimer;
import linqs.gaia.util.UnmodifiableList;

/**
 * A wrapper for LibSVM library available from:
 * <p>
 * Yasser EL-Manzalawy (2005). WLSVM. URL http://www.cs.iastate.edu/~yasser/wlsvm/.
 * <p>
 * Chih-Chung Chang, Chih-Jen Lin (2001).
 * LIBSVM - A Library for Support Vector Machines.
 * URL http://www.csie.ntu.edu.tw/~cjlin/libsvm/. 
 * <p>
 * Note: The libsvm.jar file needs to be in the classpath for use.
 * 
 * Optional Parameters:
 * <UL>
 * <LI>s-Set type of SVM (default 0)
 * <UL>
 * <LI>0-C-SVC
 * <LI>1-nu-SVC
 * <LI>2-one-class SVM
 * <LI>3-epsilon-SVR
 * <LI>4-nu-SVR
 * </UL>
 * 
 * <LI>t-Set type of kernel function (default 0)
 * <UL>
 * <LI>0-linear: u'*v
 * <LI>1-polynomial: (gamma*u'*v + coef0)^degree
 * <LI>2-radial basis function: exp(-gamma*|u-v|^2)
 * <LI>3-sigmoid: tanh(gamma*u'*v + coef0)
 * <LI>4-precomputed kernel (kernel values in training_set_file)
 * </UL>
 * 
 * <LI>d-Set degree in kernel function (default 3)
 * <LI>g-Set gamma in kernel function (default 1/num_features)
 * <LI>r-Set coef0 in kernel function (default 0)
 * <LI>c-Set the parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1)
 * <LI>n-Set the parameter nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)
 * <LI>p-Set the epsilon in loss function of epsilon-SVR (default 0.1)
 * <LI>m-Set cache memory size in MB (default 100)
 * <LI>e-Set tolerance of termination criterion (default 0.001)
 * <LI>h-Whether to use the shrinking heuristics, 0 or 1 (default 1)
 * <LI>b-Whether to train a SVC or SVR model for probability estimates, 0 or 1 (default 1)
 * <LI>normalize-If "yes", normalize the data.  Default is "no".
 * <LI>setunknownas0-If "yes", unknown values are set to 0 in the double array
 * passed to SVM.  Default is "no".
 * <LI>silent-If "no", print output during training.  Default is "yes".
 * </UL>
 * 
 * @author namatag
 *
 */
public class LibSVMClassifier extends BaseVBClassifier implements VBClassifier {
	private static final long serialVersionUID = 1L;

	private boolean initialize = true;
	private svm_model model;
	private svm_parameter param = null;
	private int predict_probability = 0;
	private int svm_type;
	private int nr_class;
	private int[] libsvminternallabels;
	private svm_print_interface print_func = svm_print_null;
	private static svm_print_interface svm_print_null = new svm_print_interface()
	{
		public void print(String s) {}
	};

	private List<String> actualfeatureids;
	private List<Boolean> actualfeatureisnumeric;
	private UnmodifiableList<String> targetcategories;
	private int numcats;

	private boolean normalize = false;
	private boolean setunknownas0 = false;
	private double[] minvalue = null;
	private double[] maxvalue = null;
	
	@Override
	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid, String targetfeatureid,
			List<String> featureids) {
		if(initialize) {
			this.initialize();
		}

		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		this.featureids = featureids;
		SimpleTimer timer = new SimpleTimer();

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
		Schema schema = sampledi.getSchema();
		this.genActualFeatureIDs(schema);
		Feature targetfeature = schema.getFeature(targetfeatureid);

		if(!(targetfeature instanceof CategFeature)) {
			throw new InvalidStateException("Model only defined for predicting"
					+" categorical valued features: "+targetfeatureid+" is of type "
					+targetfeature.getClass().getCanonicalName());
		}

		CategFeature ctargetfeature = (CategFeature) targetfeature;
		this.targetcategories = ctargetfeature.getAllCategories();
		numcats = this.targetcategories.size();
		Map<String,Integer> cat2index = new HashMap<String,Integer>();
		for(int i=0; i<numcats; i++) {
			cat2index.put(targetcategories.get(i), i);
		}

		int featureVSize = actualfeatureids.size();
		if(normalize) {
			this.minvalue = new double[featureVSize];
			for(int i=0; i<featureVSize; i++) {
				this.minvalue[i] = Double.POSITIVE_INFINITY;
			}
			
			this.maxvalue = new double[featureVSize];
			for(int i=0; i<featureVSize; i++) {
				this.maxvalue[i] = Double.NEGATIVE_INFINITY;
			}
			
			// Iterate over all items to get the min and max for each item
			ditr = trainitems.iterator();
			while(ditr.hasNext()) {
				Decorable di = ditr.next();
				double[] inst = this.convertToInst(di);
				
				for(int j=0;j<featureVSize;j++)
				{
					// Normalization only applicable for numeric features
					if(!this.actualfeatureisnumeric.get(j)) {
						continue;
					}
					
					// Get the min and max of each value
					if(this.minvalue[j]>inst[j]) {
						this.minvalue[j] = inst[j];
					}
					
					if(this.maxvalue[j]<inst[j]) {
						this.maxvalue[j] = inst[j];
					}
				}
			}
		}
		
		Vector<Double> vy = new Vector<Double>();
		Vector<svm_node[]> vx = new Vector<svm_node[]>();
		
		int gicounter = 0;
		int numinstances = IteratorUtils.numIterable(trainitems);
		SimpleTimer st = new SimpleTimer();
		ditr = trainitems.iterator();
		while(ditr.hasNext()) {
			Decorable di = ditr.next();
			double[] inst = this.convertToInst(di);
			
			// Normalize instance, if requested
			if(normalize) {
				for(int j=0;j<inst.length;j++)
				{
					if(!this.actualfeatureisnumeric.get(j)) {
						continue;
					}
					
					if(maxvalue[j]==minvalue[j]) {
						inst[j] = 0;
					} else {
						double denom = maxvalue[j] - minvalue[j];
						inst[j] = (inst[j] - minvalue[j]) / denom;
					}
				}
			}

			CategValue label = (CategValue) di.getFeatureValue(targetfeatureid);
			vy.addElement(new Double(cat2index.get(label.getCategory())));
			
			// Num non-zero values
			int numnonzero = 0;
			for(int j=0;j<featureVSize; j++) {
				if(inst[j]!=0) {
					numnonzero++;
				}
			}
			
			svm_node[] x = new svm_node[numnonzero];
			int index = 0;
			for(int j=0;j<featureVSize;j++)
			{
				if(inst[j]!=0) {
					x[index] = new svm_node();
					x[index].index = j;
					x[index].value = inst[j];
					index++;
				}
			}

			vx.addElement(x);
			
			if(Log.SHOWDEBUG){
				gicounter++;
				if((gicounter%1000==0 || !ditr.hasNext())) {
					Log.DEBUG("Converting GI: "+gicounter
							+" of "+numinstances
							+" Time="+st.timeLapse(true));
					st.start();
				}
			}
		}
		
		svm_problem prob = new svm_problem();
		prob.l = vy.size();
		prob.x = new svm_node[prob.l][];
		for(int i=0;i<prob.l;i++) {
			prob.x[i] = vx.elementAt(i);
		}

		prob.y = new double[prob.l];
		for(int i=0;i<prob.l;i++) {
			prob.y[i] = vy.elementAt(i);
		}

		if(!this.hasParameter("g") && param.gamma == 0 && featureVSize > 0) {
			param.gamma = 1.0/featureVSize;
		}

		if(param.kernel_type == svm_parameter.PRECOMPUTED) {
			for(int i=0;i<prob.l;i++)
			{
				if (prob.x[i][0].index != 0)
				{
					throw new ConfigurationException
						("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
				}
				
				if ((int)prob.x[i][0].value <= 0 || (int)prob.x[i][0].value > featureVSize)
				{
					throw new ConfigurationException
						("Wrong input format: sample_serial_number out of range\n");
				}
			}
		}

		model = svm.svm_train(prob,param);
		svm_type=svm.svm_get_svm_type(model);
		nr_class=svm.svm_get_nr_class(model);
		libsvminternallabels=new int[nr_class];
		svm.svm_get_labels(model,libsvminternallabels);
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Completed training LibSVM Classifier: "+timer.timeLapse());
			Log.DEBUG(printParams(this.param));
		}
	}
	
	 public static String printParams(svm_parameter svmparams) {
		  return "LibSVM Parameters: C="+svmparams.C+","
			  + "cache_size="+svmparams.cache_size+","
			  + "coerf0=" + svmparams.coef0+","
			  + "degree=" + svmparams.degree+","
			  + "eps=" + svmparams.eps+","
			  + "gamma=" + svmparams.gamma+","
			  + "kernel_type=" + svmparams.kernel_type+","
			  + "nr_weight=" + svmparams.nr_weight+","
			  + "nu=" + svmparams.nu+","
			  + "p=" + svmparams.p+","
			  + "probability=" + svmparams.probability+","
			  + "shrinking=" + svmparams.shrinking+","
			  + "svm_type=" + svmparams.svm_type+","
			  + "weight=[" + ArrayUtils.array2String(svmparams.weight, "-")+"],"
			  + "weight_label=["  + ArrayUtils.array2String(svmparams.weight_label, "-")+"]";
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
		actualfeatureisnumeric = new ArrayList<Boolean>();

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
			actualfeatureisnumeric.add(true);
		} else if(f instanceof StringFeature){
			actualfeatureids.add(fid);
			actualfeatureisnumeric.add(false);
		} else if(f instanceof CategFeature) {
			CategFeature cf = (CategFeature) f;
			UnmodifiableList<String> cfcats = cf.getAllCategories();

			// Add a different feature name for each possible category value
			// Categorical features are binarized where
			// for each category, we have -1 if its the same and 1 if not.
			for(String cfcat:cfcats) {
				actualfeatureids.add(fid+"="+cfcat);
				actualfeatureisnumeric.add(false);
			}
		} else {
			throw new UnsupportedTypeException("Unsupported feature type: "
					+f.getClass().getCanonicalName());
		}
	}

	/**
	 * Convert the decorable item to an array of doubles
	 * depending on the features requested.
	 * 
	 * @param di Decorable Item
	 * @return Array of doubles
	 */
	private double[] convertToInst(Decorable di) {
		double[] inst = new double[actualfeatureids.size()];
		Schema schema = di.getSchema();
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

		return inst;
	}

	public int setInstanceValue(double[] inst, int index, Feature f, FeatureValue fval) {
		if(f instanceof NumFeature) {
			// For unknown numeric values, just set as 0
			if(setunknownas0 && fval.equals(FeatureValue.UNKNOWN_VALUE)) {
				inst[index] = 0;
			} else {
				inst[index] = ((NumValue) fval).getNumber();
			}
			
			index++;
		} else if(f instanceof StringFeature) {
			// For unknown numeric values, just set as 0
			if(setunknownas0 && fval.equals(FeatureValue.UNKNOWN_VALUE)) {
				inst[index] = 0;
			} else {
				inst[index] = Double.parseDouble(
						((StringValue) fval).getString());
			}
			
			index++;
		} else if(f instanceof CategFeature) {
			String[] cfcats = (String[])
			((CategFeature) f).getAllCategories().toArray(new String[0]);
			
			if(setunknownas0 && fval.equals(FeatureValue.UNKNOWN_VALUE)) {
				// For unknown categorical values, just return 0 for all categories.
				for(int i=0; i<cfcats.length; i++) {
					inst[index] = 0;
					index++;
				}
			} else {
				String cat = ((CategValue) fval).getCategory();
				for(String cfcat:cfcats) {
					inst[index] = cat.equals(cfcat) ? 1 : 0;
					index++;
				}
			}
		} else {
			throw new UnsupportedTypeException("Unsupported feature type: "+
					f.getClass().getCanonicalName());
		}

		return index;
	}

	@Override
	public FeatureValue predict(Decorable testitem) {
		double[] prob_estimates=null;
		double[] inst = this.convertToInst(testitem);
		int featureVSize = inst.length;

		if(predict_probability == 1)
		{
			if(svm_type == svm_parameter.EPSILON_SVR ||
					svm_type == svm_parameter.NU_SVR) {
				Log.DEBUG("Prob. model for test data: target value" +
						" = predicted value + z,\nz:" +
						" Laplace distribution e^(-|z|/sigma)/(2sigma),sigma="
						+svm.svm_get_svr_probability(model)+"\n");
			} else {
				prob_estimates = new double[nr_class];
			}
		}
		
		// Num non-zero values
		int numnonzero = 0;
		for(int j=0;j<featureVSize; j++) {
			if(inst[j]!=0) {
				numnonzero++;
			}
		}
		
		int index = 0;
		svm_node[] x = new svm_node[numnonzero];
		for(int j=0;j<featureVSize;j++)
		{
			if(inst[j]!=0) {
				x[index] = new svm_node();
				x[index].index = j;
				
				if(normalize && actualfeatureisnumeric.get(j)) {
					// Only normalize numeric values
					if(maxvalue[j]==minvalue[j]) {
						x[index].value = 0;
					} else {
						double denom = maxvalue[j]-minvalue[j];
						x[index].value = (inst[j]-minvalue[j])/denom;
					}
				} else {
					x[index].value = inst[j];
				}
				
				index++;
			}
		}

		double v;
		double[] probs = new double[numcats];
		if (predict_probability==1 && (svm_type==svm_parameter.C_SVC || 
				svm_type==svm_parameter.NU_SVC)) {
			v = svm.svm_predict_probability(model,x,prob_estimates);
			for(int j=0;j<nr_class;j++) {
				probs[libsvminternallabels[j]]=prob_estimates[j];
			}
			
			// When returning probabilities,
			// always return the label with the highest probability.
			if(v!=libsvminternallabels[ArrayUtils.maxValueIndex(prob_estimates)]) {
				throw new InvalidStateException("Predicted label and most likely label are not the same: "
						+this.targetcategories.get((int) v)
						+" instead of "
						+this.targetcategories.get(libsvminternallabels[ArrayUtils.maxValueIndex(prob_estimates)])
						+" with probabilities "+Arrays.toString(probs)
						);
			}
		} else {
			v = svm.svm_predict(model,x);
			probs[(int) v] = 1;
		}
		
		return new CategValue(this.targetcategories.get((int) v), probs);
	}

	private void initialize() {
		initialize = false;
		normalize = this.hasYesNoParameter("normalize", "yes");
		setunknownas0 = this.hasYesNoParameter("setunknownas0", "yes");
		
		param = new svm_parameter();

		// default values
		param.svm_type = this.getIntegerParameter("s",0);
		param.kernel_type = this.getIntegerParameter("t", 0);
		param.degree = this.getIntegerParameter("d",3);
		param.gamma = this.getDoubleParameter("g",0);	// 1/num_features
		param.coef0 = this.getDoubleParameter("r",0);
		param.nu = this.getDoubleParameter("n",0.5);
		param.cache_size = this.getIntegerParameter("m",40);
		param.C = this.getDoubleParameter("c",1);
		param.eps = this.getDoubleParameter("e",1e-3);
		param.p = this.getDoubleParameter("p",0.1);
		param.shrinking = this.getIntegerParameter("h",1);
		param.probability = this.getIntegerParameter("b",1);
		predict_probability = param.probability;
		if(this.hasYesNoParameter("silent", "no")) {
			print_func = null;
		}
		svm.svm_set_print_string_function(print_func);

		// Do not support weights for now
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];
	}

	@Override
	public void loadVBOC(String directory) {
		this.targetcategories = new UnmodifiableList<String>(Arrays.asList(
				this.getStringParameter("saved-targetcategories").split(",")));

		this.actualfeatureids = Arrays.asList(
				this.getStringParameter("saved-actualfeatureids").split(","));

		// Initialize from saved parameters
		this.initialize();
		
		if(this.hasParameter("saved-minvalues")) {
			this.minvalue = ArrayUtils.string2ArrayDouble(
					this.getStringParameter("saved-minvalues"),",");
		}
		
		if(this.hasParameter("saved-maxvalues")) {
			this.maxvalue = ArrayUtils.string2ArrayDouble(
					this.getStringParameter("saved-maxvalues"),",");
		}

		// Overwrite, as needed, from saved values
		try {
			model = svm.svm_load_model(directory+File.separator+"libsvm.model");
			svm_type=svm.svm_get_svm_type(model);
			nr_class=svm.svm_get_nr_class(model);
			libsvminternallabels=new int[nr_class];
			svm.svm_get_labels(model,libsvminternallabels);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void saveVBOC(String directory) {
		// Save the local features
		this.setParameter("saved-targetcategories",
				IteratorUtils.iterator2string(this.targetcategories.iterator(), ","));
		this.setParameter("saved-actualfeatureids",
				ListUtils.list2string(this.actualfeatureids, ","));
		
		if(this.minvalue!=null) {
			this.setParameter("saved-minvalues", ArrayUtils.array2String(this.minvalue,","));
		}
		
		if(this.maxvalue!=null) {
			this.setParameter("saved-maxvalues", ArrayUtils.array2String(this.maxvalue,","));
		}
		
		// Save Parameters
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");

		try {
			// Save LIBSVM Model
			svm.svm_save_model(directory+File.separator+"libsvm.model",model);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
