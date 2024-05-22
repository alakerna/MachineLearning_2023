package rddl.solver.mdp.mcts;

public class MySelectionMCTS extends MCTS {

	/**
	 * You can either override the evaluateStateActionNode method given below,
	 * or copy-paste the whole code from the MCTS class and make modifications as you want. 
	 * Either way, make sure that we can run simulations using this class as a policy.
	 */
	public MySelectionMCTS(String instance_name) {
		super(instance_name);
	}
	
	/**
	 * Evaluates a StateActionNode based on some utility function. 
	 * When greedy = true, do not the exploration bias.
	 */
	@Override
	public double evaluateStateActionNode(StateActionNode node, boolean greedy) {
		/**
		 * TODO: implement your chosen tree policy (note: you need to explain your rationale in /files/mie369_project4/mymcts.txt)
		 */
		// Value of the node
		double valueTerm = node._QVal / node.getVisitCount() ;
		
		// When the best action should be chosen at the end of MCTS iterations
		if (greedy) {
			return (double)node.getVisitCount();
		}
		
		//double standDevTerm = Math.sqrt( /node.get)
		// Exploration bias from UCT
		double explorationTerm = Math.sqrt(Math.log((double)node.getParent().getVisitCount()) /node.getVisitCount());
		// progressive widening on the _c var
		double pw = _c * Math.pow(node.getVisitCount(), 0.3);

		// UCT utility
		return valueTerm + pw * explorationTerm;

	}	
}
		
	
