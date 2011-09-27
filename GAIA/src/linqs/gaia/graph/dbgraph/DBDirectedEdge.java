package linqs.gaia.graph.dbgraph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.IteratorUtils;

public class DBDirectedEdge extends DBEdge implements DirectedEdge {
	protected int id = 0;
	
	public DBDirectedEdge(DBGraph g, GraphItemID gid, int dbid) {
		super(g, gid, dbid);
	}

	public void addSourceNode(Node n) {
		if(!this.getGraph().hasNode(n.getID())) {
			throw new InvalidStateException("Node not a part of this graph: "+n);
		}
		
		if(this.isSource(n)) {
			throw new InvalidOperationException(
					"Node already in source of directed edge: "+n+" in edge "+this);
		}
		
		// Add node
		this.g.executeSQL("INSERT INTO edgenodes (edbid, type, ndbid)"+
				" VALUES ("+this.dbid+","+DBGraph.SOURCE+","+((DBNode) n).dbid+")");
	}

	public void addTargetNode(Node n) {
		if(!this.getGraph().hasNode(n.getID())) {
			throw new InvalidStateException("Node not a part of this graph: "+n);
		}
		
		if(this.isTarget(n)) {
			throw new InvalidOperationException(
					"Node already in target of directed edge: "+n+" in edge "+this);
		}
		
		// Add node
		this.g.executeSQL("INSERT INTO edgenodes (edbid, type, ndbid)"+
				"VALUES ("+this.dbid+","+DBGraph.TARGET+","+((DBNode) n).dbid+")");
	}

	public Iterator<Node> getSourceNodes() {
		return new DBNodeIterator(this.g,
				"SELECT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid" +
					" AND en.type="+DBGraph.SOURCE+
					" AND en.edbid="+this.dbid);
	}

	public Iterator<Node> getTargetNodes() {
		return new DBNodeIterator(this.g,
				"SELECT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid" +
					" AND en.type="+DBGraph.TARGET+
					" AND en.edbid="+this.dbid);
	}

	public boolean hasSameSources(DirectedEdge e) {
		Set<Node> sources = new HashSet<Node>();
		Iterator<Node> nitr = this.getSourceNodes();
		while(nitr.hasNext()) {
			sources.add(nitr.next());
		}
		
		Set<Node> esources = new HashSet<Node>();
		nitr = e.getSourceNodes();
		while(nitr.hasNext()) {
			esources.add(nitr.next());
		}
		
		return sources.equals(esources);
	}

	public boolean hasSameTargets(DirectedEdge e) {
		Set<Node> targets = new HashSet<Node>();
		Iterator<Node> nitr = this.getTargetNodes();
		while(nitr.hasNext()) {
			targets.add(nitr.next());
		}
		
		Set<Node> etargets = new HashSet<Node>();
		nitr = e.getTargetNodes();
		while(nitr.hasNext()) {
			etargets.add(nitr.next());
		}
		
		return targets.equals(etargets);
	}

	public boolean isSource(Node n) {
		Iterator<Node> nitr = this.getSourceNodes();
		
		while(nitr.hasNext()) {
			if(nitr.next().equals(n)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isTarget(Node n) {
		Iterator<Node> nitr = this.getTargetNodes();
		
		while(nitr.hasNext()) {
			if(nitr.next().equals(n)) {
				return true;
			}
		}
		
		return false;
	}

	public int numSourceNodes() {
		return IteratorUtils.numIterable(this.getSourceNodes());
	}

	public int numTargetNodes() {
		return IteratorUtils.numIterable(this.getTargetNodes());
	}

	public void removeSourceNode(Node n) {
		this.g.executeSQL("DELETE FROM edgenodes where edbid="+this.dbid
				+" AND type="+DBGraph.SOURCE
				+" AND ndbid="+((DBNode) n).dbid);
		
		if(this.numSourceNodes()==0){
			throw new InvalidStateException("Removing node resulted in invalid edge: " +
					"Removing "+GraphItemUtils.getNodeIDString(n)+" invalidated "+this);
		}
	}

	public void removeTargetNode(Node n) {
		this.g.executeSQL("DELETE FROM edgenodes" +
				" WHERE edbid="+this.dbid
				+" AND type="+DBGraph.TARGET
				+" AND ndbid="+((DBNode) n).dbid);
		
		if(this.numTargetNodes()==0){
			throw new InvalidStateException("Removing node resulted in invalid edge: " +
					"Removing "+GraphItemUtils.getNodeIDString(n)+" invalidated "+this);
		}
	}

	public Iterator<GraphItem> getAdjacentGraphItems() {
		return new DBGraphItemIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid"+
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND e.dbid<>"+this.dbid +
				" AND en.ndbid in" +
				" (SELECT ndbid" +
				" FROM edgenodes" +
				" WHERE edbid="+this.dbid+")");
	}

	public Iterator<GraphItem> getAdjacentGraphItems(String incidentsid) {
		return new DBGraphItemIterator(this.g, 
				"SELECT DISTINCT e.dbid, e.sid, e.oid" +
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND e.dbid<>"+this.dbid +
				" AND en.ndbid in " +
					" (SELECT ndbid" +
						" FROM edgenodes en, nodes n" +
						" WHERE en.ndbid=n.dbid AND n.sid='"+incidentsid+"'" +
						" AND en.edbid="+this.dbid+")");
	}

	public Iterator<GraphItem> getIncidentGraphItems() {
		return new DBGraphItemIterator(this.g, 
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid AND en.edbid="+this.dbid);
	}

	public Iterator<GraphItem> getIncidentGraphItems(String sid) {
		return new DBGraphItemIterator(this.g, 
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.sid='"+sid+"' AND n.dbid=en.ndbid AND en.edbid="+this.dbid);
	}

	public Iterator<GraphItem> getIncidentGraphItems(GraphItem adjacent) {
		return new DBGraphItemIterator(this.g, 
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid AND en.edbid="+this.dbid+
				" AND n.dbid in" +
					" (SELECT ndbid" +
					" FROM edgenodes" +
					" WHERE edbid="+((DBGraphItem) adjacent).dbid+")");
	}
	
	public Iterator<GraphItem> getIncidentGraphItems(String schemaID, GraphItem adjacent) {
		return new DBGraphItemIterator(this.g, 
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid AND en.edbid="+this.dbid +
				" AND n.sid='"+schemaID+"'" +
				" AND n.dbid in" +
					" (SELECT ndbid" +
					" FROM edgenodes" +
					" WHERE edbid="+((DBGraphItem) adjacent).dbid+")");
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

	public void removeNode(Node n) {
		boolean removed = false;
		if(this.isSource(n)){
			removed = true;
			this.removeSourceNode(n);
		}
		
		if(this.isTarget(n)){
			removed = true;
			this.removeTargetNode(n);
		}
		
		if(!removed){
			throw new InvalidOperationException("Node is not incident on this edge: "
					+GraphItemUtils.getNodeIDString(n));
		}
	}

}
