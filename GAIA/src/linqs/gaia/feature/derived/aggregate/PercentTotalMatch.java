package linqs.gaia.feature.derived.aggregate;

import java.util.ArrayList;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.BaseDerived;
import linqs.gaia.feature.derived.neighbor.Incident;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.Dynamic;

/**
 * Given neighbors and features for those neighbors,
 * this returns the proportion of feature values which
 * match across all neighbors.
 * 
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> featureschemaid-Feature schema ID of the connected graph items to consider.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> neigborclass-Class of neighbor implementation to use when calculating
 * neighborhood.  Default is linqs.gia.feature.derived.neighbor.Incident.
 * <LI>includefeatures-The parameters is treated as a
 * comma delimited list of feature ids and/or regex pattern
 * for feature IDs in the form REGEX:&lt;pattern&gt;
 * (e.g., color,size,REGEX:\\d,length).  Default is to use
 * all the features defined for the specified schema id.
 * <LI>excludefeatures-Same format as include features
 * but any matching feature id and/or regex pattern
 * is removed.
 * <LI> invert-If yes, instead of returning the percent of total match,
 * return the percent total not matched.  Defaults is no.
 * </UL>
 * 
 * @author namatag
 *
 */
public class PercentTotalMatch extends BaseDerived implements 
	DerivedFeature, GraphDependent, NumFeature {
	private List<String> featureids;
	private String featureschemaid;
	private Graph g;
	
	private String neighborclass = Incident.class.getCanonicalName();
	private Neighbor neighbor = null;
	private boolean invert = false;
	private boolean asbinary = false;
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		this.initialize();
		
		if(featureids.isEmpty()) {
			throw new ConfigurationException("No features defined");
		}
		
		if(!(di instanceof GraphItem)) {
			throw new UnsupportedTypeException("Feature only defined for graph items: "
					+di.getClass().getCanonicalName());
		}
		
		GraphItem gi = (GraphItem) di;
		
		Iterable<GraphItem> currneighbors = this.neighbor.getNeighbors(gi);
		if(!currneighbors.iterator().hasNext()) {
			// If there are no neighbors defined, just return no matches
			return new NumValue(0);
		}
		
		double nummatch = 0;
		double totalnum = 0;
		// Get if features of connected match
		for(String fid:featureids) {
			// Iterate all items connected to this items and get feature values
			List<FeatureValue> values = new ArrayList<FeatureValue>();
			for(GraphItem conn : currneighbors) {
				values.add(conn.getFeatureValue(fid));
			}
			
			FeatureValue fv = values.get(0);
			if(fv instanceof CompositeValue) {
				// A composite feature must always return a composite value
				CompositeValue compfv = (CompositeValue) fv;
				int numcompvals = compfv.getFeatureValues().size();
				
				// Handle composite values
				for(int i=0; i<numcompvals;i++) {
					// Go through each composite value one by one
					boolean match = true;
					FeatureValue firstfv = null;
					for(FeatureValue currcompfv:values) {
						FeatureValue currfv = ((CompositeValue) currcompfv).getFeatureValues().get(i);
						
						if(currfv.equals(FeatureValue.UNKNOWN_VALUE)) {
							match = false;
							break;
						}
						
						if(firstfv==null) {
							firstfv=currfv;
						} else {
							if(!currfv.equals(firstfv)) {
								match = false;
								break;
							}
						}
					}
					
					// Count number of matches
					if(match) {
						nummatch++;
					}
					
					totalnum++;
				}
			} else {
				// Handle non-composite values
				boolean match = true;
				FeatureValue firstfv = null;
				for(FeatureValue currfv:values) {
					if(currfv.equals(FeatureValue.UNKNOWN_VALUE)) {
						match = false;
						break;
					}
					
					if(firstfv==null) {
						firstfv=currfv;
					} else {
						if(this.asbinary) {
							if(!(currfv instanceof NumValue) || !(firstfv instanceof NumValue)) {
								throw new ConfigurationException("As binary feature only defined for numeric");
							}
							
							double firstnum = ((NumValue) firstfv).getNumber();
							double currnum = ((NumValue) currfv).getNumber();
							
							if((firstnum>0 && currnum<=0) || (firstnum<=0 || currnum>0)) {
								match = false;
								break;
							}
						} else if(!currfv.equals(firstfv)) {
							match = false;
							break;
						}
					}
				}
				
				// Count number of matches
				if(match) {
					nummatch++;
				}
				
				totalnum++;
			}
		}
		
		if(invert) {
			return new NumValue(1.0-(nummatch/totalnum));
		} else {
			return new NumValue(nummatch/totalnum);
		}
	}
	
	/**
	 * Initialize information required by the feature
	 */
	protected void initialize() {
		if(featureids!=null) {
			return;
		}
		
		// Initialize neighbor information
		if(this.hasParameter("neighborclass")) {
			this.neighborclass = this.getStringParameter("neighborclass");
		}
		
		this.neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, this.neighborclass);
		this.neighbor.copyParameters(this);
		
		this.featureschemaid = this.getStringParameter("featureschemaid");
		// Use specified features or, if not specified, use all features
		// for the schema of the specified feature schema id.
		Schema schema = g.getSchema(this.featureschemaid);
		this.featureids = FeatureUtils.parseFeatureList(this,
				schema, FeatureUtils.getFeatureIDs(schema, 2));
		
		this.invert = this.hasYesNoParameter("invert", "yes");
		this.asbinary = this.hasYesNoParameter("asbinary", "yes");
	}

	public void setGraph(Graph g) {
		this.g = g;
	}
}
