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
package linqs.gaia.model.oc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;

/**
 * Class containing common functions that may come
 * up in many different object classification models
 * 
 * @author namatag
 *
 */
public class OCUtils {
	/**
	 * Function which returns, for a schema and feature in that schema,
	 * a list of items where if getmissing is true, return only items
	 * with the specified feature undefined and if false, return only items
	 * with the specified feature defined.
	 * 
	 * @param g Graph
	 * @param sid Schema ID of Feature
	 * @param fname Feature Name
	 * @param getmissing Boolean for if you want those with missing or not
	 * @return List of items
	 */
	public static List<GraphItem> getItemsByFeature(Graph g, String sid, String fname, boolean getmissing) {
		List<GraphItem> selectgitems = new ArrayList<GraphItem>();
		
		Iterator<GraphItem> gitems = g.getGraphItems(sid);
		Feature f = g.getSchema(sid).getFeature(fname);
		if(!(f instanceof ExplicitFeature)){
			throw new UnsupportedTypeException("Unsupported feature type: "+f.getClass().getCanonicalName());
		}
		
		if(getmissing && ((ExplicitFeature) f).isClosed()) {
			throw new InvalidStateException("Can never get missing values for closed features: "+fname);
		}
		
		while(gitems.hasNext()) {
			GraphItem gi = gitems.next();
			boolean isUnknown = gi.getFeatureValue(fname).equals(FeatureValue.UNKNOWN_VALUE);
			
			if(getmissing == isUnknown){
				selectgitems.add(gi);
			}
		}
		
		return selectgitems;
	}
	
	public static List<Edge> getEdgesByFeature(Graph g, String sid, String fname, boolean getmissing) {
		List<Edge> selectgitems = new ArrayList<Edge>();
		
		SchemaType type = g.getSchemaType(sid);
		if(!(type.equals(SchemaType.DIRECTED)
			|| type.equals(SchemaType.UNDIRECTED))){
			throw new UnsupportedTypeException("Schema must be DIRECTED or UNDIRECTED: "
					+sid+" of type "+type);
		}
		
		Iterator<Edge> eitems = g.getEdges(sid);
		Feature f = g.getSchema(sid).getFeature(fname);
		if(!(f instanceof ExplicitFeature)){
			throw new UnsupportedTypeException("Unsupported feature type: "+f.getClass().getCanonicalName());
		}
		
		if(getmissing && ((ExplicitFeature) f).isClosed()) {
			throw new InvalidStateException("Can never get missing values for closed features: "+fname);
		}
		
		while(eitems.hasNext()) {
			Edge e = eitems.next();
			boolean isUnknown = e.getFeatureValue(fname).equals(FeatureValue.UNKNOWN_VALUE);
			
			if(getmissing == isUnknown){
				selectgitems.add(e);
			}
		}
		
		return selectgitems;
	}
	
	/**
	 * Function which returns, for a schema and feature in that schema,
	 * an iterable object of items where if getmissing is true, return only items
	 * with the specified feature undefined and if false, return only items
	 * with the specified feature defined.
	 * 
	 * @param g Graph
	 * @param sid Schema ID of Feature
	 * @param fname Feature Name.  If null, do not check for value being missing or known.
	 * @param getmissing If true, return only objects with the value missing.
	 * If false, return only objects with the value known.
	 * 
	 * @return Iterable items
	 */
	public static Iterable<GraphItem> getIterableItemsByFeature(Graph g, String sid, 
			String fname, boolean getmissing) {
		return new ByFeatureIterable(g, sid, fname, getmissing);
	}
	
	/**
	 * Function which returns, for a graph returns
	 * an iterable object of items with the specified
	 * schema id.
	 * 
	 * @param g Graph
	 * @param sid Schema ID of Feature
	 * 
	 * @return Iterable items
	 */
	public static Iterable<GraphItem> getIterableItems(Graph g, String sid) {
		return new ByFeatureIterable(g, sid, null, false);
	}
	
	/**
	 * Helper class for use with getIterableItemsByFeature
	 * 
	 * @author namatag
	 *
	 */
	private static class ByFeatureIterable implements Iterable<GraphItem> {
		private Graph g = null;
		private String sid;
		private String fname;
		private boolean getmissing;
		
		public ByFeatureIterable(Graph g, String sid, String fname, boolean getmissing) {
			this.g = g;
			this.sid = sid;
			this.fname = fname;
			this.getmissing = getmissing;
		}
		
		public Iterator<GraphItem> iterator() {
			return new ByFeatureIterator(g.getGraphItems(sid));
		}
		
		/**
		 * Special iterator where given some initial iterator,
		 * it only returns items which either has a
		 * certain feature set or not.
		 * 
		 * @author namatag
		 *
		 */
		private class ByFeatureIterator implements Iterator<GraphItem> {
			private Iterator<GraphItem> gitems = null;
			private GraphItem next = null;
			
			public ByFeatureIterator(Iterator<GraphItem> gitems) {
				this.gitems = gitems;
				this.next = this.getNext();
			}
			
			public boolean hasNext() {
				return this.next!=null;
			}

			public GraphItem next() {
				GraphItem oldnext = this.next;
				this.next = this.getNext();
				
				return oldnext;
			}
			
			private GraphItem getNext() {
				if(!gitems.hasNext()) {
					return null;
				}
				
				GraphItem newnext = gitems.next();
				while(true) {
					// If a feature name is defined,
					// check to see if the feature name is true or false.
					if(fname!=null) {
						FeatureValue fv = newnext.getFeatureValue(fname);
						boolean isUnknown =fv.equals(FeatureValue.UNKNOWN_VALUE);
						if(isUnknown == getmissing){
							break;
						}
					} else {
						break;
					}
					
					if(!gitems.hasNext()) {
						return null;
					}
					
					newnext = gitems.next();
				}
				
				return newnext;
			}

			public void remove() {
				throw new InvalidStateException("Remove feature unsupported");
			}
		}
	}
}
