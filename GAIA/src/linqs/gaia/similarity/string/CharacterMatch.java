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
