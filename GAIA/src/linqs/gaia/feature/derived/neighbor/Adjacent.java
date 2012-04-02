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
package linqs.gaia.feature.derived.neighbor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.log.Log;

/**
 * Return all unique graph items adjacent to a specified item.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>connectingsid-Schema ID of the connecting graph item.
 * For a node, this is the schema ID of the incident edges and
 * for edges, this is the schema ID of the incident nodes.
 * If this is specified, only items connected to this item
 * by something of this schema ID will be counted.
 * <LI>connectingfeature-Parameter in the form of fname:fvalue.
 * Only connecting graph items which have the specified feature value
 * will be considered.  If the feature is not specified in the schema
 * of the connecting schemaid, all connecting graph items will be used.
 * <LI>dirtype-If the connecting sid is a directed edge, this specifies the
 * types of nodes to include.  Options include:
 * <UL>
 * <LI>sourceonly-Return only nodes which are adjacent as a source to a common incident edge
 * <LI>targetonly-Return only nodes which are adjacent as a target to a common incident edge
 * <LI>all-Return all adjecent nodes (Default)
 * </UL>
 * <LI> includeself-If yes, include self as one of the neighbors.
 * Default is no.
 * </UL>
 * 
 * @author namatag
 *
 */
public class Adjacent extends NeighborWithOmmission {
	private static final long serialVersionUID = 1L;
	private int dirtype = 3;
	private String connectingsid = null;
	private boolean ignoreconnvalue = false;
	private String fname = null;
	private String fvalue = null;
	private boolean includeself = false;
	
	public Adjacent() {
		// Do nothing
	}
	
	public Adjacent(String connectingsid, String connectingfeature) {
		this.setParameter("connectingsid", connectingsid);
		this.setParameter("connectingfeature", connectingfeature);
	}
	
	protected void initialize() {
		if(this.hasParameter("connectingsid")){
			connectingsid = this.getStringParameter("connectingsid");
		}
		
		if(this.hasParameter("dirtype")) {
			String dirtype = this.getCaseParameter("dirtype", new String[]{"sourceonly","targetonly","all"});
			
			if(dirtype.equals("sourceonly")) {
				this.dirtype = 1;
			} else if(dirtype.equals("targetonly")) {
				this.dirtype = 2;
			} else {
				this.dirtype = 3;
			}
		}
		
		if(this.hasParameter("connectingfeature")){
			String fparts[] = this.getStringParameter("connectingfeature").split(":");
			if(fparts.length!=2) {
				throw new ConfigurationException("Invalid connecting feature declarationg:"
						+" Size="+fparts.length);
			}
			
			fname = fparts[0].intern();
			fvalue = fparts[1].intern();
		}
		
		ignoreconnvalue = false;
		
		includeself = this.hasYesNoParameter("includeself", "yes");
	}
	
	@Override
	protected Iterable<GraphItem> calcNeighbors(GraphItem gi, GraphItem ignoregi) {
		Set<GraphItem> deps = new HashSet<GraphItem>();
		if(gi instanceof Node){
			// Handle nodes
			Node n = (Node) gi;
			
			// Specify certain types of edges
			Iterator<? extends Edge> eitr = null;
			if(dirtype == 1) {
				eitr = (connectingsid == null) 
					? n.getEdgesWhereTarget() : n.getEdgesWhereTarget(connectingsid);
			} else if(dirtype == 2) {
				eitr = (connectingsid == null)
					? n.getEdgesWhereSource() : n.getEdgesWhereSource(connectingsid);
			} else {
				eitr = (connectingsid == null)
					? n.getAllEdges() : n.getAllEdges(connectingsid);
			}
			
			// Iterate over all edges
			while(eitr.hasNext()){
				Edge e = eitr.next();
				
				if(ignoregi!=null && ignoregi.equals(e)) {
					continue;
				}
				
				if(!ignoreconnvalue && fname != null) {
					FeatureValue f = e.getFeatureValue(fname);
					if(!f.getStringValue().equals(fvalue) || f.equals(FeatureValue.UNKNOWN_VALUE)){
						continue;
					}
				}
				
				Iterator<Node> nitr = e.getAllNodes();
				while(nitr.hasNext()) {
					Node currn = nitr.next();
					
					if(ignoregi!=null && ignoregi.equals(currn)) {
						continue;
					}
					
					deps.add(currn);
				}
			}
		} else if(gi instanceof Edge){
			// Handle edges
			Edge rel = (Edge) gi;
			Iterator<Node> nitr = rel.getAllNodes();
			while(nitr.hasNext()) {
				Node n = nitr.next();
				
				if(ignoregi!=null && ignoregi.equals(n)) {
					continue;
				}
				
				if(connectingsid != null && !n.getSchemaID().equals(connectingsid)){
					continue;
				}
				
				if(fname != null) {
					if(n.getSchema().hasFeature(fname)) {
						FeatureValue f = n.getFeatureValue(fname);
						
						if(f.equals(FeatureValue.UNKNOWN_VALUE)
								|| !f.getStringValue().equals(fvalue)){
							continue;
						}
					} else {
						Log.WARN("Connecting feature not defined: "+fname);
					}
				}
				
				Iterator<Edge> eitr = n.getAllEdges();
				while(eitr.hasNext()) {
					Edge curre = eitr.next();
					
					if(ignoregi!=null && ignoregi.equals(curre)) {
						continue;
					}
					
					deps.add(curre);
				}
			}
		} else {
			throw new UnsupportedTypeException("Unsupported type: "+gi.getClass().getCanonicalName());
		}
		
		// Do not include the data item itself
		deps.removeAll(Arrays.asList(new GraphItem[]{gi}));
		
		if(includeself) {
			deps.add(gi);
		}
		
		return deps;
	}
	
