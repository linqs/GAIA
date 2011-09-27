package linqs.gaia.graph.generator.decorator;

import java.util.Iterator;
import java.util.Random;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;

/**
 * Create an integer valued feature whose
 * value is randomly set.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of objects whose features to change
 * <LI> featureid-Feature ID of attribute to add
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> maxvalue-Maximum integer value.  The value is set
 * between 0 (inclusive) and this value (exclusive).
 * <LI> seed-Seed to use for the random number generator
 * </UL>
 * 
 * @author namatag
 *
 */
public class RandomIntegerValue extends BaseConfigurable implements Decorator {
	private boolean initialize = true;
	private String schemaid;
	private String featureid;
	private Integer maxvalue;
	private Random rand;
	
	private void initialize() {
		initialize = false;
		
		schemaid = this.getStringParameter("schemaid");
		featureid = this.getStringParameter("featureid");
		
		maxvalue = null;
		if(this.hasParameter("maxvalue")) {
			maxvalue = this.getIntegerParameter("maxvalue");
		}
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = this.getIntegerParameter("seed");
		}
		rand = new Random(seed);
	}
	
	public void decorate(Graph g) {
		if(initialize) {
			this.initialize();
		}
		
		// Add feature if not already defined
		Schema schema = g.getSchema(schemaid);
		if(!schema.hasFeature(featureid)) {
			schema.addFeature(featureid, new ExplicitNum());
			g.updateSchema(schemaid, schema);
		}
		
		// Set graph item values
		Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			int value = 0;
			if(maxvalue!=null) {
				value = rand.nextInt(maxvalue);
			} else {
				value = rand.nextInt();
			}
			
			gi.setFeatureValue(featureid, new NumValue(value));
		}
	}
}