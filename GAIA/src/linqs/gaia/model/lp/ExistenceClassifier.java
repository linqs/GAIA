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
package linqs.gaia.model.lp;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.log.Log;
import linqs.gaia.model.oc.Classifier;
import linqs.gaia.model.oc.OCUtils;
import linqs.gaia.model.oc.ncc.LibSVMClassifier;
import linqs.gaia.model.oc.ncc.VBClassifier;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.SimpleTimer;
import linqs.gaia.util.TopK;

/**
 * Predict the existence of an edge using a vector based classifier
 * over the edges attributes.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> classifierclass-lassifier ({@link Classifier}) to use,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * Default is {@link linqs.gaia.model.oc.ncc.LibSVMClassifier}.
 * <LI> checkpoint-If set, this the current number of links predicted over is printed
 * as an INFO message whenever (the current number)%(checkpoint) == 0.
 * <LI> learnkratio-If yes, learn a k-ratio from the training data where
 * (k-ratio)*(number of considered edges)=(number of existing edges).
 * <LI> tmpexistfeature-Feature ID to add temporarily for use in learning a classifier.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class ExistenceClassifier extends BaseConfigurable implements LinkPredictor {
	private static final long serialVersionUID = 1L;
	
	private Classifier classifier;
	private String tmpexistfeature = "GAIA-TMPLINKEXIST";
	private String edgeschemaid;
	private Double checkpoint = null;
	private Double kratio = null;
	private boolean learnkratio = false;

	private void initialize(String edgeschemaid) {
		this.edgeschemaid = edgeschemaid;
		
		String cclass = LibSVMClassifier.class.getCanonicalName();
		cclass = this.getStringParameter("classifierclass");
		this.classifier = (Classifier) Dynamic.forConfigurableName(Classifier.class, cclass);
		if(this.hasParameter("checkpoint")) {
			this.checkpoint = this.getDoubleParameter("checkpoint");
		}
		
		this.classifier.copyParameters(this);
		
		if(this.hasParameter("tmpexistfeature")) {
			this.tmpexistfeature = this.getStringParameter("tmpexistfeature");
		}
		
		this.learnkratio = this.hasParameter("learnkratio", "yes");
	}
	
	/**
	 * Assume the input graph is in KE+KN representation.
	 */
	public void learn(Graph graph, PotentialLinkGenerator generator, String edgeschemaid) {
		this.initialize(edgeschemaid);
		int numpositive = 0;
		int numnegative = 0;
		
		// Create exist feature
		Schema schema = graph.getSchema(edgeschemaid);
		schema.addFeature(tmpexistfeature, LinkPredictor.EXISTENCEFEATURE);
		graph.updateSchema(edgeschemaid, schema);
		
		// Annotate all the current edges as existing
		Iterator<Edge> itr = graph.getEdges(edgeschemaid);
		CategValue existcv = new CategValue(LinkPredictor.EXIST, new double[]{0,1});
		CategValue notexistcv = new CategValue(LinkPredictor.NOTEXIST, new double[]{1,0});
		while(itr.hasNext()) {
			Edge e = itr.next();
			e.setFeatureValue(tmpexistfeature, existcv);
			numpositive++;
		}
		
		// Add new edges using link generator and label those as not existing
		itr = generator.getLinksIteratively(graph, this.edgeschemaid);
		while(itr.hasNext()) {
			Edge e = itr.next();
			e.setFeatureValue(tmpexistfeature, notexistcv);
			numnegative++;
		}
		
		Log.DEBUG("Number of training existing edges: "+numpositive
				+" Number of training non-existing edges: "+numnegative);
		
		// Add all edges to a collection
		Iterable<GraphItem> alledges = OCUtils.getIterableItems(graph, this.edgeschemaid);
		
		// Pass iterable collection to vbc classifier and learn model
		classifier.learn(alledges, this.edgeschemaid, tmpexistfeature);
		
		// Reset graph
		// Remove the added non existing edges
		itr = graph.getEdges(this.edgeschemaid);
		while(itr.hasNext()) {
			Edge e = itr.next();
			if(e.getFeatureValue(tmpexistfeature).equals(notexistcv)) {
				graph.removeEdge(e);
			}
		}
		
		// Learn k ratio
		if(learnkratio) {
			kratio = (double) numpositive/(double) (numnegative+numpositive);
			Log.DEBUG("Learned K-Ratio: "+kratio);
		}
		
		// Remove exist feature
		schema.removeFeature(tmpexistfeature);
		graph.updateSchema(edgeschemaid, schema);
	}
	
	public void learn(Graph graph, Iterable<Edge> knownedges, String edgeschemaid,
			String existfeature) {
		this.initialize(edgeschemaid);
		this.tmpexistfeature = existfeature;
		
		// Check to see if exist feature exists.  Throw exception if not.
		Schema schema = graph.getSchema(edgeschemaid);
		if(!schema.hasFeature(existfeature)) {
			throw new ConfigurationException("Exist feature not previously defined: "+existfeature);
		}
		
		// Learn k ratio
		if(learnkratio) {
			int numpositive = 0;
			int numnegative = 0;
			for(Edge e:knownedges) {
				FeatureValue fv = e.getFeatureValue(existfeature);
				if(fv.equals(FeatureValue.UNKNOWN_VALUE) || ((CategValue) fv).hasCateg(LinkPredictor.EXIST)) {
					// Consider no value being set as positive edge
					numpositive++;
				} else {
					numnegative++;
				}
			}
			
			kratio = (double) numpositive/(double) (numnegative+numpositive);
			Log.DEBUG("Learned K-Ratio: "+kratio);
		}
		
		// Pass iterable collection to vbc classifier and learn model
		classifier.learn(knownedges, this.edgeschemaid, existfeature);
	}

	public void predict(Graph graph, Iterable<Edge> unknownedges) {
		int numpotential = 0;
		if(kratio!=null) {
			numpotential = IteratorUtils.numIterable(unknownedges);
		}
		
		this.predict(graph, unknownedges.iterator(),
				numpotential,
				true, null);
	}
	
	public void predict(Graph graph, Iterable<Edge> unknownedges,
			boolean removenotexist, String existfeature) {
		int numpotential = 0;
		if(kratio!=null) {
			numpotential = IteratorUtils.numIterable(unknownedges);
		}
		
		this.predict(graph, unknownedges.iterator(),
				numpotential,
				removenotexist, existfeature);
	}

	public void predict(Graph graph, PotentialLinkGenerator generator) {
		int numpotential = 0;
		if(kratio!=null) {
			Iterator<Edge> eitr = generator.getLinksIteratively(graph, edgeschemaid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				graph.removeEdge(e);
				numpotential++;
			}
		}
		
		this.predict(graph, generator.getLinksIteratively(graph, edgeschemaid),
				numpotential,
				true, null);
	}

	public void predict(Graph graph, PotentialLinkGenerator generator,
			boolean removenotexist, String existfeature) {
		
		int numpotential = 0;
		if(kratio!=null) {
			Iterator<Edge> eitr = generator.getLinksIteratively(graph, edgeschemaid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				graph.removeEdge(e);
				numpotential++;
			}
		}
			
		this.predict(graph,
				generator.getLinksIteratively(graph, edgeschemaid),
				numpotential,
				removenotexist,
				existfeature);
	}
	
	private void predict(Graph graph, Iterator<Edge> eitr, int numpotential,
			boolean removenotexist, String existfeature) {
		
		TopK<Edge> topk = null;
		if(kratio!=null) {
			double kvalue = (double) numpotential*kratio;
			Log.DEBUG("Prediction Top K="+kvalue);
			// Initialize TopK object
			topk = new TopK<Edge>((int) kvalue);
		}
		
		// Check to see if exist feature exists.  Throw exception if not.
		Schema schema = graph.getSchema(edgeschemaid);
		
		if(existfeature!=null && !schema.hasFeature(existfeature)) {
			throw new ConfigurationException("Exist feature not previously defined: "+existfeature);
		}
		
		// Create tmpexistfeature feature
		boolean addedtmp = false;
		if(!schema.hasFeature(this.tmpexistfeature)) {
			schema.addFeature(this.tmpexistfeature, LinkPredictor.EXISTENCEFEATURE);
			graph.updateSchema(edgeschemaid, schema);
			addedtmp = true;
		}
		
		if(this.classifier instanceof VBClassifier) {
			// Iteratively predict edges using iterator
			int numpredover = 0;
			int numexist = 0;
			SimpleTimer timer = new SimpleTimer();
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				FeatureValue value = ((VBClassifier) this.classifier).predict(e);
				if(value.equals(FeatureValue.UNKNOWN_VALUE)) {
					throw new UnsupportedTypeException("Unknown value not supported");
				}
				
				if(kratio==null) {
					// Handle regular
					if(removenotexist==true
							&& !value.equals(FeatureValue.UNKNOWN_VALUE)
							&& ((CategValue) value).hasCateg(LinkPredictor.NOTEXIST)) {
						// Remove edge not predicted to exist
						graph.removeEdge(e);
					} else {
						// If its not removed, set its existfeature, if defined
						if(existfeature!=null) {
							e.setFeatureValue(existfeature, value);
						}
						
						numexist++;
					}
				} else {
					// Handle topK
					// Set all initiatially as existing
					numexist++;
					if(existfeature!=null) {
						e.setFeatureValue(existfeature, LinkPredictor.EXISTVALUE);
					}
					
					CategValue cvalue = (CategValue) value;
					List<Edge> removededges = topk.add(cvalue.getProbs()[LinkPredictor.EXISTINDEX], e);
					for(Edge removede:removededges) {
						if(removenotexist) {
							graph.removeEdge(removede);
						} else {
							if(existfeature!=null) {
								removede.setFeatureValue(existfeature, LinkPredictor.NOTEXISTVALUE);
							}
						}
						
						numexist--;
					}
				}
				
				numpredover++;
				if(this.checkpoint != null && (numpredover % this.checkpoint) == 0) {
					Log.INFO("Existence Classifier model predicted: "
							+numexist+"/"+numpredover+" ("+timer.timeLapse(true)+")");
					timer.start();
				}
			}
			
			if(this.checkpoint != null) {
				Log.INFO("Existence Classifier model predicted: "
						+numexist+"/"+numpredover+" ("+timer.timeLapse(true)+")");
				timer.start();
			}
		} else {
			List<Edge> edges = IteratorUtils.iterator2edgelist(eitr);
			int numpredover = edges.size();
			int numexist = 0;
			SimpleTimer timer = new SimpleTimer();
			
			this.classifier.predict(edges);
			for(Edge e:edges) {
				FeatureValue value = e.getFeatureValue(this.tmpexistfeature);
				
				if(kratio==null) {
					// Handle regular
					if(removenotexist==true
							&& !value.equals(FeatureValue.UNKNOWN_VALUE)
							&& ((CategValue) value).hasCateg(LinkPredictor.NOTEXIST)) {
						// Remove edge not predicted to exist
						graph.removeEdge(e);
					} else {
						// If its not removed, set its existfeature, if defined
						if(existfeature!=null) {
							e.setFeatureValue(existfeature, value);
						}
						
						numexist++;
					}
				} else {
					// Handle topK
					// Set all initiatially as existing
					numexist++;
					if(existfeature!=null) {
						e.setFeatureValue(existfeature, LinkPredictor.EXISTVALUE);
					}
					
					CategValue cvalue = (CategValue) value;
					List<Edge> removededges = topk.add(cvalue.getProbs()[LinkPredictor.EXISTINDEX], e);
					for(Edge removede:removededges) {
						if(removenotexist) {
							graph.removeEdge(removede);
						} else {
							if(existfeature!=null) {
								removede.setFeatureValue(existfeature, LinkPredictor.NOTEXISTVALUE);
							}
						}
						
						numexist--;
					}
				}
			}
			
			if(this.checkpoint != null) {
				Log.INFO("Existence Classifier model predicted: "
						+numexist+"/"+numpredover+" ("+timer.timeLapse(true)+")");
				timer.start();
			}
		}
		
		// If we created the temporary feature for use since
		// it wasn't defined before, make sure to remove it
		// when you're done.
		if(addedtmp) {
			schema.removeFeature(this.tmpexistfeature);
			graph.updateSchema(edgeschemaid, schema);
		}
	}
	
	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		if(this.hasParameter("saved-kratio")) {
			this.kratio = this.getDoubleParameter("saved-kratio");
			this.learnkratio = true;
		}
		
		if(this.hasParameter("saved-tmpexistfeature")) {
			this.tmpexistfeature = this.getStringParameter("saved-tmpexistfeature");
		}
		
		String edgeschemaid = this.getStringParameter("saved-edgeschemaid");
		// Note:  This will also take care of the checkpoint.
		this.initialize(edgeschemaid);
		
		this.classifier.loadModel(directory+File.separator+"vbcmodel");
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		if(this.kratio!=null) {
			this.setParameter("saved-kratio", this.kratio);
		}
		
		this.setParameter("saved-edgeschemaid", this.edgeschemaid);
		this.setParameter("saved-tmpexistfeature", tmpexistfeature);
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
		
		this.classifier.saveModel(directory+File.separator+"vbcmodel");
	}
}
