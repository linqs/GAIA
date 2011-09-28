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
package linqs.gaia.util.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.MinMax;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.WeightedSampler;

/**
 * Test cases for TabDelimIO implementation
 * 
 * @author namatag
 *
 */
public class UtilTestCase extends TestCase {
	
	public UtilTestCase() {
		
	}
	
	public void testKeyedCount() {
		KeyedCount<String> kc = new KeyedCount<String>();
		kc.increment("a");
		kc.increment("b");
		kc.increment("b");
		kc.increment("a");
		kc.decrement("b");
		kc.decrement("c");
		
		assertEquals(2,kc.getCount("a"));
		assertEquals(1,kc.getCount("b"));
		assertEquals(-1,kc.getCount("c"));
		assertEquals(0,kc.getCount("d"));
		assertEquals(3,kc.numKeys());
		assertEquals(2,kc.totalCounted());
		assertEquals("a",kc.highestCountKey());
		assertEquals(1.0,kc.getPercent("a"));
	}
	
	public void testMinMax() {
		MinMax mm = new MinMax();
		mm.addValue(1);
		mm.addValue(2);
		mm.addValue(3);
		mm.addValue(4);
		
		assertEquals(1.0,mm.getMin());
		assertEquals(4.0,mm.getMax());
		assertEquals(10.0/4.0,mm.getMean());
		assertEquals(10.0,mm.getSumTotal());
	}
	
	public void testSimplePair() {
		SimplePair<String,Integer> paira1 = new SimplePair<String,Integer>("a",1);
		SimplePair<String,Integer> pairb2 = new SimplePair<String,Integer>("b",2);
		assertEquals("a",paira1.getFirst());
		assertEquals(new Integer(1),paira1.getSecond());
		assertEquals("b",pairb2.getFirst());
		assertEquals(new Integer(2),pairb2.getSecond());
	}
	
	public void testArrayUtils() {
		double[] array = new double[]{1.0,3.0,2.0};
		assertEquals(1.0,ArrayUtils.minValue(array));
		assertEquals(0,ArrayUtils.minValueIndex(array));
		assertEquals(3.0,ArrayUtils.maxValue(array));
		assertEquals(1,ArrayUtils.maxValueIndex(array));
	}
	
	public void testWeightedSampler() {
		Map<String,Double> probdist = new HashMap<String,Double>();
		probdist.put("A", 200.0);
		probdist.put("B", 100.0);
		probdist.put("C", 30.0);
		probdist.put("D", 4.0);
		probdist.put("E", 96.0);
		probdist.put("F", 70.0);
		
		// Test with replacement
		Random rand = new Random();
		KeyedCount<Object> count = new KeyedCount<Object>();
		List<Object> samples = WeightedSampler.performWeightedSampling(probdist, 12, true, rand);
		for(Object o:samples) {
			count.increment(o);
		}
		
		// Test that sampling with replacement works by sampling
		// over less than the number needed
		assertEquals(12,count.totalCounted());
		
		// Test without replacement
		for(int i=0; i<10000; i++) {
			count = new KeyedCount<Object>();
			samples = WeightedSampler.performWeightedSampling(probdist, 6, false, rand);
			Set<Object> nodupcheck = new HashSet<Object>(samples);
			if(nodupcheck.size()!=samples.size()) {
				throw new InvalidStateException("No sample twice: "+nodupcheck+" while "+samples);
			}
			
			for(Object o:samples) {
				count.increment(o);
			}
			
			assertEquals(6,count.numKeys());
		}
	}
}
