/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package linqs.gaia.prediction.statistic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.prediction.PositiveOnlyGroup;
import linqs.gaia.prediction.Prediction;
import linqs.gaia.prediction.PredictionGroup;
import linqs.gaia.prediction.SingleValue;
import linqs.gaia.prediction.Weighted;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.ListUtils;

/**
 * Utility functions used in calculating statistics.
 * 
 * @author namatag
 * 
 */
public class StatisticUtils {

    /**
     * Get binary confusion matrix. It will return a confusion matrix in the
     * form of an integer array of size 4 of form: [true positive, false
     * positive, true negative, false negative]
     * <p>
     * Note: The confusion matrix is calculated differently for if the
     * predictions are positive only (i.e., including only links predicted to be
     * positive instead of all possible links).
     * 
     * @param predictions
     *            Prediction group
     * @param truerootvalue
     *            Value to set as positive. All else are considered negative.
     * @return Integer array of form [tp,fp,tn,fn]
     */
    public static double[] getBinaryConfusionMatrix(PredictionGroup predictions, Object truerootvalue) {
        if (predictions instanceof PositiveOnlyGroup) {
            return StatisticUtils.calcPOBinaryConfusionMatrix((PositiveOnlyGroup) predictions, truerootvalue);
        } else {
            return StatisticUtils.calcBinaryConfusionMatrix(predictions, truerootvalue);
        }
    }

    /**
     * Return confusion matrix for prediction groups which only contains
     * positive predictions
     * 
     * @param predictions
     *            Prediction group
     * @param truerootvalue
     *            Value to set as positive. All else are considered negative.
     * @return Integer array of form [tp,fp,tn,fn]
     */
    private static double[] calcPOBinaryConfusionMatrix(PositiveOnlyGroup predictions, Object truerootvalue) {
        double tp = 0;
        double fp = 0;
        double tn = 0;
        double fn = 0;

        double oldcm[] = StatisticUtils.calcBinaryConfusionMatrix(predictions, truerootvalue);
        tp = oldcm[0];
        fp = oldcm[1];
        tn = oldcm[2]; // Should be 0
        fn = oldcm[3]; // Should be 0

        // # False Negative = # of Positives - # True Positive
        if (fn != 0) {
            throw new InvalidStateException("Invalid number of False Negatives from " + predictions.getClass().getCanonicalName() + ": " + fn);
        }

        fn = predictions.getNumPositive() - tp;
        if (fn < 0) {
            throw new InvalidStateException("Invalid number of False Negatives: " + fn);
        }

        // # of True Negative = # of Predictions - Everything not True Negative
        if (tn != 0) {
            throw new InvalidStateException("Invalid number of True Negatives from " + predictions.getClass().getCanonicalName() + ": " + tn);
        }

        tn = predictions.getNumTotal() - tp - fp - fn;
        if (tn < 0) {
            throw new InvalidStateException("Invalid number of True Negatives: " + tn);
        }

        return new double[] {
                tp, fp, tn, fn
        };
    }

