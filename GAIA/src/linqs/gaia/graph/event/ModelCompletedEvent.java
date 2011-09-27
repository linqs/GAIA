package linqs.gaia.graph.event;

import linqs.gaia.model.Model;

/**
 * Event called whenever current modification of the data is complete by a given model.
 * This event, for example, is called when a model completes a given set of
 * prediction changes to a graph.
 * 
 * @author namatag
 *
 */
public class ModelCompletedEvent implements GraphEvent {
	private Model m;
	
	/**
	 * Constructor
	 * 
	 * @param m Model that completed predictions
	 */
	public ModelCompletedEvent(Model m) {
		this.m = m;
	}
	
	/**
	 * Return the completed model
	 * 
	 * @return Completed model
	 */
	public Model getCompletedModel() {
		return m;
	}
}
