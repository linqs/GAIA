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
package linqs.gaia.feature.derived.aggregate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Node;

/**
 * Defined for binary edges, compute the difference, in terms of days,
 * between the date values for the incident nodes, given by the specified feature.
 * 
 * Required Parameters:
 * <UL>
 * <LI> featureid-Feature ID of string valued feature whose value we parse as the date.
 * <LI> dateformat-Date format, as specified by @{Calendar}, of the string value to parse.
 * (e.g., "yyyy'-'MM'-'dd'T'HH':'mm:ss.S'Z'")
 * <LI> normalizer-Normalize the difference, given in number of days, by this value.
 * If the difference in date is greater than this value, the value is rounded down.
 * <LI> invert-If specified, instead of returning the normalized value as the difference,
 * return as the inverted normalized value (i.e., 1 minus the normalized difference).
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeNodeDateDifference extends DerivedNum {
	private String fid = null;
	private static NumValue val0 = new NumValue(0);
	private static NumValue val1 = new NumValue(1);
	private SimpleDateFormat sdf = null;
	private Double normalizer = null;
	private String dateformat = null;
	private boolean invert = false;
	
	protected void initialize() {
		fid = this.getStringParameter("featureid");
		if(this.hasParameter("normalizer")) {
			normalizer = this.getDoubleParameter("normalizer");
		}
		dateformat = this.getStringParameter("dateformat");
		invert = this.getYesNoParameter("invert", "no");
		
		sdf = new SimpleDateFormat(dateformat);
	}
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
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
			return this.invert ? val1  : val0;
		}
		
		Date date1 = null;
		Date date2 = null;
		try {
			date1 = sdf.parse(fv1.getStringValue());
			date2 = sdf.parse(fv2.getStringValue());
		} catch (ParseException ex) {
			throw new RuntimeException(ex);
		}
		
		double deltaDays = Math.abs(date1.getTime() - date2.getTime())/ 86400000.0;
		if(normalizer==null) {
			return new NumValue(deltaDays);
		} else {
			double normval = deltaDays/normalizer;
			normval = normval > 1 ? 1.0 : normval;
			
			if(invert) {
				normval = 1.0 - normval;
			}
			
			return new NumValue(normval);
		}
	}
}
