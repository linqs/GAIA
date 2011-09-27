package linqs.gaia.feature.derived.composite;

import linqs.gaia.feature.Feature;
import linqs.gaia.feature.StringFeature;

public class CVString implements CVFeature, StringFeature {
	public Feature copy() {
		return new CVString();
	}
}
