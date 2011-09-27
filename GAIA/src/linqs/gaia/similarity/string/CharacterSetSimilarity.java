package linqs.gaia.similarity.string;

import java.util.HashSet;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.similarity.NormalizedSetSimilarity;
import linqs.gaia.similarity.NormalizedStringSimilarity;
import linqs.gaia.util.Dynamic;

/**
 * Compute the specified normalized set similarity of the
 * set of characters defined in the two string included.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> nssimclass-{@link NormalizedSetSimilarity} class to use defined
 * using the format defined by {@link Dynamic#forConfigurableName}.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> delimiter-Delimiter to use for the string values.
 * If specified, instead of using the set of characters,
 * the set is created from the delimited values.
 * </UL>
 * 
 * @author namatag
 *
 */
public class CharacterSetSimilarity extends BaseConfigurable implements NormalizedStringSimilarity {
	private static final long serialVersionUID = 1L;
	
	private NormalizedSetSimilarity nssim = null;
	private String delimiter = null;
	private boolean initialize = true;
	
	private void initialize() {
		initialize = false;
		
		if(this.hasParameter("delimiter")) {
			delimiter = this.getStringParameter("delimiter");
		}
		
		String nssimclass = this.getStringParameter("nssimclass");
		nssim = (NormalizedSetSimilarity) Dynamic.forConfigurableName(NormalizedSetSimilarity.class,
				nssimclass, this);
	}
	
	private Set<String> getSet(String item) {
		Set<String> itemset = new HashSet<String>();
		
		if(delimiter == null) {
			// Create set from characters
			char[] array = item.toCharArray();
			for(char currchar:array) {
				itemset.add(""+currchar);
			}
		} else {
			// Create set from delimited values
			String[] array = item.split(delimiter);
			for(String currchar:array) {
				itemset.add(""+currchar);
			}
		}
		
		return itemset;
	}
	
	public double getNormalizedSimilarity(String item1, String item2) {
		if(initialize) {
			this.initialize();
		}
		
		return nssim.getNormalizedSimilarity(this.getSet(item1), this.getSet(item2));
	}

	public double getSimilarity(String item1, String item2) {
		if(initialize) {
			this.initialize();
		}
		
		return nssim.getSimilarity(this.getSet(item1), this.getSet(item2));
	}
}