    /**
     * Get the true positive, false positive, true negative, and false negative
     * values for the given predictions. If no specific prediction id is
     * specified, this will be calculated for all SingleClass predictions.
     * 
     * @param predictions
     *            List of predictions
     * @param truerootindex
     *            In the set of classes for a single class, which is the true
     *            value using one against all. If set to -1, use the value in
     *            the last index.
     * 
     * @return Integer array with [tp,fp,tn,fn]
     */
    private static double[] calcBinaryConfusionMatrix(PredictionGroup predictions, Object truerootvalue) {
        double tp = 0;
        double fp = 0;
        double tn = 0;
        double fn = 0;

        Class<? extends Prediction> predObjClass = null;
        Iterator<? extends Prediction> pitr = predictions.getAllPredictions();
        while (pitr.hasNext()) {
            Prediction pred = pitr.next();

            // Value only defined if all predictions are of the same type
            if (predObjClass == null) {
                predObjClass = pred.getClass();
            } else if (predObjClass != pred.getClass()) {
                throw new InvalidOperationException("Multiple prediction types encountered: "
                        + predObjClass.getCanonicalName()
                        + " and " + pred.getClass().getCanonicalName());
            }

            if (!(pred instanceof SingleValue)) {
                throw new UnsupportedTypeException("Unsupported prediction type: " + pred.getClass().getCanonicalName());
            }

            SingleValue scp = (SingleValue) pred;
            Object predclass = scp.getPredValue();
            Object trueclass = scp.getTrueValue();

            if (predclass == null) {
                throw new UnsupportedTypeException("TP,FP,TN,FN not defined for unknown value predictions");
            }

            if (trueclass.equals(truerootvalue)) {
                if (predclass.equals(trueclass)) {
                    if (pred instanceof Weighted) {
                        tp += ((Weighted) pred).getWeight();
                    } else {
                        tp++;
                    }
                } else {
                    if (pred instanceof Weighted) {
                        fn += ((Weighted) pred).getWeight();
                    } else {
                        fn++;
                    }
                }
            } else {
                if (predclass.equals(truerootvalue)) {
                    if (pred instanceof Weighted) {
                        fp += ((Weighted) pred).getWeight();
                    } else {
                        fp++;
                    }
                } else {
                    if (pred instanceof Weighted) {
                        tn += ((Weighted) pred).getWeight();
                    } else {
                        tn++;
                    }
                }
            }
        }

        return new double[] {
                tp, fp, tn, fn
        };
    }

    /**
     * Return the list of string header values for the list of statistics
     * 
     * @param stats
     *            List of Statistics
     * @return String of header values
     */
    public static List<String> getHeader(List<Statistic> stats) {
        List<String> header = new ArrayList<String>();

        for (Statistic stat : stats) {
            header.addAll(stat.getHeader());
        }

        return header;
    }

    /**
     * Returns statistics from the specified set of stats. The values for each
     * statistics is separated by the provided delimiter.
     * 
     * @param stats
     *            List of statistics
     * @param preds
     *            Predictions to calculate statistic over
     * @param delimiter
     *            Separator for the values of the different statistics
     * @return String representation of the specified statistics
     */
    public static String getStatistics(List<Statistic> stats, PredictionGroup preds,
            String delimiter) {
        String output = null;

        for (Statistic stat : stats) {
            if (output == null) {
                output = "";
            } else {
                output += delimiter;
            }

            output += stat.getStatisticString(preds);
        }

        return output;
    }

    /**
     * Prints the values for the given statistics using the ordering provided by
     * the header where header is the set of keys for the statistic.
     * 
     * @param stats
     *            List of statistics
     * @param preds
     *            Predictions to calculate statistic over
     * @param header
     *            List of the statistic keys used to order the results
     * @param delimiter
     *            Separator for the values of the different statistics
     * @return Separator for the values of the different statistics
     */
    public static String getStatistics(List<Statistic> stats, PredictionGroup preds,
            List<String> header, String delimiter) {
        Map<String, String> allstats = new LinkedHashMap<String, String>();

        for (Statistic stat : stats) {
            allstats.putAll(stat.getStatisticStrings(preds));
        }

        String output = null;
        for (String key : header) {
            if (output == null) {
                output = "";
            } else {
                output += delimiter;
            }

            output += allstats.get(key);
        }

        return output;
    }

    /**
     * Given a map containing results for statistics, print the values in the
     * order of the keys specified in the header.
     * 
     * @param allstats
     *            Map of statistic values
     * @param header
     *            List of strings representing keys for the statistics
     * @param delimiter
     *            String delimiter
     * @return String representation of the calculated statistics
     */
    public static String getStatistics(Map<String, Double> allstats,
            List<String> header, String delimiter) {
        String output = null;
        for (String key : header) {
            if (output == null) {
                output = "";
            } else {
                output += delimiter;
            }

            output += allstats.get(key);
        }

        return output;
    }

