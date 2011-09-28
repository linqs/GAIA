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

import java.util.Iterator;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.IteratorUtils;

public class DBNode extends DBGraphItem implements Node {

	public DBNode(DBGraph g, GraphItemID gid, int dbid) {
		super(g, gid, dbid);
	}

	public Iterator<Node> getAdjacentSources() {
		return new DBNodeIterator(this.g,
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid" +
				" AND n.dbid<>"+this.dbid+
				" AND en.edbid IN" +
				" (SELECT edbid FROM edgenodes" +
					" WHERE type="+DBGraph.TARGET+
					" AND ndbid="+this.dbid+")");
	}

	public Iterator<Node> getAdjacentSources(String edgeschemaID) {
		return new DBNodeIterator(this.g,
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid" +
				" AND n.dbid<>"+this.dbid+
				" AND en.edbid IN" +
				" (SELECT en.edbid FROM edges e, edgenodes en" +
					" WHERE e.dbid=en.edbid" +
					" AND en.type="+DBGraph.TARGET+
					" AND e.sid='"+edgeschemaID+"'"+
					" AND en.ndbid="+this.dbid+")");
	}

	public Iterator<Node> getAdjacentTargets() {
		return new DBNodeIterator(this.g,
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid" +
				" AND n.dbid<>"+this.dbid+
				" AND en.edbid IN" +
				" (SELECT edbid FROM edgenodes" +
					" WHERE type="+DBGraph.SOURCE+
					" AND ndbid="+this.dbid+")");
	}

	public Iterator<Node> getAdjacentTargets(String edgeschemaID) {
		return new DBNodeIterator(this.g,
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid" +
				" AND n.dbid<>"+this.dbid+
				" AND en.edbid IN" +
				" (SELECT en.edbid FROM edges e, edgenodes en" +
					" WHERE e.dbid=en.edbid" +
					" AND en.type="+DBGraph.SOURCE+
					" AND e.sid='"+edgeschemaID+"'"+
					" AND en.ndbid="+this.dbid+")");
	}

	public Iterator<Edge> getAllEdges() {
		return new DBEdgeIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid AND en.ndbid="+this.dbid);
	}

