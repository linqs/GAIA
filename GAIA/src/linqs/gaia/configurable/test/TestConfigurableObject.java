package linqs.gaia.configurable.test;

import linqs.gaia.configurable.BaseConfigurable;

/**
 * Simple configurable object for use with testing
 * 
 * @author namatag
 *
 */
public class TestConfigurableObject extends BaseConfigurable {
	// Do nothing
	public TestConfigurableObject() {
		
	}
	
	// Create configurable object with configuration id
	public TestConfigurableObject(String cid) {
		this();
		this.setCID(cid);
	}
}
