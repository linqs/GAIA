package linqs.gaia.experiment.groovy.feature;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;

/**
 * Derived feature which uses the specified groovy script
 * to compute the value of the attribute.
 * In this feature, the groovy script should return
 * a double value which is the value of the numeric feature.
 * 
 * @author namatag
 *
 */
public class GroovyNum extends DerivedNum {
	private GroovyShell shell = new GroovyShell();
	private String groovyscript = null;
	private Script scriptobject = null;
	
	/**
	 * Numeric valued derived feature
	 * 
	 * @param groovyscript Script to run which returns a double
	 */
	public GroovyNum(String groovyscript) {
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
		Double value = (Double) scriptobject.run();
		
		return new NumValue(value);
	}
	
	@Override
	public Feature copy() {
		// Note: Cache values are not copied
		DerivedFeature df;
		try {
			df = new GroovyNum(this.groovyscript);
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