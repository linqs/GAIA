package linqs.gaia.similarity.node;

import java.util.HashSet;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Node;
import linqs.gaia.similarity.NormalizedNodeSimilarity;

/**
 * Compare the cosine similarity of some numeric valued features
 * for the two nodes with the assumption that the data is sparse
 * and that the specified feature has a comma delimited string
 * indicating the feature ids of the non-zero values to compute
 * the cosine similarity over.
 * <p>
 * Required Paramters:
 * <UL>
 * <LI> featureid-Feature ID of feature whose string value contains
 * the delimited set of strings to compute set similarity over
 * </UL>
 * 
 * Optional Paramters:
 * <UL>
 * <LI> delimiter-Delimited to use to split the string contents.  Default is a comma.
 * </UL>
 * 
 * @author namatag
 *
 */
public class NodeStringSetCosineSimilarity extends BaseConfigurable 
	implements NormalizedNodeSimilarity {
	private static final long serialVersionUID = 1L;
	private boolean initialize = true;
	private String featureid = null;
	private String delimiter = null;
	
	private void initialize() {
		initialize = false;
		this.featureid = this.getStringParameter("featureid");
		this.delimiter = this.getStringParameter("delimiter",",");
	}

	private double getSimilarity(Node item1, Node item2, boolean normalize) {
		if(initialize) {
			initialize();
		}
		
		// If either value is unknown, return 0 similarity
		if(!item1.hasFeatureValue(featureid) || !item2.hasFeatureValue(featureid)) {
			return 0;
		}
		
		String[] string1 = item1.getFeatureValue(featureid).getStringValue().split(delimiter);
		Set<String> string1set = new HashSet<String>(string1.length);
		for(String s:string1) {
			if(s.trim().length()!=0) {
				string1set.add(s);
			}
		}
		
		String[] string2 = item2.getFeatureValue(featureid).getStringValue().split(delimiter);
		Set<String> string2set = new HashSet<String>(string2.length);
		for(String s:string2) {
			if(s.trim().length()!=0) {
				string2set.add(s);
			}
		}
		
		Set<String> isect = new HashSet<String>(string1set);
		isect.removeAll(string2set);
		if(isect.isEmpty()) {
			// Save computation by breaking early
			return 0;
		}
		
		double numerator = 0;
		for(String fid:isect) {
			double val1 = ((NumValue) item1.getFeatureValue(fid)).getNumber();
			double val2 = ((NumValue) item2.getFeatureValue(fid)).getNumber();
			
			numerator += val1 * val2;
		}
		
		double denom1 = 0;
		for(String fid:string1set) {
			double val1 = ((NumValue) item1.getFeatureValue(fid)).getNumber();
			if(val1==0) {
				throw new InvalidStateException("Feature value cannot be 0: "
						+fid+" for "+item1);
			}
			
			denom1 += val1 * val1;
		}
		
		double denom2 = 0;
		for(String fid:string2set) {
			double val2 = ((NumValue) item2.getFeatureValue(fid)).getNumber();
			if(val2==0) {
				throw new InvalidStateException("Feature value cannot be 0: "
						+fid+" for "+item2);
			}
			
			denom2 += val2 * val2;
		}
		
		double finalsim = denom1!=0 && denom2!=0 ?
				(numerator / (Math.sqrt(denom1)*Math.sqrt(denom2)))
				: 0;
		
		return finalsim;
	}
	
	public double getSimilarity(Node item1, Node item2) {
		return this.getSimilarity(item1, item2, false);
	}

	public double getNormalizedSimilarity(Node item1, Node item2) {
		return this.getSimilarity(item1, item2, true);
	}
}
