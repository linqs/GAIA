package linqs.gaia.model;

import linqs.gaia.configurable.BaseConfigurable;

/**
 * Base class designed to provide an implementation to functions
 * common to all Models.
 * 
 * @author namatag
 *
 */
public abstract class BaseModel extends BaseConfigurable implements Model {
	private static final long serialVersionUID = 1L;

	public String toString() {
		return this.getClass().getCanonicalName();
	}
}
