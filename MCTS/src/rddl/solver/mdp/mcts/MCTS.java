package rddl.solver.mdp.mcts;

import java.util.*;
import java.util.concurrent.TimeUnit;

import dd.discrete.ADD;
import rddl.ActionGenerator;
import rddl.EvalException;
import rddl.RDDL.INSTANCE;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.State;
import rddl.policy.Policy;
import rddl.policy.SPerseusSPUDDPolicy;
import rddl.solver.DDUtils;
import rddl.solver.mdp.Action;
import rddl.translate.RDDL2Format;
import util.CString;
import util.Pair;

public class MCTS extends Policy {
	
	public final static boolean SHOW_STATE   = true;
	public final static boolean SHOW_ACTIONS = true;
	public final static boolean SHOW_ACTION_TAKEN = true;
	
	// Only for diagnostics, comment this out when evaluating
	public final static boolean DISPLAY_SPUDD_ADDS_GRAPHVIZ = false;
	public final static boolean DISPLAY_SPUDD_ADDS_TEXT = false;
	
	public RDDL2Format _translation = null;
	public INSTANCE rddlInstance = null;
	public String _logFileRoot = null; 
	
	// MCTS related variables
	public final static int TIME_SEC_TO_MILLISEC = 1000;
	public final static int TIMEOUT = (int) (10 * TIME_SEC_TO_MILLISEC); // Unit is in milliseconds
	public double _c = 80;
	public int _mctsHorizon = -1;
	public DecisionNode _rootNode;
	public int _nodeCounter = 0;
	
	// MDP related variables
	public double _discountFactor = 1.0;
	public int _remainingHorizons;
	public int _nHorizon;
	
	// Using CString wrapper to speedup hash lookups
	public ADD _context;
	public ArrayList<CString> _alStateVars;
	public ArrayList<CString> _alPrimeStateVars;
	public ArrayList<CString> _alActionNames;
	public HashMap<CString, Action> _hmActionName2Action; // Holds transition function
	public HashMap<Action, CString> _hmAction2ActionName;
	
	public MCTS() {}
	
	/**
	 * The default constructor.
	 */
	public MCTS(String instance_name) {
		super(instance_name);
	}
	
	/**
	 * The constructor which specifies the MCTS horizon.
	 */
	public MCTS(String instance_name, int mctsHorizon) {
		super(instance_name);
		_mctsHorizon = mctsHorizon;
	}
	
	/**
	 * This is the main action selection method. Given the current state s, run the MCTS algorithm
	 * and return the selected action in ArrayList<PVAR_INST_DEF> type. 
	 * Note that we only choose a single action which is the only element in the ArrayList. 
	 * The return type is set to that way just because RDDL can handle concurrent actions as well.
	 * 
	 * @param s			the current state
	 * @return 			the list of an action (but we don't consider concurrent actions)  
	 */
	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		if (s == null) {
			// This should only occur on the **first step** of a POMDP trial
			System.err.println("ERROR: NO STATE/OBS: MDP must have state.");
			System.exit(1);
		}
		
		// Get a set of all true state variables
		TreeSet<CString> true_vars = 
				CString.Convert2CString(SPerseusSPUDDPolicy.getTrueFluents(s, "states"));
		
		if (SHOW_STATE) {
			System.out.println("\n==============================================");
			System.out.println("\nTrue state variables:");
			for (CString prop_var : true_vars)
				System.out.println(" - " + prop_var);
		}
		
		// Get a map of { legal action names -> RDDL action definition }
		Map<String, ArrayList<PVAR_INST_DEF>> action_map = ActionGenerator.getLegalBoolActionMap(s);
		
		// Based on variables that are currently true, get a list of state-fluents (next state fluents will be null)
		ArrayList currStateEnumerated = DDUtils.ConvertTrueVars2DDAssign(_context, true_vars, _alStateVars);
		
		// Time allocated for MCTS at this horizon
		long timeout = getTimePerAction();
		
