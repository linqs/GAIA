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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import linqs.gaia.exception.InvalidAssignmentException;
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
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.graph.event.EdgeAddedEvent;
import linqs.gaia.graph.event.EdgeRemovedEvent;
import linqs.gaia.graph.event.FeatureSetEvent;
import linqs.gaia.graph.event.GraphEvent;
import linqs.gaia.graph.event.GraphEventListener;
import linqs.gaia.graph.event.NodeAddedEvent;
import linqs.gaia.graph.event.NodeRemovedEvent;
import linqs.gaia.graph.registry.GraphRegistry;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.SQLHelper;
import linqs.gaia.util.UnmodifiableList;

/**
 * Implementation of a GAIA graph which uses an embedded database (Derby)
 * in the backend.  This is designed for loading and processing
 * large graphs.
 * <p>
 * Warning: The count operators (e.g., numNodes()) are naively implemented
 * and will require unnecessary iteration over items.  These need
 * to be corrected before heavy use.
 * <p>
 * Note: This graph implementation requires the Derby libraries
 * be in the class path (i.e., derby.jar).
 * <p>
 * Optional:
 * <UL>
 * <LI> dbfilespath-Directory path to store database files in embedded database
 * <UL>
 * 
 * @author namatag
 *
 */
public class DBGraph extends DBSchemaManager implements Graph {
	private GraphID gid;
	private boolean isInitialized = false;
	private int dbidcounter = 0;
	
	private static int graphdbidcounter = 0;
	private int dbid = 0;

	public static int UNDIR = 0;
	public static int SOURCE = 1;
	public static int TARGET = 2;
	
	/**
	 * Store all the graph events listeners for the graph
	 */
	private Collection <GraphEventListener> eventlisteners;

	/**
	 * Database connection.  Only access through getConnection() method
	 */
	private Connection conn = null;

	public DBGraph(GraphID gid) {
		this.gid = gid;
		this.dbid = graphdbidcounter++;
		eventlisteners = new ArrayList<GraphEventListener>();
	}
	
	public Connection getConnection() {
		// Return conn if already initialized
		if(conn!=null) {
			return conn;
		}
		
		String dbName = this.getID().getObjID();
		
		String dbfilespath = null;
		if(this.hasParameter("dbfilespath")) {
			dbfilespath = this.getStringParameter("dbfilespath");
		} else {
			dbfilespath = FileIO.getTemporaryDirectory();
		}
		
		// Specify derby properties
		System.setProperty("derby.storage.pageCacheSize", "10000");
		
		String url = "jdbc:derby:"+dbfilespath+"/"+dbName+"; create=true";
		
		// Create database connection
		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			conn = DriverManager.getConnection(url);
		} catch (Exception e) {
			throw new RuntimeException("Unable to connect with" + " url="+url,e);
		}
		
		// Initialize graph during the first call to getConnection()
		if(!isInitialized) {
			this.initializeGraph();
		}

