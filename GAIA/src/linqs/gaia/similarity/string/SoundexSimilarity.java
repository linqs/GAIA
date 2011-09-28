/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
