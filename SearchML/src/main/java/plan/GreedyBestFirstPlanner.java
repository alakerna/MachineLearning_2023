package plan;

import map.MapEdge;
import map.MapNode;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
/**
/**
 * A class defining planning using Best First Search
 */
public class GreedyBestFirstPlanner extends Planner {
    /**
     * heuristics used for Best First Search
     */
    Heuristic heuristic;

    /**
     * Initializer
     *
     * @param heuristic a heuristic object
     */
    public GreedyBestFirstPlanner(Heuristic heuristic) {
        super();
        //TODO
        this.heuristic = heuristic;
    }

    /**
     * Runs Best First Search
     *
     * @param startNode the start node
     * @param goalNode  the goal node
     * @return a list of MapNode objects
     */
    @Override
    public PlanResult plan(MapNode startNode, MapNode goalNode) {
        long start_time = System.currentTimeMillis();
        double start_hn = heuristic.getHeuristics(startNode, goalNode);
        hNodePair startPair = new hNodePair(start_hn, startNode);

        HashMap<MapNode, MapNode> parents = new HashMap<>();
        Set<MapNode> expandedNodes = new HashSet<>();

        PriorityQueue<hNodePair> queue = new PriorityQueue<>(new Comparator<hNodePair>()
        {
            @Override
            public int compare(hNodePair x, hNodePair y)
            {
                return Double.compare(x.h_n,y.h_n);
            }
        });
        
    
        parents.put(startNode, null);
        queue.add(startPair);

        while (!queue.isEmpty() && System.currentTimeMillis() - start_time < 300) {
            hNodePair currNode = queue.poll(); // edit based on new queue structure
            expandedNodes.add(currNode.node);
            
            if (currNode.node == goalNode) {
                long end_time = System.currentTimeMillis();
                long total_time = end_time - start_time;
                if (total_time >= 300) {
                    System.out.printf("DNT within 300 seconds");
                }
                else{
                    System.out.printf("Elapsed Time: %d \n", total_time);
                }
                return new PlanResult(expandedNodes.size(), getNodeList(parents, goalNode));
            }
            for (MapEdge edge : currNode.node.edges) {
                MapNode nextNode = edge.destinationNode;

                //TODO change this to your heuristic cost
                double h_n = heuristic.getHeuristics(nextNode, goalNode);
                hNodePair nextPair = new hNodePair(h_n, nextNode);
                //add (node,cost) to queue
                if (!parents.containsKey(nextNode)) {
                    parents.put(nextNode, currNode.node);
                    queue.add(nextPair);
                }
            }
        }
        long end_time = System.currentTimeMillis();
        long total_time = end_time - start_time;
        if (total_time >= 300) {
            System.out.printf("DNT within 300 seconds");
        }
        else{
            System.out.printf("Elapsed Time: %d \n", total_time);
        }
        return new PlanResult(expandedNodes.size(), null);
    }

    private static class hNodePair {
        double h_n;
        MapNode node;

        public hNodePair(double h_n, MapNode node) {
            this.h_n = h_n;
            this.node = node;
        }
    }
    /**
     * Gets the name of the planner
     *
     * @return planner name
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
