package linqs.gaia.feature.derived.composite;

import java.util.List;

import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.util.UnmodifiableList;

public class CVCateg implements CVFeature, CategFeature {
	private UnmodifiableList<String> categories;

	public CVCateg(List<String> categories) {
		this(new UnmodifiableList<String>(categories));
	}
	
	public CVCateg(UnmodifiableList<String> categories) {
		this.categories = categories;
	}
	
	public UnmodifiableList<String> getAllCategories() {
		return this.categories;
	}

	public Feature copy() {
		return new CVCateg(this.categories);
	}
	
	public int numCategories() {
		return this.categories.size();
	}
}
