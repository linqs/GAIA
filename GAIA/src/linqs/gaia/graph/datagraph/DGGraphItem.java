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
package linqs.gaia.graph.datagraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.datagraph.feature.explicit.DGExplicitFeature;
import linqs.gaia.graph.event.FeatureSetEvent;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;

/**
 * Implements functions common to both {@link DGNode} and {@link DGEdge}.
 * 
 * @author namatag
 *
 */
public abstract class DGGraphItem implements GraphItem {
	private static final long serialVersionUID = 1L;

	private Graph graph;
	private GraphItemID id;
	private Integer internalid;
	
	protected abstract Set<GraphItem> getIncidentGraphItemSets();
	protected abstract Set<GraphItem> getIncidentGraphItemSets(String schemaID);
	protected abstract Set<GraphItem> getIncidentGraphItemSets(GraphItem adjacent);
	protected abstract Set<GraphItem> getIncidentGraphItemSets(String schemaID, GraphItem adjacent);

	public DGGraphItem(DataGraph g, GraphItemID id) {
		this.graph = g;
		this.id = id;

		this.internalid = g.getInternalID();
	}

	private Set<GraphItem> getAdjacentGraphItemSets() {
		HashSet<GraphItem> all = new HashSet<GraphItem>();
		Iterator<GraphItem> incident = this.getIncidentGraphItems();
		while(incident.hasNext()) {
			GraphItem gi = incident.next();
			Iterator<GraphItem> giitr = gi.getIncidentGraphItems();
			while(giitr.hasNext()) {
				GraphItem currgi = giitr.next();
				if(currgi.equals(this)) {
					continue;
				}

				all.add(currgi);
			}
		}

		return all;
	}

	private Set<GraphItem> getAdjacentGraphItemSets(String incidentsid) {
		HashSet<GraphItem> all = new HashSet<GraphItem>();
		Iterator<GraphItem> incident = this.getIncidentGraphItems(incidentsid);
		while(incident.hasNext()) {
			GraphItem gi = incident.next();
			Iterator<GraphItem> giitr = gi.getIncidentGraphItems();
			while(giitr.hasNext()) {
				GraphItem currgi = giitr.next();
				if(currgi.equals(this)) {
					continue;
				}

				all.add(currgi);
			}
		}

		return all;
	}
	
	public Iterator<GraphItem> getAdjacentGraphItems() {
		return this.getAdjacentGraphItemSets().iterator();
	}

	public Iterator<GraphItem> getAdjacentGraphItems(String incidentsid) {
		return this.getAdjacentGraphItemSets(incidentsid).iterator();
	}

	public boolean isAdjacent(GraphItem gi) {
		Iterator<GraphItem> itr = this.getAdjacentGraphItems();
		while(itr.hasNext()) {
			GraphItem currgi = itr.next();

			if(currgi.equals(gi)) {
				return true;
			}
		}

		return false;
	}

	public boolean isAdjacent(GraphItem gi, String incidentsid) {
		Iterator<GraphItem> itr = this.getAdjacentGraphItems(incidentsid);
		while(itr.hasNext()) {
			GraphItem currgi = itr.next();

			if(currgi.equals(gi)) {
				return true;
			}
		}

		return false;
	}

	public boolean isIncident(GraphItem gi) {
		Iterator<GraphItem> itr = this.getIncidentGraphItems();
		while(itr.hasNext()) {
			GraphItem currgi = itr.next();

			if(currgi.equals(gi)) {
				return true;
			}
		}

		return false;
	}

	public Graph getGraph() {
		return this.graph;
	}

	public FeatureValue getFeatureValue(String featureid) {
		Feature f = ((DataGraph) this.graph).getInternalSchema(id.getSchemaID()).getFeature(featureid);
		FeatureValue value = null;

		if(f instanceof DGExplicitFeature) {
			DGExplicitFeature dgf = (DGExplicitFeature) f;
			value = dgf.getFeatureValue(this.internalid);
		} else if(f instanceof DerivedFeature) {
			value = ((DerivedFeature) f).getFeatureValue(this);
		} else {
			throw new InvalidStateException("Unexpected feature type encountered: "
					+f.getClass().getCanonicalName());
		}

		return value;
	}

	public List<FeatureValue> getFeatureValues(List<String> featureids) {
		if(featureids==null) {
			throw new InvalidStateException("List of feature ids cannot be null");
		}

		List<FeatureValue> values = new ArrayList<FeatureValue>();

		for(String fid:featureids) {
			values.add(this.getFeatureValue(fid));
		}

		return values;
	}

	public boolean hasFeatureValue(String featureid) {		
		return !this.getFeatureValue(featureid).equals(FeatureValue.UNKNOWN_VALUE);
	}

	public Schema getSchema() {
		return this.graph.getSchema(this.getSchemaID());
	}

	public String getSchemaID() {
		return id.getSchemaID();
	}

