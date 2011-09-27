package linqs.gaia.experiment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.transformer.Transformer;
import linqs.gaia.util.Dynamic;

/**
 * Utility functions commonly used in experiment classes
 * 
 * @author namatag
 *
 */
public class ExperimentUtils {
	private static final String COMMENT_LINE = "#";

	/**
	 * Performs feature construction over the given graph using
	 * the actions specified in the feature constructor file.
	 * <p>
	 * The feature construction file is a space/tab delimited file of
	 * the following forms:
	 * <UL>
	 * <LI> ADD_FEATURE &lt;schemaid&gt; &lt;featureid&gt; &lt;simplefeatureclass&gt;[ &lt;configfile&gt;]
	 * <LI> COPY_FEATURE &lt;schemaid&gt; &lt;featureid&gt; &lt;copyfeatureid&gt;
	 * <LI> CACHE_FEATURE &lt;schemaid&gt; &lt;featureid&gt;
	 * <LI> REMOVE_FEATURE &lt;schemaid&gt; &lt;featureid&gt;
	 * <LI> RUN_TRANSFORMER &lt;trasformerclass&gt;[ &lt;configfile&gt;]
	 * <LI> ADD_SCHEMA &lt;schemaid&gt; &lt;schematype&gt;
	 * </UL>
	 * where
	 * <UL>
	 * <LI> ADD_FEATURE adds a {@link DerivedFeature}, of the specified class or name, to the schema.
	 * If a configuration file is specified, the added feature loads its configurations from this file.
	 * <LI> COPY_FEATURE copies the definition and values of the {@link ExplicitFeature},
	 * specified by the schema and first feature id, in the graph
	 * to a feature, in the same schema, with the name &lt;copyfeatureid&gt;.
	 * <LI> CACHE_FEATURE enables caching (set using {@link DerivedFeature#setCache(boolean)})
	 * for the specified {@link DerivedFeature}, specified by the schem and feature id.
	 * <LI> REMOVE_FEATURE removes the feature, specified by the schem and feature id, from the graph
	 * 
	 * <LI> &lt;schemaid&gt; is the the id of the schema to change
	 * <LI> &lt;schematype&gt; is the type of schema (i.e., NODE, DIRECTED, UNDIRECTED)
	 * <LI> &lt;featureid&gt; is the feature being added, removed, or modified
	 * <LI> &lt;simplefeatureclass&gt; is the java class of the a {@link DerivedFeature},
	 * to instantiate using in {@link Dynamic#forConfigurableName}, being added.
	 * <LI> &lt;configfile&gt; is the configuration file with the parameters for
	 * added feature or transformer class to apply
	 * <LI> &lt;transformerclass&gt; is the java class of the {@link Transformer} to run on the graph
	 * to instantiate using in {@link Dynamic#forConfigurableName}
	 * <LI> &lt;schematype&gt; is the type of schema (NODE, DIRECTED, or UNDIRECTED) to add to graph
	 * </UL>
	 * 
	 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
	 * 
	 * @param g Graph to add feature to
	 * @param fcfile Filename of feature construction file
	 * @param c Configurable object from which to copy configurations for the features.
	 * Set to null if no parameters should be copied.
	 */
	public static void loadFeatureConstruction(Graph g, String fcfile, Configurable c) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(fcfile));
			String line = ExperimentUtils.getNonCommentLine(br);
			while(line != null) {
				String[] parts = line.split("\\s+");
				if(parts[0].equals("ADD_FEATURE")) {
					DerivedFeature df = null;
					String schemaid = parts[1];
					String featureid = parts[2];
					String featureclass = parts[3];

					// Instantiate Object
					df = (DerivedFeature)
					Dynamic.forConfigurableName(DerivedFeature.class, featureclass);

					// Load configurations
					if(parts.length==5) {
						// Load coded derived feature
						String configfile = parts[4];
						df.loadParametersFile(configfile);
					} else if(c != null) {
						df.copyParameters(c);
					}

					// Handle graph dependent features
					if(df instanceof GraphDependent) {
						((GraphDependent) df).setGraph(g);
					}

					// Add feature to schema
					Schema schema = g.getSchema(schemaid);
					schema.addFeature(featureid, df);
					g.updateSchema(schemaid, schema);
				} else if(parts[0].equals("REMOVE_FEATURE")) {
					String schemaid = parts[1];
					String featureid = parts[2];

					// Remove feature from schema
					Schema schema = g.getSchema(schemaid);
					schema.removeFeature(featureid);
					g.updateSchema(schemaid, schema);
				} else if(parts[0].equals("CACHE_FEATURE")) {
					String schemaid = parts[1];
					String featureid = parts[2];
					boolean shouldcache = true;
					if(parts.length==4) {
						shouldcache = parts[3].equalsIgnoreCase("yes");
					}

					// Remove feature from schema
					Schema schema = g.getSchema(schemaid);
					Feature f = schema.getFeature(featureid);
					if(!(f instanceof DerivedFeature)) {
						throw new UnsupportedTypeException(
								"Cache feature only supported for derived features: "+
								featureid+" "+f.getClass().getCanonicalName());
					}
					
					((DerivedFeature) f).setCache(shouldcache);
				} else if(parts[0].equals("COPY_FEATURE")) {
					String schemaid = parts[1];
					String featureid = parts[2];
					String copyfeatureid = parts[3];

					Schema schema = g.getSchema(schemaid);
					Feature f = schema.getFeature(featureid).copy();
					Feature copyf = null;

					if(!(f instanceof ExplicitFeature)) {
						throw new ConfigurationException("Can only copy explicit features: "
								+featureid+" is "+f);
					}

					// Add feature if not already defined
					if(!schema.hasFeature(copyfeatureid)) {
						schema.addFeature(copyfeatureid, f);
						g.updateSchema(schemaid, schema);
					}

					// Verify that the copy is the same type as the original
					copyf = schema.getFeature(copyfeatureid);
					if(f.getClass().equals(copyf.getClass())) {
						throw new ConfigurationException("Feature and features copy must be the same: "
								+featureid+" is "+f.getClass().getCanonicalName()
								+copyfeatureid+" is "+copyf.getClass().getCanonicalName());
					}

					// Set values for all the nodes
					Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
					while(gitr.hasNext()) {
						GraphItem gi = gitr.next();
						gi.setFeatureValue(copyfeatureid, gi.getFeatureValue(featureid));
					}
				} else if(parts[0].equals("RUN_TRANSFORMER")) {
					String transformerclass = parts[1];
					
					Transformer transformer = 
						(Transformer) Dynamic.forConfigurableName(Transformer.class, transformerclass);
					if(parts.length==2) {
						String configfile = parts[2];
						transformer.loadParametersFile(configfile);
					} else if(c!=null) {
						transformer.copyParameters(c);
					}
					
					transformer.transform(g);
				} else if(parts[0].equals("ADD_SCHEMA")) {
					String schemaid = parts[1];
					String schematype = parts[2];
					SchemaType type = null;
					if(schematype.equals(SchemaType.NODE.toString())) {
						type = SchemaType.NODE;
					} else if(schematype.equals(SchemaType.DIRECTED.toString())) {
						type = SchemaType.DIRECTED;
					} else if(schematype.equals(SchemaType.UNDIRECTED.toString())) {
						type = SchemaType.UNDIRECTED;
					} else {
						throw new InvalidStateException("Invalid schema type specified: "+schematype);
					}
					
					g.addSchema(schemaid, new Schema(type));
				} else {
					throw new UnsupportedTypeException("Unsupported feature construction type: "
							+parts[0]);
				}

				line = ExperimentUtils.getNonCommentLine(br);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Wrapper for loadFeatureConstruction with the
	 * configurable object set to null.
	 * 
	 * @param g Graph to add feature to
	 * @param fcfile Filename of feature construction file
	 */
	public static void loadFeatureConstruction(Graph g, String fcfile) {
		loadFeatureConstruction(g, fcfile, null);
	}

	/**
	 * Return next non comment line (non-empty or starts with a #)
	 * 
	 * @param br Buffered reader
	 * @return Return the next non-comment line in the file
	 * 
	 * @throws Exception
	 */
	private static String getNonCommentLine(BufferedReader br) throws Exception {
		String line = br.readLine();

		while(line != null && (line.startsWith(COMMENT_LINE) || line.trim().length()==0)){
			line = br.readLine();
		}

		return line;
	}
}
