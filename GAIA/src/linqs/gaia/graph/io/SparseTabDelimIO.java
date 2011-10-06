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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.FileFormatException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.feature.values.UnknownValue;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * A tab delimited format designed for dealing with sparse
 * data.  The format is the same as that of {@link TabDelimIO}
 * with the exception of how and what feature values are saved.
 * Unlike {@link TabDelimIO}, {@link SparseTabDelimIO} does not save
 * the values of features for nodes, edges, or graphs
 * whose value is not known and for closed features whose
 * value is the same as the default value.
 * As a result, this format can save a significant amount of disk
 * space to store the graph, as well as speed up
 * loading and saving for graphs with sparse features.
 * <p>
 * The data is available in a sparse, tab delimited format.
 * There is a separate file for each type of node and edge, as well as one for the graph itself.
 * The basic format for node and graph files is as follows:<br>
 * [GRAPH|NODE]\t&lt;schemaid<br>
 * [NO_FEATURES|&lt;featuretype&gt;:&lt;featurename&gt;[:&lt;defaultvalue&gt;][\t&lt;featuretype&gt;:&lt;featurename&gt;[:&lt;defaultvalue&gt;]]*]<br>
 * &lt;id&gt;\t&lt;values&gt;*<br>
 * <p>
 * The first line indicates the type of object defined in this file (i.e., graph, node)
 * as well as a string which identifies this set of objects (e.g., people nodes, friendship edges).
 * The second line either contains NO_FEATURES, which indicates that this set of objects
 * do not have any attributes defined for it, or it contains a tab delimited set of attribute
 * declarations which specifies the type of the attribute (string,numeric,cat=[&lt;category&gt;][,&lt;category&gt;]*),
 * the name of the attribute (i.e., age, height, hair color), and, optionally, a default value.
 * <p>
 * The subsequent lines each correspond to one object
 * (for graph files, you have exactly one line which defines
 * the id and attribute values of the graph itself).
 * A unique ID is specified for each object in the first column and the subsequent
 * columns contain a key/value pair (of the form key=value) where key is
 * the name of an attribute (defined in the second line) and
 * the value is a string representation of the attribute value of that attribute
 * for the object described by that line.
 * <p>
 * Example:<br>
 * NODE person<br>
 * string:name	numeric:age	cat=undergrad,grad:type:undergrad<br>
 * s1	name=Bob	age=22	type=grad<br>
 * s2	name=Ann	age=34<br>
 * <p>
 * [DIRECTED|UNDIRECTED]\t&lt;schemaid<br>
 * [NO_FEATURES|&lt;featuretype&gt;:&lt;featurename&gt;[:&lt;defaultvalue&gt;][\t&lt;featuretype&gt;:&lt;featurename&gt;[:&lt;defaultvalue&gt;]]*]<br>
 * &lt;id&gt;\t&lt;values&gt;*<br>
 * <p>
 * For edges, after the ID for each edge (directed or undirected),
 * we have a similar format but where the format for each line referring
 * to each edge also contains information about the nodes that the edge is incident upon.
 * For undirected edges, each line has the form:<br>
 * &lt;id&gt;\t&lt;nodetype&gt;&lt;nodeid&gt;[\t&lt;nodeid&gt;]*\t|\t&lt;values&gt;*<br>
 * where node type refers to the type id (defined in the first line of each node file)
 * and nodeid refers to the object id (defined by the first column of a node file)
 * that the undirected edge is incident to.
 * <p>
 * For directed edges, each line has the form:
 * <p>
 * &lt;id&gt;\t&lt;sourcenodetype&gt;:&lt;sourcenodeid&gt;[\t&lt;sourcenodetype&gt;:&lt;sourcenodeid&gt;]*\t|\t&lt;targetnodetype&gt;:&lt;targetnodeid&gt;[\t&lt;targetnodetype&gt;:&lt;targetnodeid&gt;]*\t|\t&lt;values&gt;*
 * <p>
 * where sourcenodetype and sourcenodeid is the node type and id of a source of this directed edge and targetnodetype and targetnodeid is a target of this directed edge.  Note that this format support hyperedges (undirected edges can have 1 or more nodes and directed edges can have one or more source and one or more target nodes).
 * <p>
 * Example:<br>
 * DIRECTED	Friends<br>
 * NO_FEATURES<br>
 * Friendship1	Person:s1	Person:s2<br>
 * <br>
 * UNDIRECTED	MemberOf<br>
 * NO_FEATURES<br>
 * M1	Person:s1	|	Group:g1<br>
 * <p>
 * Required Parameters: (Same as {@link TabDelimIO})
 * <UL>
 * <LI> For loading:
 *      <UL>
 *      <LI> files-Comma delimited list of the files to use.
 *      Files must be listed in order Graph file, Node files and Edge files.
 *      This parameter is used over filedirectory if both are specified.
 *      Not required if filedir is specified.
 *      <LI> filedirectory-Directory of files to load.
 *      The input will try to load
 *      all files in the directory and will throw a warning
 *      for files it cannot load.
 *      Not required if files is specified or if using
 *      {@link DirectoryBasedIO} methods.
 * </UL>
 * <LI> For saving:
 *      <UL>
 *      <LI> filedirectory-Directory to store all the resulting files
 *      </UL>
 * </UL>
 * 
 * Optional Parameters: (Same as {@link TabDelimIO})
 * <UL>
 * <LI> For loading:
 *       <UL>
 *       <LI> graphclass-Full java class for the graph,
 *       instantiated using {@link Dynamic#forConfigurableName}.
 *       Default is {@link linqs.gaia.graph.datagraph.DataGraph}.
 *       <LI> loadfids-Comma delimited list of feature ids.  If set,
 *       load only the feature values for the specified feature ids.
 *       This will save both time and memory for loading graphs
 *       with large numbers of features.
 *       <LI> fileprefix-Prefix the files must have to be loaded, specifically
 *       when using the filedirectory option.  Default is to load all specified
 *       files.
 *      <LI> graphobjid-If specifided, the following object id will
 *       used in place of the graphs object ID when loading.
 *       This ID is ignored when loading with a specified graph object id.
 *       </UL>
 * <LI> For saving:
 *       <UL>
 *       <LI> fileprefix-Prefix to use in naming the resulting files.  Default
 *       is to use the object id of the graph.
 *       <LI> savesids-Comma delimited list of feature schema IDs.
 *       If specified, during saving, only the graph items with the specified
 *       schema ID will be saved
 *       <LI> savederived-If yes, save the values of derived features.  If no,
 *       do not save the value of derived features.  Default is no.
 *       <LI> graphobjid-If specifided, the following object id will
 *       used in place of the graphs object ID when saving.
 *       </UL>
 * </UL>
 * 
 * @see linqs.gaia.graph.io.TabDelimIO
 * 
 * @author namatag
 *
 */
public class SparseTabDelimIO extends TabDelimIO implements IO {
	public final static String FEATURENAME_DELIMITER = "=";
	
	/**
	 * Process the feature values for the given decorable item
	 */
	protected void addFeatureValues(Decorable di, String[] values, Set<String> loadfids){
		if(values==null) {
			return;
		}
		
		Schema schema = di.getSchema();
		
		List<String> addfids = new ArrayList<String>();
		List<FeatureValue> addvalues = new ArrayList<FeatureValue>();
		
		for(String rawvalue:values) {
			String fid = rawvalue.split(FEATURENAME_DELIMITER)[0];
			
			// Don't load feature value if not in list
			if(loadfids!=null && !loadfids.contains(fid)) {
				continue;
			}
			
			String value = rawvalue.substring(fid.length()+1);
			Feature f = schema.getFeature(fid);
			FeatureValue fvalue = parseFeatureValue(f, value);
			
			// Don't bother adding a value which matches the closed default
			if(((ExplicitFeature) f).isClosed()
					&& ((ExplicitFeature) f).getClosedDefaultValue().equals(fvalue)) {
				continue;
			}
			
			// Do not add a value for something that's specified as unknown
			if(fvalue != null){
				// A closed feature cannot be defined as unknown
				if(fvalue.equals(FeatureValue.UNKNOWN_VALUE)
						&& ((ExplicitFeature) f).isClosed()) {
					throw new FileFormatException(
							"Closed features cannot be set to unknown: "
							+di+"."+fid);
				}
				
				addfids.add(fid);
				addvalues.add(fvalue);
			}
		}
		
		// Insert all feature values all at once
		di.setFeatureValues(addfids, addvalues);
	}
	
	protected void writeValues(BufferedWriter out, List<String> fids,
			Decorable di, boolean savederived) throws Exception {
		Schema schema = di.getSchema();
		for(String fid:fids) {
			Feature f = schema.getFeature(fid);
			
			if(f instanceof CompositeFeature) {
				CompositeFeature cf = (CompositeFeature) f;
				UnmodifiableList<SimplePair<String, CVFeature>> cfpairs = cf.getFeatures();
				FeatureValue cffv = di.getFeatureValue(fid);
				
				// Get composite values, if known
				List<FeatureValue> cfvalues = null;
				if(cffv instanceof CompositeValue) {
					CompositeValue cvfv = (CompositeValue) cffv;
					cfvalues = cvfv.getFeatureValues().copyAsList();
					
					if(cfvalues.size() != cfpairs.size()) {
						throw new InvalidStateException("Number of composite features does not match "+
								"number of composite values returned: " +
								"#features="+cfvalues.size()+" #values="+cfvalues.size());
					}
				}
				
				// Write out the values of the individual features
				for(int i=0; i<cfpairs.size(); i++) {
					String cffid = cfpairs.get(i).getFirst();
					FeatureValue fv = null;
					if(cfvalues==null) {
						// Encountered unknown value for the composite feature
						fv = FeatureValue.UNKNOWN_VALUE;
					} else {
						// Get corresponding composite value
						fv = cfvalues.get(i);
					}
					
					if(fv instanceof UnknownValue
							|| (!savederived && f instanceof DerivedFeature)) {
						// Don't write unknown values, default values, or derived features
					} else if(f instanceof ExplicitFeature
							&& ((ExplicitFeature) f).isClosed()
							&& ((ExplicitFeature) f).getClosedDefaultValue().equals(fv)) {
						// Don't write values to features match the closed default
					} else {
						out.write(DELIMITER);
						
						// Write the name prior to each value
						out.write(this.getNewCFFID(fid, cffid)+FEATURENAME_DELIMITER);
						// Save the value, making sure to replace
						// any tabs with a space.
						out.write(fv.getStringValue().replaceAll("[\\t\\n\\r]+", " "));
						
						// Print probability, if applicable
						if(fv instanceof CategValue){
							out.write(PROB_DELIMITER+ArrayUtils.array2String(((CategValue) fv).getProbs(),","));
						} else if(fv instanceof MultiCategValue){
							out.write(PROB_DELIMITER+ArrayUtils.array2String(((MultiCategValue) fv).getProbs(),","));
						}
					}
				}
			} else {
				FeatureValue fv = di.getFeatureValue(fid);
				if(fv instanceof UnknownValue
						|| (!savederived && f instanceof DerivedFeature)) {
					// Don't wring unknown values, default values, or derived features
				} else if(f instanceof ExplicitFeature
						&& ((ExplicitFeature) f).isClosed()
						&& ((ExplicitFeature) f).getClosedDefaultValue().equals(fv)) {
					// Don't write values to features match the closed default
				} else {
					out.write(DELIMITER);
					
					// Write the name prior to each value
					out.write(fid+FEATURENAME_DELIMITER);
					// Save the value, making sure to replace
					// any tabs with a space.
					out.write(fv.getStringValue().replaceAll("[\\t\\n\\r]+", " "));
					
					// Print probability, if applicable
					if(fv instanceof CategValue && ((CategValue) fv).getProbs()!=null){
						// Do not print probability if the probability is 1
						double[] probs = ((CategValue) fv).getProbs();
						boolean catprob1 = false;
						double sum = 0;
						for(double prob:probs) {
							if(prob==1) {
								catprob1 = true;
							}
							
							sum+=prob;
						}
						
						// We are currently not enforcing that the probability
						// has to add up to 1.  This ensures that if the probability
						// is being used in a different way, the probabilities are saved.
						if(!catprob1 || sum>1) {
							out.write(PROB_DELIMITER+ArrayUtils.array2String(probs,","));
						}
					} else if(fv instanceof MultiCategValue && ((MultiCategValue) fv).getProbs()!=null){
						out.write(PROB_DELIMITER+ArrayUtils.array2String(((MultiCategValue) fv).getProbs(),","));
					}
				}
			}
		}
	}
}