	public Iterator<Edge> getAllEdges(String schemaID) {
		return new DBEdgeIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND e.sid="+schemaID +
				" AND en.ndbid="+this.dbid);
	}

	public Iterator<DirectedEdge> getDirEdges() {
		return new DBDirectedEdgeIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND en.type<>" + DBGraph.UNDIR+
				" AND en.ndbid="+this.dbid);
	}

	public Iterator<DirectedEdge> getEdgesWhereSource() {
		return new DBDirectedEdgeIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND en.type=" + DBGraph.SOURCE+
				" AND en.ndbid="+this.dbid);
	}

	public Iterator<DirectedEdge> getEdgesWhereSource(String schemaID) {
		return new DBDirectedEdgeIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND en.type=" + DBGraph.SOURCE+
				" AND e.sid=" + schemaID+
				" AND en.ndbid="+this.dbid);
	}

	public Iterator<DirectedEdge> getEdgesWhereTarget() {
		return new DBDirectedEdgeIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND en.type=" + DBGraph.TARGET+
				" AND en.ndbid="+this.dbid);
	}

	public Iterator<DirectedEdge> getEdgesWhereTarget(String schemaID) {
		return new DBDirectedEdgeIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND en.type=" + DBGraph.TARGET+
				" AND e.sid=" + schemaID+
				" AND en.ndbid="+this.dbid);
	}

	public Iterator<UndirectedEdge> getUndirEdges() {
		return new DBUndirectedEdgeIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND en.type=" + DBGraph.UNDIR+
				" AND en.ndbid="+this.dbid);
	}

	public boolean isAdjacentSource(Node n) {
		Iterator<Node> itr = this.getAdjacentSources();
		while(itr.hasNext()) {
			if(itr.next().equals(n)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isAdjacentSource(Node n, String edgeschemaID) {
		Iterator<Node> itr = this.getAdjacentSources(edgeschemaID);
		while(itr.hasNext()) {
			if(itr.next().equals(n)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isAdjacentTarget(Node n) {
		Iterator<Node> itr = this.getAdjacentTargets();
		while(itr.hasNext()) {
			if(itr.next().equals(n)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isAdjacentTarget(Node n, String edgeschemaID) {
		Iterator<Node> itr = this.getAdjacentTargets(edgeschemaID);
		while(itr.hasNext()) {
			if(itr.next().equals(n)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isIncident(Edge e) {
		return this.g.queryHasResult("SELECT edbid" +
				" FROM edgenodes" +
				" WHERE edbid="+((DBEdge) e).dbid+
				" AND ndbid="+this.dbid);
	}

	public int numAdjacentSources() {
		return IteratorUtils.numIterable(this.getAdjacentSources());
	}

	public int numAdjacentSources(String schemaID) {
		return IteratorUtils.numIterable(this.getAdjacentSources(schemaID));
	}

	public int numAdjacentTargets() {
		return IteratorUtils.numIterable(this.getAdjacentTargets());
	}

	public int numAdjacentTargets(String edgeschemaID) {
		return IteratorUtils.numIterable(this.getAdjacentTargets(edgeschemaID));
	}

	public int numEdges() {
		return IteratorUtils.numIterable(this.getAllEdges());
	}

	public void removeIncidentEdges() {
		Iterator<Edge> itr = this.getAllEdges();
		while(itr.hasNext()) {
			Edge e = itr.next();
			
			try {
				e.removeNode(this);
			} catch(InvalidStateException ex) {
				g.removeEdge(e);
			}
		}
	}

	public void removeIncidentEdges(String edgeschemaid) {
		Iterator<Edge> itr = this.getAllEdges(edgeschemaid);
		while(itr.hasNext()) {
			Edge e = itr.next();
			
			try {
				e.removeNode(this);
			} catch(InvalidStateException ex) {
				g.removeEdge(e);
			}
		}
	}

	public Iterator<GraphItem> getAdjacentGraphItems() {
		return new DBGraphItemIterator(this.g,
			"SELECT DISTINCT n.dbid, n.sid, n.oid" +
			" FROM nodes n, edgenodes en" +
			" WHERE n.dbid=en.ndbid" +
			" AND n.dbid<>"+this.dbid+
			" AND en.edbid IN" +
			" (SELECT edbid FROM edgenodes WHERE ndbid="+this.dbid+")"	
			);
	}

	public Iterator<GraphItem> getAdjacentGraphItems(String incidentsid) {
		return new DBGraphItemIterator(this.g,
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid" +
				" AND n.dbid<>"+this.dbid+
				" AND en.edbid in" +
				" (SELECT edbid FROM edgenodes en, edges e" +
				" WHERE en.edbid=e.dbid AND e.sid='"+incidentsid+"'"+
				" AND ndbid="+this.dbid+")"	
				);
	}
	
	public Iterator<Node> getAdjacentNodes() {
		return new DBNodeIterator(this.g,
			"SELECT DISTINCT n.dbid, n.sid, n.oid" +
			" FROM nodes n, edgenodes en" +
			" WHERE n.dbid=en.ndbid" +
			" AND n.dbid<>"+this.dbid+
			" AND en.edbid IN" +
			" (SELECT edbid FROM edgenodes WHERE ndbid="+this.dbid+")"	
			);
	}

	public Iterator<Node> getAdjacentNodes(String incidentsid) {
		return new DBNodeIterator(this.g,
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid" +
				" AND n.dbid<>"+this.dbid+
				" AND en.edbid in" +
				" (SELECT edbid FROM edgenodes en, edges e" +
				" WHERE en.edbid=e.dbid AND e.sid='"+incidentsid+"'"+
				" AND ndbid="+this.dbid+")"	
				);
	}

	public Iterator<GraphItem> getIncidentGraphItems() {
		return new DBGraphItemIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid AND en.ndbid="+this.dbid);
	}

	public Iterator<GraphItem> getIncidentGraphItems(String sid) {
		return new DBGraphItemIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND e.sid='"+sid+"'"+
				" AND en.ndbid="+this.dbid);
	}

	public Iterator<GraphItem> getIncidentGraphItems(GraphItem adjacent) {
		return new DBGraphItemIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE en.ndbid="+((DBGraphItem) adjacent).dbid +
				" AND en.edbid in (SELECT edbid FROM edgenodes WHERE ndbid="+this.dbid+")"
				);
	}
	
	public Iterator<GraphItem> getIncidentGraphItems(String schemaID, GraphItem adjacent) {
		return new DBGraphItemIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE en.ndbid="+((DBGraphItem) adjacent).dbid +
				" AND e.sid='"+schemaID+"'" +
				" AND en.edbid in (SELECT edbid FROM edgenodes WHERE ndbid="+this.dbid+")"
				);
	}

	public boolean isAdjacent(GraphItem gi) {
		Iterator<GraphItem> itr = this.getAdjacentGraphItems();
		while(itr.hasNext()) {
			if(itr.next().equals(gi)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isAdjacent(GraphItem gi, String incidentsid) {
		Iterator<GraphItem> itr = this.getAdjacentGraphItems(incidentsid);
		while(itr.hasNext()) {
			if(itr.next().equals(gi)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isIncident(GraphItem gi) {
		Iterator<GraphItem> itr = this.getIncidentGraphItems();
		while(itr.hasNext()) {
			if(itr.next().equals(gi)) {
				return true;
			}
		}
		
		return false;
	}

	public int numAdjacentGraphItems() {
		return IteratorUtils.numIterable(this.getAdjacentGraphItems());
	}

	public int numAdjacentGraphItems(String incidentsid) {
		return IteratorUtils.numIterable(this.getAdjacentGraphItems(incidentsid));
	}

	public int numIncidentGraphItems() {
		return IteratorUtils.numIterable(this.getIncidentGraphItems());
	}

	public int numIncidentGraphItems(String sid) {
		return IteratorUtils.numIterable(this.getIncidentGraphItems(sid));
	}

	public int numIncidentGraphItems(GraphItem adjacent) {
		return IteratorUtils.numIterable(this.getIncidentGraphItems(adjacent));
	}
	
	public int numAdjacentNodes() {
		return IteratorUtils.numIterable(this.getAdjacentNodes());
	}

	public int numAdjacentNodes(String edgeschemaID) {
		return IteratorUtils.numIterable(this.getAdjacentNodes(edgeschemaID));
	}
	
	public int numEdgesWhereSource() {
		return IteratorUtils.numIterable(this.getEdgesWhereSource());
	}

	public int numEdgesWhereSource(String schemaID) {
		return IteratorUtils.numIterable(this.getEdgesWhereSource(schemaID));
	}

	public int numEdgesWhereTarget() {
		return IteratorUtils.numIterable(this.getEdgesWhereTarget());
	}

	public int numEdgesWhereTarget(String schemaID) {
		return IteratorUtils.numIterable(this.getEdgesWhereTarget(schemaID));
	}

	public int numUndirEdges() {
		return IteratorUtils.numIterable(this.getUndirEdges());
	}

	public int numDirEdges() {
		return IteratorUtils.numIterable(this.getDirEdges());
	}
	
	public String toString(){
		return GraphItemUtils.getNodeIDString(this);
	}
}
