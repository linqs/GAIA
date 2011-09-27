package linqs.gaia.visualization.lightviz;

import prefuse.util.ui.UILib;
import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.graph.Graph;
import linqs.gaia.visualization.Visualization;

/**
 * Visualize the graph using the LighViz Visualization.
 * This visualization requires the Prefuse library to be in the class
 * path (i.e., prefuse.jar).
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> key-Prefuse key to use
 * <LI> layout-Layout to use.  Options are the layout options specified in VizGraph.
 * </UL>
 * @author namatag
 *
 */
public class LightVizVisualization extends BaseConfigurable implements Visualization {
	public void visualize(Graph g) {
		String key = null;
		if(this.hasParameter("key")) {
			key = this.getStringParameter("key");
		}

		String layout = "RadialTreeLayout";
		if(this.hasParameter("layout")) {
			layout = this.getStringParameter("layout");
		}

		VizGraph vzgraph = new VizGraph(g, key, layout);

		/* Example Code for creating a simple Frame with the current
         * graph visualization
         */
    	UILib.setPlatformLookAndFeel();

    	// Visualize the graph
    	MainFrameLight main = new MainFrameLight();
    	main.setVisible(true);
        main.setVizGraph(vzgraph);
	}
}
