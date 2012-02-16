package linqs.gaia.graph.filter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.util.KeyedCount;

/**
 * For the specified schema and feature id,
 * remove all graph items whose label appears
 * less than some minimum number of items.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of items to remove
 * <LI> featureid-Feature ID of feature whose value we are considering
 * <LI> mincount-Minimum number of appearances of each value.
 * All graph items whose value appears less than this specified number
 * is removed from the graph.
 * </UL>
 * Optional Parameters:
 * <UL>
 * <LI> removenodeedges-If the item to remove is a node type and this value is "yes",
 * remove all edges incident to these nodes.  Default is "no".
 * </UL>
 * 
 * @author namatag
 *
 */
public class RemoveRareValuedItems extends Filter {

	@Override
	public void filter(Graph graph) {
		String schemaid = this.getStringParameter("schemaid");
		String featureid = this.getStringParameter("featureid");
		int mincount = this.getIntegerParameter("mincount");
		boolean removenodeedges = this.getYesNoParameter("removenodeedges","no");
		
		// Count items
		KeyedCount<String> labelcount = new KeyedCount<String>();
		Iterator<GraphItem> gitr = graph.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			labelcount.increment(gi.getFeatureValue(featureid).getStringValue());
		}
		
		// Identify items to remove
		List<GraphItem> toremove = new LinkedList<GraphItem>();
		gitr = graph.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			if(labelcount.getCount(gi.getFeatureValue(featureid).getStringValue())<mincount) {
				toremove.add(gi);
			}
		}
		
		// Remove items
		for(GraphItem gi:toremove) {
			if(removenodeedges && gi instanceof Node) {
				graph.removeNodeWithEdges((Node) gi);
			} else {
				graph.removeGraphItem(gi);
			}
		}
	}
}
