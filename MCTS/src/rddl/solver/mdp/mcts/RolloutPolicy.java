package rddl.solver.mdp.mcts;

/**
 * TODO: implement the rollout policy 
 */
public class RolloutPolicy extends MCTS {
	
	public RolloutPolicy(String instance_name) {
		super(instance_name);
		_mctsHorizon = 1;
	}
}
