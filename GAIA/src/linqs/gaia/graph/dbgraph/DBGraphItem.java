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
package linqs.gaia.graph.dbgraph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;

public abstract class DBGraphItem implements GraphItem, Decorable  {
	protected DBGraph g = null;
	protected GraphItemID gid = null;
	protected int dbid;
	
	public DBGraphItem(DBGraph g, GraphItemID gid, int dbid) {
		this.g = g;
		this.gid = gid;
		this.dbid = dbid;
	}

	public Graph getGraph() {
		return g;
	}
	
	public FeatureValue getFeatureValue(String featureid) {
		return DBGraph.getDBFeatureValue(g, this, featureid);
	}

	public Schema getSchema() {
		return this.getGraph().getSchema(this.getSchemaID());
	}

	public String getSchemaID() {
		return this.getID().getSchemaID();
	}

	public boolean hasFeatureValue(String featureid) {
		return DBGraph.getDBFeatureValue(this.g, this, featureid).equals(FeatureValue.UNKNOWN_VALUE)
			? false: true;
	}

	public void removeFeatureValue(String featureid) {
		this.setFeatureValue(featureid, FeatureValue.UNKNOWN_VALUE);
	}

	public void setFeatureValue(String featureid, FeatureValue value) {	
		DBGraph.setDBFeatureValue(g, this, featureid, value);
	}
	
	public void setFeatureValue(String featureid, String value) {
		Feature f = this.getSchema().getFeature(featureid);
		
		FeatureValue newvalue = null;
		if(f instanceof StringFeature) {
			newvalue = new StringValue(value);
		} else if(f instanceof NumFeature) {
			newvalue = new NumValue(Double.parseDouble(value));
		} else if(f instanceof CategFeature) {
			newvalue = new CategValue(value);
		} else if(f instanceof MultiCategFeature) {
			Set<String> set = new HashSet<String>(Arrays.asList(value.split(",")));
			newvalue = new MultiCategValue(set);
		} else if(f instanceof MultiIDFeature) {
			String[] ids = value.split(",");
			Set<ID> idset = new HashSet<ID>();
			for(String id:ids) {
				idset.add(ID.parseID(id));
			}
			newvalue = new MultiIDValue(idset);
		} else {
			throw new UnsupportedTypeException("Unsupported feature type: "
					+featureid+" of type "+f.getClass().getCanonicalName());
		}
		
		DBGraph.setDBFeatureValue(g, this, featureid, newvalue);
	}
	
	public void setFeatureValue(String featureid, double value) {
		Feature f = this.getSchema().getFeature(featureid);
		
		FeatureValue newvalue = null;
		if(f instanceof NumFeature) {
			newvalue = new NumValue(value);
		} else if(f instanceof StringFeature) {
			newvalue = new StringValue(""+value);
		} else if(f instanceof CategFeature) {
			newvalue = new CategValue(((CategFeature) f).getAllCategories().get((int) value));
		} else {
			throw new UnsupportedTypeException("Unsupported feature type: "
					+featureid+" of type "+f.getClass().getCanonicalName());
		}
		
		DBGraph.setDBFeatureValue(g, this, featureid, newvalue);
	}

	public GraphItemID getID() {
		return this.gid;
	}
	
	public int hashCode() {
	    return this.getID().hashCode();
	}
	
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
	    if (this == obj) {
	      return true;
	    }
	    
		if(!(obj instanceof GraphItem)){
			return false;
		}
		
		return this.getID().equals(((GraphItem) obj).getID());
	}
	
	public List<FeatureValue> getFeatureValues(List<String> featureids) {
		return DBGraph.getDBFeatureValues(this.g, this, featureids);
	}

	public void setFeatureValues(List<String> featureids,
			List<FeatureValue> values) {
		DBGraph.setDBFeatureValues(this.g, this, featureids, values);
	}
	
}
