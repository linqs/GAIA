package linqs.gaia.graph.noise.graphitem;

import linqs.gaia.graph.Graph;
import linqs.gaia.graph.noise.Noise;

/**
 * Removes all graph items with the given schema.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of graph items to remove
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> removeschema-If yes, remove the schema for the schemaid.  Default is no.
 * </UL>
 * 
 * @author namatag
 *
 */
public class RemoveGraphItems extends Noise {

	@Override
	public void addNoise(Graph g) {
		String schemaid = this.getStringParameter("schemaid");
		g.removeAllGraphItems(schemaid);
		
		if(this.hasYesNoParameter("removeschema", "yes")) {
			g.removeSchema(schemaid);
		}
	}
}
