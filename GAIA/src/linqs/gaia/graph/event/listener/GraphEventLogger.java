package linqs.gaia.graph.event.listener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.graph.event.CustomEvent;
import linqs.gaia.graph.event.EdgeAddedEvent;
import linqs.gaia.graph.event.EdgeRemovedEvent;
import linqs.gaia.graph.event.FeatureSetEvent;
import linqs.gaia.graph.event.GraphEvent;
import linqs.gaia.graph.event.GraphEventListener;
import linqs.gaia.graph.event.ModelCompletedEvent;
import linqs.gaia.graph.event.NodeAddedEvent;
import linqs.gaia.graph.event.NodeRemovedEvent;
import linqs.gaia.log.Log;

/**
 * This graph event listener prints the events, as they happen,
 * to a given log file.
 * <p>
 * Each line in the log file corresponds to a graph event.
 * The format is as follows:<br>
 * NodeAddedEvent=NODE_ADDED\tnode<br>
 * NodeRemovedEvent=NODE_REMOVED\tnode<br>
 * EdgeAddedEvent=EDGE_ADDED\tedge<br>
 * EdgeRemovedEvent=EDGE_REMOVED\tedge<br>
 * FeatureSetEvent=FEATURE_SET\tgraphitem\tfeatureid\tpreviousvalue\tcurrentvalue<br>
 * ModelCompletedEvent=MODEL_COMPLETED\tmodel<br>
 * CustomEvent=CUSTOM_EVENT\tmessage<br>
 * <br>
 * where node, edge, graphitem, and model are the string representations of the corresponding
 * items and \t is the tab character.  Specifically, graphitem, node, and edge are of the form
 * &lt;graphschemaid&gt;.&lt;graphobjectid&gt;.&lt;schemaid&gt;.&lt;itemobjectid&gt;.
 * The String representation of the model varies per model.<br>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> logfile-Filename to write log messages to
 * <LI> printinfo-If yes, print the log messages to INFO.  Default is to not print.
 * </UL>
 * 
 * @author namatag
 *
 */
public class GraphEventLogger extends BaseConfigurable implements GraphEventListener {
	private String delimiter = "\t";
	private boolean initialized = false;
	private boolean printinfo = false;
	private BufferedWriter fileout = null;
	
	public void initialize() {
		try {
			printinfo = this.hasParameter("printinfo", "yes");
			
			if(this.hasParameter("logfile")) {
				// Initialize output file and keep handler for later
				String file = this.getStringParameter("logfile");
				FileWriter fstream = new FileWriter(file);
				fileout = new BufferedWriter(fstream);
			}
			
			this.initialized = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void execute(GraphEvent graphevent) {
		// Initialize the first time
		if(!initialized) {
			this.initialize();
		}
		
		String output = null;
		if(graphevent instanceof NodeAddedEvent) {
			NodeAddedEvent e = (NodeAddedEvent) graphevent;
			output = "NODE_ADDED"+delimiter+e.getAddedNode();
		} else if(graphevent instanceof NodeRemovedEvent) {
			NodeRemovedEvent e = (NodeRemovedEvent) graphevent;
			output = "NODE_REMOVED"+delimiter+e.getRemovedNode();
		} else if(graphevent instanceof EdgeAddedEvent) {
			EdgeAddedEvent e = (EdgeAddedEvent) graphevent;
			output = "EDGE_ADDED"+delimiter+e.getAddedEdge();
		} else if(graphevent instanceof EdgeRemovedEvent) {
			EdgeRemovedEvent e = (EdgeRemovedEvent) graphevent;
			output = "EDGE_REMOVED"+delimiter+e.getRemovedEdge();
		} else if(graphevent instanceof FeatureSetEvent) {
			FeatureSetEvent e = (FeatureSetEvent) graphevent;
			
			output = "FEATURE_SET"
				+delimiter+e.getChangedItem()
				+delimiter+e.getChangedFeatureID()
				+delimiter+e.getPreviousValue()
				+delimiter+e.getCurrentValue();
		} else if(graphevent instanceof ModelCompletedEvent) {
			ModelCompletedEvent e = (ModelCompletedEvent) graphevent;
			output = "MODEL_COMPLETED"+delimiter+e.getCompletedModel();
		} else if(graphevent instanceof CustomEvent) {
			CustomEvent e = (CustomEvent) graphevent;
			output = "CUSTOM_EVENT"+delimiter+e.getMessage();
		} else {
			throw new UnsupportedTypeException("Unsupported Graph Event Type: "
					+graphevent.getClass().getCanonicalName());
		}
		
		// If requested, print to INFO
		if(printinfo) {
			Log.INFO(output);
		}
		
		// If requested, write to file
		if(fileout != null) {
			try {
				fileout.write(output+"\n");
				fileout.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
