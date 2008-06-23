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
package org.apache.openjpa.lib.graph;

import java.util.Collection;
import java.util.Iterator;

import org.apache.openjpa.lib.test.AbstractTestCase;

/**
 * <p>Tests the {@link DepthFirstAnalysis} type.</p>
 *
 * @author Abe White
 */
public class TestDepthFirstAnalysis
    extends AbstractTestCase {

    private DepthFirstAnalysis _dfa = null;

    public void setUp() {
        Graph graph = new Graph();
        Object node1 = new Object();
        Object node2 = new Object();
        Object node3 = new Object();
        Object node4 = new Object();
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addNode(node4);
        graph.addEdge(new Edge(node1, node2, true));
        graph.addEdge(new Edge(node2, node3, true));
        graph.addEdge(new Edge(node3, node1, true));
        graph.addEdge(new Edge(node3, node4, true));
        graph.addEdge(new Edge(node2, node2, true));
        _dfa = new DepthFirstAnalysis(graph);
    }

    public void testNodeSorting() {
        Collection nodes = _dfa.getSortedNodes();
        assertEquals(4, nodes.size());

        int time = Integer.MAX_VALUE;
        Object node;
        for (Iterator itr = nodes.iterator(); itr.hasNext();) {
            node = itr.next();
            assertTrue(time >= _dfa.getFinishedTime(node));
            time = _dfa.getFinishedTime(node);
        }
    }

    public void testEdgeTyping() {
        Collection edges = _dfa.getEdges(Edge.TYPE_BACK);
        assertEquals(2, edges.size());
        Iterator itr = edges.iterator();
        Edge edge0 = (Edge) itr.next();
        Edge edge1 = (Edge) itr.next();
        assertTrue((edge0.getTo() == edge0.getFrom())
            || edge1.getTo() == edge1.getFrom());
    }

    public static void main(String[] args) {
        main(TestDepthFirstAnalysis.class);
    }
}
