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
package linqs.gaia.experiment.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.configurable.Configurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.filter.Filter;
import linqs.gaia.graph.generator.Generator;
import linqs.gaia.graph.generator.decorator.Decorator;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.noise.Noise;
import linqs.gaia.graph.statistic.GraphStatistic;
import linqs.gaia.graph.transformer.Transformer;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;
import linqs.gaia.model.er.ERUtils;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.prediction.PredictionGroup;
import linqs.gaia.prediction.existence.ExistencePred;
import linqs.gaia.prediction.existence.ExistencePredGroup;
import linqs.gaia.prediction.feature.CategValuePred;
import linqs.gaia.prediction.feature.CategValuePredGroup;
import linqs.gaia.prediction.statistic.Statistic;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.MapUtils;
import linqs.gaia.util.SimpleTimer;
import linqs.gaia.visualization.Visualization;

/**
 * Collection of static functions to simplify using GAIA with Groovy
 * 
 * @author namatag
 *
 */
public class GroovyExperiment extends BaseConfigurable {
	private static Configurable conf = new GroovyExperiment();
	private static GroovyShell shell = new GroovyShell();
	
	/**
	 * Load an object of the specified object class.  The object class
	 * can be a Java object (e.g., {@link linqs.gaia.graph.io})
	 * or the name of a parameter, previously loaded using
	 * {@link #LOADPARAMETERS},
	 * whose value is the Java object (e.g., io={@link linqs.gaia.graph.io}).
	 * In the latter case, the default is to use the parameter name
	 * as the {@link Configurable} id.
	 * See {@link Configurable} and {@link Dynamic#forConfigurableName(Class, String)} for details.
	 * 
	 * @param objectclass Object class to load
	 * @return Dynamically instantiated object
	 */
	public static Object LOAD(String objectclass) {
		objectclass = getClass(objectclass);
		Object o = (Object) Dynamic.forConfigurableName(Object.class, objectclass);
		
		if(o instanceof Configurable) {
			((Configurable) o).copyParameters(conf);
		}
		
		return o;
	}
	
	/**
	 * Load the class using the specified class and using
	 * the parameters previously loaded using
	 * {@link #LOADPARAMETERS}.
	 * The class can either be an implementation of
	 * {@link IO} or {@link Generator}.
	 * The loaderclass is of the same form as the
	 * object class in {@link #LOAD}.
	 * 
	 * @param loaderclass Object class of loader
	 * @return Object
	 */
	public static Graph LOADGRAPH(String loaderclass) {
		loaderclass = getClass(loaderclass);
		Object loader = Dynamic.forConfigurableName(Object.class, loaderclass, conf);
		Graph g = null;
		if(loader instanceof IO) {
			g = ((IO) loader).loadGraph();
		} else if(loader instanceof Generator) {
			g = ((Generator) loader).generateGraph();
		} else {
			throw new UnsupportedTypeException(loader.getClass().getCanonicalName());
		}
		
		return g;
	}
	
	/**
	 * Load the class using the specified class and using
	 * the parameters previously loaded using
	 * {@link #LOADPARAMETERS}.
	 * The class can either be an implementation of
	 * {@link IO} or {@link Generator}.
	 * The loaderclass is of the same form as the
	 * object class in {@link #LOAD}.
	 * 
	 * @param loaderclass Object class of loader
	 * @param objid Object ID to load graph with
	 * @return Object
	 */
	public static Graph LOADGRAPH(String loaderclass, String objid) {
		loaderclass = getClass(loaderclass);
		Object loader = Dynamic.forConfigurableName(Object.class, loaderclass, conf);
		Graph g = null;
		if(loader instanceof IO) {
			g = ((IO) loader).loadGraph(objid);
		} else if(loader instanceof Generator) {
			g = ((Generator) loader).generateGraph(objid);
		} else {
			throw new UnsupportedTypeException(loader.getClass().getCanonicalName());
		}
		
		return g;
	}
	
	/**
	 * Save the class using the specified {@link IO} class and using
	 * the parameters previously loaded using
	 * {@link #LOADPARAMETERS}.
	 * The io is of the same form as the
	 * object class in {@link #LOAD}.
	 * 
	 * @param ioclass Object class of an implementation of IO
	 */
	public static void SAVEGRAPH(Graph g, String ioclass) {
		ioclass = getClass(ioclass);
		IO io = (IO) Dynamic.forConfigurableName(IO.class, ioclass, conf);
		io.saveGraph(g);
	}
	
	/**
	 * Add the provided {@link Feature} using the specified featureID
	 * to the specified schema of the graph.
	 * 
	 * @param g Graph to add feature to
	 * @param schemaID Schema ID to add feature to
	 * @param featureID Feature ID of new feature
	 * @param f {@link Feature} object to add to graph
	 * @return Added feature object
	 */
	public static Feature ADDFEATURE(Graph g, String schemaID, String featureID, Feature f) {
		if(f instanceof Configurable) {
			((Configurable) f).copyParameters(conf);
		}
		
		if(f instanceof GraphDependent) {
			((GraphDependent) f).setGraph(g);
		}
		
		Schema schema = g.getSchema(schemaID);
		schema.addFeature(featureID, f);
		g.updateSchema(schemaID, schema);
		
		return f;
	}
	
