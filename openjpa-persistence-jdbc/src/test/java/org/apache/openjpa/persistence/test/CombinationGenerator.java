/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.persistence.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates combinations given multiple choices in each dimension.
 * 
 * Usage:
 * <code>
 *    CombinationGenerator combo = new CombinationGenerator();
 *    combo.addDimension(new String[]{"A","B","C"});
 *    combo.addDimension(new int[]{1,2});
 *    List[] combos = combo.generate();
 * </code>   
 * will generate 3*2=6 combinations
 * <code>
 *    combos[0] => List("A",1);
 *    combos[1] => List("B",1);
 *    combos[2] => List("C",1);
 *    combos[3] => List("A",2);
 *    combos[4] => List("B",2);
 *    combos[5] => List("C",2);
 * </code>
 * 
 * @author Pinaki Poddar
 *
 */
public class CombinationGenerator {
	private List<List> dimensions = new ArrayList();
	
	/**
	 * Adds a dimension. null or empty argument has no effect.
	 */
	public void addDimension(List dim) {
		if (dim == null || dim.isEmpty())
			return;
		dimensions.add(dim);
		
	}
	
	/**
	 * Adds a dimension. null or empty argument has no effect.
	 */
	public void addDimension(Object[] dim) {
		if (dim == null || dim.length == 0)
			return;
		dimensions.add(Arrays.asList(dim));
	}
	
	/**
	 * Generates all combinations.
	 * Each array element is a list which has elements in the same order as 
	 * the dimensions were added.
	 */
	public List[] generate() {
		int n = getSize();
		List[] result = new ArrayList[n];
		for (int i = 0; i < n; i++) {
			ArrayList elem = new ArrayList(dimensions.size());
			for (int j=0; j < dimensions.size(); j++)
				elem.add(null);
			result[i] = elem;
		}
		
		for (int i = 0; i < dimensions.size(); i++) {
			fill(i, dimensions.get(i), result);
		}
		return result;
	}
	
	private void fill(int where, List elements, List[] fill) {
		if (fill.length%elements.size() != 0)
			throw new RuntimeException();
		int n = fill.length/elements.size();
		int k = 0;
		for (int i = 0; i < n; i++) {
			for (Object e : elements)	
				fill[k++].set(where, e);
		}
	}
	
	/**
	 * Gets the total number of combinations generated. The total number is 
	 * the product of cardinality of each dimension. 
	 * 
	 */
	public int getSize() {
		int size = 1;
		for (List d : dimensions) 
			size *= d.size();
		return size;
	}
}