    /**
     * Given a map containing results for statistics, print the values in the
     * order of the keys specified in the header.
     * 
     * @param allstats
     *            Map of statistic values
     * @param header
     *            List of strings representing keys for the statistics
     * @param valdelimiter
     *            String delimiter between values
     * @param keyvaldelimiter
     *            String delimiter between key and value
     * @return String representation of the calculated statistics
     */
    public static String getStatistics(Map<String, Double> allstats,
            List<String> header, String valdelimiter, String keyvaldelimiter) {
        String output = null;
        for (String key : header) {
            if (output == null) {
                output = "";
            } else {
                output += valdelimiter;
            }

            output += key + keyvaldelimiter + allstats.get(key);
        }

        return output;
    }

    /**
     * Return all double statistics defined
     * 
     * @param stats
     *            Statistics to use
     * @param preds
     *            Prediction group to use
     * @param header
     *            Header to use printing
     * @param delimiter
     *            Delimiter to separate statistic values
     * @return Map of double statistics
     */
    public static Map<String, Double> getDoubleStatistics(List<Statistic> stats, PredictionGroup preds,
            List<String> header, String delimiter) {
        Map<String, Double> allstats = new LinkedHashMap<String, Double>();

        for (Statistic stat : stats) {
            allstats.putAll(stat.getStatisticDoubles(preds));
        }

        return allstats;
    }

    /**
     * Get count of a feature. Whenever the graph item has a feature value
     * matching the specified true value, number of positive is incremented.
     * Otherwise, number of negative is incremented. An array is returned of the
     * form [number of negative, number of positive].
     * 
     * @param gitems
     *            Graph Items
     * @param featurename
     *            Name of feature
     * @param truevalue
     *            Value to count as positive
     * @return Double array in form of [numnegative,numpositive]
     */
    public static double[] getBinaryCounts(List<GraphItem> gitems,
            String featurename,
            String truevalue) {

        int numneg = 0;
        int numpos = 0;
        for (GraphItem gi : gitems) {
            // Only defined over categorical features
            FeatureValue fvalue = gi.getFeatureValue(featurename);
            if (fvalue == null || !(fvalue instanceof CategValue)) {
                throw new UnsupportedTypeException("Feature only defined over categorical values: "
                        + fvalue);
            }

            String value = ((CategValue) fvalue).getCategory();
            if (truevalue.equals(value)) {
                numpos++;
            } else {
                numneg++;
            }
        }

        return new double[] {
                numneg, numpos
        };
    }

    /**
     * Average the values of the maps in the list where the maps were returned
     * by getStatisticDoubles call.
     * 
     * @param statmaps
     *            List of statistic maps
     * @return Statistics map with average values
     */
    public static Map<String, Double> getAverageStatistics(List<Map<String, Double>> statmaps) {
        int size = statmaps.size();
        Map<String, Double> counts = new LinkedHashMap<String, Double>();
        if (statmaps.size() == 0) {
            throw new InvalidOperationException("No maps to average over");
        }

        Set<String> keys = statmaps.get(0).keySet();
        for (String key : keys) {
            for (Map<String, Double> m : statmaps) {
                if (counts.containsKey(key)) {
                    counts.put(key, m.get(key) + counts.get(key));
                } else {
                    counts.put(key, m.get(key));
                }
            }
        }

        // Take average
        for (String key : keys) {
            counts.put(key, (double) counts.get(key) / size);
        }

        return counts;
    }

    public static Map<String, Double> getStdDevStatistics(List<Map<String, Double>> statmaps) {
        Map<String, Double> counts = new LinkedHashMap<String, Double>();
        if (statmaps.size() == 0) {
            throw new InvalidOperationException("No maps to average over");
        }

        Set<String> keys = statmaps.get(0).keySet();
        for (String key : keys) {
            List<Double> allvalues = new LinkedList<Double>();
            for (Map<String, Double> m : statmaps) {
                allvalues.add(m.get(key));
            }

            counts.put(key, ArrayUtils.stddev(ListUtils.doubleList2array(allvalues)));
        }

        return counts;
    }
}
