package linqs.gaia.feature.derived.composite;

import linqs.gaia.feature.Feature;
import linqs.gaia.feature.NumFeature;

public class CVNum implements CVFeature, NumFeature {
	public Feature copy() {
		return new CVNum();
	}
}
