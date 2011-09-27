package linqs.gaia.experiment.groovy.feature;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedString;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.StringValue;

/**
 * Derived feature which uses the specified groovy script
 * to compute the value of the attribute.
 * In this feature, the groovy script should return
 * a string value which is the value of the string feature.
 * 
 * @author namatag
 *
 */
public class GroovyString extends DerivedString {
	private GroovyShell shell = new GroovyShell();
	private String groovyscript = null;
	private Script scriptobject = null;
	
	/**
	 * String valued derived feature
	 * 
	 * @param groovyscript Script to run which returns a string
	 */
	public GroovyString(String groovyscript) {
		this.groovyscript = groovyscript;
		
		String importline = "import static "+FeatureConstruction.class.getCanonicalName()+".*";
		if(!this.groovyscript.contains(importline)) {
			this.groovyscript = importline+"\n"+groovyscript;
		}
		
		scriptobject = shell.parse(this.groovyscript);
	}
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		scriptobject.getBinding().setVariable("di", di);
		String value = (String) scriptobject.run();
		
		return new StringValue(value);
	}
	
	@Override
	public Feature copy() {
		// Note: Cache values are not copied
		DerivedFeature df;
		try {
			df = new GroovyString(this.groovyscript);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		// Perform copy tasks common to all Derived Objects
		// Copy the configurable ID and configurations
		df.setCID(this.getCID());
		df.copyParameters(this);
		
		return df;
	}
}
