package linqs.gaia.model.er.merger.feature;

import java.io.Serializable;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.model.er.merger.Merger;

/**
 * Abstract class for defining how to merge the features
 * of the specified decorable items.
 * 
 * @author namatag
 *
 */
public abstract class FeatureMerger extends Merger<Decorable> implements Serializable {
	private static final long serialVersionUID = 1L;

}
