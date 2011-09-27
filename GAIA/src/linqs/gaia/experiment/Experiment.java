package linqs.gaia.experiment;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.log.Log;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.SimpleTimer;

/**
 * Base experiment class to be implemented by all general experiments.
 * 
 * @author namatag
 *
 */
public abstract class Experiment extends BaseConfigurable {
	/**
	 * Perform the experiment
	 */
	public abstract void runExperiment();
	
	/**
	 * Main class to run experiment
	 * 
	 * To execute, the command is:
	 * <p>
	 * <code>
	 * java <@link linqs.gaia.experiment.Experiment} &lt;experimentclass&gt &lt;configfile&gt
	 * </code>
	 * <p>
	 * where experimentclass is an {@link Experiment} implementation
	 * (i.e.: {@link linqs.gaia.experiment.OCExperiment})
	 * to instantiate using in {@link Dynamic#forConfigurableName}
	 * and configfile is the configuration file to load for the experiment.
	 * 
	 * @param args Arguments
	 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
	 * 
	 * @throws Exception Exception from running experiment
	 */
	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			throw new ConfigurationException("Arguments: <ExperimentClass> <configfile>");
		}
		
		SimpleTimer timer = new SimpleTimer();
		Experiment e = (Experiment) Dynamic.forConfigurableName(Experiment.class, args[0]);
		e.loadParametersFile(args[1]);
		e.runExperiment();
		Log.INFO("Experiment Runtime: "+timer.timeLapse(true)+"\t"+timer.timeLapse(false));
	}
}
