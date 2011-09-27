package linqs.gaia.experiment;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.io.IO;
import linqs.gaia.log.Log;
import linqs.gaia.util.Dynamic;

/**
 * Simple experiment.  Takes a graph loaded using the specified
 * input io and saves that graph using the specified output io.
 * <p>
 * Note: This experiment has the default configuration id of "exp".
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> inputioclass-Class for the input {@link IO} to instantiate using {@link Dynamic#forConfigurableName}
 * <LI> outputioclass-Class for the output {@link IO} to instantiate using {@link Dynamic#forConfigurableName}
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class IOConversionExperiment extends Experiment {

	@Override
	public void runExperiment() {
		// Load Input IO class
		IO io = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("inputioclass"));
		io.copyParameters(this);
		
		Graph graph = io.loadGraph();
		Log.INFO("Loaded graph: "+GraphUtils.getSimpleGraphOverview(graph));
		
		// Load Output IO class
		io = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("outputioclass"));
		io.copyParameters(this);
		io.saveGraph(graph);
		Log.INFO("Saved graph using "+this.getStringParameter("outputioclass"));
		
		graph.destroy();
	}
	
	public static void main(String[] args) {
		if(args.length != 1) {
			throw new ConfigurationException("Arguments: <configfile>");
		}
		
		Experiment e = new IOConversionExperiment();
		e.setCID("exp");
		e.loadParametersFile(args[0]);
		e.runExperiment();
	}
}
