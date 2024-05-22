package plan;

import map.MapEdge;
import map.MapNode;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
/**
 * A class defining planning using Uniform Cost Search
 */
public class UniformCostPlanner extends Planner {
    /**
     * heuristics used for Uniform Cost Search
     */
    CostFunction costFunction;

    /**
     * Initializer
     *
     * @param costFunction a costFunction object
     */
    public UniformCostPlanner(CostFunction costFunction) {
        super();
        //TODO
        this.costFunction = costFunction;
    }
    /**
     * Runs Uniform Cost Search
     *
     * @param startNode the start node
     * @param goalNode  the goal node
     * @return a list of MapNode objects
     */


    @Override
    public PlanResult plan(MapNode startNode, MapNode goalNode) {
        //TODO
        long start_time = System.currentTimeMillis();
        CostNodePair startPair = new CostNodePair(0, startNode);
        HashMap<MapNode, MapNode> parents = new HashMap<>();
        Set<MapNode> expandedNodes = new HashSet<>();

        PriorityQueue<CostNodePair> queue = new PriorityQueue<>(new Comparator<CostNodePair>()
        {
            @Override
            public int compare(CostNodePair x, CostNodePair y)
            {
                return Double.compare(x.cost,y.cost);
            }
        });
        
    
        parents.put(startNode, null);
        queue.add(startPair);

        while (!queue.isEmpty() && System.currentTimeMillis() - start_time < 300) {
            CostNodePair node = queue.poll(); // edit based on new queue structure
            expandedNodes.add(node.node);

            double cost = node.cost;
            if (node.node == goalNode) {
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
            for (MapEdge edge : node.node.edges) {
                MapNode nextNode = edge.destinationNode;
                double totalCost = costFunction.getCost(edge) + cost;
            
                // if you've already visited the node that exists in parents (which means it exists in costParents)
                // then if we are visiting it with less cost, replace the node 
                if (!parents.containsKey(nextNode)) {
                    CostNodePair nextPair = new CostNodePair(totalCost, nextNode);
                    parents.put(nextNode, node.node);
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

    private static class CostNodePair {
        double cost;
        MapNode node;

        public CostNodePair(double cost, MapNode node) {
            this.cost = cost;
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