	@Override
	protected Iterable<GraphItem> calcNeighbors(GraphItem gi, Set<GraphItem> ignoreset) {
		GraphItem ignoregi = ignoreset.size()==1 ? ignoreset.iterator().next() : null;
		
		Set<GraphItem> deps = new HashSet<GraphItem>();
		if(gi instanceof Node){
			// Handle nodes
			Node n = (Node) gi;
			
			// Specify certain types of edges
			Iterator<? extends Edge> eitr = null;
			if(dirtype == 1) {
				eitr = (connectingsid == null) 
					? n.getEdgesWhereTarget() : n.getEdgesWhereTarget(connectingsid);
			} else if(dirtype == 2) {
				eitr = (connectingsid == null)
					? n.getEdgesWhereSource() : n.getEdgesWhereSource(connectingsid);
			} else {
				eitr = (connectingsid == null)
					? n.getAllEdges() : n.getAllEdges(connectingsid);
			}
			
			// Iterate over all edges
			while(eitr.hasNext()){
				Edge e = eitr.next();
				
				if(ignoregi!=null) {
					if(ignoregi==n) {
						continue;
					}
				} else if(ignoreset.contains(e)) {
					continue;
				}
				
				if(!ignoreconnvalue && fname != null) {
					FeatureValue f = e.getFeatureValue(fname);
					if(!f.getStringValue().equals(fvalue) || f.equals(FeatureValue.UNKNOWN_VALUE)){
						continue;
					}
				}
				
				Iterator<Node> nitr = e.getAllNodes();
				while(nitr.hasNext()) {
					Node currn = nitr.next();
					
					if(ignoregi!=null) {
						if(ignoregi==currn) {
							continue;
						}
					} else if(ignoreset.contains(currn)) {
						continue;
					}
					
					deps.add(currn);
				}
			}
		} else if(gi instanceof Edge){
			// Handle edges
			Edge rel = (Edge) gi;
			Iterator<Node> nitr = rel.getAllNodes();
			while(nitr.hasNext()) {
				Node n = nitr.next();
				
				if(ignoregi!=null) {
					if(ignoregi==n) {
						continue;
					}
				} else if(ignoreset.contains(n)) {
					continue;
				}
				
				if(connectingsid != null && !n.getSchemaID().equals(connectingsid)){
					continue;
				}
				
				if(fname != null) {
					if(n.getSchema().hasFeature(fname)) {
						FeatureValue f = n.getFeatureValue(fname);
						
						if(f.equals(FeatureValue.UNKNOWN_VALUE)
								|| !f.getStringValue().equals(fvalue)){
							continue;
						}
					} else {
						Log.WARN("Connecting feature not defined: "+fname);
					}
				}
				
				Iterator<Edge> eitr = n.getAllEdges();
				while(eitr.hasNext()) {
					Edge curre = eitr.next();
					
					if(ignoregi!=null) {
						if(ignoregi==curre) {
							continue;
						}
					} else if(ignoreset.contains(curre)) {
						continue;
					}
					
					deps.add(curre);
				}
			}
		} else {
			throw new UnsupportedTypeException("Unsupported type: "+gi.getClass().getCanonicalName());
		}
		
		// Do not include the data item itself
		deps.removeAll(Arrays.asList(new GraphItem[]{gi}));
		
		if(includeself) {
			deps.add(gi);
		}
		
		return deps;
	}
	
	@Override
	protected Iterable<GraphItem> calcNeighbors(GraphItem gi) {
		GraphItem ignoregi = null;
		return calcNeighbors(gi, ignoregi);
	}
}
