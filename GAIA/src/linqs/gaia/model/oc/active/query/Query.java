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
package linqs.gaia.model.oc.active.query;

import linqs.gaia.feature.decorable.Decorable;

/**
 * Query object returned by active learning algorithms.
 * The query consist of the decorable object we are querying,
 * the feature id of the feature we are interested in, and,
 * optionally, the score of this query according the active learning algorithm.
 * 
 * @author namatag
 *
 */
public class Query implements Comparable<Query>{
	private Decorable d = null;
	private String featureid = null;
	private double score = 0;
	
	/**
	 * Query object constructor
	 * 
	 * @param d Decorable object
	 */
	public Query(Decorable d) {
		this.d = d;
	}
	
	/**
	 * Query object constructor
	 * 
	 * @param d Decorable object
	 * @param featureid Feature to query about
	 */
	public Query(Decorable d, String featureid) {
		this.d = d;
		this.featureid = featureid;
	}
	
	/**
	 * Query object constructor
	 * 
	 * @param d Decorable object
	 * @param featureid Feature to query about
	 * @param score Score of query
	 */
	public Query(Decorable d, String featureid, double score) {
		this.d = d;
		this.featureid = featureid;
		this.score = score;
	}
	
	/**
	 * Get Decorable object
	 * @return Decorable object
	 */
	public Decorable getDecorable() {
		return this.d;
	}
	
	/**
	 * Get feature ID string
	 * @return String representing feature ID
	 */
	public String getFeatureID() {
		return this.featureid;
	}
	
	/**
	 * Get the score of the query.  Higher score means
	 * more informative query.
	 * 
	 * @return Double value representing score
	 */
	public double getScore() {
		return this.score;
	}
	
	/**
	 * Objects are equal if their first and second values are equal.
	 */
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
	    if (this == obj) {
	      return true;
	    }
	    
	    if (obj==null || !(obj instanceof Query)) {
	      return false;
	    }
	    
		Query p = (Query) obj;
	    
	    return this.score==p.score && this.featureid.equals(featureid) && this.d.equals(p.d);
	}
	
	public int hashCode() {
	  int hash = 1 + (int) this.score;
	  hash = hash * 31 + this.d.hashCode();
	  hash = hash * 31 + this.featureid.hashCode();

	  return hash;
	}
	
	/**
	 * Queries are ordered only by score
	 */
	public int compareTo(Query o) {
		if(this == o || this.equals(o)) {
			return 0;
		}
		
		Double oscore = o.getScore();
		Double myscore = this.getScore();
		return myscore.compareTo(oscore);
	}
	
	/**
	 * String representation of the query
	 * in the form: decorable.featureid=score
	 */
	public String toString() {
		return d+"."+featureid+"="+score;
	}
}
