/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package linqs.gaia.experiment.groovy.feature;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedCateg;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.util.UnmodifiableList;

/**
 * Derived feature which uses the specified groovy script
 * to compute the value of the attribute.
 * In this feature, the groovy script should return
 * a string where the string
 * is one of the specified categorical features.
 * 
 * @author namatag
 *
 */
public class GroovyCateg extends DerivedCateg {
	private GroovyShell shell = new GroovyShell();
	private UnmodifiableList<String> categorylist;
	private String categories = null;
	private String groovyscript = null;
	private Script scriptobject = null;
	
	/**
	 * Categorical valued derived feature
	 * 
	 * @param categories Comma delimited list of categories
	 * @param groovyscript Script to run which returns a string valued category
	 */
	public GroovyCateg(String categories, String groovyscript) {
		this.groovyscript = groovyscript;
		this.categories = categories;
		this.categorylist = new UnmodifiableList<String>(categories.split(","));
		
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
		
		return new CategValue(value);
	}

	@Override
	public UnmodifiableList<String> getAllCategories() {
		return this.categorylist;
	}
	
	@Override
	public Feature copy() {
		// Note: Cache values are not copied
		DerivedFeature df;
		try {
			df = new GroovyCateg(this.categories, this.groovyscript);
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