	/**
	 * Add a {@link Feature} of the defined class using the specified featureID
	 * to the specified schema of the graph.
	 * The featureclass is any of the same form as the
	 * object class in {@link #LOAD}.
	 * 
	 * @param g Graph to add feature to
	 * @param schemaID Schema ID to add feature to
	 * @param featureID Feature ID of new feature
	 * @param featureclass {@link Feature} object class of feature to add to graph
	 * @return Added feature object
	 */
	public static Feature ADDFEATURE(Graph g, String schemaID, String featureID, String featureclass) {
		featureclass = getClass(featureclass);
		Schema schema = g.getSchema(schemaID);
		Feature f = (Feature) Dynamic.forConfigurableName(Feature.class, featureclass);
		if(f instanceof Configurable) {
			((Configurable) f).copyParameters(conf);
		}
		
		if(f instanceof GraphDependent) {
			((GraphDependent) f).setGraph(g);
		}
		
		schema.addFeature(featureID, f);
		g.updateSchema(schemaID, schema);
		
		return f;
	}
	
	/**
	 * Add a {@link Feature}, defined in the specified Groovy script,
	 * using the specified featureID
	 * to the specified schema of the graph.
	 * 
	 * @param g Graph to add feature to
	 * @param schemaID Schema ID to add feature to
	 * @param featureID Feature ID of new feature
	 * @param groovyfile Groovy file which defines feature object
	 * @return Added feature object
	 */
	public static Feature ADDGROOVYFEATURE(Graph g, String schemaID, String featureID, String groovyfile) {
		Schema schema = g.getSchema(schemaID);
		
		Feature f = null;
		try {
			// Handle groovy file
			GroovyClassLoader gcl = shell.getClassLoader();
			Class<?> clazz = gcl.loadClass(groovyfile);
			//Class<?> clazz = gcl.parseClass(new File(groovyfile));
			f = (Feature) clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Error loading groovy feature file",e);
		}
		
		if(f instanceof Configurable) {
			((Configurable) f).copyParameters(conf);
		}
		
		if(f instanceof GraphDependent) {
			((GraphDependent) f).setGraph(g);
		}
		
		schema.addFeature(featureID, f);
		g.updateSchema(schemaID, schema);
		
		return f;
	}
	
	/**
	 * Load configuration file for use in the abstract classes
	 * 
	 * @param file Configuration file
	 */
	public static void LOADPARAMETERS(String file) {
		conf.loadParametersFile(file);
	}
	
