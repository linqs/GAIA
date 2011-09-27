package linqs.gaia.graph.transformer.edge;

import linqs.gaia.graph.EdgeUtils;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.transformer.Transformer;

/**
 * Copy directed edge to undirected edge.
 * The result is that you will have two sets of edges,
 * the original directed edges and the undirected
 * edges which correspond to it.
 * 
 * Required Parameters:
 * <UL>
 * <LI>dirschemaid-Schema ID of directed edges to convert from
 * <LI>undirschemaid-Schema ID of undirected edges to convert to
 * </UL>
 * 
 * @author namatag
 *
 */
public class ConvertDirToUndir extends Transformer {
	@Override
	public void transform(Graph g) {
		// Get parameters
		String dirschemaid = this.getStringParameter("dirschemaid");
		String undirschemaid = this.getStringParameter("undirschemaid");
		
		EdgeUtils.copyDir2Undir(g, dirschemaid, undirschemaid, true);
	}
}
