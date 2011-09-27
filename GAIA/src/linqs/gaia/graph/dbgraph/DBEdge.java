package linqs.gaia.graph.dbgraph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.IteratorUtils;

public abstract class DBEdge extends DBGraphItem implements Edge {
	public DBEdge(DBGraph g, GraphItemID gid, int dbid) {
		super(g, gid, dbid);
	}

	public Iterator<Node> getAllNodes() {
		return new DBNodeIterator(this.g, "SELECT DISTINCT n.dbid, n.sid, n.oid"+
				" FROM nodes n, edgenodes en" +
				" WHERE en.edbid="+this.dbid+
				" AND n.dbid=en.ndbid");
	}

	public Iterator<Node> getAllNodes(String schemaID) {
		return new DBNodeIterator(this.g, "SELECT DISTINCT n.dbid, n.sid, n.oid"+
				" FROM nodes n, edgenodes en" +
				" WHERE en.edbid="+this.dbid+
				" AND n.dbid=en.ndbid"+
				" AND n.sid='"+schemaID+"'");
	}

	public boolean hasSameNodes(Edge e) {
		Iterator<Node> nitr = new DBNodeIterator(this.g, "SELECT DISTINCT n.dbid, n.sid, n.oid"+
				" FROM nodes n, edgenodes en" +
				" WHERE en.edbid="+this.dbid+
				" AND n.dbid=en.ndbid");
		
		Set<Node> currnodes = new HashSet<Node>();
		while(nitr.hasNext()) {
			currnodes.add(nitr.next());
		}
		
		DBEdge dbe = (DBEdge) this.g.getEdge(e.getID());
		nitr = new DBNodeIterator(this.g, "SELECT DISTINCT n.dbid, n.sid, n.oid"+
				" FROM nodes n, edgenodes en" +
				" WHERE en.edbid="+dbe.dbid+
				" AND n.dbid=en.ndbid");
		
		Set<Node> enodes = new HashSet<Node>();
		while(nitr.hasNext()) {
			enodes.add(nitr.next());
		}
		
		return currnodes.equals(enodes);
	}

	public boolean isIncident(Node n) {
		Iterator<Node> nitr = new DBNodeIterator(this.g, "SELECT DISTINCT n.dbid, n.sid, n.oid"+
				" FROM nodes n, edgenodes en" +
				" WHERE n.dbid=en.ndbid" +
				" AND en.edbid="+this.dbid);
		
		while(nitr.hasNext()) {
			if(nitr.next().equals(n)) {
				return true;
			}
		}
		
		return false;
	}

	public int numNodes() {
		Iterator<Node> nitr = this.getAllNodes();
		return IteratorUtils.numIterable(nitr);
	}
	
	public String toString(){
		return GraphItemUtils.getEdgeIDString(this);
	}

}
