package linqs.gaia.similarity.string;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.Soundex;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.similarity.NormalizedStringSimilarity;

/**
 * Compute the soundex similarity of the two strings.
 * This implementation is a wrapper of {@link org.apache.commons.codec.language.Soundex}
 * which returns a similarity from 0 through 4 where
 * 0 indicates little or no similarity,
 * and 4 indicates strong similarity or identical values. 
 * The normalized value is returned by dividing the unnormalized similarity by 4.
 * 
 * @author namatag
 *
 */
public class SoundexSimilarity extends BaseConfigurable implements NormalizedStringSimilarity {
	private static final long serialVersionUID = 1L;
	private static Soundex soundex = new Soundex();
	
	public double getNormalizedSimilarity(String item1, String item2) {
		double normalizesim = this.getSimilarity(item1, item2)/4.0;
		
		return normalizesim;
	}
	
	public double getSimilarity(String item1, String item2) {
		try {
			int similarity = soundex.difference(item1, item2);
			
			// Verify valid result
			if(similarity<0 || similarity>4) {
				throw new InvalidStateException("Invalid returned value by soundex: "
						+item1+" and "+item2+" has difference of "+similarity);
			}
			
			return similarity;
		} catch (EncoderException e) {
			throw new RuntimeException(e);
		}
	}
}
