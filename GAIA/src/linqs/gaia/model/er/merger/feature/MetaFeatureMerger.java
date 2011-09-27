package linqs.gaia.model.er.merger.feature;

import java.util.ArrayList;
import java.util.List;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.util.Dynamic;

/**
 * Applies the specified list of feature mergers in the order provided.
 * 
 * Required Parameters:
 * <UL>
 * <LI>fmergers-Comma delimited list of feature mergers to use.
 * </UL>  
 * 
 * @author namatag
 *
 */
public class MetaFeatureMerger extends FeatureMerger {
	private static final long serialVersionUID = 1L;
	
	private List<FeatureMerger> fmergers = null;
	
	private void initialize() {
		if(fmergers != null) {
			return;
		}
		
		// Instantiate all feature mergers
		fmergers = new ArrayList<FeatureMerger>();
		String[] fmergerclasses = this.getStringParameter("fmergers").split(",");
		
		for(String fmergerclass:fmergerclasses) {
			FeatureMerger fmerger = (FeatureMerger)
				Dynamic.forConfigurableName(FeatureMerger.class, fmergerclass, this);
			
			fmergers.add(fmerger);
		}
	}
	
	public void merge(Iterable<Decorable> items, Decorable mergeditem) {
		this.initialize();
		
		// Iterate over all mergers
		for(FeatureMerger fmerger:fmergers) {
			fmerger.merge(items, mergeditem);
		}
	}
}
