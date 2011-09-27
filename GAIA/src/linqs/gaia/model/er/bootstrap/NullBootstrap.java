package linqs.gaia.model.er.bootstrap;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.graph.GraphItem;

/**
 * Do nothing for bootstrapping
 * 
 * @author namatag
 *
 */
public class NullBootstrap extends BaseConfigurable implements ERBootstrap {
	private static final long serialVersionUID = 1L;

	public boolean isSameEntity(GraphItem gi1, GraphItem gi2) {
		return false;
	}
}