		return conn;
	}

	private void initializeGraph() {
		isInitialized = true;

		// Create table to hold graph schema
		String sid = this.getSchemaID();
		this.addSchema(sid, new Schema(SchemaType.GRAPH));
		
		// Add node and edges graph
		this.dropTableIfExists("nodes");
		this.dropTableIfExists("edges");
		this.dropTableIfExists("edgenodes");
		this.executeSQL("CREATE TABLE nodes (dbid int PRIMARY KEY," +
				" sid varchar(256), oid varchar(256))");
		this.executeSQL("CREATE INDEX nodeindex on nodes (sid,oid)");
		this.executeSQL("CREATE TABLE edges (dbid int PRIMARY KEY," +
				" sid varchar(256), oid varchar(256))");
		this.executeSQL("CREATE INDEX edgesindex on edges (sid,oid)");
		this.executeSQL("CREATE TABLE edgenodes (edbid int, type int, ndbid int," +
				" FOREIGN KEY (edbid) REFERENCES edges(dbid) ON DELETE CASCADE," +
				" FOREIGN KEY (ndbid) REFERENCES nodes(dbid) ON DELETE CASCADE)");
		this.executeSQL("CREATE INDEX enindex1 on edgenodes (edbid,type,ndbid)");
		this.executeSQL("CREATE INDEX enindex2 on edgenodes (ndbid)");
		
		// Create system data tables
		this.initializeSystemManager();
	}

	public DirectedEdge addDirectedEdge(GraphItemID id, Iterator<Node> sources,
			Iterator<Node> targets) {
		String sid = id.getSchemaID();
		String oid = id.getObjID();
		
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.getID(), sid, oid);
		} else if(!id.getGraphID().equals(this.getID())) {
			throw new InvalidOperationException("Adding a edge with wrong Graph ID: "
					+id.getGraphID()
					+" not "
					+this.getID());
		}

		if(!this.hasSchema(sid)) {
			throw new InvalidOperationException("Adding an edge with an undefined schema: "+id);
		}

		SchemaType idschematype = this.getSchema(sid).getType();
		if(!idschematype.equals(SchemaType.DIRECTED)) {
			throw new InvalidOperationException("Specified schema id is not a directed edge schema: "
					+id+" with schema type "+idschematype.name());
		}

		// Add edge to edges table
		int dbid = dbidcounter++;
		this.executeSQL("INSERT INTO edges (dbid, sid, oid) VALUES ("+dbid+",'"+sid+"','"+oid+"')");

		DBDirectedEdge dbde = new DBDirectedEdge(this, id, dbid);

		// Add sources and targets to the edge table
		int numsources = 0;
		while(sources.hasNext()) {
			Node n = sources.next();
			dbde.addSourceNode(n);
			numsources++;
		}

		int numtargets = 0;
		while(targets.hasNext()) {
			Node n = targets.next();
			dbde.addTargetNode(n);
			numtargets++;
		}

		if(numsources == 0 || numtargets == 0) {
			throw new InvalidStateException("Attempting to add invalid edge: "+id);
		}
		
		DirectedEdge e = new DBDirectedEdge(this, id, dbid);
		this.processListeners(new EdgeAddedEvent(e));
		
		return e;
	}

	public DirectedEdge addDirectedEdge(GraphItemID id, Iterable<Node> sources,
			Iterable<Node> targets) {
		return this.addDirectedEdge(id, sources.iterator(), targets.iterator());
	}

	public DirectedEdge addDirectedEdge(GraphItemID id, Node source, Node target) {
		if(source == null) {
			throw new InvalidOperationException("Source node is null for edge "+id);
		}

		if(target == null) {
			throw new InvalidOperationException("Target node is null for edge "+id);
		}

		return addDirectedEdge(id,
				Arrays.asList(new Node[]{source}).iterator(),
				Arrays.asList(new Node[]{target}).iterator());
	}

	public void addListener(GraphEventListener gel) {
		this.eventlisteners.add(gel);
	}

	public Node addNode(GraphItemID id) {
		String sid = id.getSchemaID();
		String oid = id.getObjID();
		
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.getID(), sid, oid);
		} else if(!id.getGraphID().equals(this.getID())) {
			throw new InvalidOperationException("Adding a node with wrong Graph ID: "
					+id.getGraphID()
					+" not "
					+this.getID());
		}

		if(!this.hasSchema(id.getSchemaID())) {
			throw new InvalidOperationException("Adding a node with an undefined schema: "
					+id+" with schema id "+id.getSchemaID());
		}

		SchemaType schematype = this.getSchema(id.getSchemaID()).getType();
		if(!schematype.equals(SchemaType.NODE)) {
			throw new InvalidOperationException("Specified schema id is not a node schema: "
					+id+" with schema type "+schematype.name());
		}

		// Add node to nodes table
		int dbid = dbidcounter++;
		this.executeSQL("INSERT INTO nodes (dbid, sid, oid) VALUES ("+dbid+",'"+sid+"','"+oid+"')");
		
		Node n = new DBNode(this, id, dbid);
		this.processListeners(new NodeAddedEvent(n));
		
		return n;
	}

	public UndirectedEdge addUndirectedEdge(GraphItemID id, Iterator<Node> nodes) {
		String sid = id.getSchemaID();
		String oid = id.getObjID();

		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.getID(), sid, oid);
		} else if(!id.getGraphID().equals(this.getID())) {
			throw new InvalidOperationException("Adding a edge with wrong Graph ID: "
					+id.getGraphID()
					+" not "
					+this.getID());
		}

		if(!this.hasSchema(sid)) {
			throw new InvalidOperationException("Adding an edge with an undefined schema: "+id);
		}

		SchemaType idschematype = this.getSchema(sid).getType();
		if(!idschematype.equals(SchemaType.UNDIRECTED)) {
			throw new InvalidOperationException("Specified schema id is not a directed edge schema: "
					+id+" with schema type "+idschematype.name());
		}

		// Add edge to edges table
		int dbid = dbidcounter++;
		this.executeSQL("INSERT INTO edges (dbid, sid, oid) VALUES ("+dbid+",'"+sid+"','"+oid+"')");

		DBUndirectedEdge dbde = new DBUndirectedEdge(this, id, dbid);

		// Add sources and targets to the edge table
		int numnodes = 0;
		while(nodes.hasNext()) {
			Node n = nodes.next();
			dbde.addNode(n);
			numnodes++;
		}

		if(numnodes == 0) {
			throw new InvalidStateException("Attemting to add invalid edge: "+id);
		}
		
		UndirectedEdge e = new DBUndirectedEdge(this, id, dbid);
		this.processListeners(new EdgeAddedEvent(e));

		return e;
	}

	public UndirectedEdge addUndirectedEdge(GraphItemID id, Iterable<Node> nodes) {
		return this.addUndirectedEdge(id, nodes.iterator());
	}

	public UndirectedEdge addUndirectedEdge(GraphItemID id, Node node1,
			Node node2) {
		if(node1 == null) {
			throw new InvalidOperationException("First node is null for edge "+id);
		}

		if(node2 == null) {
			throw new InvalidOperationException("Second node is null for edge "+id);
		}

		return addUndirectedEdge(id, Arrays.asList(new Node[]{node1,node2}));
	}
	
	public UndirectedEdge addUndirectedEdge(GraphItemID id, Node node) {
		if(node == null) {
			throw new InvalidOperationException("First node is null for edge "+id);
		}

		return addUndirectedEdge(id, Arrays.asList(new Node[]{node}));
	}

	public Graph copy(String objid) {
		GraphID copyid = new GraphID(this.getSchemaID(), objid);
		Graph copyg = new DBGraph(copyid);

		GraphUtils.copy(this, copyg);

		return copyg;
	}
	
	public void copy(Graph g) {
		GraphUtils.copy(this, g);
	}
	
	public Graph copySchema(String objid) {
		GraphID copyid = new GraphID(this.getSchemaID(), objid);
		Graph copyg = new DBGraph(copyid);
		GraphUtils.copySchema(this, copyg);
		
		return copyg;
	}
	
	public void destroy() {
		this.removeAllSchemas();
		this.dropTableIfExists("edgenodes");
		this.dropTableIfExists("edges");
		this.dropTableIfExists("nodes");
		
		GraphRegistry.removeGraph(this);
		
		// Close the connection
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Edge getEdge(GraphItemID id) {
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.gid, id.getSchemaID(), id.getObjID());
		} else if(!gid.equals(this.gid)) {
			return null;
		}

		List<Integer> results = this.queryDatabaseInteger(
				"SELECT dbid FROM edges"
				+" WHERE sid='"+id.getSchemaID()+"'"
				+" AND oid='"+id.getObjID()+"'", "dbid");

		if(results.isEmpty()) {
			return null;
		}

		int dbid = results.get(0);
		SchemaType stype = this.getSchemaType(id.getSchemaID());
		if(stype.equals(SchemaType.DIRECTED)) {
			return new DBDirectedEdge(this, id, dbid);
		} else if(stype.equals(SchemaType.UNDIRECTED)) {
			return new DBUndirectedEdge(this, id, dbid);
		} else {
			throw new InvalidStateException("Unrecognized edge schema type: "+stype);
		}
	}

	public Iterator<Edge> getEdges() {
		return new DBEdgeIterator(this, "SELECT dbid, sid, oid FROM edges");
	}

	public Iterator<Edge> getEdges(String schemaID) {
		return new DBEdgeIterator(this, "SELECT dbid, sid, oid FROM edges WHERE sid='"+schemaID+"'");
	}

	public GraphItem getEquivalentGraphItem(GraphItemID id) {
		return this.getGraphItem(new GraphItemID(this.getID(),
				id.getSchemaID(), id.getObjID()));
	}
	
	public GraphItem getEquivalentGraphItem(GraphItem gi) {
		return this.getEquivalentGraphItem(gi.getID());
	}

	public GraphItem getGraphItem(GraphItemID id) {
		String sid = id.getSchemaID();
		SchemaType stype = this.getSchemaType(sid);
		if(stype.equals(SchemaType.NODE)) {
			return this.getNode(id);
		} else if(stype.equals(SchemaType.DIRECTED) || stype.equals(SchemaType.UNDIRECTED)) {
			return this.getEdge(id);
		} else {
			throw new InvalidStateException("Unrecognized Graph Item SchemaType: "+stype);
		}
	}

	public Iterator<GraphItem> getGraphItems(String schemaID) {
		SchemaType stype = this.getSchemaType(schemaID);

		if(stype.equals(SchemaType.NODE)) {
			return new DBGraphItemIterator(this,
					"SELECT dbid, sid, oid"
					+" FROM nodes"
					+" WHERE sid='"+schemaID+"'");
		} else if(stype.equals(SchemaType.DIRECTED) || stype.equals(SchemaType.UNDIRECTED)) {
			return new DBGraphItemIterator(this,
					"SELECT dbid, sid, oid"
					+" FROM edges"
					+" WHERE sid='"+schemaID+"'");
		} else {
			throw new InvalidStateException("Unrecognized Graph Item SchemaType: "+stype);
		}
	}

	public Iterator<GraphItem> getGraphItems() {
		return new DBGraphItemIterator(this,
				"SELECT dbid, sid, oid FROM nodes"
				+" UNION"
				+" SELECT dbid, sid, oid from edges");
	}

	public Iterable<Edge> getIterableEdges() {
		return new DBEdgeIterable(this, "SELECT dbid, sid, oid FROM edges");
	}

	public Iterable<Edge> getIterableEdges(String schemaID) {
		return new DBEdgeIterable(this, "SELECT dbid, sid, oid"
				+" FROM edges"
				+" WHERE sid='"+schemaID+"'");
	}

	public Iterable<GraphItem> getIterableGraphItems(String schemaID) {
		SchemaType stype = this.getSchemaType(schemaID);

		if(stype.equals(SchemaType.NODE)) {
			return new DBGraphItemIterable(this,
					"SELECT dbid, sid, oid FROM nodes WHERE sid='"+schemaID+"'");
		} else if(stype.equals(SchemaType.DIRECTED) || stype.equals(SchemaType.UNDIRECTED)) {
			return new DBGraphItemIterable(this,
					"SELECT dbid, sid, oid FROM edges WHERE sid='"+schemaID+"'");
		} else {
			throw new InvalidStateException("Unrecognized Graph Item SchemaType: "+stype);
		}
	}

	public Iterable<GraphItem> getIterableGraphItems() {
		return new DBGraphItemIterable(this,
				"SELECT dbid, sid, oid FROM nodes"
				+" UNION"
				+" SELECT dbid, sid, oid from edges");
	}

	public Iterable<Node> getIterableNodes() {
		return new DBNodeIterable(this, "SELECT dbid, sid, oid FROM nodes");
	}

	public Iterable<Node> getIterableNodes(String schemaID) {
		return new DBNodeIterable(this, "SELECT dbid, sid, oid FROM nodes WHERE sid='"+schemaID+"'");
	}

	public Node getNode(GraphItemID id) {
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.gid, id.getSchemaID(), id.getObjID());
		} else if(!gid.equals(this.gid)) {
			return null;
		}

		List<Integer> results = this.queryDatabaseInteger(
				"SELECT dbid" +
				" FROM nodes" +
				" WHERE sid='"+id.getSchemaID()+"' AND oid='"+id.getObjID()+"'", "dbid");

		if(results.isEmpty()) {
			return null;
		}

		int dbid = results.get(0);

		return new DBNode(this, id, dbid);
	}

	public Iterator<Node> getNodes() {
		return new DBNodeIterator(this, "SELECT dbid, sid, oid FROM nodes");
	}

	public Iterator<Node> getNodes(String schemaID) {
		return new DBNodeIterator(this, "SELECT dbid, sid, oid FROM nodes where sid='"+schemaID+"'");
	}

	public boolean hasEdge(GraphItemID id) {
		GraphID gid = id.getGraphID();
		if(gid!=null && !gid.equals(this.getID())) {
			return false;
		}
		
		return this.queryHasResult("SELECT dbid" +
				" FROM edges" +
				" WHERE sid='"+id.getSchemaID()+"' AND oid='"+id.getObjID()+"'");
	}

	public boolean hasGraphItem(GraphItemID id) {
		GraphID gid = id.getGraphID();
		if(gid!=null && !gid.equals(this.getID())) {
			return false;
		}
		
		return this.queryHasResult("SELECT dbid" +
				" FROM edges" +
				" WHERE sid='"+id.getSchemaID()+"' AND oid='"+id.getObjID()+"'")
				||
				this.queryHasResult("SELECT dbid" +
						" FROM nodes" +
						" WHERE sid='"+id.getSchemaID()+"' AND oid='"+id.getObjID()+"'");
	}
	
	public boolean hasEquivalentGraphItem(GraphItemID id) {
		return this.queryHasResult("SELECT dbid" +
				" FROM edges" +
				" WHERE sid='"+id.getSchemaID()+"' AND oid='"+id.getObjID()+"'")
				||
				this.queryHasResult("SELECT dbid" +
						" FROM nodes" +
						" WHERE sid='"+id.getSchemaID()+"' AND oid='"+id.getObjID()+"'");
	}
	
	public boolean hasEquivalentGraphItem(GraphItem gi) {
		return this.hasEquivalentGraphItem(gi.getID());
	}

	public boolean hasNode(GraphItemID id) {
		GraphID gid = id.getGraphID();
		if(gid!=null && !gid.equals(this.getID())) {
			return false;
		}
		
		return this.queryHasResult("SELECT dbid" +
				" FROM nodes" +
				" WHERE sid='"+id.getSchemaID()+"' AND oid='"+id.getObjID()+"'");
	}

	public int numEdges() {
		return this.queryDatabaseInteger("SELECT count(*) rowcount" +
				" FROM edges", "rowcount").get(0);
	}

	public int numGraphItems(String schemaID) {
		SchemaType stype = this.getSchemaType(schemaID);

		if(stype.equals(SchemaType.NODE)) {
			return this.queryDatabaseInteger("SELECT count(*) rowcount" +
					" FROM nodes" +
					" WHERE sid='"+schemaID+"'", "rowcount").get(0);
		} else if(stype.equals(SchemaType.DIRECTED) || stype.equals(SchemaType.UNDIRECTED)) {
			return this.queryDatabaseInteger("SELECT count(*) rowcount" +
					" FROM edges" +
					" WHERE sid='"+schemaID+"'", "rowcount").get(0);
		} else {
			throw new InvalidStateException("Unrecognized Graph Item SchemaType: "+stype);
		}
	}

	public int numNodes() {
		return this.queryDatabaseInteger("SELECT count(*) rowcount FROM nodes", "rowcount").get(0);
	}

	public void processListeners(GraphEvent event) {
		for(GraphEventListener gel:this.eventlisteners){
			gel.execute(event);
		}
	}
	
	public void removeAllSchemas() {
		Set<String> schemaids = new HashSet<String>(this.id2schema.keySet());
		for(String sid:schemaids){
			if(sid.equals(this.getSchemaID())) {
				// Cannot delete the schema of the containing graph
				continue;
			}
			
			this.removeSchema(sid);
		}
	}

	public void removeAllEdges() {
		// Delete relationship tables
		//this.executeSQL("TRUNCATE TABLE edgenodes");
		this.truncateTable("edgenodes");
		
		// Go through all schemas and process edge schemas
		Iterator<String> sitr = this.getAllSchemaIDs();
		while(sitr.hasNext()) {
			String sid = sitr.next();
			SchemaType stype = this.getSchemaType(sid);
			if(stype.equals(SchemaType.DIRECTED) || stype.equals(SchemaType.UNDIRECTED)) {
				// Delete tables with edge schema
				//this.executeSQL("TRUNCATE TABLE "+this.getTableName(sid));
				this.truncateTable(this.getTableName(sid));
			}
		}

		//this.executeSQL("TRUNCATE TABLE edges");
		this.truncateTable("edges");
	}

	public void removeAllGraphItems(String schemaID) {
		SchemaType stype = this.getSchemaType(schemaID);

		if(stype.equals(SchemaType.NODE)) {
			//this.executeSQL("TRUNCATE table "+this.getTableName(schemaID));
			this.truncateTable(this.getTableName(schemaID));
			
			this.executeSQL("DELETE FROM nodes WHERE sid='"+schemaID+"'");
		} else if(stype.equals(SchemaType.DIRECTED) || stype.equals(SchemaType.UNDIRECTED)) {
			this.executeSQL("DELETE FROM "+this.getTableName(schemaID));
			this.executeSQL("DELETE FROM edgenodes WHERE dbid IN" +
					" (SELECT dbid from edges where sid='"+schemaID+"')");
			//this.executeSQL("TRUNCATE table "+this.getTableName(schemaID));
			this.truncateTable(this.getTableName(schemaID));
			this.executeSQL("DELETE from edges where sid='"+schemaID+"'");
		} else {
			throw new InvalidStateException("Unrecognized Graph Item SchemaType: "+stype);
		}
	}

	public void removeAllListeners() {
		this.eventlisteners.clear();
	}

	public void removeAllNodes() {
		// Remove all edges
		this.removeAllEdges();

		//this.executeSQL("TRUNCATE TABLE nodes");
		this.truncateTable("nodes");
	}
	
	public void removeNodesWithEdges(String schemaID) {
		List<Node> nodes = IteratorUtils.iterator2nodelist(this.getNodes(schemaID));
		
		for(Node n:nodes){
			this.removeNodeWithEdges(n);
		}
	}

	public void removeEdge(Edge e) {
		this.removeEdge(e.getID());
	}

	public void removeEdge(GraphItemID id) {
		Edge e = this.getEdge(id);
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.getID(), id.getSchemaID(), id.getObjID());
		} else if(!gid.equals(this.getID())) {
			throw new InvalidOperationException("Removing an edge with wrong Graph ID: "
					+id.getGraphID()
					+" not "
					+this.getID());
		}
		
		List<Integer> dbids = this.queryDatabaseInteger("SELECT dbid" +
				" FROM edges" +
				" WHERE sid='"+id.getSchemaID()+"' AND oid='"+id.getObjID()+"'", "dbid");

		if(dbids.size()==0) {
			throw new InvalidStateException("Edge does not exists: "+id);
		}

		int dbid = dbids.get(0);
		this.removeGraphItemFromSchema(id.getSchemaID(), dbid);
		this.executeSQL("DELETE from edges where dbid="+dbid);
		
		// Create edge removed event
		this.processListeners(new EdgeRemovedEvent(e));
	}

	public void removeGraphItem(GraphItem gi) {
		this.removeGraphItem(gi.getID());
	}

	public void removeGraphItem(GraphItemID id) {
		String schemaID = id.getSchemaID();
		SchemaType stype = this.getSchemaType(schemaID);

		if(stype.equals(SchemaType.NODE)) {
			this.removeNode(id);
		} else if(stype.equals(SchemaType.DIRECTED) || stype.equals(SchemaType.UNDIRECTED)) {
			this.removeEdge(id);
		} else {
			throw new InvalidStateException("Unrecognized Graph Item SchemaType: "+stype);
		}
	}

	public void removeListener(GraphEventListener gel) {
		this.eventlisteners.remove(gel);
	}

	public void removeNode(Node n) {
		this.removeNode(n.getID());
	}

	public void removeNode(GraphItemID id) {
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.getID(), id.getSchemaID(), id.getObjID());
		} else if(!gid.equals(this.getID())) {
			throw new InvalidOperationException("Removing a node with wrong Graph ID: "
					+id.getGraphID()
					+" not "
					+this.getID());
		}
		
		DBNode n = (DBNode) this.getNode(id);

		if(n==null) {
			throw new InvalidStateException("Node does not exists: "+id);
		}

		// Remove nodes from edges
		// Notify affected edges
		Iterator<Edge> edges = n.getAllEdges();
		while(edges.hasNext()) {
			Edge curre = edges.next();
			try{
				curre.removeNode(n);
			} catch(InvalidStateException e) {
				// Remove edge if the node removal causes
				// an edge to be invalid
				this.removeEdge(curre);
			}
		}

		this.removeGraphItemFromSchema(id.getSchemaID(), n.dbid);
		this.executeSQL("DELETE FROM nodes WHERE dbid="+n.dbid);
		
		// Apply graph event
		this.processListeners(new NodeRemovedEvent(n));
	}
	
	public void removeNodeWithEdges(Node n) {
		n.removeIncidentEdges();
		this.removeNode(n);
	}

	public void removeNodeWithEdges(GraphItemID id) {
		Node n = this.getNode(id);
		this.removeNodeWithEdges(n);
	}
	
	private void removeGraphItemFromSchema(String sid, int dbid) {
		this.executeSQL("DELETE FROM "+this.getTableName(sid)+" WHERE dbid="+dbid);
	}

	public FeatureValue getFeatureValue(String featureid) {
		return DBGraph.getDBFeatureValue(this, this, featureid);
	}

	public Schema getSchema() {
		return this.getSchema(this.getSchemaID());
	}

	public String getSchemaID() {
		return this.gid.getSchemaID();
	}

	public boolean hasFeatureValue(String featureid) {
		return DBGraph.getDBFeatureValue(this, this, featureid).equals(FeatureValue.UNKNOWN_VALUE)
			? false: true;
	}

	public void removeFeatureValue(String featureid) {
		this.setFeatureValue(featureid, FeatureValue.UNKNOWN_VALUE);
	}

	public void setFeatureValue(String featureid, FeatureValue value) {
		DBGraph.setDBFeatureValue(this, this, featureid, value);
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
		
		DBGraph.setDBFeatureValue(this, this, featureid, newvalue);
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
		
		DBGraph.setDBFeatureValue(this, this, featureid, newvalue);
	}
	
	protected static void setDBFeatureValue(DBGraph g, Decorable d, String featureid, FeatureValue value) {
		ID id = null;
		int dbid = 0;
		if(d instanceof DBGraphItem) {
			DBGraphItem dbgi = (DBGraphItem) d;
			id = dbgi.getID();
			dbid = dbgi.dbid;
		} else if(d instanceof DBGraph) {
			DBGraph dbgi = (DBGraph) d;
			id = dbgi.getID();
			dbid = dbgi.dbid;
		} else {
			throw new UnsupportedTypeException("Unsupported Decorable type: "+d);
		}
		
		// Can only set explicit features
		Feature f = g.getSchema(id.getSchemaID()).getFeature(featureid);
		if(!(f instanceof ExplicitFeature)) {
			throw new InvalidStateException("Cannot set the feature value of a non-explicit feature");
		}
		
		// Verify that the value is valid
		ExplicitFeature ef = (ExplicitFeature) f;
		if(!ef.isValidValue(value)){
			throw new InvalidAssignmentException("Invalid Value Specified: "+value
					+" is not valid for "+featureid+" "+f);
		}
		
		String stable = g.getTableName(id.getSchemaID());
		FeatureValue previous = d.getFeatureValue(featureid);
		
		// Create a feature value based on the feature type
		if(value.equals(FeatureValue.UNKNOWN_VALUE)) {
			// Handle unknown value
			g.executeSQL("DELETE FROM "+stable
					+" WHERE dbid="+dbid
					+" AND fid='"+featureid+"'");
		} else if(ef.isClosed() && ef.getClosedDefaultValue().equals(value)) {
			// Handle closed default values
			g.executeSQL("DELETE FROM "+stable
					+" WHERE dbid="+dbid
					+" AND fid='"+featureid+"'");
		} else {
			String[] vppair = DBGraph.fv2string(g, f, value);
			String currvalue = vppair[0];
			String currprob = vppair[1];
			//g.executeSQL("INSERT INTO "+stable
			//		+" (dbid, fid, value, prob)"
			//		+" VALUES ("+dbid+", '"+featureid+"', "+currvalue+", "+currprob+")"
			//		+" ON DUPLICATE KEY UPDATE value="+currvalue+", prob="+currprob+"");
			
			g.executeSQL("DELETE FROM "+stable+" where dbid="+dbid+" and fid='"+featureid+"'");
			g.executeSQL("INSERT INTO "+stable
							+" (dbid, fid, value, prob)"
							+" VALUES ("+dbid+", '"+featureid+"', "+currvalue+", "+currprob+")");
		}
		
		g.processListeners(new FeatureSetEvent(d, featureid, previous, value));
	}
	
	private static String[] fv2string(Graph g, Feature f, FeatureValue value) {
		String currvalue = null;
		String currprob =  null;
		
		// Create a feature value based on the feature type
		if(value instanceof NumValue || value instanceof StringValue) {
			currvalue = value.getStringValue();
			currprob = "NULL";
		} else if(value instanceof CategValue) {
			CategValue cv = (CategValue) value;
			double[]  probs = cv.getProbs();
			// Handle categorical values where the prob is set to null
			// Note:  Set true category to 1 and false to 0
			if(probs==null) {
				UnmodifiableList<String> cats = ((CategFeature) f).getAllCategories();
				probs = new double[cats.size()];
				probs[cats.indexOf(cv.getCategory())]=1;
			}
			
			currvalue = cv.getCategory();
			currprob = "'"+ArrayUtils.array2String(probs,",")+"'";
		} else if(value instanceof MultiCategValue) {
			MultiCategValue mcv = (MultiCategValue) value;
			String categories = IteratorUtils.iterator2string(mcv.getCategories().iterator(),",");
			double[]  probs = mcv.getProbs();
			// Handle categorical values where the prob is set to null
			// Note:  Set all probabilities to 1.
			if(probs==null) {
				UnmodifiableList<String> cats = ((MultiCategFeature) f).getAllCategories();
				probs = new double[cats.size()];
				for(int i=0; i<probs.length; i++) {
					probs[i] = 1;
				}
			}
			
			currvalue = categories;
			currprob = "'"+ArrayUtils.array2String(probs,",")+"'";
		} else if(value instanceof MultiIDValue) {
			MultiIDValue mid = (MultiIDValue) value;
			String ids = IteratorUtils.iterator2string(mid.getIDs().iterator(),",");
			
			currvalue = ids;
		} else {
			throw new UnsupportedTypeException("Unsupported Feature Value Type: "+value);
		}
		
		currvalue = SQLHelper.escapeSQLCharacters(currvalue);
		return new String[]{"'"+currvalue+"'",currprob};
	}
	
	protected static void setDBFeatureValues(DBGraph g, Decorable d,
			List<String> featureids, List<FeatureValue> values) {
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
		
		if(!g.eventlisteners.isEmpty()) {
			// To support graph listeners
			// Note: Updating all at once is faster so may need to change this later.
			int size = featureids.size();
			for(int i=0; i<size; i++) {
				d.setFeatureValue(featureids.get(i), values.get(i));
			}
		}
		
		ID id = null;
		int dbid = 0;
		if(d instanceof DBGraphItem) {
			DBGraphItem dbgi = (DBGraphItem) d;
			id = dbgi.getID();
			dbid = dbgi.dbid;
		} else if(d instanceof DBGraph) {
			DBGraph dbgi = (DBGraph) d;
			id = dbgi.getID();
			dbid = dbgi.dbid;
		} else {
			throw new UnsupportedTypeException("Unsupported Decorable type: "+d);
		}
		
		String stable = g.getTableName(id.getSchemaID());
		
		String deletefids = null;
		String inserts = null;
		int counter = featureids.size();
		for(int i=0; i<counter; i++) {
			String featureid = featureids.get(i);
			FeatureValue value = values.get(i);
			
			// Can only set explicit features
			Feature f = g.getSchema(id.getSchemaID()).getFeature(featureid);
			if(!(f instanceof ExplicitFeature)) {
				throw new InvalidStateException("Cannot set the feature value of a non-explicity feature");
			}
			
			// Verify that the value is valid
			ExplicitFeature ef = (ExplicitFeature) f;
			if(!ef.isValidValue(value)){
				throw new InvalidAssignmentException("Invalid Value Specified: "+value
						+" is not valid for "+featureid+" "+f);
			}
			
			// Create a feature value based on the feature type
			if(value.equals(FeatureValue.UNKNOWN_VALUE)) {
				if(deletefids==null) {
					deletefids="";
				} else {
					deletefids+=", ";
				}
				
				// Handle unknown value
				deletefids += "'"+featureid+"'";
			} else if(ef.isClosed() && ef.getClosedDefaultValue().equals(value)) {
				if(deletefids==null) {
					deletefids="";
				} else {
					deletefids+=", ";
				}
				
				// Handle closed default values
				deletefids += "'"+featureid+"'";
			} else {
				String[] vppair = DBGraph.fv2string(g, f, value);
				String currvalue = vppair[0];
				String currprob = vppair[1];
				
				if(inserts==null) {
					inserts="";
				} else {
					inserts+=", ";
				}
				
				// Need remove old values first
				if(deletefids==null) {
					deletefids="";
				} else {
					deletefids+=", ";
				}
				
				deletefids += "'"+featureid+"'";
				
				inserts += "("+dbid+", '"+featureid+"', "+currvalue+", "+currprob+")";
			}
		}
		
		// Handle all the deletions at once
		if(deletefids != null) {
			g.executeSQL("LOCK TABLE "+stable+" IN EXCLUSIVE MODE");
			g.executeSQL("DELETE FROM "+stable
					+" WHERE dbid="+dbid
					+" AND fid IN ("+deletefids+")");
		}
		
		// Handle all the insertions at once
		if(inserts != null) {
			g.executeSQL("LOCK TABLE "+stable+" IN EXCLUSIVE MODE");
			g.executeSQL("INSERT INTO "+stable
					+" (dbid, fid, value, prob)"
					+" VALUES "+inserts);
		}
	}
	
	protected static FeatureValue getDBFeatureValue(DBGraph g, Decorable d, String featureid) {
		int dbid = 0;
		if(d instanceof DBGraphItem) {
			DBGraphItem dbgi = (DBGraphItem) d;
			dbid = dbgi.dbid;
		} else if(d instanceof DBGraph) {
			DBGraph dbgi = (DBGraph) d;
			dbid = dbgi.dbid;
		} else {
			throw new UnsupportedTypeException("Unsupported Decorable type: "+d);
		}
		
		Feature f = d.getSchema().getFeature(featureid);
		String stable = g.getTableName(d.getSchemaID());
		
		if(f instanceof ExplicitFeature) {
			ExplicitFeature ef = (ExplicitFeature) f;
			
			ResultSet rs = g.queryDatabase("SELECT value, prob"
					+" FROM "+stable
					+" WHERE dbid="+dbid
					+" AND fid='"+featureid+"'");
			
			String value = null;
			double[] probs = null;
			try {
				if(rs.next()) {
					value = rs.getString("value");
					
					String probstring = rs.getString("prob");
					if(probstring != null) {
						String[] probstringarray = probstring.split(",");
						probs = new double[probstringarray.length];
						for(int i=0; i<probs.length; i++) {
							probs[i] = Double.parseDouble(probstringarray[i]);
						}
					}
				} else {
					// Handle case of no return values:
					if(ef.isClosed()) {
						// Handle closed default
						return ef.getClosedDefaultValue();
					} else {
						// Handle unknown values
						return FeatureValue.UNKNOWN_VALUE;
					}
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			} finally {
				try {
					rs.close();
					rs.getStatement().close();
				} catch (SQLException e) {
					// Do nothing
				}
			}

			return DBGraph.parseFeatureValue(f, value, probs);
		} else if(f instanceof DerivedFeature) {
			return ((DerivedFeature) f).getFeatureValue(d);
		} else {
			throw new InvalidStateException("Feature is neither explicit or derived: "+f);
		}
	}
	
	private static FeatureValue parseFeatureValue(Feature f, String value, double[] probs) {
		FeatureValue fv = null;
		
		// Create a feature value based on the feature type
		if(f instanceof NumFeature) {
			fv = new NumValue(Double.parseDouble(value));
		} else if(f instanceof CategFeature) {
			fv = new CategValue(value, probs);
		} else if(f instanceof StringFeature) {
			fv = new StringValue(value);
		} else if(f instanceof MultiCategFeature) {
			Set<String> mvals = new HashSet<String>();
			String[] vals = value.split(",");
			for(String v:vals) {
				mvals.add(v);
			}

			fv = new MultiCategValue(mvals, probs);
		} else if(f instanceof MultiIDFeature) {
			Set<ID> mvals = new HashSet<ID>();
			String[] vals = value.split(",");
			for(String v:vals) {
				ID currid = ID.parseID(v);
				mvals.add(currid);
			}

			fv = new MultiIDValue(mvals);
		} else {
			throw new UnsupportedTypeException("Unsupported Feature Type: "+f);
		}

		return fv;
	}
	
	protected static List<FeatureValue> getDBFeatureValues(DBGraph g, Decorable d,
			List<String> featureids) {
		int dbid = 0;
		if(d instanceof DBGraphItem) {
			DBGraphItem dbgi = (DBGraphItem) d;
			dbid = dbgi.dbid;
		} else if(d instanceof DBGraph) {
			DBGraph dbgi = (DBGraph) d;
			dbid = dbgi.dbid;
		} else {
			throw new UnsupportedTypeException("Unsupported Decorable type: "+d);
		}
		
		String stable = g.getTableName(d.getSchemaID());
		Map<String,FeatureValue> fid2val = new HashMap<String,FeatureValue>();
		String fids = null;
		for(String featureid:featureids) {
			Feature f = d.getSchema().getFeature(featureid);
			
			if(f instanceof ExplicitFeature) {
				if(fids==null) {
					fids = "";
				} else {
					fids += ", ";
				}
				
				fids += "'"+featureid+"'";
			} else if(f instanceof DerivedFeature) {
				fid2val.put(featureid, ((DerivedFeature) f).getFeatureValue(d));
			} else {
				throw new InvalidStateException("Feature is neither explicit or derived: "+f);
			}
		}
		
		// Get all the results at once
		ResultSet rs = g.queryDatabase("SELECT fid, value, prob"
				+" FROM "+stable
				+" WHERE dbid="+dbid
				+" AND fid in ("+fids+")");
		
		try {
			Schema schema = d.getSchema();
			while(rs.next()) {
				String fid = rs.getString("fid");
				String value = rs.getString("value");
				double[] probs = null;
				
				String probstring = rs.getString("prob");
				if(probstring != null) {
					String[] probstringarray = probstring.split(",");
					probs = new double[probstringarray.length];
					for(int i=0; i<probs.length; i++) {
						probs[i] = Double.parseDouble(probstringarray[i]);
					}
				}
				
				Feature f = schema.getFeature(fid);
				FeatureValue fv = DBGraph.parseFeatureValue(f, value, probs);
				
				fid2val.put(fid, fv);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				rs.close();
				rs.getStatement().close();
			} catch (SQLException e) {
				// Do nothing
			}
		}
		
		List<FeatureValue> fvalues = new ArrayList<FeatureValue>();
		for(String featureid:featureids) {
			FeatureValue fv = fid2val.get(featureid);
			
			if(fv == null) {
				fvalues.add(FeatureValue.UNKNOWN_VALUE);
			} else {
				fvalues.add(fv);
			}
		}
		
		return fvalues;
	}

	public GraphID getID() {
		return this.gid;
	}

	@Override
	public void removeSchema(String schemaID) {
		if(!this.hasSchema(schemaID)) {
			throw new InvalidOperationException("No schema with the given ID defined: "+schemaID);
		}

		// When removing schema, remove all instances with that schema
		if(this.getSchemaID().equals(schemaID)) {
			throw new InvalidOperationException("Cannot remove graph schema: "+schemaID);
		}

		try {
			Connection conn = this.getConnection();
			Statement stmt = conn.createStatement();
			stmt.execute("DROP TABLE "+this.getTableName(schemaID));		
			stmt.close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		this.id2schema.remove(schemaID.intern());
	}

	public String toString(){
		return GraphItemUtils.getGraphIDString(this);
	}

	public int hashCode() {
		return this.getID().hashCode();
	}

	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
		if (this == obj) {
			return true;
		}

		if(!(obj instanceof Graph)){
			return false;
		}

		return this.getID().equals(((Graph) obj).getID());
	}

	public List<FeatureValue> getFeatureValues(List<String> featureids) {
		return DBGraph.getDBFeatureValues(this, this, featureids);
	}

	public void setFeatureValues(List<String> featureids,
			List<FeatureValue> values) {
		DBGraph.setDBFeatureValues(this, this, featureids, values);
	}
}
