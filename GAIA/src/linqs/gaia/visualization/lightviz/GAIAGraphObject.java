package linqs.gaia.visualization.lightviz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.IteratorUtils;

import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.tuple.TableTuple;
import prefuse.data.tuple.TupleSet;

public class GAIAGraphObject {
	public HashMap<Long,ArrayList<Node>> prefGaiaNodeIDMap = new HashMap<Long,ArrayList<Node>>();
	public HashMap<Long,ArrayList<Edge>> prefGaiaEdgeIDMap = new HashMap<Long,ArrayList<Edge>>();
	private Graph g;


	public GAIAGraphObject(Graph g){
		this.g=g;
	}

	public String getNodeFeature(){
		return g.getNodes().next().getSchema().getFeatureIDs().next();
	}

	public boolean isOrdinalFeature(String selectedFeature, boolean isNodeFeature){
		if (isNodeFeature) {
			//get Only Node Attributes
			Iterator<String> sitr = g.getAllSchemaIDs(SchemaType.NODE);
			while(sitr.hasNext()){
				linqs.gaia.feature.schema.Schema schema=g.getSchema(sitr.next());
				if(schema.hasFeature(selectedFeature)
					&& !(schema.getFeature(selectedFeature) instanceof NumFeature)) {
					return false;
				}
			}
			
			return true;
		} else {
			//get Only Node Attributes
			Iterator<String> sitr = g.getAllSchemaIDs(SchemaType.NODE);
			while(sitr.hasNext()){
				linqs.gaia.feature.schema.Schema schema=g.getSchema(sitr.next());
				if(schema.hasFeature(selectedFeature)
						&& !(schema.getFeature(selectedFeature) instanceof NumFeature)) {
					return false;
				}
			}
			
			//get Only Node Attributes
			sitr = g.getAllSchemaIDs(SchemaType.NODE);
			while(sitr.hasNext()){
				linqs.gaia.feature.schema.Schema schema=g.getSchema(sitr.next());
				if(schema.hasFeature(selectedFeature)
						&& !(schema.getFeature(selectedFeature) instanceof NumFeature)) {
					return true;
				}
			}
			
			return false;
		}
	}

	public List<?> getFeatures(boolean isNodeFeatures){
		Iterator<String> it = null;
		List<String> allfids = new ArrayList<String>();
		if(isNodeFeatures){
			//get Only Node Attributes
			Iterator<String> sitr = g.getAllSchemaIDs(SchemaType.NODE);
			while(sitr.hasNext()){
				it=g.getSchema(sitr.next()).getFeatureIDs();
				allfids.addAll(IteratorUtils.iterator2stringlist(it));
			}
		}
		else {
			//get Only Edge Attributes
			Iterator<String> sitr = g.getAllSchemaIDs(SchemaType.DIRECTED);
			while(sitr.hasNext()){
				it=g.getSchema(sitr.next()).getFeatureIDs();
				allfids.addAll(IteratorUtils.iterator2stringlist(it));
			}
			
			sitr = g.getAllSchemaIDs(SchemaType.UNDIRECTED);
			while(sitr.hasNext()){
				it=g.getSchema(sitr.next()).getFeatureIDs();
				allfids.addAll(IteratorUtils.iterator2stringlist(it));
			}
		}
		
		return allfids;
	}

	public void populatePrefuseGraphTable(String fid,boolean isNodeFeature,Table table, String column){
		HashMap<?,?> map;
		Iterator<?> itr;
		String item = null;
		ArrayList<String> allItems;

		if(isNodeFeature) {
			map=prefGaiaNodeIDMap;
		}
		else {
			map=prefGaiaEdgeIDMap;
		}

		for(int i=0;i<table.getRowCount();i++){
			allItems=new ArrayList<String>();
			itr=((ArrayList<?>)map.get(table.get(i, 0))).iterator();
			while(itr.hasNext()){
				GraphItem gi = (GraphItem) itr.next();
				if(gi.getSchema().hasFeature(fid)) {
					item=gi.getFeatureValue(fid).getStringValue();
				} else {
					item = "INVALID";
				}
				
				if (!allItems.contains(item))
					allItems.add(item);
			}
			itr=allItems.iterator();
			item="";
			while(itr.hasNext()) {
				item+=itr.next().toString()+",";
			}
			
			table.set(i,column,item.substring(0, item.length()-1));
		}
	}

	/*
	 * Approximate Numeric Features to Integers for use with visual features that cannot handle doubles
	 */
	public void populatePrefuseGraphTable(String fid,boolean isNodeFeature,Table table,boolean approximateNum){
		HashMap<?,?> map;
		Iterator<?> itr;
		int item;

		if(isNodeFeature) {
			map=prefGaiaNodeIDMap;
		} else {
			map=prefGaiaEdgeIDMap;
		}

		for(int i=0;i<table.getRowCount();i++){
			itr=((ArrayList<?>)map.get(table.get(i, 0))).iterator();
			item=0;
			while(itr.hasNext())
				item+=(int)Double.parseDouble(((GraphItem)itr.next()).getFeatureValue(fid).getStringValue());
			item/=((ArrayList<?>)map.get(table.get(i, 0))).size();
			table.setInt(i,"cacheNodeColumnNumeric",item);
		}
	}

