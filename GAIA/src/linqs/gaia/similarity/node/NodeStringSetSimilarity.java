package linqs.gaia.similarity.node;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.graph.Node;
import linqs.gaia.similarity.NormalizedNodeSimilarity;
import linqs.gaia.similarity.NormalizedSetSimilarity;
import linqs.gaia.similarity.SetSimilarity;
import linqs.gaia.similarity.set.CommonNeighbor;
import linqs.gaia.util.Dynamic;

/**
 * Compare the similarity of two sets of strings given the
 * specified pair of nodes.  The set of strings are given by
 * a delimited feature (e.g., comma delimited) and a set similarity
 * over the string sets are computed.  For example, if one node
 * has the string "dog,cat,goat" and another has the string "dog,elephant,zebra",
 * the unnormalized {@link CommonNeighbor} value of similarity is 1/3. 
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
 * <LI> setsimclass-Class of the set similarity measure,
 * instantiated using in {@link Dynamic#forConfigurableName}, to use.
 * Default is {@link linqs.gaia.similarity.set.CommonNeighbor}.
 * </UL>
 * 
 * @author namatag
 *
 */
public class NodeStringSetSimilarity extends BaseConfigurable 
	implements NormalizedNodeSimilarity {
	private static final long serialVersionUID = 1L;
	private boolean initialize = true;
	private String featureid = null;
	private SetSimilarity setsim = null;
	private String delimiter = null;
	
	private void initializeSimilarity() {
		// Return quickly, if initialization definitely done
		if(!initialize) {
			return;
		}
		
		// Ensure that it is only initialized once
		synchronized(this) {
			if(initialize) {
				this.featureid = this.getStringParameter("featureid");
				this.delimiter = this.getStringParameter("delimiter",",");
				
				String setsimclass = this.getStringParameter("setsimclass",
						CommonNeighbor.class.getCanonicalName());
				setsim = (SetSimilarity) Dynamic.forConfigurableName(SetSimilarity.class, setsimclass, this);
				
				initialize = false;
			}
		}
	}

	private double getSimilarity(Node item1, Node item2, boolean normalize) {
		initializeSimilarity();
		
		// If either value is unknown, return 0 similarity
		if(!item1.hasFeatureValue(featureid) || !item2.hasFeatureValue(featureid)) {
			return 0;
		}
		
		String[] string1 = item1.getFeatureValue(featureid).getStringValue().split(delimiter);
		Set<String> string1set = new HashSet<String>(Arrays.asList(string1));
		
		String[] string2 = item2.getFeatureValue(featureid).getStringValue().split(delimiter);
		Set<String> string2set = new HashSet<String>(Arrays.asList(string2));
		
		double finalsim = normalize ?
			((NormalizedSetSimilarity) setsim).getNormalizedSimilarity(string1set, string2set) :
			setsim.getSimilarity(string1set, string2set);
		
		return finalsim;
	}
	
	public double getSimilarity(Node item1, Node item2) {
		return this.getSimilarity(item1, item2, false);
	}

	public double getNormalizedSimilarity(Node item1, Node item2) {
		return this.getSimilarity(item1, item2, true);
	}
}
