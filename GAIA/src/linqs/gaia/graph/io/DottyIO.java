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
package linqs.gaia.graph.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UndefinedFunctionException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.ID;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedSet;
import linqs.gaia.util.ListUtils;
import linqs.gaia.util.MinMax;
import linqs.gaia.util.SimplePair;

/**
 * IO implementation which allows for saving a GAIA graph in the dotty
 * format.  This is useful when wanting to use a visualization tool
 * that supports the dotty format.
 * Note:  This IO does not currently allow loading a graph in the dotty format.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> filedirectory-Directory to store or load the graph
 * Note: Parameter not required if using {@link DirectoryBasedIO} methods.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> fileprefix-Optional prefix to use in the file names of the generated files.
 * <LI> schemaprops-Properties to set for all items with the given schema.
 * The format is schema1=properties1;schema2=properties2
 * (i.e., webpage=label=\"\",shape=rectangle,style=filled)
 * <LI> valueprops-Properties to set for all items which have a given value.
 * The format is schema1.feature1.value1=properties1;schema2.feature2.value2=properties2
 * (i.e., webpage.label.student=color=red,shape=triangle;webpage.label.faculty=color=blue)
 * <LI> heightprops-If specified, set the height property of objects with the given schema using the numeric
 * value given by the specified feature.  The format is schema1.feature1;schema2.feature2
 * (i.e., webpage.w2)
 * <LI> widthprops-If specified, set the weight property of objects with the given schema using the numeric
 * value given by the specified feature.  The format is schema1.feature1;schema2.feature2
 * (i.e., webpage.w2)
 * <LI> graphprops-Dotty properties to set for the whole graph (i.e., overlap=scale;)
 * </UL>
 * 
 * @author namatag
 *
 */
public class DottyIO extends BaseConfigurable implements IO, DirectoryBasedIO {
	Map<String, String> schemaprops = new HashMap<String, String>();
	Map<String, SimplePair<String,MinMax>> heightprops = new HashMap<String, SimplePair<String,MinMax>>();
	Map<String, SimplePair<String,MinMax>> widthprops = new HashMap<String, SimplePair<String,MinMax>>();
	KeyedSet<String, String> schema2cats = new KeyedSet<String, String>();
	Map<String, String> value2prop = new HashMap<String, String>();
	
	/**
	 * Cannot currently load Dotty input format.
	 */
	public Graph loadGraphFromDir(String Directory) {
		throw new UndefinedFunctionException("Dotty IO cannot load graph");
	}
	
	/**
	 * Cannot currently load Dotty input format.
	 */
	public Graph loadGraph() {
		throw new UndefinedFunctionException("Dotty IO cannot load graph");
	}
	
	/**
	 * Cannot currently load Dotty input format.
	 */
	public Graph loadGraphFromDir(String directory, String objid) {
		throw new UndefinedFunctionException("Dotty IO cannot load graph");
	}
	
	/**
	 * Cannot currently load Dotty input format.
	 */
	public Graph loadGraph(String objid) {
		throw new UndefinedFunctionException("Dotty IO cannot load graph");
	}

