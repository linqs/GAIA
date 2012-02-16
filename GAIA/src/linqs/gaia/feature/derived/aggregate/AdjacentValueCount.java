package linqs.gaia.feature.derived.aggregate;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedComposite;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.derived.composite.CVNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.util.KeyedSum;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * Do a weighted count (or percent) of neighbors with a given label.
 * Unlike {@link NeighborValueCount}, this is designed specifically for aggregating
 * over adjacent items, specially cases where the count is weighted (i.e., multiplied)
 * by some value defined over the incident items either as a numeric weight value or
 * as the existence in some categorical valued feature (see {@link LinkPredictor#EXISTENCEFEATURE}).
 * <p>
 * Note: An exception is throw if an item is adjacent to the specified decorable item
 * by more than one incident item (i.e., two nodes have multiple edges of the specified type between them
 * or if two edges share more than 1 node).
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> featureid-Feature ID of feature to aggregate over
 * <LI> featureschemaid-Schema ID of feature to aggregate over
 * <LI> incidentsid-Schema ID of incident nodes to consider
 * <LI> adjtype-Type of adjacency to consider.  Options are: sourceonly,targetonly,all.
 * <LI> aspercent-If yes, use the percentages instead of the counts.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> weightfid-Feature ID of numeric valued weight feature of incident items.
 * If neither weightfid or existfid are specified, we use a weight of 1.
 * <LI> existfid-Feature ID of categorical valued existence feature of the incident items.
 * If neither weightfid or existfid are specified, we use a weight of 1.
 * <LI> weightbylabelprob-If yes, multiply the probability of existence to also take into
 * account the probability of a given label.  Default is no.
 * <LI> allowmultadj-If yes, allow that items maybe adjacent through more than
 * one incident item.  In that case, the weight is taken for over all incident items
 * separately.  Default is no and, if encountered, will result in an exception.
 * </UL>
 * 
 * @author namatag
 *
 */
public class AdjacentValueCount extends DerivedComposite implements GraphDependent {
	
	protected UnmodifiableList<SimplePair<String, CVFeature>> features = null;
	protected String featureid;
	protected String featureschemaid;
	private String incidentsid = null;
	private String weightfid = null;
	private String existfid = null;
	private String type = null;
	private boolean aspercent = false;
	private boolean weightbylabelprob = false;
	private boolean allowmultadj = false;
	private Map<String,Integer> cat2index = null;
	protected Graph g;
	
	private static NumValue numvalue0 = new NumValue(0.0);
	
	public FeatureValue calcFeatureValue(Decorable di) {
		// Get counts of features from neighbors
		KeyedSum<String> sum = this.getCount(di);
		double total = sum.totalSum();
		List<FeatureValue> fvalues = new LinkedList<FeatureValue>();
		for(SimplePair<String, CVFeature> pair:features) {
			double value = sum.getSum(pair.getFirst());
			
			if(aspercent) {
				if(total==0) {
					value = 0;
				} else {
					value = value / total;
				}
			}
			
			if(value==0) {
				fvalues.add(numvalue0);
			} else {
				fvalues.add(new NumValue(value));
			}
		}
		
		return (FeatureValue) new CompositeValue(fvalues);
	}
	
	protected KeyedSum<String> getCount(Decorable di) {
		if(!(di instanceof GraphItem)) {
			throw new UnsupportedTypeException("Feature only defined for graph items: "
					+di.getClass().getCanonicalName());
		}
		
		GraphItem gi = (GraphItem) di;
		KeyedSum<String> sum = new KeyedSum<String>();
		
		Iterator<? extends GraphItem> iitr = null;
		if(type==null || type.equals("all")) {
			iitr = gi.getIncidentGraphItems(incidentsid);
		} else if(type.equals("sourceonly")) {
			iitr = ((Node) gi).getEdgesWhereTarget(incidentsid);
		} else if(type.equals("targetonly")) {
			iitr = ((Node) gi).getEdgesWhereSource(incidentsid);
		} else {
			iitr = gi.getIncidentGraphItems(incidentsid);
		}
		
		Set<GraphItem> processed = new HashSet<GraphItem>();
		while(iitr.hasNext()) {
			GraphItem igi = iitr.next();
			
			// Get weight
			double weight = 1;
			if(weightfid!=null) {
				weight = ((NumValue) igi.getFeatureValue(weightfid)).getNumber().intValue();
			} else if(existfid!=null) {
				weight = ((CategValue) igi.getFeatureValue(existfid)).getProbs()[LinkPredictor.EXISTINDEX];
			}
			
			// Get incident items of the specified schema
			Iterator<GraphItem> aitr = igi.getIncidentGraphItems(featureschemaid);
			while(aitr.hasNext()) {
				GraphItem agi = aitr.next();
				
				// Skip self
				if(agi.equals(gi)) {
					continue;
				}
				
				if(!allowmultadj && processed.contains(agi)) {
					throw new UnsupportedTypeException("Cannot be incident in multiple ways: "+agi);
				}
				
				FeatureValue fvalue = agi.getFeatureValue(featureid);
				if(fvalue.equals(FeatureValue.UNKNOWN_VALUE)) {
					// Don't count neighbors with missing values
					continue;
				} else {
					CategValue cvalue = (CategValue) fvalue;
					String cat = cvalue.getCategory();
					sum.add(cat, weightbylabelprob ? (weight*cvalue.getProbs()[cat2index.get(cat)]) : weight);
				}
				
				processed.add(agi);
			}
		}
		
		return sum;
	}
	
	public UnmodifiableList<SimplePair<String, CVFeature>> getFeatures() {
		this.initializeFeature();
		
		return features;
	}
	
	protected void initialize() {
		featureid = this.getStringParameter("featureid");
		featureschemaid = this.getStringParameter("featureschemaid");
		incidentsid = this.getStringParameter("incidentsid");
		type = this.getCaseParameter("adjtype", new String[]{"sourceonly","targetonly","all"}, "all");
		aspercent = this.hasYesNoParameter("aspercent", "yes");
		
		weightfid = this.getStringParameter("weightfid", null);
		existfid = this.getStringParameter("existfid", null);
		weightbylabelprob = this.getYesNoParameter("weightbylabelprob","no");
		
		allowmultadj = this.getYesNoParameter("allowmultadj", "no");
		
		if(this.g == null) {
			throw new InvalidStateException("Graph item not set");
		}
		
		Schema schema = g.getSchema(featureschemaid);
		Feature f = schema.getFeature(featureid);
		
		if(!(f instanceof CategFeature)) {
			throw new ConfigurationException("Feature expected to be categorical: "
					+f.getClass().getCanonicalName());
		}
		
		UnmodifiableList<String> cats = ((CategFeature) f).getAllCategories();
		this.cat2index = new HashMap<String,Integer>(cats.size());
		List<SimplePair<String, CVFeature>> catpairs = new LinkedList<SimplePair<String, CVFeature>>();
		int index = 0;
		for(String cat:cats) {
			catpairs.add(new SimplePair<String,CVFeature>(cat, new CVNum()));
			cat2index.put(cat, index);
			index++;
		}
		
		this.features = new UnmodifiableList<SimplePair<String, CVFeature>>(catpairs);
	}

	public void setGraph(Graph g) {
		this.g = g;
	}

	public int numFeatures() {
		return this.getFeatures().size();
	}
}
