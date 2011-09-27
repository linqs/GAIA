package linqs.gaia.graph.event;

import linqs.gaia.configurable.Configurable;

/**
 * Graph event listener.  Use to perform some action when certain
 * changes are made in the graph.
 * 
 * @author namatag
 *
 */
public interface GraphEventListener extends Configurable {
	/**
	 * Execute the listener, passing in what kind of event is causing this extension
	 */
	void execute(GraphEvent graphevent);
}
