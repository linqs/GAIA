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
package linqs.gaia.model.gd;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.graph.converter.jung.JungConverter;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.model.BaseModel;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;

import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;

/**
 * A wrapper of the JUNG EdgeBetweennessClusterer which
 * is an implementation of the betweeness based clustering algorithm
 * described in:
 * <p>
 * Girvan, M. & Newman, M. E. J.
 * Community structure in social and biological networks.
 * Proc. of National Academy of Science, 2002
 * <p>
 * The algorithm creates clusters by removing the top K
 * (specified by parameter "numedgestoremove") edges
 * with the highest edge betweeness.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> jcclass-JUNG graph converter, ({@link JungConverter}),
 * instantiated using in {@link Dynamic#forConfigurableName},
 * to use in converting the GAIA graph to a JUNG graph.
 * Default is to use a JUNG graph converter with default settings.
 * <LI> numedgestoremove-Number of edges to remove in the algorithm.
 * If not specified, 10 percent of the edges are removed.
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeBetweenness extends BaseModel implements GroupDetection {
	private static final long serialVersionUID = 1L;
	private Integer numedgestoremove = null;
	
	private boolean initialize = true;
	private void initialize() {
		if(this.hasParameter("numedgestoremove")) {
			numedgestoremove = this.getIntegerParameter("numedgestoremove");
		}
		
		initialize = false;
	}
	
	public void learn(Graph graph, String nodeschemaid, String groupschemaid,
			String memberofschemaid) {
		if(initialize) {
			this.initialize();
		}
	}

	public void learn(Graph graph, String nodeschemaid, String groupschemaid) {
		if(initialize) {
			this.initialize();
		}
	}

	public void predictAsEdge(Graph graph, String groupschemaid) {
		if(initialize) {
			this.initialize();
		}
		
		if(!graph.hasSchema(groupschemaid)) {
			graph.addSchema(groupschemaid, new Schema(SchemaType.UNDIRECTED));
		}
		
		int numremove = 0;
		if(numedgestoremove!=null) {
			numremove = numedgestoremove;
		} else {
			numremove = (int) ((double) graph.numEdges() * 0.1);
		}
		
		String jcclass = this.getStringParameter("jcclass",JungConverter.class.getCanonicalName());
		JungConverter jc = (JungConverter) Dynamic.forConfigurableName(JungConverter.class, jcclass, this);
		
		edu.uci.ics.jung.graph.Graph<Object,Object> jungg = jc.exportGraph(graph);
		EdgeBetweennessClusterer<Object,Object> ebc = new EdgeBetweennessClusterer<Object,Object>(numremove);
		Set<Set<Object>> clusters = ebc.transform(jungg);
		
		for(Set<Object> c:clusters) {
			UndirectedEdge e = null;
			for(Object id: c) {
				Node n = graph.getNode(GraphItemID.parseGraphItemID((String) id));
				if(e==null) {
					e = graph.addUndirectedEdge(GraphItemID.generateGraphItemID(graph, groupschemaid), n);
				} else {
					e.addNode(n);
				}
			}
		}
		
		// Put nodes, not in a group, into their own group
		Iterator<Node> nitr = graph.getNodes();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			if(n.numIncidentGraphItems(groupschemaid)==0) {
				graph.addUndirectedEdge(GraphItemID.generateGraphItemID(graph, groupschemaid), n);
			}
		}
	}

	public void predictAsNode(Graph graph, String groupschemaid,
			String memberofschemaid) {
		if(initialize) {
			this.initialize();
		}
		
		if(!graph.hasSchema(groupschemaid)) {
			graph.addSchema(groupschemaid, new Schema(SchemaType.NODE));
		}
		
		if(!graph.hasSchema(memberofschemaid)) {
			graph.addSchema(memberofschemaid, new Schema(SchemaType.DIRECTED));
		}
		
		int numremove = 0;
		if(numedgestoremove!=null) {
			numremove = numedgestoremove;
		} else {
			numremove = (int) ((double) graph.numEdges() * 0.5);
		}
		
		JungConverter converter = new JungConverter();
		edu.uci.ics.jung.graph.Graph<Object,Object> jungg = converter.exportGraph(graph);
		EdgeBetweennessClusterer<Object,Object> ebc = new EdgeBetweennessClusterer<Object,Object>(numremove);
		Set<Set<Object>> clusters = ebc.transform(jungg);
		
		for(Set<Object> c:clusters) {
			Node group = graph.addNode(GraphItemID.generateGraphItemID(graph, groupschemaid));
			for(Object id: c) {
				Node n = graph.getNode(GraphItemID.parseGraphItemID((String) id));
				graph.addDirectedEdge(GraphItemID.generateGraphItemID(graph, memberofschemaid), n, group);
			}
		}
		
		// Put nodes, not in a group, into their own group
		Iterator<Node> nitr = graph.getNodes();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			if(!n.getSchemaID().equals(groupschemaid) && n.numIncidentGraphItems(memberofschemaid)==0) {
				Node group = graph.addNode(GraphItemID.generateGraphItemID(graph, groupschemaid));
				graph.addDirectedEdge(GraphItemID.generateGraphItemID(graph, memberofschemaid), n, group);
			}
		}
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		this.initialize();
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}
}
