package linqs.gaia.graph.dbgraph;

import java.util.Iterator;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.IteratorUtils;

public class DBUndirectedEdge extends DBEdge implements UndirectedEdge {
	protected int id = 0;
	
	public DBUndirectedEdge(DBGraph g, GraphItemID gid, int dbid) {
		super(g, gid, dbid);
	}

	public void addNode(Node n) {
		if(this.isIncident(n)) {
			throw new InvalidOperationException("Node already part of the undirected edge: "
						+n+" in edge "+this);
		}
		
		// Add node
		this.g.executeSQL("INSERT INTO edgenodes" +
				" (edbid, type, ndbid)"+
				"VALUES ("+this.dbid+","+DBGraph.UNDIR+","+((DBNode) n).dbid+")");
	}

	public Iterator<GraphItem> getAdjacentGraphItems() {
		return new DBGraphItemIterator(this.g,
				"SELECT DISTINCT e.dbid, e.sid, e.oid"+
				" FROM edges e, edgenodes en" +
				" WHERE e.dbid=en.edbid" +
				" AND e.dbid<>"+ this.dbid +
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
				" AND e.dbid<>"+ this.dbid +
				" AND en.ndbid in " +
					" (SELECT ndbid FROM edgenodes en, nodes n" +
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
					" (SELECT ndbid FROM edgenodes" +
					" WHERE edbid="+((DBGraphItem) adjacent).dbid+")");
	}
	
	public Iterator<GraphItem> getIncidentGraphItems(String schemaID, GraphItem adjacent) {
		return new DBGraphItemIterator(this.g, 
				"SELECT DISTINCT n.dbid, n.sid, n.oid" +
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid AND en.edbid="+this.dbid+
				" AND n.sid='"+schemaID+"'" +
				" AND n.dbid in" +
					" (SELECT ndbid FROM edgenodes" +
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
		// Add node
		this.g.executeSQL("DELETE FROM edgenodes"
				+" WHERE edbid="+this.dbid
				+" AND ndbid="+((DBNode) n).dbid);
		
		if(this.numNodes()==0){
			throw new InvalidStateException("Removing node resulted in invalid edge: " +
					"Removing "+GraphItemUtils.getNodeIDString(n)+" invalidated "+this);
		}
	}

}