	public synchronized void setFeatureValue(String featureid, FeatureValue value) {

		Schema schema = ((DataGraph) this.graph).getInternalSchema(id.getSchemaID());

		if(!schema.hasFeature(featureid)) {
			throw new InvalidOperationException("Feature "+featureid+" not defined for "+this.getSchemaID());
		}

		Feature f = schema.getFeature(featureid);

		if(!(f instanceof ExplicitFeature)) {
			throw new InvalidOperationException("Feature value cannot be set for: "+f);
		}

		FeatureValue previous = this.getFeatureValue(featureid);
		((DGExplicitFeature) f).setFeatureValue(this.internalid, value);

		this.graph.processListeners(new FeatureSetEvent(this, featureid, previous, value));
	}

	public synchronized void setFeatureValue(String featureid, String value) {
		Schema schema = ((DataGraph) this.graph).getInternalSchema(id.getSchemaID());

		if(!schema.hasFeature(featureid)) {
			throw new InvalidOperationException("Feature "+featureid+" not defined for "+this.getSchemaID());
		}

		Feature f = schema.getFeature(featureid);

		if(!(f instanceof ExplicitFeature)) {
			throw new InvalidOperationException("Feature value cannot be set for: "
					+featureid+" of type "+f.getClass().getCanonicalName());
		}
		
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
		
		FeatureValue previous = this.getFeatureValue(featureid);
		((DGExplicitFeature) f).setFeatureValue(this.internalid, newvalue);

		this.graph.processListeners(new FeatureSetEvent(this, featureid, previous, newvalue));
	}

	public synchronized void setFeatureValue(String featureid, double value) {
		Schema schema = ((DataGraph) this.graph).getInternalSchema(id.getSchemaID());

		if(!schema.hasFeature(featureid)) {
			throw new InvalidOperationException("Feature "+featureid+" not defined for "+this.getSchemaID());
		}

		Feature f = schema.getFeature(featureid);

		if(!(f instanceof ExplicitFeature)) {
			throw new InvalidOperationException("Feature value cannot be set for: "
					+featureid+" of type "+f.getClass().getCanonicalName());
		}
		
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
		
		FeatureValue previous = this.getFeatureValue(featureid);
		((DGExplicitFeature) f).setFeatureValue(this.internalid, newvalue);

		this.graph.processListeners(new FeatureSetEvent(this, featureid, previous, newvalue));
	}

	public synchronized void setFeatureValues(List<String> featureids, List<FeatureValue> values) {
		if(featureids==null || values==null) {
			throw new InvalidStateException("Feature ids or values is null:"
					+" featureids="+featureids
					+" values="+values);
		}

		if(featureids.size()!=values.size()) {
			throw new InvalidStateException("The number of feature ids and values must match:" +
					" #ids="+featureids.size()
					+" #ofvalues="+values.size());
		}

		int size = featureids.size();
		for(int i=0; i<size; i++) {
			this.setFeatureValue(featureids.get(i), values.get(i));
		}
	}

	public synchronized void removeFeatureValue(String featureid) {
		if(this.getFeatureValue(featureid).equals(FeatureValue.UNKNOWN_VALUE)) {
			throw new InvalidOperationException("Value to remove is already unknown: "+featureid);
		}

		this.setFeatureValue(featureid, FeatureValue.UNKNOWN_VALUE);
	}

	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
		if (this == obj) {
			return true;
		}

		if(!(obj instanceof GraphItem)){
			return false;
		}

		return this.id.equals(((GraphItem) obj).getID());
	}

	public GraphItemID getID() {
		return this.id;
	}

	public int hashCode() {
		return this.id.hashCode();
	}

	public int numAdjacentGraphItems() {
		return this.getAdjacentGraphItemSets().size();
	}

	public int numAdjacentGraphItems(String sid) {
		return this.getAdjacentGraphItemSets(sid).size();
	}

	public int numIncidentGraphItems() {
		return this.getIncidentGraphItemSets().size();
	}

	public int numIncidentGraphItems(String sid) {
		return this.getIncidentGraphItemSets(sid).size();
	}

	public int numIncidentGraphItems(GraphItem adjacent) {
		return this.getIncidentGraphItemSets(adjacent).size();
	}
	
	public Iterator<GraphItem> getIncidentGraphItems() {
		return this.getIncidentGraphItemSets().iterator();
	}
	
	public Iterator<GraphItem> getIncidentGraphItems(String schemaID) {
		return this.getIncidentGraphItemSets(schemaID).iterator();
	}
	
	public Iterator<GraphItem> getIncidentGraphItems(GraphItem adjacent) {
		return this.getIncidentGraphItemSets(adjacent).iterator();
	}
	
	public Iterator<GraphItem> getIncidentGraphItems(String schemaID,
			GraphItem adjacent) {
		return this.getIncidentGraphItemSets(schemaID, adjacent).iterator();
	}
}
