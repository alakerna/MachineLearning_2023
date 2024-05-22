package rddl.solver.mdp.mcts;

/**
 * This class will be used for the competitive portion of the project.
 * Implement the best working selection and backpropate methods and explain in my_mcts.txt.
 * You can either override the two methods given below,
 * or copy-paste the whole code from the MCTS class and make modifications as you want. 
 * Either way, make sure that we can run simulations using this class as a policy for evaluation.
 * 
 * Note: we'll use the Elevators domain with 2 elevators and 5 floors for grading.
 */
public class CompetitiveMCTS extends MCTS {

	public final static double gamma_const = 0.9;

	public CompetitiveMCTS(String instance_name) {
		super(instance_name);
	}
	
	/**
	 * Returns the utility of the given StateActionNode for action selection.  
	 */
	@Override
	public double evaluateStateActionNode(StateActionNode node, boolean greedy) {
		/**
		 * TODO: implement your chosen tree policy
		 */

		 double valueTerm = node._QVal / node.getVisitCount() ;
		
		 // When the best action should be chosen at the end of MCTS iterations
		 if (greedy) {
			 return (double)node.getVisitCount();
		 }
		 
		 //double standDevTerm = Math.sqrt( /node.get)
		 // Exploration bias from UCT
		 double explorationTerm = Math.sqrt(Math.log((double)node.getParent().getVisitCount()) /node.getVisitCount());
		 // progressive widening on the _c var
		 double pw = _c * Math.pow(node.getVisitCount(), 0.2);
 
		 // UCT utility
		return valueTerm + pw * explorationTerm;
	}
	
	
	/**
	 * Backpropagates the cumulative reward from a rollout trajectory to ancestral nodes.
	 * 
	 * @param node				the currently updated node (initially, a leaf node) 
	 * @param cumRewardFromLeaf	the cumulative reward to backpropagate (initially, the cumReward from simulation) 
	 */
	@Override
	public void backPropagate(TreeNode node, double cumRewardFromLeaf) {
		/**
		 * TODO: implement your chosen back-up strategy
		 */

				/**
		 * TODO: implement your chosen back-up strategy (note: you need to explain your rationale in /files/mie369_project4/mymcts.txt)
		 */
		// When having backpropagated all the way to the root node, end the process
		if (node == null) {
			return;
		}
		
		// Increase the visit count of the node
		node.increaseVisitCount();
		
		// Add the cumulative reward from the leaf to the current node
		if (node instanceof StateActionNode) {
			StateActionNode saNode = (StateActionNode)node;			
			//saNode._QVal += cumRewardFromLeaf;
			saNode._QVal += gamma_const * cumRewardFromLeaf;
		}
		
		// Recurse
		backPropagate(node.getParent(), cumRewardFromLeaf);

	}
}
