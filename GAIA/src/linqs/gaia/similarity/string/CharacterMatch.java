package linqs.gaia.similarity.string;

import java.util.Arrays;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.similarity.NormalizedStringSimilarity;
import linqs.gaia.similarity.list.FeatureMatch;

/**
 * Calculate the match between two strings.
 * <p>
 * This is related to the Hamming distance where 
 * of the two object strings, add 1 if corresponding characters are not equal
 * and 0 if they are.  For the normalized similarity, instead of returning
 * a distance, we return ((list size) - distance)/(list size).
 * 
 * @author namatag
 *
 */
public class CharacterMatch extends BaseConfigurable implements NormalizedStringSimilarity {
	private static final long serialVersionUID = 1L;
	
	public double getNormalizedSimilarity(String item1, String item2) {
		FeatureMatch fm = new FeatureMatch();
		
		return fm.getNormalizedSimilarity(
				Arrays.asList(item1.toCharArray()),
				Arrays.asList(item2.toCharArray()));
	}

	public double getSimilarity(String item1, String item2) {
		FeatureMatch fm = new FeatureMatch();
		
		return fm.getSimilarity(Arrays.asList(item1.toCharArray()),
				Arrays.asList(item2));
	}
}