	/**
	 * Set the parameters defined in the string.
	 * Calling this is equivalent to loading a file with the same information.
	 * Note:  Since the values, including the regex, are represented in a string
	 * instead of in a file, be careful with how you represent escape charaters.
	 * "\\w" in the string is equivalent to just "\w" if in a file.
	 * 
	 * @param parameters String containing parameters
	 */
	public static void SETPARAMETERS(String parameters) {
		try {
			LinkedList<String> parameterlist =
				new LinkedList<String>(Arrays.asList(parameters.split("[\\n\\r]+")));
			String line;
			while ((line = getNonCommentLine(parameterlist)) != null) {
				conf.setParameters(line);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Private class designed so that {@link #SETPARAMETERS} behaves
	 * similar to {@link linqs.gaia.configurable.Configurable#loadParametersFile}.
	 * 
	 * @param parameterlist List of parameters where each entry is treated as one line
	 * @return Next non-comment line
	 * @throws Exception
	 */
	private static String getNonCommentLine(LinkedList<String> parameterlist) throws Exception {
		if(parameterlist.size()==0) {
			return null;
		}
		
		// Skip lines starting with COMMENT_CHAR and lines which consists of whitespace
		String line = parameterlist.remove();
		
		boolean incomment = false;
		while(line!=null) {
			// Remove leading and trailing whitespaces
			line = line.trim();
			if(line.startsWith(BaseConfigurable.COMMENT_START)) {
				incomment = true;
			} else if(line.endsWith(BaseConfigurable.COMMENT_STOP)) {
				incomment = false;
			} else if(line.startsWith(BaseConfigurable.COMMENT_CHAR) || line.trim().length()==0) {
				// Still don't have a non-comment line
			} else {
				// If this is a non-comment line, non-whitespace,
				// and not in a comment region, break.
				if(!incomment) {
					break;
				}
			}
			
			// Get next line
			line = parameterlist.remove();
		}
		
		if(line==null) {
			return null;
		}
		
		// Support splitting configuration over multiple lines
		if(line.endsWith(""+ESCAPE_CHAR)) {
			line = line.substring(0,line.length()-1);
			String append = getNonCommentLine(parameterlist);
			if(append!=null) {
				line += append;
			}
		}
		
		// Remove anything in the line past the comment character
		int index = line.indexOf(COMMENT_CHAR);
		while(index!=-1) {
			// Allow for escape character \#
			if(line.charAt(index-1)==ESCAPE_CHAR) {
				if(index >= line.length()) {
					break;
				}
				
				index = line.indexOf(COMMENT_CHAR, index + 1);
				continue;
			} else {
				line = line.substring(0, index);
				break;
			}
		}
		
		// Remove all escape symbols from the escaped characters
		line = line.replaceAll("\\\\"+COMMENT_CHAR, COMMENT_CHAR);
		
		return line;
	}
	
	/**
	 * Remove the values of the feature with the given feature id
	 * for all objects with the specified schema id.
	 * 
	 * @param g Graph to remove values from
	 * @param schemaid Schema ID of schema which contains the feature
	 * @param featureid Feature ID of the feature whose value we need to remove
	 */
	public static void REMOVEVALUES(Graph g, String schemaid, String featureid) {
		Iterator<? extends Decorable> testitems = g.getGraphItems(schemaid);
		while(testitems.hasNext()) {
			Decorable d = testitems.next();
			d.removeFeatureValue(featureid);
		}
	}
	
	/**
	 * Remove the values of the feature with the given feature id
	 * from the given iterable set of objects
	 * 
	 * @param testitems Decorable objects to remove values from
	 * @param featureid Feature ID of the feature whose value we need to remove
	 */
	public static void REMOVEVALUES(Iterable<? extends Decorable> testitems, String featureid) {
		for(Decorable d:testitems) {
			d.removeFeatureValue(featureid);
		}
	}
	
	/**
	 * Remove the values of the feature with the given feature id
	 * from the equivalent graph items in the specified graph
	 * equivalent to given iterable set of objects.
	 * 
	 * @param g Graph with equivalent graph items
	 * @param testitems Decorable objects to remove values from
	 * @param featureid Feature ID of the feature whose value we need to remove
	 */
	public static void REMOVEVALUES(Graph g, Iterable<? extends Decorable> testitems, String featureid) {
		for(Decorable d:testitems) {
			g.getEquivalentGraphItem((GraphItem) d).removeFeatureValue(featureid);
		}
	}
	
	/**
	 * Return the object classification predictions
	 * 
	 * @param schemaID Schema ID of object whose value we predicted over
	 * @param featureID Feature ID of object whose value we predicted over
	 * @param predgraph Graph containing predicted values
	 * @param truegraph Graph containing ground truth
	 * @return Prediction group
	 */
	public static PredictionGroup CREATEOCPREDS(String schemaID, String featureID,
			Graph predgraph, Graph truegraph) {
		return CREATEOCPREDS(schemaID, featureID, null, predgraph, truegraph);
	}
	
	/**
	 * Return the object classification predictions
	 * 
	 * @param schemaID Schema ID of object whose value we predicted over
	 * @param featureID Feature ID of object whose value we predicted over
	 * @param referstosid "Refers-To" edge between a reference and its node.
	 * This may happen if entity resolution was applied prior to collective classification.
	 * If null, ignore value.  Otherwise, compute predictions for labels of entity
	 * based on the number of references.
	 * @param predgraph Graph containing predicted values
	 * @param truegraph Graph containing ground truth
	 * @return Prediction group
	 */
	public static PredictionGroup CREATEOCPREDS(String schemaID, String featureID, String referstosid,
			Graph predgraph, Graph truegraph) {
		// Go over all the nodes we predicted a label over
		return CREATEOCPREDS(schemaID, featureID, referstosid, predgraph.getIterableNodes(schemaID), truegraph);
	}
	
	/**
	 * Return the object classification predictions
	 * 
	 * @param schemaID Schema ID of object whose value we predicted over
	 * @param featureID Feature ID of object whose value we predicted over
	 * @param decorable Iterable group of decorable items whose value we predicted
	 * @param truegraph Graph containing ground truth
	 * @return Prediction group
	 */
	public static PredictionGroup CREATEOCPREDS(String schemaID, String featureID,
			Iterable<? extends Decorable> decorable, Graph truegraph) {
		return CREATEOCPREDS(schemaID, featureID, null, decorable, truegraph);
	}
	
	/**
	 * Return the object classification predictions
	 * 
	 * @param schemaID Schema ID of object whose value we predicted over
	 * @param featureID Feature ID of object whose value we predicted over
	 * @param referstosid "Refers-To" edge between a reference and its node.
	 * This may happen if entity resolution was applied prior to collective classification.
	 * If null, ignore value.  Otherwise, compute predictions for labels of entity
	 * based on the number of references.
	 * @param decorable Iterable group of decorable items whose value we predicted
	 * @param truegraph Graph containing ground truth
	 * @return Prediction group
	 */
	public static PredictionGroup CREATEOCPREDS(String schemaID, String featureID, String referstosid,
			Iterable<? extends Decorable> decorable, Graph truegraph) {
		return GroovyExperiment.CREATEOCPREDS(schemaID, featureID, referstosid, decorable, truegraph, false);
	}
	
	/**
	 * Return the object classification predictions
	 * 
	 * @param schemaID Schema ID of object whose value we predicted over
	 * @param featureID Feature ID of object whose value we predicted over
	 * @param referstosid "Refers-To" edge between a reference and its node.
	 * This may happen if entity resolution was applied prior to collective classification.
	 * If null, ignore value.  Otherwise, compute predictions for labels of entity
	 * based on the number of references.
	 * @param decorable Iterable group of decorable items whose value we predicted
	 * @param truegraph Graph containing ground truth
	 * @param shouldweightbyentity Weight the prediction by entity
	 * @return Prediction group
	 */
	public static PredictionGroup CREATEOCPREDS(String schemaID, String featureID, String referstosid,
			Iterable<? extends Decorable> decorable, Graph truegraph, boolean shouldweightbyentity) {
		// Get categories
		Schema schema = truegraph.getSchema(schemaID);
		CategFeature f = (CategFeature) schema.getFeature(featureID);
		CategValuePredGroup cpgroup = new CategValuePredGroup(f.getAllCategories());
		
		// Go over all the nodes we predicted a label over
		Iterator<? extends Decorable> ditr = decorable.iterator();
		while(ditr.hasNext()) {
			GraphItem gi = (GraphItem) ditr.next();
			
			if(referstosid!=null) {
				// If the label was made over a graph item resulting from entity resolution.
				CategValue predval = (CategValue) gi.getFeatureValue(featureID);
				
				Set<Node> refs = ERUtils.getRefersToReferences((Node) gi, referstosid);
				for(Node ref:refs) {
					// Create and add prediction
					Node entity = ERUtils.getRefersToEntity(
							(Node) truegraph.getEquivalentGraphItem(ref.getID()), referstosid);
					double weight = 1.0;
					if(shouldweightbyentity) {
						weight = 1.0 / entity.numAdjacentGraphItems(referstosid);
					}
					
					CategValue trueval = (CategValue) entity.getFeatureValue(featureID);
					CategValuePred cvp = new CategValuePred(trueval.getCategory(),
							predval.getCategory(), predval.getProbs(), weight);
					cpgroup.addPrediction(cvp);
				}
			} else {
				// If the label was made over a graph item not resulting from
				// entity resolution.
				
				// Create and add prediction
				GraphItem outputgi = truegraph.getEquivalentGraphItem(gi.getID());
				CategValue trueval = (CategValue) outputgi.getFeatureValue(featureID);
				CategValue predval = (CategValue) gi.getFeatureValue(featureID);
				
				CategValuePred cvp = new CategValuePred(trueval.getCategory(),
						predval.getCategory(), predval.getProbs());
				cpgroup.addPrediction(cvp);
			}
		}
		
		return cpgroup;
	}
	
	/**
	 * Create the predictions associated with an entity resolution tasks.
	 * This utility assumes that both the predicted and true
	 * graphs represent the predictions using entity nodes and
	 * "refers-to" edges between the node and its references.
	 * 
	 * @param entitysid Schema ID of entity nodes
	 * @param refsid Schema ID of reference nodes
	 * @param referstosid Schema ID of "refers-to" edges
	 * @param predictedgraph Predicted graph
	 * @param truegraph True Graph
	 * @return Predictions associated with entity resolution
	 */
	public static PredictionGroup CREATEERPREDS(String entitysid,
			String refsid, String referstosid,
			Graph predictedgraph, Graph truegraph) {
		// For all the nodes referring to the same entity in an entity,
		// make a positive prediction for all pairs.
		ExistencePredGroup epg = new ExistencePredGroup();
		
		Iterator<Node> nitr = predictedgraph.getNodes(entitysid);
		while(nitr.hasNext()) {
			Node prede = nitr.next();
			
			List<Node> reflist = new ArrayList<Node>(ERUtils.getRefersToReferences(prede,referstosid));
			
			// Make a positive prediction for all pairs
			for(int i=0; i<reflist.size(); i++) {
				// Create all pairs involving i
				for(int j=i+1; j<reflist.size(); j++) {
					Node ientity = ERUtils.getRefersToEntity(
							(Node) truegraph.getEquivalentGraphItem(reflist.get(i).getID()), referstosid);
					Node jentity = ERUtils.getRefersToEntity(
							(Node) truegraph.getEquivalentGraphItem(reflist.get(j).getID()), referstosid);
					
					// Check to see if the merged items belong to the same entity in the output graph
					ExistencePred ep = new ExistencePred(
							ientity.equals(jentity)
							? ExistencePredGroup.EXIST : ExistencePredGroup.NOTEXIST);
					
					epg.addPrediction(ep);
				}
			}
		}
		
		// Set the total number to the number of possible pairs
		int totalrefs = predictedgraph.numGraphItems(refsid);
		epg.setNumTotal((totalrefs * (totalrefs-1)) / 2);
		
		// Set the total positive number to the number of possible pairs
		// of references for each entity
		int numpositives = 0;
		nitr = truegraph.getNodes(entitysid);
		while(nitr.hasNext()) {
			Node n = nitr.next();
			int numrefs = n.numAdjacentSources(referstosid);
			numpositives += ((numrefs * (numrefs-1)) / 2);
		}
		
		epg.setNumPositive(numpositives);
		
		return epg;
	}
	
	/**
	 * Create the predictions associated with an entity resolution tasks.
	 * This utility assumes that the predicted
	 * graph represent the predictions using entity nodes and
	 * "refers-to" edges between the node and its references,
	 * and that the ID of the corresponding entity, for each reference,
	 * is given by the specified feature.
	 * 
	 * @param entitysid Schema ID of entity nodes
	 * @param refsid Schema ID of reference nodes
	 * @param referstosid Schema ID of "refers-to" edges
	 * @param entityfid Feature ID of feature in the predicted graph
	 * which contains the ID of the corresponding entity in the true graph
	 * @param predictedgraph Predicted graph
	 * @param truegraph True Graph
	 * @return Predictions associated with entity resolution
	 */
	public static PredictionGroup CREATEERPREDS(String entitysid,
			String refsid, String referstosid, String entityfid,
			Graph predictedgraph, Graph truegraph) {
		// For all the nodes referring to the same entity in an entity,
		// make a positive prediction for all pairs.
		ExistencePredGroup epg = new ExistencePredGroup();
		
		Iterator<Node> nitr = predictedgraph.getNodes(entitysid);
		while(nitr.hasNext()) {
			Node prede = nitr.next();
			
			List<Node> reflist = new ArrayList<Node>(ERUtils.getRefersToReferences(prede,referstosid));
			
			// Make a positive prediction for all pairs
			for(int i=0; i<reflist.size(); i++) {
				// Create all pairs involving i
				for(int j=i+1; j<reflist.size(); j++) {
					Node ientity = (Node) truegraph.getEquivalentGraphItem(
							(GraphItemID) ((MultiIDValue) reflist.get(i).getFeatureValue(entityfid)).getID());
					Node jentity = (Node) truegraph.getEquivalentGraphItem(
							(GraphItemID) ((MultiIDValue) reflist.get(j).getFeatureValue(entityfid)).getID());
					
					// Check to see if the merged items belong to the same entity in the output graph
					ExistencePred ep = new ExistencePred(
							ientity.equals(jentity)
							? ExistencePredGroup.EXIST : ExistencePredGroup.NOTEXIST);
					
					epg.addPrediction(ep);
				}
			}
		}
		
		// Set the total number to the number of possible pairs
		int totalrefs = predictedgraph.numGraphItems(refsid);
		epg.setNumTotal((totalrefs * (totalrefs-1)) / 2);
		
		// Set the total positive number to the number of possible pairs
		// of references for each entity
		int numpositives = 0;
		nitr = predictedgraph.getNodes(refsid);
		KeyedCount<ID> refcount = new KeyedCount<ID>();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			ID eid = ((MultiIDValue) n.getFeatureValue(entityfid)).getID();
			refcount.increment(eid);
		}
		
		Set<ID> eids = refcount.getKeys();
		for(ID eid:eids) {
			int numrefs = refcount.getCount(eid);
			numpositives += ((numrefs * (numrefs-1)) / 2);
		}
		
		epg.setNumPositive(numpositives);
		
		return epg;
	}
	
	/**
	 * Create the predictions associated with an entity resolution tasks.
	 * This utility assumes that both the predicted and true
	 * graphs represent the predictions using "co-reference" edges.
	 * 
	 * @param refsid Schema ID of reference nodes
	 * @param corefsid Schema ID of "co-reference" edges
	 * @param predictedgraph Predicted graph
	 * @param truegraph True Graph
	 * @return Predictions associated with entity resolution
	 */
	public static PredictionGroup CREATEERPREDS(String refsid, String corefsid,
			Graph predictedgraph, Graph truegraph) {
		
		ExistencePredGroup epg = new ExistencePredGroup();
		
		// Create positive prediction for all edges
		Iterator<Edge> eitr = predictedgraph.getEdges(corefsid);
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			if(e.numNodes()!=2) {
				throw new UnsupportedTypeException("Only binary edges supported");
			}
			
			Iterator<Node> nitr = e.getAllNodes();
			Node n1 = nitr.next();
			Node n2 = nitr.next();
			
			Node equivn1 = (Node) truegraph.getEquivalentGraphItem(n1);
			Node equivn2 = (Node) truegraph.getEquivalentGraphItem(n2);
			
			// Check to see if the nodes share a "co-reference" edge in the true graph
			ExistencePred ep = new ExistencePred(
					equivn1.isAdjacent(equivn2, corefsid)
					? ExistencePredGroup.EXIST : ExistencePredGroup.NOTEXIST);
			
			epg.addPrediction(ep);
		}
		
		// Set the total number to the number of possible pairs
		int totalrefs = predictedgraph.numGraphItems(refsid);
		epg.setNumTotal((totalrefs * (totalrefs-1)) / 2);
		
		// Set the total positive number to the true
		// number of co-reference edges in the true graph
		epg.setNumPositive(truegraph.numGraphItems(corefsid));
		
		return epg;
	}
	
	/**
	 * Return the predictions related to link prediction.
	 * This utility evaluates over the existence of all edges in
	 * the predicted graph over all the edges of the output graph.
	 * It also assumes that the edges are predicted between
	 * nodes with the same schema ID.
	 * 
	 * @param predictedgraph Predicted gra
	 * @param truegraph True graph
	 * @param edgesid Schema ID of edges predicted
	 * @param nodesid Schema ID of the nodes incident the predicted edges
	 * @param existfid Feature ID of existence featere
	 * @return Predictions resulting from link prediction
	 */
	public static PredictionGroup CREATELPPREDS(Graph predictedgraph,Graph truegraph,
			String edgesid, String nodesid, String existfid) {
		return GroovyExperiment.CREATELPPREDS(predictedgraph, truegraph,
				edgesid, nodesid,
				null, null, existfid);
	}
	
	/**
	 * Return the predictions related to link prediction.
	 * This utility evaluates over the existence of all edges in
	 * the predicted graph over all the edges of the output graph.
	 * It also assumes that the edges are predicted between
	 * nodes with the same schema ID.
	 * 
	 * @param predictedgraph Predicted graph
	 * @param truegraph True graph
	 * @param edgesid Schema ID of edges predicted
	 * @param nodesid Schema ID of the nodes incident the predicted edges
	 * @param referstosid "Refers-To" edge between a reference and its node.
	 * This may happen if entity resolution was applied prior to collective classification.
	 * If null, ignore value.  Otherwise, compute predictions for labels of entity
	 * based on the number of references.
	 * @param refsid Schema ID of the references connected to the entities.
	 * If null, assume the edges were predicted over unambiguous entities
	 * @param existfid Feature ID of existence feature.  Set to null if no such feature exists.
	 * @return Predictions resulting from link prediction
	 */
	public static PredictionGroup CREATELPPREDS(Graph predictedgraph,Graph truegraph,
			String edgesid, String nodesid, String referstosid, String refsid, String existfid) {
		return GroovyExperiment.CREATELPPREDS(predictedgraph, truegraph,
				edgesid, nodesid, referstosid, refsid, existfid, false);
	}
	
	/**
	 * Return the predictions related to link prediction.
	 * This utility evaluates over the existence of all edges in
	 * the predicted graph over all the edges of the output graph.
	 * It also assumes that the edges are predicted between
	 * nodes with the same schema ID.
	 * 
	 * @param predictedgraph Predicted graph
	 * @param truegraph True graph
	 * @param edgesid Schema ID of edges predicted
	 * @param nodesid Schema ID of the nodes incident the predicted edges
	 * @param referstosid "Refers-To" edge between a reference and its node.
	 * This may happen if entity resolution was applied prior to collective classification.
	 * If null, ignore value.  Otherwise, compute predictions for labels of entity
	 * based on the number of references.
	 * @param refsid Schema ID of the references connected to the entities.
	 * If null, assume the edges were predicted over unambiguous entities
	 * @param existfid Feature ID of existence feature.  Set to null if no such feature exists.
	 * @param shouldweightbyentity Weight the prediction by entity
	 * @return Predictions resulting from link prediction
	 */
	public static PredictionGroup CREATELPPREDS(Graph predictedgraph,Graph truegraph,
			String edgesid, String nodesid, String referstosid, String refsid, String existfid,
			boolean shouldweightbyentity) {
		return GroovyExperiment.CREATELPPREDS(predictedgraph, truegraph, edgesid, nodesid,
				referstosid, refsid, existfid, shouldweightbyentity, false);
	}
	
	/**
	 * Return the predictions related to link prediction.
	 * This utility evaluates over the existence of all edges in
	 * the predicted graph over all the edges of the output graph.
	 * It also assumes that the edges are predicted between
	 * nodes with the same schema ID.
	 * 
	 * @param predictedgraph Predicted graph
	 * @param truegraph True graph
	 * @param edgesid Schema ID of edges predicted
	 * @param nodesid Schema ID of the nodes incident the predicted edges
	 * @param referstosid "Refers-To" edge between a reference and its node.
	 * This may happen if entity resolution was applied prior to collective classification.
	 * If null, ignore value.  Otherwise, compute predictions for labels of entity
	 * based on the number of references.
	 * @param refsid Schema ID of the references connected to the entities.
	 * If null, assume the edges were predicted over unambiguous entities
	 * @param existfid Feature ID of existence feature.  Set to null if no such feature exists.
	 * @param shouldweightbyentity Weight the prediction by entity
	 * @param allownorefentity If true,allow entities not to have reference, and thus not be counted
	 * when computing the number of true edges (when unweighted).
	 * @return Predictions resulting from link prediction
	 */
	public static PredictionGroup CREATELPPREDS(Graph predictedgraph,Graph truegraph,
			String edgesid, String nodesid, String referstosid, String refsid, String existfid,
			boolean shouldweightbyentity, boolean allownorefentity) {
		boolean isdirected = predictedgraph.getSchemaType(edgesid).equals(SchemaType.DIRECTED);
		SimpleTimer timer = new SimpleTimer();
		
		// Cache information to make the computation more efficient
		// Cache reference to entity mappings
		Map<Node,Node> trueref2entity = new HashMap<Node,Node>();
		
		// Precompute adjacencies
		Map<Node,Set<Node>> trueentityadjacencies = new HashMap<Node,Set<Node>>();
		
		// Precompute the references of predicted entities
		Map<Node,Set<Node>> predentity2refs = new HashMap<Node,Set<Node>>();
		
		// Between all the references in edges of the predicted graph,
		// make a positive prediction for all pairs.
		ExistencePredGroup epg = new ExistencePredGroup();
		Iterator<Edge> eitr = predictedgraph.getEdges(edgesid);
		int counter = 0;
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			counter++;
			if(counter%1000==0) {
				Log.DEBUG(counter+"of"+predictedgraph.numGraphItems(edgesid)+" "+timer.checkpointTime());
			}
			
			boolean ispredexisting = true;
			double[] prob = null;
			if(existfid!=null) {
				FeatureValue fv = e.getFeatureValue(existfid);
				if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
					// Assume edges with unknown values are predicted as true
					prob = new double[2];
					prob[LinkPredictor.EXISTINDEX] = 1;
				} else {
					CategValue cv = (CategValue) fv;
					String cat = cv.getCategory();
					if(cat.equals(LinkPredictor.EXIST)) {
						ispredexisting = true;
					} else if(cat.equals(LinkPredictor.NOTEXIST)) {
						ispredexisting = false;
					} else {
						throw new InvalidStateException("Invalid existence category: "+cat);
					}
					
					prob = cv.getProbs();
				}
			}
			
			// Only defined for binary edges right now
			if(e.numNodes() != 2) {
				throw new UnsupportedTypeException("Only defined for binary edges: "
						+e.numNodes());
			}
			
			Node n1 = null;
			Node n2 = null;
			if(isdirected) {
				n1 = ((DirectedEdge) e).getSourceNodes().next();
				n2 = ((DirectedEdge) e).getTargetNodes().next();
			} else {
				Iterator<Node> nitr = e.getAllNodes();
				n1 = nitr.next();
				n2 = nitr.next();
			}
			
			// Get references merged into the merged nodes
			Set<Node> n1refs = null;
			if(predentity2refs.containsKey(n1)) {
				n1refs = predentity2refs.get(n1);
			} else {
				n1refs = ERUtils.getRefersToReferences(n1, referstosid);
				predentity2refs.put(n1, n1refs);
			}
			
			Set<Node> n2refs = null;
			if(predentity2refs.containsKey(n2)) {
				n2refs = predentity2refs.get(n2);
			} else {
				n2refs = ERUtils.getRefersToReferences(n2, referstosid);
				predentity2refs.put(n2, n2refs);
			}
			
			boolean n1noref = false;
			if(n1refs.isEmpty()) {
				n1noref = true;
				n1refs.add(n1);
			}
			
			boolean n2noref = false;
			if(n2refs.isEmpty()) {
				n2noref = true;
				n2refs.add(n2);
			}
			
			// Create a positive prediction for all pairwise
			for(Node n1ref:n1refs) {
				Node equivn1ref = (Node) truegraph.getEquivalentGraphItem(n1ref);
				
				Node n1entity = null;
				if(n1noref) {
					n1entity = equivn1ref;
				} else {
					if(trueref2entity.containsKey(equivn1ref)) {
						n1entity = trueref2entity.get(equivn1ref);
					} else {
						n1entity = ERUtils.getRefersToEntity(equivn1ref, referstosid);
						trueref2entity.put(equivn1ref, n1entity);
					}
				}
				
				double n1weight = 0;
				if(shouldweightbyentity) {
					if(allownorefentity) {
						n1weight = n2noref ? 0 : n1entity.numAdjacentGraphItems(referstosid);
					} else {
						n1weight = n2noref ? 1 : n1entity.numAdjacentGraphItems(referstosid);
					}
				}
				
				for(Node n2ref:n2refs) {
					Node equivn2ref = (Node) truegraph.getEquivalentGraphItem(n2ref);
					
					Node n2entity = null;
					if(n2noref) {
						n2entity = equivn2ref;
					} else {
						if(trueref2entity.containsKey(equivn2ref)) {
							n2entity = trueref2entity.get(equivn2ref);
						} else {
							n2entity = ERUtils.getRefersToEntity(equivn2ref, referstosid);
							trueref2entity.put(equivn2ref, n2entity);
						}
					}
					
					double n2weight = 0;
					if(shouldweightbyentity) {
						if(allownorefentity) {
							n2weight = n2noref ? 0 : n2entity.numAdjacentGraphItems(referstosid);
						} else {
							n2weight = n2noref ? 1 : n2entity.numAdjacentGraphItems(referstosid);
						}
					}
					
					boolean trueexists;
					if(isdirected) {
						if(!trueentityadjacencies.containsKey(n1entity)) {
							Iterator<Node> adjnitr = n1entity.getAdjacentTargets(edgesid);
							// Add a null value just to have any entry
							HashSet<Node> adjacencies = new HashSet<Node>();
							while(adjnitr.hasNext()) {
								adjacencies.add(adjnitr.next());
							}
							trueentityadjacencies.put(n1entity, adjacencies);
						}
						
						trueexists = trueentityadjacencies.get(n1entity).contains(n2entity);
					} else {
						if(!trueentityadjacencies.containsKey(n1entity)) {
							Iterator<GraphItem> adjnitr = n1entity.getAdjacentGraphItems(edgesid);
							// Add a null value just to have any entry
							HashSet<Node> adjacencies = new HashSet<Node>();
							while(adjnitr.hasNext()) {
								adjacencies.add((Node) adjnitr.next());
							}
							trueentityadjacencies.put(n1entity, adjacencies);
						}
						
						trueexists = trueentityadjacencies.get(n1entity).contains(n2entity);
					}
					
					String trueexistence =  trueexists ?
							ExistencePredGroup.EXIST : ExistencePredGroup.NOTEXIST;
					
					if(existfid==null) {
						// If an existence feature is not defined, assume predicted existing
						// with a probability of 1
						ExistencePred ep = null;
						if(shouldweightbyentity) {
							// Each predicted is weighted by 1/(num refs of entity 1)*(numrefs of entity 2)
							double weight = 1.0 / (n1weight * n2weight);
							ep = new ExistencePred(trueexistence, weight);
						} else {
							ep = new ExistencePred(trueexistence);
						}
						
						epg.addPrediction(ep);
					} else {
						ExistencePred ep = null;
						if(shouldweightbyentity) {
							double weight = 1;
							if(allownorefentity && (n1weight==0 || n2weight==0)) {
								weight = 0;
							} else {
								weight = 1.0 / (n1weight * n2weight);
							}
							// Each predicted is weighted by 1/(num refs of entity 1)*(numrefs of entity 2)
							ep = new ExistencePred(trueexistence, prob, weight);
						} else {
							ep = new ExistencePred(trueexistence, prob);
						}
						
						// Add positive and negative predictions differently
						if(ispredexisting) {
							epg.addPrediction(ep);
						} else {
							epg.addNegativePrediction(ep);
						}
					}
				}
			}
		}
		
		if(shouldweightbyentity) {
			// Set the total number to the number of possible pairs between entities
			// Note: Need to figure out how to do this when not considering all pairs
			int totalrefs = truegraph.numGraphItems(nodesid);
			if(isdirected) {
				epg.setNumTotal(totalrefs * (totalrefs-1));
			} else {
				epg.setNumTotal((totalrefs * (totalrefs-1)) / 2);
			}
			
			// Set the total positive number to the number of edges in output graph
			int numpositives = truegraph.numGraphItems(edgesid);
			epg.setNumPositive(numpositives);
		} else {
			// Set the total number to the number of possible pairs
			int totalrefs = truegraph.numGraphItems(refsid==null?nodesid:refsid);
			if(isdirected) {
				epg.setNumTotal(totalrefs * (totalrefs-1));
			} else {
				epg.setNumTotal((totalrefs * (totalrefs-1)) / 2);
			}
			
			// Set the total positive number to the number of possible pairs
			// of references for each entity
			int numpositives = 0;
			eitr = truegraph.getEdges(edgesid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				
				// Only defined for binary edges right now
				if(e.numNodes() != 2) {
					throw new UnsupportedTypeException("Only defined for binary edges: "
							+e.numNodes());
				}
				
				if(isdirected) {
					DirectedEdge de = (DirectedEdge) e;
					Node n1 = de.getSourceNodes().next();
					Node n2 = de.getTargetNodes().next();
					
					// Get references merged into the merged nodes
					Set<Node> n1refs = ERUtils.getRefersToReferences(n1, referstosid);
					Set<Node> n2refs = ERUtils.getRefersToReferences(n2, referstosid);
					
					if(referstosid==null && n1refs.isEmpty() && !allownorefentity) {
						n1refs.add(n1);
					}
					
					if(referstosid==null && n2refs.isEmpty() && !allownorefentity) {
						n2refs.add(n2);
					}
					
					numpositives += n1refs.size() * n2refs.size();
				} else {
					Iterator<Node> edgenitr = e.getAllNodes();
					Node n1 = edgenitr.next();
					Node n2 = edgenitr.next();
					
					// Get references merged into the merged nodes
					Set<Node> n1refs = ERUtils.getRefersToReferences(n1, referstosid);
					Set<Node> n2refs = ERUtils.getRefersToReferences(n2, referstosid);
					
					if(referstosid==null && n1refs.isEmpty() && !allownorefentity) {
						n1refs.add(n1);
					}
					
					if(referstosid==null && n2refs.isEmpty() && !allownorefentity) {
						n2refs.add(n2);
					}
					
					numpositives += n1refs.size() * n2refs.size();
				}
			}
			
			epg.setNumPositive(numpositives);
		}
		
		Log.DEBUG(timer.checkpointTime());
		
		return epg;
	}
	
	/**
	 * Apply the prediction statistics over the provided set of predictions.
	 * The argument "statclass" is a comma delimited list of the object
	 * classes of implementations of {@link Statistic} using the same
	 * form, per object class, specified in {@link #LOAD(String)}.
	 * Parameters for the statistics must be previously loaded using
	 * {@link #LOADPARAMETERS}.
	 * 
	 * @param preds Prediction group
	 * @param statclass Comma delimited list of prediction statistic classes
	 */
	public static void PRINTPREDSTATS(PredictionGroup preds, String statclass) {
		String[] statclasses = getClass(statclass).split(",");
		for(String currstat:statclasses) {
			currstat = getClass(currstat);
			Statistic stat = (Statistic) Dynamic.forConfigurableName(Statistic.class, currstat, conf);
			String statstring = stat.getStatisticString(preds);
			
			String statcid = stat.getCID();
			statcid = (statcid==null) ? "" : " "+stat.getCID();
			Log.INFO("Statistic string"+statcid+": "+statstring);
		}
	}
	
	/**
	 * Apply the graph statistics over the provided graph.
	 * The argument "statclass" is a comma delimited list of the object
	 * classes of implementations of {@link GraphStatistic} using the same
	 * form, per object class, specified in {@link #LOAD(String)}.
	 * Parameters for the statistics must be previously loaded using
	 * {@link #LOADPARAMETERS}.
	 * 
	 * @param g Graph
	 * @param statclass Comma delimited list of graph statistic classes
	 */
	public static void PRINTGRAPHSTATS(Graph g, String statclass) {
		String[] statclasses = getClass(statclass).split(",");
		for(String currstat:statclasses) {
			currstat = getClass(currstat);
			GraphStatistic stat = (GraphStatistic) Dynamic.forConfigurableName(GraphStatistic.class,
					currstat, conf);
			Map<String,String> statmap = stat.getStatisticStrings(g);
			
			String statcid = stat.getCID();
			statcid = (statcid==null) ? " "+currstat : " "+stat.getCID();
			Log.INFO("Graph Statistic string"+statcid+":\n"
					+(statmap.size()!=0 ? MapUtils.map2string(statmap,"=","\n") : "None")
					+"\n");
		}
	}
	
	/**
	 * Apply some operation over the specified graph.
	 * The operations vary based on the class of object:
	 * Supported operations are:
	 * <UL>
	 * <LI> {@link Filter}-Apply {@link Filter#filter(Graph)}
	 * <LI> {@link Transformer}-Apply {@link Transformer#transform(Graph)}
	 * <LI> {@link Noise}-Apply {@link Noise#addNoise(Graph)}
	 * <LI> {@link Decorator}-Apply {@link Decorator#decorate(Graph)}
	 * <LI> {@link Visualization}-Apply {@link Visualization#visualize(Graph)}
	 * </UL>
	 * Parameters for the statistics must be previously loaded using
	 * (e.g., {@link #LOADPARAMETERS}).
	 * 
	 * @param g Graph
	 * @param classes Comma delimited object classes to apply to graph
	 */
	public static void APPLY(Graph g, String classes) {
		String[] parts = classes.split(",");
		for(String p:parts) {
			p = getClass(p);
			
			Object obj = Dynamic.forConfigurableName(Object.class, p, conf);
			if(obj instanceof Filter) {
				((Filter) obj).filter(g);
			} else if(obj instanceof Transformer) {
				((Transformer) obj).transform(g);
			} else if(obj instanceof Noise) {
				((Noise) obj).addNoise(g);
			} else if(obj instanceof Decorator) {
				((Decorator) obj).decorate(g);
			} else if(obj instanceof Visualization) {
				((Visualization) obj).visualize(g);
			} else {
				throw new UnsupportedTypeException(obj.getClass().getCanonicalName());
			}
		}
	}
	
	/**
	 * Internal method to interpret the provided class strings.
	 * If a parameter matches the classstring provided,
	 * the value of the parameter is used as the Java object
	 * class in the format described in
	 * {@link Dynamic#forConfigurableName(Class, String)}.
	 * 
	 * @param classstring
	 * @return
	 */
	private static String getClass(String classstring) {
		String newclassstring = classstring;
		if(conf.hasParameter(classstring)) {
			newclassstring = conf.getStringParameter(classstring);
			
			if(!newclassstring.contains(":")) {
				newclassstring = classstring+":"+newclassstring;
			}
		}
		
		return newclassstring;
	}
	
	/**
	 * Main experiment method for unning experiment.
	 * 
	 * @param args Arguments for experiment.  Arguments in the form: &lt;groovyscript&gt; [&lt;configfile&gt;]
	 */
	public static void main(String[] args) {
		if(args.length < 1 || args.length > 2) {
			Log.INFO("Invalid Arguments.  Format: <groovyscript> [<configfile>]");
			return;
		}
		
		String groovyfile = args[0];
		try {
			shell.evaluate(new File(groovyfile));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		// Load config file, if specified
		if(args.length==2) {
			LOADPARAMETERS(args[1]);
		}
	}
	
	public static void PRINTOVERVIEW(Graph g) {
		Log.INFO("Loaded graph "+g+": "+GraphUtils.getSimpleGraphOverview(g));
	}
}
