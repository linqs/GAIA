package linqs.gaia.graph.statistic;

import java.util.Map;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * Interface for statistics computed over the graph.
 * 
 * @author namatag
 *
 */
public interface GraphStatistic extends Configurable {
	/**
	 * Get numeric value of this evaluation metric over the provided data.
	 * 
	 * @param g Graph to calculate statistic over
	 * @return Numeric values of the statistic.  Value is set to -1 if not applicable.
	 */
    Map<String,Double> getStatisticDoubles(Graph g);
    
    /**
     * Get string representation of this evaluation metric over the provided data.
     * 
     * @param g Graph to calculate statistic over
     * @return String values of the evaluation metric.
     */
    Map<String,String> getStatisticStrings(Graph g);
    
    /**
     * Get string representation of this evaluation metric over the provided data.
     * 
     * @param g Graph to calculate statistic over
     * @return String value of the evaluation metric.
     */
    String getStatisticString(Graph g);
}
