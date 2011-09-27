package linqs.gaia.graph.event;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;

/**
 * Feature value was set.
 * 
 * @author namatag
 *
 */
public class FeatureSetEvent implements GraphEvent {
	private Decorable decorable;
	private String featureid;
	private FeatureValue previous;
	private FeatureValue current;
	
	/**
	 * 
	 * @param d Decorabl item whose feature was set
	 * @param featureid Feature ID of changed feature
	 * @param previous Previous value of feature
	 * @param current Current value of feature
	 */
	public FeatureSetEvent(Decorable d, String featureid, FeatureValue previous, FeatureValue current) {
		this.decorable = d;
		this.featureid = featureid;
		this.previous = previous;
		this.current = current;
	}
	
	/**
	 * Return the decorable item whose feature value was changed
	 * 
	 * @return Changed decorable item
	 */
	public Decorable getChangedItem() {
		return this.decorable;
	}
	
	/**
	 * Return the feature ID of the changed feature
	 * 
	 * @return Changed feature ID
	 */
	public String getChangedFeatureID() {
		return this.featureid;
	}
	
	/**
	 * Return the previous feature value
	 * 
	 * @return Previous feature value
	 */
	public FeatureValue getPreviousValue() {
		return this.previous;
	}
	
	/**
	 * Return the current feature value
	 * 
	 * @return Current feature value
	 */
	public FeatureValue getCurrentValue() {
		return this.current;
	}
}
