package linqs.gaia.model;

/**
 * General interface for all models which has a bootstrapping step.
 * This interface will allow you to specify whether or not you
 * want the model to apply bootstrapping.  Useful when applying
 * predictions in succession.
 * 
 * @author namatag
 *
 */
public interface BootstrapModel {
	/**
	 * Set whether or not to apply bootstrapping in the prediction.
	 * 
	 * @param bootstrap If true, apply bootstrapping.  If false, don't apply.
	 */
	void shouldBootstrap(boolean bootstrap);
}
