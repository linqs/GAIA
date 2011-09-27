package linqs.gaia.feature.derived.composite;

import java.util.List;

import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.util.UnmodifiableList;

public class CVMultiCateg implements CVFeature, MultiCategFeature {
	private UnmodifiableList<String> categories;
	
	public CVMultiCateg(List<String> categories) {
		this(new UnmodifiableList<String>(categories));
	}
	
	public CVMultiCateg(UnmodifiableList<String> categories) {
		this.categories = categories;
	}
	
	public UnmodifiableList<String> getAllCategories() {
		return this.categories;
	}

	public Feature copy() {
		return new CVMultiCateg(this.categories);
	}
	
	public int numCategories() {
		return this.categories.size();
	}
}
