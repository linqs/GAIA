package linqs.gaia.similarity.set;

import java.util.HashSet;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.similarity.NormalizedSetSimilarity;

/**
 * Calculate Jaccard Coefficient between two sets of objects.
 * <p>
 * Reference:
 * "Collective Entity Resolution in Relational Data", Indrajit Bhattacharya and Lise Getoor,
 * ACM Transactions on Knowledge Discovery from Data (ACM-TKDD), 2007.
 * <p>
 * JaccardCoefficient(ci,cj)=|(Nbr(ci) intersect Nbr(cj))|/|(Nbr(ci) union Nbr(cj))|
 * 
 * @author namatag
 */
public class JaccardCoefficient extends BaseConfigurable implements NormalizedSetSimilarity {
	private static final long serialVersionUID = 1L;
	
	public double getSimilarity(Set<? extends Object> item1, Set<? extends Object> item2) {
		double sim = 0;
		
		Set<Object> isect = new HashSet<Object>(item1);
		isect.retainAll(item2);
		
		Set<Object> union = new HashSet<Object>(item1);
		union.addAll(item2);
		
		if(union.size()==0){
			sim = 0;
		} else {
			sim = ((double) isect.size())/((double) union.size());
		}
		
		return sim;
	}
	
	public double getNormalizedSimilarity(Set<? extends Object> item1,
			Set<? extends Object> item2) {
		return this.getSimilarity(item1, item2);
	}
}
