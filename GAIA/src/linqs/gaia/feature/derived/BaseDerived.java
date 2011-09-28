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
package linqs.gaia.feature.derived;

import java.util.HashMap;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;

/**
 * Base implementation of derived class to simplify implementations
 * of derived features.
 * 
 * @author namatag
 *
 */
public abstract class BaseDerived extends BaseConfigurable implements DerivedFeature {
	private boolean isCaching = false;
	private HashMap<Decorable, FeatureValue> cache;
	
	public boolean isCaching() {
		return this.isCaching;
	}

	public void resetCache() {
		if(this.isCaching) {
			if(cache == null) {
				cache = new HashMap<Decorable, FeatureValue> ();
			} else {
				cache.clear();
			}
		} else {
			throw new InvalidOperationException("Unable to reset cache for a non-caching feature: "
					+this.getClass().getCanonicalName());
		}
	}
	
	public void resetCache(Decorable d) {
		if(this.isCaching) {
			if(cache == null) {
				throw new InvalidStateException("Cache uninitialized.");
			}
			
			if(cache.containsKey(d)) {
				cache.remove(d);
			}
		} else {
			throw new InvalidOperationException("Unable to reset cached for a non-caching feature: "
					+this.getClass().getCanonicalName());
		}
	}

	public void setCache(boolean shouldCache) {
		this.isCaching = shouldCache;
		
		if(this.isCaching) {
			cache = new HashMap<Decorable, FeatureValue> ();
		} else {
			cache = null;
		}
	}
	
	/**
	 * Caches the feature value for the given item
	 * 
	 * @param d Decorable item
	 * @param value Feature value to cache
	 */
	protected void cacheValue(Decorable d, FeatureValue value) {
		this.cache.put(d, value);
	}
	
	/**
	 * Get the cached value.  A null is returned if the value is not cached.
	 * 
	 * @param d Decorable item
	 * @return Cached feature value for item
	 */
	protected FeatureValue getCachedValue(Decorable d) {
		return this.cache.get(d);
	}
	
	/**
	 * This function wraps the getFeatureValue of
	 * implementing classes.  It handles
	 * the caching for the function.
	 */
	public FeatureValue getFeatureValue(Decorable di) {
		FeatureValue value = null;
		// If caching, check cache first
		if(this.isCaching()) {
			value = this.getCachedValue(di);
		}
		
		// If not caching or value not in cache,
		// calculate value.
		if(value==null) {
			value = this.calcFeatureValue(di);
		}
		
		// Cache value if requested
		if(this.isCaching()) {
			this.cacheValue(di, value);
		}
		
		return value;
	}
	
	public Feature copy() {
		// Note: Cache values are not copied
		DerivedFeature df;
		try {
			df = this.getClass().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		// Perform copy tasks common to all Derived Objects
		// Copy the configurable ID and configurations
		df.setCID(this.getCID());
		df.copyParameters(this);
		
		return df;
	}
	
	/**
	 * Protected function to calculate values.
	 * Use with getFeatureValue.
	 * 
	 * @param di Decorable item
	 * @return Computed FeatureValue
	 */
	abstract protected FeatureValue calcFeatureValue(Decorable di);
}