		// Run MCTS starting from the current state for timeout seconds
		Pair<Integer, Long> mctsResult = runMCTS(currStateEnumerated, timeout);
		int completedSearches = mctsResult._o1;					// number of completed searches
		long elapsedTime = mctsResult._o2;
		
		// Select the best action without the exploration term (greedy action selection)
		Pair<String, Double> result = getBestAction(_rootNode);
		String actionTaken = result._o1;
		double reward = result._o2;
		
		
		System.out.printf("Action: [%s] selected with reward [%.4f] after [%d] searches in [%.4f] seconds.", 
				actionTaken, reward, completedSearches, (double)elapsedTime / 1000);
		System.out.println();
		
		// Moving one time step forward
		this._remainingHorizons--;
		
		return action_map.get(actionTaken);
	}
	
	
	/**
	 * Runs MCTS iterations until reaching the timelimit
	 *  
	 * @param currState		the state from which MCTS is run
	 * @param timeLimit		time limit in seconds
	 */
	public Pair<Integer, Long> runMCTS(ArrayList<Boolean> currState, long timeLimit) {

		int completedSearches = 0;
		long startTime = System.nanoTime();
		long elapsedTime = 0;
		
		// MCTS horizon should be adjusted, reflecting currently remaining horizons
		_mctsHorizon = Math.max(1, Math.min(_mctsHorizon, _remainingHorizons));
		
		// Create the root node and reset the node counter
		_nodeCounter = 0;
		_rootNode = new DecisionNode(currState, _mctsHorizon, _remainingHorizons);
		
		// Run MCTS iterations until the time budget is depleted
		int i=0;
		do {
			runSingleMCTS(_rootNode);
			completedSearches++;
			elapsedTime = System.nanoTime() - startTime;
			i++;
			//elapsedTime < timeLimit
		} while (i<=60);
		
		return new Pair<Integer, Long>(completedSearches, TimeUnit.NANOSECONDS.toMillis(elapsedTime));
	}
	
	
	/**
	 * Runs a single iteration of MCTS:
	 * 	
	 * 	1) search: selection + expansion
	 * 	2) Simulation
	 * 	3) Backpropagation
	 * 
	 * @param currState		a list of booleans corresponding to the current state
	 */
	public void runSingleMCTS(DecisionNode node) {
		
		// Best-first search: selection and expansion
		DecisionNode leafNode 		 = search(node);
		
		// Simulation with the random rollout policy
		ArrayList<Boolean> leafState = leafNode.getEnumeratedBooleanStates();
		int numRollout 				 = Math.min(40, leafNode._nRemainingHorizons); 
		double cumReward 			 = 0;
		cumReward 					 = simulate(leafState, cumReward, numRollout, numRollout);
		
		// Backpropagation
		backPropagate(leafNode, cumReward);
	}
	
	/**
	 * The selection phase where a tree policy is used to select child nodes.
	 * A tree policy should balance exploration - exploitation. 
	 * One such policy is called UCT (Upper Confidence Bound applied to Trees).
	 * 
	 * @param node		a DecisionNode from which the MCTS search is recursively performed
	 */
	public DecisionNode search(DecisionNode node) {

		if (node._nRemainingMCTSHorizons == 0) {
			// Stop the tree search 
			return node;
			
		} else if (!node.isFullyExpanded()) {
			// Expand a node that is not fully expanded (not all actions have been selected)
			return expand(node);
			
		} else {
			// Othewise, use the tree policy to select a child node and recurse
			node = bestChild(node);
			return search(node);
		}
	}
	
	/**
	 * Returns the best child among all children of the current node. 
	 * The **best** can be chosen based on some utility function defined in the Utility class.
	 * As a default, we provide the UCT utility. 
	 * 
	 * @param node		a decision node whose child we are going to choose
	 * @return a next state decision node resulting from choosing the best action from node
	 */
	public DecisionNode bestChild(DecisionNode node) {
		DecisionNode nextStateNode;
		double utility;
		double maxUtility = -9e100;
		StateActionNode bestStateActionNode = null;
		
		// Loop through all children of the current DecisionNode to select the best child
		for (Map.Entry<CString, StateActionNode> me : node._hmActionName2Children.entrySet()) {
			StateActionNode child = me.getValue();
			
			// Evaluate the child node
			utility = evaluateStateActionNode(child);
			
			// Get the best child node
			if (utility > maxUtility) {
				maxUtility = utility;
				bestStateActionNode = child;
			}
		}
		
		if (bestStateActionNode == null) {
			System.out.println("Unexpected best node selection: no child selected!");
			System.exit(1);
		}
		
		// Given the best action, sample the next state and reward
		Pair<ArrayList<Boolean>, Double> nextStateAndReward = getNextStateAndReward(node.getEnumeratedBooleanStates(), 
																					bestStateActionNode.getAction());
		ArrayList<Boolean> nextState 	= nextStateAndReward._o1;
		double reward 					= nextStateAndReward._o2;
		
		// Action node stores the obtained reward on this trajectory
		bestStateActionNode.setReward(reward);
		
		// If next state is visited for the first time, create a new DecisionNode
		if (!bestStateActionNode.isNextStateAdded(nextState)) {
			nextStateNode = new DecisionNode(nextState, bestStateActionNode);
			
			// Add the next state node to the (state, action) node 
			bestStateActionNode.addNextState(nextState, nextStateNode);
			
		} else {
			// Otherwise, retrieve from cache to return the node
			nextStateNode = bestStateActionNode.getNextState(nextState);
		}
		
		return nextStateNode;
	}
	
	
	/**
	 * Expands a node that is not fully expanded by randomly selecting an action that hasn't been tried  
	 * 
	 * @param node		a node to expand
	 * @return			a resulting node
	 */
	public DecisionNode expand(DecisionNode node) {
		
		// Randomly select one action among the unexpanded actions
		CString action_name = node.getUnexpandedAction();
		Action action 		= _hmActionName2Action.get(action_name);
		
		// Retrieve the current boolean assignments to state
		ArrayList currStateAssign = node.getEnumeratedBooleanStates();
		
		// Sample the next state and reward given the current state and action
		Pair<ArrayList<Boolean>,Double> nextStateAndReward = getNextStateAndReward(currStateAssign, action);
		ArrayList<Boolean> nextState 			= (ArrayList<Boolean>)nextStateAndReward._o1;
		double reward 							= (double)nextStateAndReward._o2;
		
		// Create a StateActionNode and link it with the current node
		StateActionNode stateAndActionNode = new StateActionNode(nextState, action, reward, node);
		node._hmActionName2Children.put(action_name, stateAndActionNode);
		
		// Create a next state DecisionNode and link it with the StateActionNode
		DecisionNode nextStateNode = new DecisionNode(nextState, stateAndActionNode);
		stateAndActionNode.addNextState(nextState, nextStateNode);
		
		return nextStateNode;
	}
	
	/**
	 * Starting from a leaf node, simulate a trajectory for a pre-defined number of steps. 
	 * Note that states visited during the rollout phase are not updated (in fact, we don't even create
	 * TreeNode objects for those). 
	 * 
	 * @param currState 		the state of the leaf node from which the simulation starts
	 * @param cumReward			cumulative reward (initially 0 is given and accumulated as recursion proceeds)
	 * @param rolloutHorizons	the total number of rollout horizon
	 * @param remainingRolloutHorizons	currently remaining rollout horizon
	 * @return					the cumulative reward obtained via the simulation
	 */
	public double simulate(ArrayList<Boolean> currState, double cumReward, int rolloutHorizons, int remainingRolloutHorizons) {

		// End of simulation, return the cumulative reward
		if (remainingRolloutHorizons == 0) return cumReward;
	
		// Randomly select an action
		int index 			= _random.nextInt(0, _alActionNames.size() - 1);
		CString action_name = _alActionNames.get(index);
		Action action 		= _hmActionName2Action.get(action_name);
		
		// Sample the next state and reward
		Pair<ArrayList<Boolean>, Double> nextStateAndReward = getNextStateAndReward(currState, action);
		ArrayList<Boolean> nextState 						= nextStateAndReward._o1;
		double reward 										= (double)nextStateAndReward._o2;
		
		// Compute the discounted cumulative reward starting from the leaf node of rollout
		cumReward += Math.pow(_discountFactor, rolloutHorizons - remainingRolloutHorizons) * reward;
		
		// Recurse from the next state
		return simulate(nextState, cumReward, rolloutHorizons, remainingRolloutHorizons-1);
	}
	
	
	/**
	 * Backpropagates the cumulative reward from a rollout trajectory to ancestral nodes.
	 * 
	 * @param node				the currently updated node (intially, a leaf node) 
	 * @param cumRewardFromLeaf	the cumulative reward to backpropagate (initially, the cumReward from simulation) 
	 */
	public void backPropagate(TreeNode node, double cumRewardFromLeaf) {
		
		// When having backpropagated all the way to the root node, end the process
		if (node == null) {
			return;
		}
		
		// Increase the visit count of the node
		node.increaseVisitCount();
		
		// Add the cumulative reward from the leaf to the current node
		if (node instanceof StateActionNode) {
			StateActionNode saNode = (StateActionNode)node;			
			saNode._QVal += cumRewardFromLeaf;
		}
		
		// Recurse
		backPropagate(node.getParent(), cumRewardFromLeaf);
	}
	
	/**
	 * Evaluates a StateActionNode based on some utility function
	 */
	public double evaluateStateActionNode(StateActionNode node) {
		return evaluateStateActionNode(node, false);
	}
	
	/**
	 * Evaluates a StateActionNode based on some utility function. 
	 * When greedy = true, do not include the exploration bias term.
	 */
	public double evaluateStateActionNode(StateActionNode node, boolean greedy) {
		
		// Value of the node
		double valueTerm = node._QVal / node.getVisitCount() ;
		
		// When the best action should be chosen at the end of MCTS iterations
		if (greedy) {
			return (double)node.getVisitCount();
		}
		
		// Exploration bias from UCT
		double explorationTerm = Math.sqrt(Math.log((double)node.getParent().getVisitCount()) /node.getVisitCount());
		
		// UCT utility
		return valueTerm + _c * explorationTerm;
	}
	
	/**
	 * Given the current state and action, samples the next state and reward.
	 * 
	 * @param currState		an array list of booleans representing the current state 
	 * @param action		an action to be taken
	 * @return				a pair of next state and reward
	 */
	public Pair<ArrayList<Boolean>, Double> getNextStateAndReward(ArrayList<Boolean> currState, Action action){		

		// Initialize a list which will store the boolean values corresponding to the next state
		ArrayList<Boolean> nextStates = new ArrayList<Boolean>();
		for (int i = 0; i <  currState.size(); i++) {
			nextStates.add(null);
		}
		
		// Get the reward decision diagram associated with an action
		int reward_dd = action._reward;
		
		// Evaluate the reward by substituting current state boolean variables to the reward decision diagram
		double reward = _context.evaluate(reward_dd, currState);
		 
		// Evaluate (state, action) to get the next state
		for (Map.Entry<CString, Integer> me : action._hmStateVar2CPT.entrySet()) {
			
			// Clone the current state
			ArrayList clonedState = (ArrayList) currState.clone();
			
			// Get current and next state variable as Strings
			String nextStateVar = me.getKey()._string;
			String currStateVar = nextStateVar.substring(0, nextStateVar.length()-1);
			
			// Get the state transition decision diagram 
			Integer cpt_dd = me.getValue();
			
			// Get the level of current and next state variables
			Integer currStateVarId = (Integer)_context._hmVarName2ID.get(currStateVar);
			Integer currStateLevel = (Integer)_context._hmGVarToLevel.get(currStateVarId);
			Integer nextStateVarId = (Integer)_context._hmVarName2ID.get(nextStateVar);
			Integer nextStateLevel = (Integer)_context._hmGVarToLevel.get(nextStateVarId);
	
			// Get the probability of next state being true
			clonedState.set(nextStateLevel, true);
			double probNextStateTrue = _context.evaluate(cpt_dd, clonedState);
			
			// Sample a Bernoulli random variable which corresponds to next state 
			double r = _random.nextUniform(0, 1);
			if (r < probNextStateTrue) {
				nextStates.set(currStateLevel, true);
			} else {
				nextStates.set(currStateLevel, false);
			}
		}
		return new Pair<ArrayList<Boolean>, Double>(nextStates, reward);
	}
		
	
	/**
	 * After running MCTS, get the best action at a given state
	 * 
	 * @param node		a DecisionNode associated with the current state (root node)
	 * @return
	 */
	public Pair<String, Double> getBestAction(DecisionNode node) {
		
		double maxQVal = - 9e100;
		String argmaxAction = null;
		
		// Loop through all actions to get the best one
		for (Map.Entry<CString, StateActionNode> me : node._hmActionName2Children.entrySet()) {
			
			String action_name 		  = me.getKey()._string;
			StateActionNode childNode = me.getValue();
			
			// Evaluate the action node (greedily)
			double actionValue = evaluateStateActionNode(childNode, true);
			
			// Get the best action value and the associated action
			if (actionValue > maxQVal) {
				maxQVal = actionValue;
				argmaxAction = action_name;
			}
		}
		return new Pair<String, Double>(argmaxAction, maxQVal);
	}
	
	
	/**
	 * Gets the amount of time to execute a single action.
	 * The total amount of time for the entire horizon is set by TIMEOUT. 
	 * MCTS search will be called at each horizon, so distribute the time for search accordingly.
	 * 
	 * You can modify this function in your implementations, provided that the sum is TIMEOUT. 
	 * Otherwise, you'll get 0 mark for the competitive portion. 
	 */
	public long getTimePerAction() {
		int t = this._remainingHorizons;
		int n = this._nHorizon;
		double toNano = TIMEOUT * Math.pow(10, 6);
		
		double s = n * (n+1) * (2*n + 1) / 6;
		double timePortion = t * t / s;
		
		return (long) (toNano * timePortion);
	}
	
	
	/**
	 * Overrides the roundInit method of Policy class. This method is called at the very start of  
	 * simulation runs.
	 * 
	 * @param time_left			Set at Double.MAX_VALUE in Simulator.java
	 * @param horizon			Number of horizon
	 * @param round_number		Just set at 1 in Simulator.java
	 * @param total_rounds		Just set at 1 in Simulator.java
	 */
	@Override
	public void roundInit(double time_left, int horizon, int round_number, int total_rounds) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> ROUND INIT " + round_number + "/" + total_rounds + "; time remaining = " + time_left + ", horizon = " + horizon);
		System.out.println("*********************************************************");
		
		// Set up the rddl instance & file name for visualizing a graph
		this.rddlInstance = _rddl._tmInstanceNodes.get(_sInstanceName);
		
		// Set the horizons
		this._remainingHorizons = horizon;
		this._nHorizon 			= horizon;
		
		// Adjust the MCTS horizon accordingly
		if (this._mctsHorizon > this._nHorizon | this._mctsHorizon == -1) this._mctsHorizon = this._nHorizon;
		
		// Set the discount factor
		this._discountFactor = this.rddlInstance._dDiscount;
		
		// Build ADDs for transition, reward and value function (if not already built)
		if (_translation == null) {
			
			// Use RDDL2Format to build SPUDD ADD translation of _sInstanceName
			try {
				_translation = new RDDL2Format(_rddl, _sInstanceName, RDDL2Format.SPUDD_CURR, "");
			} catch (Exception e) {
				System.err.println("Could not construct MDP for: " + _sInstanceName + "\n" + e);
				e.printStackTrace(System.err);
				System.exit(1);
			}
			
			// Get ADD context and initialize value function ADD
			_context = _translation._context;
			
			// Get the state var and action names
			_alStateVars = new ArrayList<CString>();
			_alPrimeStateVars = new ArrayList<CString>();
			for (String s : _translation._alStateVars) {
				_alStateVars.add(new CString(s));
				_alPrimeStateVars.add(new CString(s + ""));
			}
			
			_alActionNames = new ArrayList<CString>();
			for (String a : _translation._hmActionMap.keySet())
				_alActionNames.add(new CString(a));
			
			// Now extract the reward and transition ADDs
			_hmActionName2Action = new HashMap<CString, Action>();
			_hmAction2ActionName = new HashMap<Action, CString>();			
			for (String a : _translation._hmActionMap.keySet()) {
				HashMap<CString, Integer> cpts = new HashMap<CString, Integer>();
				int reward = _context.getConstantNode(0d);
				
				// Build reward from additive decomposition
				ArrayList<Integer> reward_summands = _translation._act2rewardDD.get(a);
				for (int summand : reward_summands)
					reward = _context.applyInt(reward, summand, ADD.ARITH_SUM);
				
				// Build CPTs
				for (String s : _translation._alStateVars) {
					int dd = _translation._var2transDD.get(new Pair(a, s));
					
					int dd_true = _context.getVarNode(s + "'", 0d, 1d);
					dd_true = _context.applyInt(dd_true, dd, ADD.ARITH_PROD);
					
					int dd_false = _context.getVarNode(s + "'",  1d, 0d);
					
					int one_minus_dd = _context.applyInt(_context.getConstantNode(1d), dd, ADD.ARITH_MINUS);
					dd_false = _context.applyInt(dd_false, one_minus_dd, ADD.ARITH_PROD);
					
					// Now have "dual action diagram" cpt DD
					int cpt = _context.applyInt(dd_true, dd_false, ADD.ARITH_SUM);
					
					cpts.put(new CString(s + "'"), cpt);
				}
				
				// Build Action and add to HashMap
				CString action_name = new CString(a);
				Action action = new Action(_context, action_name, cpts, reward);
				_hmActionName2Action.put(action_name, action);
				_hmAction2ActionName.put(action, action_name);
			}
			
			// Display ADDs on terminal?
			if (DISPLAY_SPUDD_ADDS_TEXT) {
				System.out.println("State variables: " + _alStateVars);
				System.out.println("Action names: " + _alActionNames);
				
				for (CString a : _alActionNames) {
					Action action = _hmActionName2Action.get(a);
					System.out.println("Content of action '" + a + "'\n" + action);
				}
			}
		}
	}
	
	
	public void roundEnd(double reward) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> ROUND END, reward = " + reward);
		System.out.println("*********************************************************");
	}

	
	/**
	 * A tree node abstract class. Due to stochastic transitions, taking an action a at state s can lead to
	 * different child nodes with different states. Hence, we need to branch out for all possible next states
	 * given a (s, a) pair.
	 * 
	 * To this end, we have two kinds of TreeNodes. One that is linked to a specific state and the other that
	 * is associated with a specific (state, action) pair. We call the first type as DecisionNode because
	 * from the node, we can select an action among possible actions. 
	 * The second type is called StateActionNode. The children of this node are the next state DecisionNodes 
	 * that are sampled by taking an action at a state.  
	 * 
	 */
	public abstract class TreeNode {
		
		public TreeNode _parent;								// the parent node
		public ArrayList<Boolean> _enumeratedBooleanStates;		// state associated with the node
		public int _nRemainingMCTSHorizons;						// remaining MCTS horizon
		public int _nRemainingHorizons;							// remaining horizon until the end of the horizon
		public int _nVisitCount; 								// how many times this node has been visited during search
		public int _id;											// unique id
		
		// An empty constructor
		public TreeNode() {}
		
		public TreeNode(int mctsHorizons, int remainingHorizons) {
			this._nRemainingMCTSHorizons = mctsHorizons;
			this._nRemainingHorizons = remainingHorizons;
			this._id = _nodeCounter;
			_nodeCounter++;
		}

		/**
		 * Returns a copied list of enumerated boolean states
		 */
		public ArrayList<Boolean> getEnumeratedBooleanStates(){
			ArrayList<Boolean> copiedState = new ArrayList<Boolean>();
			for (Boolean s : _enumeratedBooleanStates) {
				copiedState.add(s);
			}
			return copiedState;
		}
				
		/**
		 * Returns the parent TreeNode of this node. 
		 * For a StateActionNode, the returned node will be a DecisionNode, and vice versa.
		 */
		public TreeNode getParent() {
			return _parent;
		}
		
		/**
		 * Returns the number of visits to this node
		 */
		public int getVisitCount() {
			return _nVisitCount;
		}
		
		/**
		 * Increments the visit count by 1 when this node is selected during backpropagation
		 */
		public void increaseVisitCount() {
			_nVisitCount++;
		}
		
		/**
		 * Returns the id of this node
		 * @return
		 */
		public int getId() {
			return _id; 
		}
	}
	
	
	
	public class DecisionNode extends TreeNode {
		
		// State value function associated with this node
		public double _value = 0; 
		
		// Child StateActionNodes 
		public HashMap<CString, StateActionNode> _hmActionName2Children;
		
		public ArrayList<CString> _expandedActions;
		public ArrayList<CString> _unexpandedActions;
		
		/**
		 * Constructor for the root decision node. 
		 * Note that the MCTS horizon is set when instantiating the root node.
		 * 				Remaining horizon
		 *    |<------------->|<---------------------->|
		 *      MCTS horizon        	Rollout
		 *      
		 * The MCTS selection phase is done until the mctsHorizon or until a not-fully-expanded node is reached.
		 * From there, the rollout phase starts. 
		 *  
		 * @param states			a list of enumerated boolean states
		 * @param mctsHorizons
		 * @param remainngHorizons   
		 */
		public DecisionNode(ArrayList<Boolean> states, int mctsHorizons, int remainngHorizons) {
			
			super(mctsHorizons, remainngHorizons);
			this._enumeratedBooleanStates = states;
			
			// Root node has no parent
			this._parent = null;
			
			// Add all actions to the unexpanded actions, while expandedActions is an empty list
			this._expandedActions 	= new ArrayList<CString>();
			this._unexpandedActions = new ArrayList<CString>();
			for (int i = 0; i < _alActionNames.size(); i++) {
				this._unexpandedActions.add(_alActionNames.get(i));
			}
			
			// Initialize the hashmap: { action name -> child nodes }
			_hmActionName2Children = new HashMap<CString, StateActionNode>();
		}
		
		/**
		 * Constructor for non-root nodes
		 * 
		 * @param states	a list of enumerated boolean states
		 * @param parent	the parent TreeNode (should be StateActionNode)
		 */
		public DecisionNode(ArrayList<Boolean> states, TreeNode parent) {
			
			// Decrease the remaining horizons by 1 compared to this node's parent
			super(parent._nRemainingMCTSHorizons - 1, parent._nRemainingHorizons - 1);
			
			this._parent = parent;
			this._enumeratedBooleanStates = states;
						
			// Add all actions to the unexpanded actions, while expandedActions is an empty list
			this._expandedActions 	= new ArrayList<CString>();
			this._unexpandedActions = new ArrayList<CString>();
			for (int i = 0; i < _alActionNames.size(); i++) {
				this._unexpandedActions.add(_alActionNames.get(i));
			}

			// Initialize the hashmap: { action name -> child nodes }
			_hmActionName2Children = new HashMap<CString, StateActionNode>();
		}
		
		/**
		 * The constructor that should only be called when creating a temporary TreeNode for simulation. 
		 * @param states
		 */
		public DecisionNode(ArrayList<Boolean> states) {
			this._enumeratedBooleanStates = states;
			this._nRemainingMCTSHorizons  = 0;
			this._nRemainingHorizons 	  = -1;
		}
		
		/**
		 * Returns true if all of the actions have been tried (expanded).
		 */
		public boolean isFullyExpanded() {
			return (_unexpandedActions.size() == 0);
		}
		
		/**
		 * Returns one of unexpanded actions and removes it from the list  
		 */
		public CString getUnexpandedAction() {
			int index;
			
			// If only a single action is left to be tried
			if (_unexpandedActions.size() == 1) {
				index = 0; 
			} else {
				index = _random.nextInt(0, _unexpandedActions.size()-1);
			}
			
			// Get the action name, remove from unexpanded actions, and add to expanded actions
			CString action = _unexpandedActions.get(index);
			_unexpandedActions.remove(index);
			_expandedActions.add(action);
			return action;
		}

				
		public String toString() {
			String ret = String.format("Decision node: [ %d ], parent=%d, count=%d", getParent()._id, _id, _nVisitCount);
			return ret;
		}
	}
	
	public class StateActionNode extends TreeNode {
		
		// The action associated with this node
		public Action _action;
		
		// State-action value
		public double _QVal = 0;
		
		// Reward associated with this node; whenever a next state and reward are sampled, this reward is updated 
		public double _reward = 0;
		
		// Next state children 
		public HashMap<ArrayList<Boolean>, DecisionNode> _hmNextState2NextStateNode; 
		
		/**
		 * 
		 * @param states
		 * @param action
		 * @param reward
		 * @param parent
		 */
		public StateActionNode(ArrayList<Boolean> states, Action action, double reward, DecisionNode parent) {

			// StateActionNode has the same horizon as its parent DecisionNode 
			super(parent._nRemainingMCTSHorizons, parent._nRemainingHorizons);
			this._parent = (TreeNode)parent;
			this._action = action;
			this._reward = reward;
			this._enumeratedBooleanStates = states;
			
			// Initialize the hashmap of next state children: { next state as boolean list -> next state decision node }
			this._hmNextState2NextStateNode = new HashMap<ArrayList<Boolean>, DecisionNode>();
		}
		
		
		/**
		 * Returns the associated Action object
		 */
		public Action getAction() {
			return _action;
		}
		
		
		/**
		 * Sets the reward after sampling a next state and reward from the (state, action) pair associated with this node
		 */
		public void setReward(double reward) {
			_reward = reward;
		}
		
		
		/**
		 * Adds the next state node to hashmap. The boolean list of next state is the key that is mapped to 
		 * the corresponding next state decision node.
		 */
		public void addNextState(ArrayList<Boolean> nextState, DecisionNode nextStateNode) {
			if (!_hmNextState2NextStateNode.containsKey(nextState)) {
				_hmNextState2NextStateNode.put(nextState, nextStateNode);
			} 
		}
		
		/*
		 * Checks if nextState has already been sampled from this (state, action) node
		 */
		public boolean isNextStateAdded(ArrayList<Boolean> nextState) {
			return _hmNextState2NextStateNode.containsKey(nextState);
		}
		
		/**
		 * Returns the next state decision node linked to the boolean list of next state 
		 */
		public DecisionNode getNextState(ArrayList<Boolean> nextState) {
			return _hmNextState2NextStateNode.get(nextState);
		}
		
		/**
		 * Updates the q value
		 */
		public void setQValue(double q) {
			_QVal = q;
		}
		
		/**
		 * Returns the Q value
		 */
		public double getQValue() {
			return _QVal;
		}
		

		public String toString() {
			String ret = String.format("(S,A) node: [ %d ], parent=%d, count=%d", getParent()._id, _id, _nVisitCount);
			return ret;
		}
	}
}
