package linqs.gaia.feature.derived.aggregate;

import java.util.Iterator;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Node;

/**
 * Numeric valued feature defined for binary edges.
 * This features returns a 1 if the string representation
 * of a features value is the same for both incident nodes.
 * 
 * Required Parameters:
 * <UL>
 * <LI> featureid-Feature ID of string valued feature to compare
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> commonvalue-If specified, we add the additional constraint that
 * a 1 is only returned if both values
 * are not equal to this string value specified in this parameter.
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeNodeValueMatch extends DerivedNum {
	private String fid = null;
	private String commonvalue = null;
	private static NumValue val0 = new NumValue(0);
	private static NumValue val1 = new NumValue(1);
	private boolean initialize = true;
	
	private void initialize() {
		initialize = false;
		fid = this.getStringParameter("featureid");
		commonvalue = this.getStringParameter("commonvalue",null);
	}
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		if(initialize) {
			this.initialize();
		}
		
		// Feature applicable only to edges
		if(!(di instanceof Edge)) {
			throw new UnsupportedTypeException("Feature only defined for edges: "+
					di.getClass().getCanonicalName());
		}
		
		// Feature applicable only to binary edges
		Edge e = (Edge) di;
		if(e.numNodes()!=2) {
			throw new UnsupportedTypeException("Feature only defined for binary edges: "+
					e.numNodes());
		}
		
		Iterator<Node> itr = e.getAllNodes();
		Node n1 = itr.next();
		Node n2 = itr.next();
		
		FeatureValue fv1 = n1.getFeatureValue(fid);
		FeatureValue fv2 = n2.getFeatureValue(fid);
		
		if(fv1.equals(FeatureValue.UNKNOWN_VALUE) || fv2.equals(FeatureValue.UNKNOWN_VALUE)) {
			return val0;
		}
		
		// If sparse value is specified and at least one value is not the sparse
		// value, return 0.
		if(commonvalue!=null && fv1.equals(commonvalue)) {
			return val0;
		}
			
		return fv1.getStringValue().equals(fv2.getStringValue()) ? val1 : val0;
	}
}