	public prefuse.data.Graph getPrefuseGraph(String key){
		prefuse.data.Graph gOut = null;
		Iterator<Node> nodeIter=g.getNodes();
		Iterator<Edge> edgeIter=g.getEdges();

		prefuse.data.Node prefNode;
		Node gaiaNode;

		prefuse.data.Edge prefEdge;
		Edge gaiaEdge;

		long prefIDs=0;
		String keyVal;

		Table ntable= null;
		Table etable= null;

		Schema nsch = new Schema();
		Schema esch = new Schema();

		// Set up schemas
		nsch.addColumn("nodeID", long.class);
		nsch.addColumn("cacheNodeColumn", Object.class);
		nsch.addColumn("cacheNodeColumnNumeric", int.class);
		nsch.addColumn("nodeType", Object.class);

		esch.addColumn("edgeID", long.class);
		esch.addColumn("startNode", long.class);
		esch.addColumn("endNode", long.class);
		esch.addColumn("cacheEdgeColumn", Object.class);
		esch.addColumn("cacheEdgeColumnNumeric", int.class);

		nsch.lockSchema();
		esch.lockSchema();
		ntable = nsch.instantiate();
		etable = esch.instantiate();

		String NODEKEY = "nodeID";
		String SOURCEKEY = "startNode";
		String TARGETKEY = "endNode";
		gOut = new prefuse.data.Graph(ntable, etable, true, NODEKEY, SOURCEKEY, TARGETKEY);
		//			if(key==null) key=g.getNodes().next().getSchema().getFeatureIDs().next();

		prefGaiaNodeIDMap.clear();
		prefGaiaEdgeIDMap.clear();

		while (nodeIter.hasNext()){
			gaiaNode = (Node)nodeIter.next();

			ArrayList<Node> tmpList;
			TableTuple seenNode = null;

			if(key==null){
				keyVal = gaiaNode.getID().getObjID();
			}
			else{
				keyVal = gaiaNode.getFeatureValue(key).getStringValue();

				TupleSet ts = gOut.getNodes();
				Iterator<?> tuples = ts.tuples();
				while (tuples.hasNext()){
					TableTuple tt = (TableTuple)tuples.next();
					if (tt.get("nodeType").equals(keyVal)){
						seenNode = tt;
						break;
					}
				}
			}

			if(seenNode!=null){
				((ArrayList<Node>)prefGaiaNodeIDMap.get(seenNode.getLong("nodeID"))).add(gaiaNode);
			}
			else{
				prefNode= gOut.addNode();
				prefNode.set("nodeID", prefIDs);
				prefNode.set("nodeType", keyVal);
				tmpList=new ArrayList<Node>();
				tmpList.add(gaiaNode);
				prefGaiaNodeIDMap.put(prefIDs,tmpList);
				prefIDs++;
			}
		}

		while (edgeIter.hasNext()){
			gaiaEdge = (Edge)edgeIter.next();
			if(gaiaEdge.numNodes()==2){
				long id1, id2, currentKey;
				Node start,end;
				Iterator<?> itr=prefGaiaNodeIDMap.keySet().iterator();
				ArrayList<Edge> tmpList;

				nodeIter=gaiaEdge.getAllNodes();
				start=(Node)nodeIter.next();
				end=(Node)nodeIter.next();
				id1=-1;
				id2=-1;

				while((itr.hasNext())&&((id1==-1)||(id2==-1))){
					currentKey=Long.parseLong(itr.next().toString());
					if(((ArrayList<?>)prefGaiaNodeIDMap.get(currentKey)).contains(start))
						id1=currentKey;
					if(((ArrayList<?>)prefGaiaNodeIDMap.get(currentKey)).contains(end))
						id2=currentKey;
				}

				//Edge between non-existent nodes
				if((id1==-1)||(id2==-1)) {
					throw new InvalidStateException("An edge is defined between non-existing nodes");
				}

				TupleSet ts = gOut.getEdges();
				Iterator<?> tuples = ts.tuples();
				TableTuple seen = null;
				while (tuples.hasNext()){
					TableTuple tt = (TableTuple)tuples.next();
					if (tt.getLong("startNode")==id1 &&
							tt.getLong("endNode")==id2)
					{
						seen = tt;
						break;
					}
				}

				if(seen!=null){
					((ArrayList<Edge>)prefGaiaEdgeIDMap.get(seen.getLong("edgeID"))).add(gaiaEdge);
				}
				else {
					prefEdge= gOut.addEdge(gOut.getNodeFromKey(id1), gOut.getNodeFromKey(id2));
					prefEdge.set("startNode",id1);
					prefEdge.set("endNode",id2);
					prefEdge.set("edgeID", prefIDs);
					tmpList=new ArrayList<Edge>();
					tmpList.add(gaiaEdge);
					prefGaiaEdgeIDMap.put(prefIDs,tmpList);
					prefIDs++;
				}
			}
		}

		return gOut;
	}

	public ArrayList<?> getGAIAItemList(long prefKey, boolean isNode){
		if(isNode)
			return (ArrayList<?>)prefGaiaNodeIDMap.get(prefKey);
		return (ArrayList<?>)prefGaiaEdgeIDMap.get(prefKey);
	}

	public String getPropertyString(long id, boolean isNode){
		String currFeat,output="";
		Iterator<?> fItr,itr;
		ArrayList<? extends GraphItem> list;

		if(isNode){
			list=(ArrayList<Node>) prefGaiaNodeIDMap.get(id);
			fItr=list.get(0).getSchema().getFeatureIDs();
		} else{
			list=(ArrayList<Edge>) prefGaiaEdgeIDMap.get(id);
			fItr=list.get(0).getSchema().getFeatureIDs();
		}

		while(fItr.hasNext()){
			currFeat=fItr.next().toString();
			output+=currFeat+": ";
			itr=list.iterator();
			while(itr.hasNext()) {
				FeatureValue fv = ((GraphItem) itr.next()).getFeatureValue(currFeat);
				output+=fv.getStringValue();
				if(fv instanceof CategValue) {
					output+=" ["+ArrayUtils.array2String(((CategValue) fv).getProbs(),",")+"]";
				}
				
				output+=",";
			}

			output=output.substring(0,output.length()-1);
			output+="\n";
		}

		return output;
	}
}
