package linqs.gaia.feature.derived.aggregate;

import java.util.Iterator;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Node;
import linqs.gaia.similarity.NormalizedStringSimilarity;
import linqs.gaia.util.Dynamic;

/**
 * Defined for binary edges, compute the string similarity
 * of a string valued attribute in the adjacent nodes.
 * 
 * Required Parameters:
 * <UL>
 * <LI> featureid-Feature ID of string feature to use
 * <LI> stringsimclass-Class of the normalized string similarity to use
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeNodeStringSimilarity extends DerivedNum {
	private String fid = null;
	private NormalizedStringSimilarity stringsim = null;
	private static NumValue val0 = new NumValue(0);
	private boolean initialize = true;
	
	private void initialize() {
		initialize = false;
		fid = this.getStringParameter("featureid");
		String stringsimclass = this.getStringParameter("stringsimclass");
		stringsim = (NormalizedStringSimilarity) Dynamic.forConfigurableName(NormalizedStringSimilarity.class,
				stringsimclass, this);
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
		
		double sim = stringsim.getNormalizedSimilarity(fv1.getStringValue(), fv2.getStringValue());
		
		return new NumValue(sim);
	}
}
