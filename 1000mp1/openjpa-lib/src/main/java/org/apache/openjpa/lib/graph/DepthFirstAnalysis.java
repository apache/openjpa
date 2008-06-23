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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>Performs a depth-first analysis of a given {@link Graph}, caching
 * information about the graph's nodes and edges.  See the DFS algorithm
 * in the book 'Introduction to Algorithms' by Cormen, Leiserson, and
 * Rivest.  The algorithm has been modified to group sibling nodes without
 * connections together during the topological sort.</p>
 *
 * @author Abe White
 * @since 1.0.0
 * @nojavadoc
 */
public class DepthFirstAnalysis {

    private final Graph _graph;
    private final Map _nodeInfo = new HashMap();
    private Comparator _comp;

    /**
     * Constructor.  Performs the analysis on the given graph and caches
     * the resulting information.
     */
    public DepthFirstAnalysis(Graph graph) {
        _graph = graph;

        // initialize node infos
        Collection nodes = graph.getNodes();
        for (Iterator itr = nodes.iterator(); itr.hasNext();)
            _nodeInfo.put(itr.next(), new NodeInfo());

        // visit all nodes -- see intro to algo's book
        NodeInfo info;
        Object node;
        for (Iterator itr = nodes.iterator(); itr.hasNext();) {
            node = itr.next();
            info = (NodeInfo) _nodeInfo.get(node);
            if (info.color == NodeInfo.COLOR_WHITE)
                visit(graph, node, info, 0);
        }
    }

    /**
     * Visit a node.  See Introduction to Algorithms book for details.
     */
    private int visit(Graph graph, Object node, NodeInfo info, int time) {
        // discover node
        info.color = NodeInfo.COLOR_GRAY;

        // explore all vertices from that node depth first
        Collection edges = graph.getEdgesFrom(node);
        Edge edge;
        Object other;
        NodeInfo otherInfo;
        int maxChildTime = time - 1;
        int childTime;
        for (Iterator itr = edges.iterator(); itr.hasNext();) {
            edge = (Edge) itr.next();
            other = edge.getOther(node);
            otherInfo = (NodeInfo) _nodeInfo.get(other);
            if (otherInfo.color == NodeInfo.COLOR_WHITE) {
                // undiscovered node; recurse into it
                childTime = visit(graph, other, otherInfo, time);
                edge.setType(Edge.TYPE_TREE);
            } else if (otherInfo.color == NodeInfo.COLOR_GRAY) {
                childTime = -1;
                edge.setType(Edge.TYPE_BACK);
            } else {
                childTime = otherInfo.finished;
                edge.setType(Edge.TYPE_FORWARD);
            }
            maxChildTime = Math.max(maxChildTime, childTime);
        }

        // finished with node
        info.color = NodeInfo.COLOR_BLACK;
        info.finished = maxChildTime + 1;
        return info.finished;
    }

    /**
     * Set the comparator that should be used for ordering groups of nodes
     * with the same dependencies.
     */
    public void setNodeComparator(Comparator comp) {
        _comp = comp;
    }

    /**
     * Return the nodes in topologically-sorted order.  This is often used
     * to order dependencies.  If each graph edge (u, v) represents a
     * dependency of v on u, then this method will return the nodes in the
     * order that they should be evaluated to satisfy all dependencies.  Of
     * course, if the graph is cyclic (has back edges), then no such ordering
     * is possible, though this method will still return the correct order
     * as if edges creating the cycles did not exist.
     */
    public List getSortedNodes() {
        Map.Entry[] entries = (Map.Entry[]) _nodeInfo.entrySet().
            toArray(new Map.Entry[_nodeInfo.size()]);
        Arrays.sort(entries, new NodeInfoComparator(_comp));
        return new NodeList(entries);
    }

    /**
     * Return all edges of the given type.  This method can be used to
     * discover all edges that cause cycles in the graph by passing it
     * the {@link #EDGE_BACK} edge type.
     */
    public Collection getEdges(int type) {
        Collection typed = null;
        Edge edge;
        Object node;
        for (Iterator nodes = _graph.getNodes().iterator(); nodes.hasNext();) {
            node = nodes.next();
            for (Iterator itr = _graph.getEdgesFrom(node).iterator();
                itr.hasNext();) {
                edge = (Edge) itr.next();
                if (edge.getType() == type) {
                    if (typed == null)
                        typed = new ArrayList();
                    typed.add(edge);
                }
            }
        }
        return (typed == null) ? Collections.EMPTY_LIST : typed;
    }

    /**
     * Return the logical time that the given node was finished in
     * the graph walk, or -1 if the node is not part of the graph.
     */
    public int getFinishedTime(Object node) {
        NodeInfo info = (NodeInfo) _nodeInfo.get(node);
        if (info == null)
            return -1;
        return info.finished;
    }

    /**
     * Comparator for toplogically sorting entries in the node info map.
     */
    private static class NodeInfoComparator
        implements Comparator {

        private final Comparator _subComp;

        public NodeInfoComparator(Comparator subComp) {
            _subComp = subComp;
        }

        public int compare(Object o1, Object o2) {
            Map.Entry e1 = (Map.Entry) o1;
            Map.Entry e2 = (Map.Entry) o2;
            NodeInfo n1 = (NodeInfo) e1.getValue();
            NodeInfo n2 = (NodeInfo) e2.getValue();

            // reverse finished order
            int ret = n2.finished - n1.finished;
            if (ret == 0 && _subComp != null)
                ret = _subComp.compare(e1.getKey(), e2.getKey());
            return ret;
        }
    }

    /**
     *	List of node-to-nodeinfo entries that exposes just the nodes.
     */
    private static class NodeList
        extends AbstractList {

        private final Map.Entry[] _entries;

        public NodeList(Map.Entry[] entries) {
            _entries = entries;
        }

        public Object get(int idx) {
            return _entries[idx].getKey();
        }

        public int size() {
            return _entries.length;
		}
	}
}
