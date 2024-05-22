import map.Graph;
import map.MapNode;
import org.junit.Test;

import plan.*;

import map.Graph;
import map.MapEdge;
import map.MapNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Tests {
    @Test
    public void sampleTest() {
        
        String osmFile = "./data/toronto_full.osm";
        String cyclistsAccidentFile = "./data/Cyclists.csv";
        Graph torontoGraph = new Graph(osmFile, cyclistsAccidentFile);

        CostFunction cost = new CostFunctionAllFeatures(torontoGraph);
        Heuristic heuristic = new AStarHeuristic(torontoGraph);

        Planner bfsPlanner = new BFSPlanner();
        Planner astar = new AStarPlanner(heuristic, cost);
        Planner ucs = new UniformCostPlanner(cost);

        //Manually specify sourceNode and endNode
        //long sourceNodeId = 6374148719L;
        //astarcase0
        //long sourceNodeId = 1410863625L;
        //long endNodeId = 3932115351L;
        //ucscas6
        //, End node id: 6382089805
        long sourceNodeId = 3157174752L;
        long endNodeId = 6382089805L;

        //long endNodeId = 6374051128L;
        MapNode sourceNode = torontoGraph.nodes.get(sourceNodeId);
        MapNode endNode = torontoGraph.nodes.get(endNodeId);

        //List<MapNode> nodeList = bfsPlanner.plan(sourceNode, endNode).path;
       // List<MapNode> nodeList = astar.plan(sourceNode, endNode).path;
        List<MapNode> ucsnodeList = ucs.plan(sourceNode, endNode).path;

        List<Long> actual = new ArrayList<>();
        for (MapNode node : ucsnodeList) {
            actual.add(node.id);
        }
        
        double path_cost = getPathCost(cost, ucsnodeList);
        //astar case0
        //assertEquals(5144.00, path_cost, 0);
        // ucs test case 6: 9396.000000
        assertEquals(9396.000000, path_cost, 0);


        
        // List<Long> expected = Arrays.asList(6374148719L, 6662926503L, 389678174L, 389678175L, 1480794735L, 389678176L,
        //         3983181527L, 3983181528L, 389678212L, 389678213L, 389678214L, 389678215L, 389678216L, 7311057931L,
        //         389678220L, 389678221L, 389678222L, 389677908L, 749952029L, 389677909L, 389677910L,
        //         389677911L, 389677912L, 391186184L, 389677913L, 389677914L, 6374051128L);
        // getPathCost(new CostFunctionAllFeatures(torontoGraph), result.path)
        //long expectedCost = nodeList[0].getPathCost(cost, nodeList[0].path);
       // assertEquals(expected, actual);
    }
    public static double getPathCost(CostFunction costFunction, List<MapNode> path) {
        double cost = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            MapNode curNode = path.get(i);
            MapNode nextNode = path.get(i + 1);
            System.out.printf("Current Node %d, Cost so far: %f \n", curNode.id, cost);
            for (MapEdge edge : curNode.edges) {
                if (edge.destinationNode.id == nextNode.id) {
                    System.out.printf("\tchoosing cost: %f\n", costFunction.getCost(edge));
                    cost += costFunction.getCost(edge);
                    
                }
            }
        }
        return cost;
    }
}
