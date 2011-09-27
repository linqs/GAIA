package linqs.gaia.graph.filter;

import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.filter.Filter;

/**
 * Filter to remove the specified feature from the given schema.
 * <p>
 * Required:
 * <UL>
 * <LI> schemaID-Feature schema id of schema to change
 * <LI> featureID-Comma delimited list of features to remove
 * </UL>
 * 
 * @author namatag
 *
 */
public class RemoveFeature extends Filter {
	@Override
	public void filter(Graph graph) {
		String schemaID = this.getStringParameter("schemaID");
		String fids[] = this.getStringParameter("featureID").split(",");
		
		Schema schema = graph.getSchema(schemaID);
		for(String fid:fids){
			schema.removeFeature(fid);
		}
		
		graph.updateSchema(schemaID, schema);
	}
}
