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
package linqs.gaia.identifiable;

import linqs.gaia.exception.StringFormatException;
import linqs.gaia.global.Global;
import linqs.gaia.graph.Graph;

/**
 * Unique identifier for graph items
 * <p>
 * Graph items are unique given a graph id, a schema id and an object id.
 * The graph id maybe set to null when in the context
 * of a specific graph (e.g., getting a node with a given GraphItemID from the graph).
 * 
 * @author namatag
 *
 */
public class GraphItemID extends ID {
	private static final long serialVersionUID = 1L;
	
	private GraphID graphid = null;
	
	/**
	 * Constructor
	 * 
	 * @param graphid ID of Graph this object belongs to
	 * @param schemaid Schema ID
	 * @param objid Object ID
	 */
	public GraphItemID(GraphID graphid, String schemaid, String objid) {
		super(schemaid, objid);
		this.graphid = graphid;
	}
	
	/**
	 * Constructor
	 * <P>
	 * Note: All GraphItems returned from the
	 * Graph must have the GraphID of that graph
	 * as the return value of getGraphID().
	 * 
	 * @param schemaid Schema ID
	 * @param objid Object ID
	 */
	public GraphItemID(String schemaid, String objid) {
		super(schemaid, objid);
	}
	
	/**
	 * Get Graph ID for the Graph Item.
	 * 
	 * @return Graph ID
	 */
	public GraphID getGraphID() {
		return graphid;
	}
	
	/**
	 * Return a copy of this ID without the Graph identifier
	 * (i.e., containing only schema and object id)
	 * 
	 * @return Copy of Graph Item ID
	 */
	public GraphItemID copyWithoutGraphID() {
		return new GraphItemID(this.schemaid, this.objid);
	}
	
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
	    if (this == obj) {
	      return true;
	    }
	    
		if (!(obj instanceof GraphItemID)) {
			return false;
		}
		
		GraphItemID objid = (GraphItemID) obj;
		
		// Supports no GraphID being provided
	    return super.equals(obj)
	    	&& (this.graphid == objid.graphid ||
	    		(this.graphid != null && objid.graphid != null && this.graphid.equals(objid.graphid)));
	}
	
	public int hashCode() {
	    int hash = super.hashCode() * 31;
	    
	    // Supports no GraphID being provided
	    if(this.graphid != null) {
	    	hash+=this.graphid.hashCode();
	    }
	    
	    return hash;
	}
	
	/**
	 * Output id in the form:<br>
	 * &lt;graph schema id&gt;.&lt;graph object id&gt;.&lt;graph item schema id&gt;.&lt;graph item object id&gt;
	 * <br>
	 * i.e., SocialNetwork.Facebook.Person.BobSmith
	 */
	public String toString() {
		if(graphid==null) {
			return "null"+"."+"null"+"."+this.schemaid+"."+this.objid;
		} else {
			return graphid.schemaid+"."+graphid.objid+"."+this.schemaid+"."+this.objid;
		}
	}
	
	/**
	 * Parse a graph item ID from a string in the same
	 * format as that output by toString.
	 * 
	 * @param s String to parse
	 * @return Graph Item ID
	 */
	public static GraphItemID parseGraphItemID(String s) {
		String[] parts = s.split("\\.");
		if(parts.length!=4) {
			throw new StringFormatException("Unable to parse: "+s);
		}
		
		if(parts[0].equals("null") && parts[1].equals("null")) {
			return new GraphItemID(parts[2], parts[3]);
		}
		
		GraphID gid = new GraphID(parts[0], parts[1]);
		
		return new GraphItemID(gid, parts[2], parts[3]);
	}
	
	/**
	 * Generate a graph item id for the given graph and schema.
	 * The ID is guaranteed not to conflict with another graph item
	 * already in the graph.
	 * 
	 * @param g Graph of graph item
	 * @param schemaid Schema ID
	 * @return GraphItemID
	 */
	public static GraphItemID generateGraphItemID(Graph g, String schemaid) {
		return GraphItemID.generateGraphItemID(g, schemaid, "");
	}
	
	/**
	 * Generate a graph item id for the given graph and schema.
	 * The ID is guaranteed not to conflict with another graph item
	 * already in the graph.
	 * 
	 * @param g Graph of graph item
	 * @param schemaid Schema ID
	 * @param objidprefix Prefix to add to the object ID
	 * @return GraphItemID
	 */
	public static GraphItemID generateGraphItemID(Graph g, String schemaid, String objidprefix) {
		GraphItemID newid = null;
		do {
			newid = new GraphItemID(g.getID(), schemaid, objidprefix+Global.requestGlobalCounterValue());
		} while(g.hasGraphItem(newid));
		
		return newid;
	}
}
