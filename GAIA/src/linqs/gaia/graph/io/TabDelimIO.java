package linqs.gaia.graph.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.FileFormatException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitMultiCateg;
import linqs.gaia.feature.explicit.ExplicitMultiID;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.feature.values.UnknownValue;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.ListUtils;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * Tab delimited input format consisting of files in the form of:
 * <p>
 * <code>
 * [GRAPH|NODE|DIRECTED|UNDIRECTED]\t&lt;schemaid&gt;<br>
 * &lt;schemaline&gt;<br>
 * &lt;&lt;instanceline\n&gt;*&gt;<br>
 * </code>
 * <p>
 * where
 * <p>
 * <code>
 * &lt;schemaline&gt; := &lt;feature declaration&gt;\t&lt;&lt;feature declaration&gt;+&gt;<br>
 * &lt;schemaline&gt; := NO_FEATURES<br>
 * &lt;feature declaration&gt; := 
 * [string,numeric,cat=&lt;categories&gt;,multicat=&lt;categories&gt;,multiid=&lt;ids&gt;]:&lt;featureid&gt;[:&lt;defaultvalue&gt;]<br>
 * NO_FEATURES means this schema has no features defined for it<br>
 * &lt;categories&gt; is a comma delimited list of string categories<br>
 * &lt;ids&gt; is a comma delimited list of ID toString values<br>
 * &lt;defaultvalue&gt; is an optional setting for each feature<br>
 * </code>
 * <p>
 * For GRAPH and NODE, the instance lines are of the form:
 * <p>
 * <code>
 * &lt;instanceline&gt; := &lt;objectid&gt;\t&lt;featurevalue&gt;+<br>
 * &lt;featurevalue&gt; := &lt;value&gt[:P=&lt;probability&gt;]|?<br>
 * &lt;probability&gt; is a comma delimited list of probabilities.  See cat and multicat below for details.
 * </code>
 * <p>
 * Edge files have a different form whether or not it is a DIRECTED or UNDIRECTED edge.
 * An UNDIRECTED edge files need an additional term to include all the nodes it is adjacent to
 * in the form:
 * <p>
 * <code>
 * &lt;instanceline&gt; := &lt;objectid&gt;\t&lt;nodeid&gt;+\t|\t&lt;featurevalue&gt;<br>
 * nodeid := &lt;schemaid&gt;:&lt;objectid&gt;
 * </code>
 * <p>
 * An DIRECTED edge files need two additional terms to include all the nodes it is adjacent to
 * (the first set being the source nodes and the second set being the target nodes)
 * in the form:
 * <p>
 * <code>
 * &lt;instanceline&gt; := &lt;objectid&gt;\t&lt;nodeid&gt;+\t|\t&lt;nodeid&gt;+\t|\t&lt;featurevalue&gt;<br>
 * nodeid := &lt;schemaid&gt;:&lt;objectid&gt;
 * </code>
 * <p>
 * To simplify formats for very simple edge types with all nodes belonging to the same schema,
 * immediately before the &lt;schemaline&gt;, you can specify a line of the form:
 * <p>
 * <code>
 * NODESCHEMAID\t&lt;nodeschemaid&gt;
 * </code>
 * <p>
 * where nodeschemaid is the schema id all nodes in this edge file
 * should be assigned.  If this header is set, instances of &lt;nodeid&gt;
 * should only contain &lt;objectid&gt;.
 * <p>
 * The input format consists of multiple files which contain the declaration for
 * the graph, nodes, and edges of the graph being loaded.  The files contain information
 * about the schema of each item, as well as the declaration of the node and edge existence
 * and feature values.
 * <p>
 * There are four supported feature types:
 * <UL>
 * <LI> string-String valued feature
 * <LI> numeric-Numeric valued feature (treated as a Double)
 * <LI> cat-Categorical valued feature.  Similar to string but the values are limited to the specified categories.
 * Categorical features have corresponding probabilities assigned to them.  For a feature with n categories,
 * n numbers can be specified to represent the likelihood of each category.
 * <LI> multicat-Multi Categorical valued features.  Similar to cat but can hold multiple values.  Multi categorical
 * features have corresponding probabilities assigned to them.  If an instance has n categories as values, n
 * numbers can be specified to represent the likelihood of each category.
 * </UL>
 * <p>
 * Notes:
 * <UL>
 * <LI> Sample files available under resource/SampleFiles/TabDelimIOSamples.
 * <LI> All lines which begin with a pound sign (#)
 * and all lines which are empty (including those consisting only of whitespace) are skipped.
 * <LI> You can declare items with the same schema across multiple files
 * (i.e., declare people nodes across multiple files for manageability).  The schema declaration
 * is assumed to match across all those files and exception is thrown if it isn't.
 * <LI> If a node specified in an edge file does not exist in the graph, a warning is printed
 * and that edge is not added.
 * <LI> Composite features are saved by converting each feature within the composite
 * feature as an individual value with the feature id of &lt;fid&gt;-&lt;cf&gt;-&lt;internalfid&gt;.
 * For example, a composite feature for a 'gaussian' distribution might consists
 * of two numeric features, 'mean' and 'variance'.  Those features are saved as two
 * non-composite numeric features with the feature id of 'gaussian-cf-mean'
 * and 'gaussian-cf-variance'.
 * </UL>
 * 
 * Required Parameters:
 * <UL>
 * <LI> For loading:
 *      <UL>
 *      <LI> files-Comma delimited list of the files to use.
 *      Files must be listed in order Graph file, Node files and Edge files.
 *      This parameter is used over filedirectory if both are specified.
 *      Not required if filedir is specified.
 *      <LI> filedirectory-Directory of files to load.  The input will try to load
 *      all files in the directory and will throw a warning for files it cannot load.
 *      Not required if files is specified or if using {@link DirectoryBasedIO} methods.
 * </UL>
 * <LI> For saving:
 *      <UL>
 *      <LI> filedirectory-Directory to store all the resulting files
 *      </UL>
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> For loading:
 * 		<UL>
 * 		<LI> graphclass-Full java class for the graph,
 * 		instantiated using {@link Dynamic#forConfigurableName}.
 *      Default is {@link linqs.gaia.graph.datagraph.DataGraph}.
 * 		<LI> loadfids-Comma delimited list of feature ids.  If set,
 * 		load only the feature values for the specified feature ids.
 * 		This will save both time and memory for loading graphs
 * 		with large numbers of features.
 * 		<LI> fileprefix-Prefix the files must have to be loaded, specifically
 * 		when using the filedirectory option.  Default is to load all specified
 *		files.
 *		<LI> graphobjid-If specifided, the following object id will
 * 		used in place of the graphs object ID when loading.
 * 		This ID is ignored when loading with a specified graph object id.
 * 		</UL>
 * <LI> For saving:
 * 		<UL>
 * 		<LI> fileprefix-Prefix to use in naming the resulting files.  Default
 *      is to use the object id of the graph.
 * 		<LI> savesids-Comma delimited list of feature schema IDs.  If specified,
 * 		during saving, only the graph items with the specified
 * 		schema ID will be saved
 * 		<LI> savederived-If yes, save the values of derived features.  If no,
 * 		do not save the value of derived features.  Default is no.
 * 		<LI> graphobjid-If specifided, the following object id will
 * 		used in place of the graphs object ID when saving.
 * 		</UL>
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 * @author srhuang
 */
public class TabDelimIO extends BaseConfigurable implements IO,DirectoryBasedIO {
	public final static String COMMENT_LINE="#";
	public final static String NODESCHEMAID_LINE="NODESCHEMAID";
	public final static String NO_FEATURES="NO_FEATURES";
	public final static String ID="ID";
	public final static String UNKNOWNVALUE = "?";
	public final static String PROB_DELIMITER = ":P=";
	public final static String DELIMITER = "\t";
	public final static String EDGE_DELIMITER = "|";
	public final static String NODE_ID_DELIMITER = ":";
	public final static String FEATURE_DELIMITER = ":";
	public final static String CATEG_DELIMITER = "=";
	private final static String DEFAULT_GRAPH_CLASS = DataGraph.class.getCanonicalName();

	/**
	 * Supported Types
	 * 
	 * @author namatag
	 */
	private enum SupportedTypes {
		string,
		numeric,
		cat,
		multicat,
		multiid
	}
	
	public Graph loadGraph() {
		String graphobjid = null;
		if(this.hasParameter("graphobjid")) {
			graphobjid = this.getStringParameter("graphobjid");
		}
		
		return internalLoadGraph(null, graphobjid);
	}
	
	public Graph loadGraph(String objid) {
		if(objid==null) {
			throw new InvalidStateException("Graph object ID cannot be null");
		}
		
		return internalLoadGraph(null, objid);
	}
	
	public Graph loadGraphFromDir(String directory) {
		return  internalLoadGraph(directory, null);
	}
	
	public Graph loadGraphFromDir(String directory, String objid) {
		if(objid==null) {
			throw new InvalidStateException("Graph object ID cannot be null");
		}
		
		return internalLoadGraph(directory, objid);
	}
	
	private Graph internalLoadGraph(String filedirectory, String objid) {
		String prefix = null;
		if(this.hasParameter("fileprefix")) {
			prefix = this.getStringParameter("fileprefix");
		}
		
		String filenames[] = null;
		// Get the appropriate files
		if(filedirectory!=null) {
			filenames = this.getFilesFromDir(filedirectory, prefix);
		} else if(this.hasParameter("files")) {
			filenames = this.getStringParameter("files").split(",");
		} else if(this.hasParameter("filedirectory")) {
			filenames = this.getFilesFromDir(this.getStringParameter("filedirectory"), prefix);
		} else {
			throw new ConfigurationException("Neither files, nor filedirectory was specified");
		}
		
		// Load the values only for the specified features
		Set<String> loadfids = null;
		if(this.hasParameter("loadfids")) {
			loadfids = new HashSet<String>();
			loadfids.addAll(Arrays.asList(this.getStringParameter("loadfids").split(",")));
		}
	
		if(filenames == null || filenames.length < 1)
			throw new IllegalArgumentException("Must specify at least one file to load data from");
		Log.DEBUG("Number of files given: " + filenames.length);

		// The first file must be the graph features file
		Graph g = this.handle_graph_file(filenames[0], objid, loadfids);
		
		for(int q = 1; q < filenames.length; q++) {
			handle_graphitem_file(filenames[q], g, loadfids);
		}
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Graph Loaded: "+GraphUtils.getSimpleGraphOverview(g));
		}

		return g;
	}

	private String[] getFilesFromDir(String dirpath, String prefix) {
		File dir = new File(dirpath);

		// Skip files that start with `.'.
		FileFilter filefilter = new FileFilter() {
			public boolean accept(File file) {
				return !file.isDirectory() && !file.isHidden();
			}
		};

		File[] children = dir.listFiles(filefilter);
		if (children == null) {
			throw new ConfigurationException("Either dir does not exist or is not a directory: "+dirpath);
		}

		Set<String> graphfiles = new HashSet<String>();
		Set<String> nodefiles = new HashSet<String>();
		Set<String> edgefiles = new HashSet<String>();

		for (int i=0; i<children.length; i++) {
			try {
				// Get filename of file or directory
				File file = children[i];
				
				// Do not include files not matching the prefix
				String filename = file.getName();
				if(prefix != null && !filename.startsWith(prefix)) {
					Log.DEBUG("Prefix '"+prefix+"' not matched. " +
							"Skipping file: "+filename);
					continue;
				}

				// Check to see what type of file it is
				BufferedReader in = new BufferedReader(new FileReader(file));
				String firstline = getNonCommentLine(in);
				if(firstline==null) {
					Log.WARN("Skipping: "+file.getAbsolutePath());
				} else if(firstline.startsWith("DIRECTED") || firstline.startsWith("UNDIRECTED")) {
					edgefiles.add(file.getAbsolutePath());
				} else if(firstline.startsWith("NODE")) {
					nodefiles.add(file.getAbsolutePath());
				} else if(firstline.startsWith("GRAPH")) {
					graphfiles.add(file.getAbsolutePath());
				} else {
					Log.WARN("Skipping: "+file.getAbsolutePath());
				}
			} catch (Exception e) {
				Log.WARN("Skipping: "+children[i].getAbsolutePath());
			}    
		}
		
		if(graphfiles.size()!=1) {
			throw new ConfigurationException("Expected only one graph file: "+graphfiles);
		}

		// Add in order: graph files, node files, edge files
		List<String> filelist = new ArrayList<String>();
		filelist.addAll(graphfiles);
		filelist.addAll(nodefiles);
		filelist.addAll(edgefiles);
		
		return filelist.toArray(new String[filelist.size()]);
	}

	/**
	 * Return next non comment line.
	 * 
	 * @param br
	 * @return
	 * @throws Exception
	 */
	private String getNonCommentLine(BufferedReader br) throws Exception {
		String line = br.readLine();

		while(line != null && (line.startsWith(COMMENT_LINE) || line.trim().length()==0)){
			line = br.readLine();
		}

		return line;
	}

	/**
	 * Handle graph item file.
	 * 
	 * @param filename
	 * @param g
	 */
	private void handle_graphitem_file(String filename, Graph g, Set<String> loadfids) {
		Log.DEBUG("Handling graph item file: " + filename);
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String firstline = this.getNonCommentLine(br);
			String secondline = this.getNonCommentLine(br);

			String[] params = firstline.split("\t");
			if(params[0].equals(SchemaType.DIRECTED.name()) ) {
				String nodeschemaid = null;
				if(secondline.startsWith(NODESCHEMAID_LINE)) {
					nodeschemaid = secondline.split(DELIMITER)[1];
					secondline = this.getNonCommentLine(br);
				}

				handle_schema_line(g, SchemaType.DIRECTED, params[1], secondline);
				handle_directededge_file(br, g, params[1], nodeschemaid, loadfids, filename);
			} else if(params[0].equals(SchemaType.UNDIRECTED.name()) ) {
				String nodeschemaid = null;
				if(secondline.startsWith(NODESCHEMAID_LINE)) {
					nodeschemaid = secondline.split(DELIMITER)[1];
					secondline = this.getNonCommentLine(br);
				}

				handle_schema_line(g, SchemaType.UNDIRECTED, params[1], secondline);
				handle_undirectededge_file(br, g, params[1], nodeschemaid, loadfids, filename);
			} else if(params[0].equals(SchemaType.NODE.name())) {
				handle_schema_line(g, SchemaType.NODE, params[1], secondline);
				handle_node_file(br, g, params[1], loadfids, filename);
			} else {
				throw new FileFormatException("Problem parsing file " + filename 
						+ ": Header \""+ firstline+"\"");
			}
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Parse the schema line and add the graph.
	 * 
	 * @param g
	 * @param type
	 * @param schemaID
	 * @param schemaline
	 */
	private void handle_schema_line(Graph g, SchemaType type, 
			String schemaID, String schemaline){

		// If the schema is already defined, assume thats the one we're using.
		// Note:  Handle graph schema differently since its automatically generated.
		if(g.hasSchema(schemaID) && !type.equals(SchemaType.GRAPH)){
			Log.DEBUG("Schema already defined: "+schemaID);
			return;
		}

		// Parse schema 
		Schema schema = new Schema(type);
		if(!schemaline.equals(NO_FEATURES)){
			String[] parts = schemaline.split(DELIMITER);
			for(String part:parts){
				// Format is <type>:<name>:<default>
				String[] fpart = part.split(FEATURE_DELIMITER);
				String fid = fpart[1];
				String ftype = fpart[0];
				String categoryvalue = null;
				
				// Adjust to support features which require a declaration
				// of valid values (i.e., valid categories)
				if(ftype.contains(CATEG_DELIMITER)) {
					String[] ftypeparts = ftype.split(CATEG_DELIMITER);
					
					if(ftypeparts.length!=2) {
						throw new FileFormatException("Invalid category declaration: "+ftype);
					}
					
					ftype = ftypeparts[0];
					if(!ftype.equals(SupportedTypes.cat.name()) & !ftype.equals(SupportedTypes.multicat.name())) {
						throw new FileFormatException("Feature type cannot have categories: "+ftype);
					}
					
					categoryvalue = ftypeparts[1];
				}
				
				Feature newfeature = null;
				String closeddefault = null;

				if(fpart.length==3){
					closeddefault = fpart[2];
				}

				if(ftype.equals(SupportedTypes.numeric.name())){
					if(closeddefault==null) {
						newfeature = new ExplicitNum();
					} else {
						newfeature = new ExplicitNum(
								this.parseFeatureValue(new ExplicitNum(), closeddefault));
					}
				} else if(ftype.equals(SupportedTypes.string.name())){
					if(closeddefault==null) {
						newfeature = new ExplicitString();
					} else {
						newfeature = new ExplicitString(
								this.parseFeatureValue(new ExplicitString(), closeddefault));
					}
				} else if(ftype.equals(SupportedTypes.cat.name())){
					List<String> categories = Arrays.asList(categoryvalue.split(","));

					if(closeddefault==null) {
						newfeature = new ExplicitCateg(categories);
					} else {
						newfeature = new ExplicitCateg(categories,
								this.parseFeatureValue(new ExplicitCateg(categories), closeddefault));
					}
				} else if(ftype.equals(SupportedTypes.multicat.name())){
					List<String> categories = Arrays.asList(categoryvalue.split(","));

					if(closeddefault==null) {
						newfeature = new ExplicitMultiCateg(categories);
					} else {
						newfeature = new ExplicitMultiCateg(categories,
								this.parseFeatureValue(new ExplicitMultiCateg(categories), closeddefault));
					}
				} else if(ftype.equals(SupportedTypes.multiid.name())){
					if(closeddefault==null) {
						newfeature = new ExplicitMultiID();
					} else {
						newfeature = new ExplicitMultiID(
								this.parseFeatureValue(new ExplicitMultiID(), closeddefault));
					}
				} else {
					throw new FileFormatException("Unsupported feature type: "+ftype);
				}

				schema.addFeature(fid, newfeature);
			}
		}
		
		if(type.equals(SchemaType.GRAPH)) {
			g.updateSchema(schemaID, schema);
		} else {
			g.addSchema(schemaID, schema);
		}
	}

	/**
	 * Parse the probabilities from the value.  If not specified,
	 * set the value matching the category to 1 and all other 0.
	 * 
	 * @param value
	 * @param categories
	 * @return
	 */
	private SimplePair<String,double[]> parseProb(String value, Iterator<String> categories) {
		String[] valueparts = value.split(PROB_DELIMITER);
		List<Double> probs = new LinkedList<Double>();

		if(valueparts.length==2){
			String[] probvals = valueparts[1].split(",");
			for(int i=0; i<probvals.length; i++){
				probs.add(Double.parseDouble(probvals[i]));
			}
		} else {
			while(categories.hasNext()){
				if(categories.next().equals(valueparts[0])) {
					probs.add(new Double(1));
				} else {
					probs.add(new Double(0));
				}
			}
		}

		return new SimplePair<String,double[]>(valueparts[0], ListUtils.doubleList2array(probs));
	}

	/**
	 * Process the feature values for the given decorable item
	 */
	protected void addFeatureValues(Decorable di, String[] values, Set<String> loadfids){
		Schema schema = di.getSchema();
		Iterator<String> itr = schema.getFeatureIDs();
		
		List<String> addfids = new ArrayList<String>();
		List<FeatureValue> addvalues = new ArrayList<FeatureValue>();
		int counter = 0;
		while(itr.hasNext()){
			String fid = itr.next();
			
			// Don't load feature value if not in list
			if(loadfids!=null && !loadfids.contains(fid)) {
				counter++;
				continue;
			}
			
			Feature f = schema.getFeature(fid);
			String value = values[counter];

			FeatureValue fvalue = parseFeatureValue(f, value);
			
			// Don't bother adding a value which matches the closed default
			if(((ExplicitFeature) f).isClosed()
					&& ((ExplicitFeature) f).getClosedDefaultValue().equals(fvalue)) {
				counter++;
				continue;
			}
			
			// Do not add a value for something that's specified as unknown
			if(fvalue != null){
				// A closed feature cannot be defined as unknown
				if(fvalue.equals(FeatureValue.UNKNOWN_VALUE) && ((ExplicitFeature) f).isClosed()) {
					throw new FileFormatException(
							"Closed features cannot be set to unknown: "
							+di+"."+fid);
				}
				
				addfids.add(fid);
				addvalues.add(fvalue);
			}

			counter++;
		}
		
		// Insert all feature values all at once
		di.setFeatureValues(addfids, addvalues);
	}

	/**
	 * Parse the value from a given value entry
	 * 
	 * Note: These are placed in a common place since
	 * its used for both getting the closed default value,
	 * as well as parsing the individual values.
	 * 
	 * @param f Feature to parse for
	 * @param value String representation to parse
	 * @return Feature value to return
	 */
	protected FeatureValue parseFeatureValue(Feature f, String value) {
		FeatureValue fvalue = null;

		if(value.equals(UNKNOWNVALUE)){
			fvalue = null;
		} else if(f instanceof NumFeature){
			Double val = Double.parseDouble(value);
			fvalue = new NumValue(val);
		} else if(f instanceof StringFeature){
			fvalue = new StringValue(value);
		} else if(f instanceof CategFeature){
			SimplePair<String,double[]> probpair =
				this.parseProb(value, ((CategFeature) f).getAllCategories().iterator());
			fvalue = new CategValue(probpair.getFirst(), probpair.getSecond());
		} else if(f instanceof MultiCategFeature){
			SimplePair<String,double[]> probpair =
				this.parseProb(value, ((MultiCategFeature) f).getAllCategories().iterator());
			Set<String> catset = new HashSet<String>(Arrays.asList(probpair.getFirst().split(",")));
			fvalue = new MultiCategValue(catset, probpair.getSecond());
		} else if(f instanceof MultiIDFeature){
			Set<ID> ids = new HashSet<ID>();
			String[] parts = value.split(",");
			for(String p:parts) {
				ids.add(linqs.gaia.identifiable.ID.parseID(p));
			}
			
			fvalue = new MultiIDValue(ids);
		} else {
			throw new FileFormatException("Unsupported Feature Type: "
					+f.getClass().getCanonicalName());
		}

		return fvalue;
	}

	/**
	 * Get node with the given id to parse.
	 * A null is returned if the node cannot be found
	 * 
	 * @param g Graph
	 * @param nodeschemaID Default schema ID for nodes.  If null, parse nodeid for schema ID.
	 * @param nodeid String representation of node id
	 * @return Node
	 */
	private Node getNode(Graph g, String nodeschemaID, String nodeid){
		GraphItemID nid = null;
		String schemaid = null;
		String objid = null;
		if(nodeschemaID==null) {
			String[] idparts = nodeid.split(NODE_ID_DELIMITER);
			if(idparts.length<2) {
				return null;
			}
			
			schemaid = idparts[0];
			objid = nodeid.substring(schemaid.length()+1);
		} else {
			schemaid = nodeschemaID;
			objid = nodeid;
		}
		
		nid = new GraphItemID((GraphID) g.getID(), schemaid, objid);
		Node n = g.getNode(nid);
		if(n == null) {
			Log.WARN("Node given by string does not exist:" +
					" schemaid="+schemaid+" objid="+objid);
		}
		
		return n;
	}

	/**
	 * Private method to handle the insertion of all edges from a given
	 * filename into graph
	 *
	 */
	private void handle_directededge_file(BufferedReader br, Graph g, String schemaID,
			String nodeschemaid, Set<String> loadfids, String filename) throws Exception{
		String currline = null;
		try {
			currline = this.getNonCommentLine(br);

			int count = 0;
			while(currline!=null){
				if(count % 1000 == 0) {
					Log.DEBUG("Processing non-comment line "+count+" in "+filename);
				}
				count++;
				
				Set<Node> sources = new HashSet<Node>();
				Set<Node> targets = new HashSet<Node>();

				// Each line corresponds to 1 edge and that edge's nodes and features
				String[] lineparts = currline.split(DELIMITER);
				String objid = lineparts[0];

				// Gather Nodes
				boolean skipedge = false;
				int index = 1;
				boolean parsesource = true;
				while(index<lineparts.length){
					if(lineparts[index].equals(EDGE_DELIMITER)){
						if(parsesource){
							parsesource = false;
							index++;
							continue;
						} else {
							break;
						}
					}

					// Add nodes to edge
					Node n = this.getNode(g, nodeschemaid, lineparts[index]);
					if(n == null) {
						// Skip adding edge if one of the edge nodes does not exist
						skipedge = true;
						break;
					} else if(parsesource){
						sources.add(n);
					} else {
						targets.add(n);
					}

					index++;
				}

				// Add edge only if all nodes are valid
				if(!skipedge) {
					// Create Edge
					GraphItemID gid = new GraphItemID((GraphID) g.getID(), schemaID, objid);
					DirectedEdge e = g.addDirectedEdge(gid, sources.iterator(), targets.iterator());
					this.addFeatureValues(e, (String[]) ArrayUtils.subarray(lineparts, index+1), loadfids);
				}

				currline = this.getNonCommentLine(br);
			}
		} catch (Exception e) {
			Log.WARN("Exception thrown processing line: "+currline);
			throw e;
		}
	}

	/**
	 * Private method to handle the insertion of all edges from a given
	 * filename into graph
	 *
	 */
	private void handle_undirectededge_file(BufferedReader br, Graph g,
			String schemaID, String nodeschemaid,
			Set<String> loadfids, String filename) throws Exception {
		String currline = null;
		
		int count = 0;
		try {
			currline = this.getNonCommentLine(br);
			while(currline!=null){
				if(count % 1000 == 0) {
					Log.DEBUG("Processing non-comment line "+count+" in "+filename);
				}
				count++;
				
				Set<Node> nodes = new HashSet<Node>();

				// Each line corresponds to 1 edge and that edge's nodes and features
				String[] lineparts = currline.split(DELIMITER);
				String objid = lineparts[0];

				// Gather Nodes
				boolean skipedge = false;
				int index = 1;
				while(index<lineparts.length){
					if(lineparts[index].equals(EDGE_DELIMITER)){
						break;
					}

					// Add nodes to edge
					Node n = this.getNode(g, nodeschemaid, lineparts[index]);
					if(n == null) {
						skipedge = true;
						break;
					} else {
						if(nodes.contains(n)) {
							Log.WARN("Adding the same node twice in undirected edge: "
									+lineparts[index]
									           +" in edge "+objid
									           +" of schema id "+schemaID);
						}

						nodes.add(n);
					}

					index++;
				}

				// Add edge only if all nodes are valid
				if(!skipedge) {
					// Create Edge
					GraphItemID gid = new GraphItemID((GraphID) g.getID(), schemaID, objid);
					UndirectedEdge e = g.addUndirectedEdge(gid, nodes.iterator());
					this.addFeatureValues(e, (String[]) ArrayUtils.subarray(lineparts, index+1), loadfids);
				}

				currline = this.getNonCommentLine(br);
			}
		} catch (Exception e) {
			Log.WARN("Exception thrown processing line: "+currline);
			throw e;
		}
	}

	/**
	 * Private method to handle the insertion of all nodes from a given
	 * filename into graph
	 *
	 */
	private void handle_node_file(BufferedReader br, Graph g, String schemaID,
			Set<String> loadfids, String filename) throws Exception {
		String currline = null;
		int count = 0;
		try {
			currline = this.getNonCommentLine(br);
			while(currline!=null){
				if(count % 1000 == 0) {
					Log.DEBUG("Processing non-comment line "+count+" in "+filename);
				}
				count++;
				
				String nodeline[] = currline.split(DELIMITER);
				String nodeid = nodeline[0];
				GraphItemID gid = new GraphItemID((GraphID) g.getID(), schemaID, nodeid);
				Node node = g.addNode(gid);

				// Add feature
				this.addFeatureValues(node, (String[]) ArrayUtils.subarray(nodeline, 1), loadfids);

				currline = this.getNonCommentLine(br);
			}
		} catch (Exception e) {
			Log.WARN("Exception thrown processing line: "+currline);
			throw e;
		}
	}

	/**
	 * Private method to handle the insertion of graph features into graph
	 *
	 */
	private Graph handle_graph_file(String filename, String objid, Set<String> loadfids) {
		Log.DEBUG("Handling graph file: " + filename);

		Graph g = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String firstline = this.getNonCommentLine(br);
			String secondline = this.getNonCommentLine(br);

			// Verify this is a graph file
			String[] params = firstline.split("\t");
			if(!params[0].equals(SchemaType.GRAPH.name())) {
				throw new FileFormatException("First file should ge a graph file.  File " + filename 
						+ "has header: \""+ firstline+"\"");
			}

			// A graph file should only have one line for features
			String features = this.getNonCommentLine(br);
			String valparts[] = features.split(DELIMITER);

			// Create Graph
			String schemaID = params[1];
			String objID = objid==null ? valparts[0] : objid;
			GraphID id = new GraphID(schemaID, objID);
			Class<?>[] argsClass = new Class[]{GraphID.class};
			Object[] argValues = new Object[]{id};

			String graphclass = TabDelimIO.DEFAULT_GRAPH_CLASS;
			if(this.hasParameter("graphclass")){
				graphclass = this.getStringParameter("graphclass");
			}

			g = (Graph) Dynamic.forName(Graph.class,
					graphclass,
					argsClass,
					argValues);
			
			g.copyParameters(this);

			// Create schema for graph
			handle_schema_line(g, SchemaType.GRAPH, params[1], secondline);

			this.addFeatureValues(g, (String[]) ArrayUtils.subarray(valparts, 1), loadfids);

		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}

		return g;
	}
	
	public void saveGraph(Graph g) {
		String dirpath = this.getStringParameter("filedirectory");
		saveGraphToDir(dirpath, g);
	}
	
	public void saveGraphToDir(String dirpath, Graph g) {
		try {
			String prefix = g.getID().getObjID();
			if(this.hasParameter("fileprefix")) {
				prefix = this.getStringParameter("fileprefix");
			}
			
			boolean savederived = false;
			if(this.hasParameter("savederived")) {
				savederived = this.getYesNoParameter("savederived");
			}

			// Create directory if it doesn't already exist
			FileIO.createDirectories(dirpath);

			// Save Graph File
			this.saveGraph(g, dirpath, prefix, savederived);

			// Save Node and Edge Files
			this.saveGraphItems(g, dirpath, prefix, savederived);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<String> writeSchemaLine(BufferedWriter out, Schema schema, boolean savederived)
	throws Exception {
		List<String> fids = new LinkedList<String>();
		Iterator<String> itr = schema.getFeatureIDs();
		boolean first = true;
		while(itr.hasNext()){

			// Print feature declaration
			String fid = itr.next();
			Feature f = schema.getFeature(fid);
			
			// Handle whether or not to save derived features
			if(!savederived && f instanceof DerivedFeature) {
				Log.WARN("Not saving derived feature "+fid+" of type: "+f.getClass().getCanonicalName());
				continue;
			}
			
			StringBuffer buf = new StringBuffer();
			if(f instanceof CompositeFeature) {
				CompositeFeature cf = (CompositeFeature) f;
				UnmodifiableList<SimplePair<String, CVFeature>> cffeatures= cf.getFeatures();
				for(SimplePair<String,CVFeature> cfpair:cffeatures) {
					// Write delimiter, if needed
					if(!first){
						buf.append(DELIMITER);
					} else {
						first = false;
					}
					
					// Handle composite features by saving values therein as
					// individual features with feature name <fid>-cf-<internalfid>
					buf.append(this.getFeatureStringRepresentation(cfpair.getSecond(),
							this.getNewCFFID(fid, cfpair.getFirst())));
				}
			} else {
				// Write delimiter, if needed
				if(!first){
					buf.append(DELIMITER);
				} else {
					first = false;
				}
				
				// Handle non-composite features
				buf.append(this.getFeatureStringRepresentation(f, fid));
			}
			
			// Write string representation
			out.write(buf.toString());
			
			// Do not support explicit, composite features
			if(f instanceof ExplicitFeature && f instanceof CompositeFeature) {
				throw new UnsupportedTypeException("Cannot save explicit, composite features: "
						+fid+" of type "+f.getClass().getCanonicalName());
			}
			
			if(f instanceof ExplicitFeature) {
				ExplicitFeature ef = (ExplicitFeature) f;

				if(ef.isClosed()){
					out.write(FEATURE_DELIMITER+ef.getClosedDefaultValue().getStringValue());
				}
			}

			fids.add(fid);
		}

		if(fids.isEmpty()) {
			out.write(NO_FEATURES);
		}

		out.write("\n");

		return fids;
	}
	
	protected String getNewCFFID(String fid, String internalfid) {
		return fid+"-cf-"+internalfid;
	}
	
	private String getFeatureStringRepresentation(Feature f, String fid) {
		String featurestring = "";
		if(f instanceof NumFeature) {
			featurestring += SupportedTypes.numeric+FEATURE_DELIMITER+fid;
		} else if(f instanceof StringFeature) {
			featurestring += SupportedTypes.string+FEATURE_DELIMITER+fid;
		} else if(f instanceof CategFeature) {
			String categories =
				IteratorUtils.iterator2string(
						((CategFeature) f).getAllCategories().iterator(), ",");
			featurestring += SupportedTypes.cat+"="+categories+FEATURE_DELIMITER+fid;
		} else if(f instanceof MultiCategFeature) {
			String categories =
				IteratorUtils.iterator2string(
						((MultiCategFeature) f).getAllCategories().iterator(), ",");
			featurestring += SupportedTypes.multicat+"="+categories+FEATURE_DELIMITER+fid;
		} else if(f instanceof MultiIDFeature) {
			featurestring += SupportedTypes.multiid+FEATURE_DELIMITER+fid;
		} else if(f instanceof CompositeFeature) {
			featurestring += SupportedTypes.multiid+FEATURE_DELIMITER+fid;
		} else {
			throw new UnsupportedTypeException("Unable to save feature "+fid+" of type: "+f.getClass().getCanonicalName());
		}
		
		return featurestring;
	}

	protected void writeValues(BufferedWriter out, List<String> fids,
			Decorable di, boolean savederived) throws Exception {
		Schema schema = di.getSchema();
		for(String fid:fids) {
			out.write(DELIMITER);
			Feature f = schema.getFeature(fid);
			
			if(f instanceof CompositeFeature) {
				CompositeFeature cf = (CompositeFeature) f;
				UnmodifiableList<SimplePair<String, CVFeature>> cfpairs = cf.getFeatures();
				FeatureValue cffv = di.getFeatureValue(fid);
				
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
					FeatureValue fv = null;
					if(cfvalues==null) {
						fv = FeatureValue.UNKNOWN_VALUE;
					} else {
						fv = cfvalues.get(i);
					}
					
					// Do not write at 0 since a tab exists for the first value
					if(i!=0) {
						out.write(DELIMITER);
					}
					
					if(fv instanceof UnknownValue || (!savederived && f instanceof DerivedFeature)) {
						out.write(UNKNOWNVALUE);
					} else {
						out.write(fv.getStringValue().replaceAll("[\\t\\n\\r]+", " "));
					}
		
					// Print probability, if applicable
					if(fv instanceof CategValue){
						out.write(PROB_DELIMITER+ArrayUtils.array2String(((CategValue) fv).getProbs(),","));
					} else if(fv instanceof MultiCategValue){
						out.write(PROB_DELIMITER+ArrayUtils.array2String(((MultiCategValue) fv).getProbs(),","));
					}
				}
			} else {
				FeatureValue fv = di.getFeatureValue(fid);
				if(fv instanceof UnknownValue || (!savederived && f instanceof DerivedFeature)) {
					out.write(UNKNOWNVALUE);
				} else {
					out.write(fv.getStringValue().replaceAll("[\\t\\n\\r]+", " "));
				}
	
				// Print probability, if applicable
				if(fv instanceof CategValue){
					out.write(PROB_DELIMITER+ArrayUtils.array2String(((CategValue) fv).getProbs(),","));
				} else if(fv instanceof MultiCategValue){
					out.write(PROB_DELIMITER+ArrayUtils.array2String(((MultiCategValue) fv).getProbs(),","));
				}
			}
		}
	}

	private void saveGraph(Graph g, String dirpath, String prefix, boolean savederived) throws Exception {
		// Create file 
		String schemaID = g.getSchemaID();
		ID id = g.getID();
		String file = dirpath+"/"+prefix+"."+SchemaType.GRAPH +"."+schemaID+".tab";

		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		List<String> fids = this.writeHeader(out, schemaID, g.getSchema(), savederived);
		
		String graphobjid = null;
		if(this.hasParameter("graphobjid")) {
			graphobjid = this.getStringParameter("graphobjid");
		} else {
			graphobjid = id.getObjID();
		}
		out.write(graphobjid);
		
		this.writeValues(out, fids, g, savederived);

		//Close the output stream
		out.close();
	}

	private List<String> writeHeader(BufferedWriter out,
			String schemaID, Schema schema, boolean savederived) throws Exception {
		out.write(schema.getType().name()+DELIMITER+schemaID+"\n");
		return this.writeSchemaLine(out, schema, savederived);
	}

	private void saveGraphItems(Graph g, String dirpath, String prefix, boolean savederived) throws Exception {
		Set<String> savesids = null;
		if(this.hasParameter("savesids")) {
			String[] sids = this.getStringParameter("savesids").split(",");
			savesids = new HashSet<String>(Arrays.asList(sids));
		}
		
		Iterator<String> itr = g.getAllSchemaIDs();
		while(itr.hasNext()){
			String schemaID = itr.next();
			Schema schema = g.getSchema(schemaID);

			// Skip Graph Schema.  We're handling that elsewhere
			if(schema.getType().equals(SchemaType.GRAPH)) {
				continue;
			}
			
			// Handle save sids
			if(savesids != null && !savesids.contains(schemaID)) {
				continue;
			}

			String file = dirpath+"/"+prefix+"."+schema.getType()+"."+schemaID +".tab";
			SchemaType type = schema.getType();
			FileWriter fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);

			List<String> fids = this.writeHeader(out, schemaID, schema, savederived);
			Iterator<GraphItem> gitr = g.getGraphItems(schemaID);
			while(gitr.hasNext()){
				// Write ID
				GraphItem currgi = gitr.next();

				if(type.equals(SchemaType.NODE)){
					out.write(currgi.getID().getObjID());
				} else if(type.equals(SchemaType.DIRECTED)){
					// Print id
					out.write(currgi.getID().getObjID());

					DirectedEdge de = (DirectedEdge) currgi;
					// Print sources
					Iterator<Node> sources = de.getSourceNodes();
					while(sources.hasNext()){
						out.write(DELIMITER);

						ID id = sources.next().getID();
						out.write(id.getSchemaID()+NODE_ID_DELIMITER+id.getObjID());
					}

					out.write(DELIMITER);
					out.write(EDGE_DELIMITER);

					// Print targets
					Iterator<Node> targets = de.getTargetNodes();
					while(targets.hasNext()){
						out.write(DELIMITER);

						ID id = targets.next().getID();
						out.write(id.getSchemaID()+NODE_ID_DELIMITER+id.getObjID());
					}

					if(!fids.isEmpty()) {
						out.write(DELIMITER);
						out.write(EDGE_DELIMITER);
					}

				} else if(type.equals(SchemaType.UNDIRECTED)){
					// Print id
					out.write(currgi.getID().getObjID());

					UndirectedEdge ue = (UndirectedEdge) currgi;
					// Print node
					Iterator<Node> nodes = ue.getAllNodes();
					while(nodes.hasNext()){
						out.write(DELIMITER);

						ID id = nodes.next().getID();
						out.write(id.getSchemaID()+NODE_ID_DELIMITER+id.getObjID());
					}

					if(!fids.isEmpty()) {
						out.write(DELIMITER);
						out.write(EDGE_DELIMITER);
					}
				} else {
					throw new FileFormatException("Unsupported Schema Type:"+type);
				}

				// Write feature value
				this.writeValues(out, fids, currgi, savederived);
				out.write("\n");
			}

			out.close();
		}
	}
}