	public void saveGraphToDir(String directory, Graph g) {
		try {
			String prefix = g.getID().getObjID();
			if(this.hasParameter("fileprefix")) {
				prefix = this.getStringParameter("fileprefix");
			}

			// Create directory if it doesn't already exist
			FileIO.createDirectories(directory);
			
			List<String> esids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.DIRECTED));
			esids.addAll(IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.UNDIRECTED)));
			
			// If all edges are directed, show as directed.
			// Otherwise, show as undirected.
			boolean isdirected = true;
			for(String esid:esids) {
				if(!g.getSchemaType(esid).equals(SchemaType.DIRECTED)) {
					isdirected = false;
					break;
				}
			}
			
			List<String> nsids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.NODE));
			
			/*************************************/
			// Get properties to use
			// Get schema based properties
			if(this.hasParameter("schemaprops")) {
				String currsprops = this.getStringParameter("schemaprops");
				String parts[] = currsprops.split(";");
				for(String part:parts) {
					int index = part.indexOf("=");
					schemaprops.put(part.substring(0, index), part.substring(index+1));
				}
			}
			
			// Specify attribute category based properties
			if(this.hasParameter("valueprops")) {
				String parts[] = this.getStringParameter("valueprops").split(";");
				for(String part:parts) {
					int index = part.indexOf("=");
					String key = part.substring(0, index);
					String value = part.substring(index+1);
					
					String[] sav = key.split("\\.");
					schema2cats.addItem(sav[0], sav[1]);
					value2prop.put(key, value);
				}
			}
			
			if(this.hasParameter("heightprops")) {
				String parts[] = this.getStringParameter("heightprops").split(";");
				for(String part:parts) {
					String[] sf = part.split("\\.");
					String sid = sf[0];
					String fid = sf[1];
					
					// Get min and max to normalize
					Iterator<GraphItem> gitr = g.getGraphItems(sid);
					MinMax mm = new MinMax();
					while(gitr.hasNext()) {
						GraphItem gi = gitr.next();
						FeatureValue fv = gi.getFeatureValue(fid);
						if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
							continue;
						}
						
						mm.addValue(((NumValue) fv).getNumber());
					}
					
					this.heightprops.put(sid, new SimplePair<String,MinMax>(fid,mm));
				}
			}
			
			if(this.hasParameter("widthprops")) {
				String parts[] = this.getStringParameter("widthprops").split(";");
				for(String part:parts) {
					String[] sf = part.split("\\.");
					String sid = sf[0];
					String fid = sf[1];
					
					// Get min and max to normalize
					Iterator<GraphItem> gitr = g.getGraphItems(sid);
					MinMax mm = new MinMax();
					while(gitr.hasNext()) {
						GraphItem gi = gitr.next();
						FeatureValue fv = gi.getFeatureValue(fid);
						if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
							continue;
						}
						
						mm.addValue(((NumValue) fv).getNumber());
					}
					
					this.widthprops.put(sid, new SimplePair<String,MinMax>(fid,mm));
				}
			}
			
			/*************************************/
			
			// Create file 
			ID id = g.getID();
			String file = directory+"/"+prefix+".dot";
			
			// Write the graph to file
			FileWriter fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			
			// Print graph declaration
			String edelim = null;
			if(isdirected) {
				out.write("digraph");
				edelim = " -> ";
			} else {
				out.write("graph");
				edelim = " -- ";
			}
			out.write(" "+this.getObjID(id)+" {\n");
			
			String spacer = "     ";
			
			// Specify graph properties
			if(this.hasParameter("graphprops")) {
				String[] props = this.getStringParameter("graphprops").split(";");
				for(String prop:props) {
					out.write(spacer+prop+";\n");
				}
			}
			
			// Specify the set of nodes
			for(String nsid:nsids) {
				Iterator<Node> nitr = g.getNodes(nsid);
				while(nitr.hasNext()) {
					Node n = nitr.next();
					out.write(spacer);
					out.write(this.getObjID(n.getID()));
					
					// If there are attributes to specify, do that here.
					String nprops = this.getProps(n);
					if(nprops.length()!=0) {
						out.write("["+nprops+"]");
					}
					
					out.write(";\n");
				}
			}
			
			// Specify the set of edges
			for(String esid:esids) {
				boolean iscurrdirected = g.getSchemaType(esid).equals(SchemaType.DIRECTED);
				Iterator<Edge> eitr = g.getEdges(esid);
				while(eitr.hasNext()) {
					Edge e = eitr.next();
					out.write(spacer);
					
					// Only binary edges supported
					if(e.numNodes()>2) {
						throw new UnsupportedTypeException("Only binary edges supported: "
								+e+" has "+e.numNodes());
					}
					
					Node n1, n2;
					if(iscurrdirected){
						DirectedEdge de = (DirectedEdge) e;
						n1 = de.getSourceNodes().next();
						n2 = de.getTargetNodes().next();
					} else {
						if(e.numNodes()==1) {
							// For self loops
							n1 = e.getAllNodes().next();
							n2 = n1;
						} else {
							Iterator<Node> nitr = e.getAllNodes();
							n1 = nitr.next();
							n2 = nitr.next();
						}
					}
					
					// Specify edge
					out.write(this.getObjID(n1.getID())+edelim+this.getObjID(n2.getID()));
					
					// If there are attributes to specify, do that here.
					String eprops = this.getProps(e);
					if(eprops.length()!=0) {
						out.write("["+eprops+"]");
					}
					
					out.write(";\n");
				}
			}	
			
			// Close graph declaration
			out.write("}");
			out.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void saveGraph(Graph g) {
		String dirpath = this.getStringParameter("filedirectory");

		this.saveGraphToDir(dirpath, g);
	}
			
	
	/**
	 * Return an object id consistent with dotty syntax
	 * @param id
	 * @return
	 */
	private String getObjID(ID id) {
		String objid = id.getObjID();
		objid = objid.replaceAll("[^a-zA-Z0-9]", "");
		return objid;
	}
	
	private String getProps(GraphItem gi) {
		String sid = gi.getSchemaID();
		
		List<String> prop = new ArrayList<String>();
		
		// Get properties by schema
		// i.e., schemaprops=person=shape=box,style=filled;place=shape=circle
		if(schemaprops.containsKey(sid)) {
			prop.add(schemaprops.get(sid));
		}
		
		// Get color by attribute
		// i.e., catcolorprops=person.car.corolla=blue;person.car.honda=red
		if(schema2cats.hasKey(sid)) {
			Set<String> fids = schema2cats.getSet(sid);
			for(String fid:fids) {
				String category = gi.getFeatureValue(fid).getStringValue();
				if(this.value2prop.containsKey(sid+"."+fid+"."+category)) {
					prop.add(this.value2prop.get(sid+"."+fid+"."+category));
				}
			}
		}
		
		// Get height and width by attribute
		// Note: Normalize by inch
		// i.e., heightprops=person.age;company.age
		// i.e., widthprops=person.weight
		if(heightprops.containsKey(sid)) {
			SimplePair<String,MinMax> pair = heightprops.get(sid);
			String fid = pair.getFirst();
			MinMax mm = pair.getSecond();
			
			FeatureValue fv = gi.getFeatureValue(fid);
			if(!fv.equals(FeatureValue.UNKNOWN_VALUE)
					&& mm.getNumConsidered()!=0
					&& mm.getMax()!=mm.getMin()) {
				Double size = ((NumValue) fv).getNumber();
				
				// Normalize
				size = (size-mm.getMin())/(mm.getMax()-mm.getMin());
				if(size==0) {
					size = .1;
				}
				
				prop.add("height="+size);
			}
		}
		
		if(widthprops.containsKey(sid)) {
			SimplePair<String,MinMax> pair = widthprops.get(sid);
			String fid = pair.getFirst();
			MinMax mm = pair.getSecond();
			
			FeatureValue fv = gi.getFeatureValue(fid);
			if(!fv.equals(FeatureValue.UNKNOWN_VALUE)
					&& mm.getNumConsidered()!=0
					&& mm.getMax()!=mm.getMin()) {
				Double size = ((NumValue) fv).getNumber();
				
				// Normalize
				size = (size-mm.getMin())/(mm.getMax()-mm.getMin());
				if(size==0) {
					size = .1;
				}
				
				prop.add("width="+size);
			}
		}
		
		return ListUtils.list2string(prop, ",");
	}
}
