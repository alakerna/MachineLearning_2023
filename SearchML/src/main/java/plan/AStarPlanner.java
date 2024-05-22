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
 * A class defining planning using A* search
 */
public class AStarPlanner extends Planner {
    /**
     * heuristics used for A*
     */
    Heuristic heuristic;

    CostFunction costFunction;
    /**
     * Initializer
     *
     * @param heuristic a heuristic object
     * @param costFunction    cost function option
     */
    public AStarPlanner(Heuristic heuristic, CostFunction costFunction) {
        super();
        //TODO
        this.heuristic = heuristic;
        this.costFunction = costFunction;

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
        //start timer
        long start_time = System.currentTimeMillis();

        double start_fn = heuristic.getHeuristics(startNode, goalNode);

        costNodeTup startTup = new costNodeTup(0, startNode, start_fn);

        HashMap<MapNode, MapNode> parents = new HashMap<>();
        Set<MapNode> expandedNodes = new HashSet<>();
        PriorityQueue<costNodeTup> queue = new PriorityQueue<>(new Comparator<costNodeTup>()
        {
            @Override
            public int compare(costNodeTup x, costNodeTup y)
            {
                return Double.compare(x.fn,y.fn);
            }
        });
        
    
        parents.put(startNode, null);
        queue.add(startTup);

        while (!queue.isEmpty() && System.currentTimeMillis() - start_time < 300) {
            costNodeTup currNode = queue.poll(); // edit based on new queue structure
            expandedNodes.add(currNode.node);
            double cost = currNode.cost;
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
                
                double hn = heuristic.getHeuristics(nextNode, goalNode);
                double gn = costFunction.getCost(edge) + cost;
                double fn =  hn + gn;

                costNodeTup nextTup = new costNodeTup(gn, nextNode, fn);

                if (!parents.containsKey(nextNode)) {
                    parents.put(nextNode, currNode.node);
                    queue.add(nextTup);
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

    private static class costNodeTup {
        double cost;
        MapNode node;
        double fn;

        public costNodeTup(double cost, MapNode node, double fn) {
            this.cost = cost;
            this.node = node;
            this.fn  = fn;
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
