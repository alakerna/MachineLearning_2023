# Project 4: RDDL and Monte-Carlo Tree Search (MCTS)
---

RDDL has been covered during the lecture, but you will find the resources below useful for completing this project.

#### RDDL Resources:

* [The original RDDL repository](https://github.com/ssanner/rddlsim)
* [RDDL Language Guide](http://users.cecs.anu.edu.au/~ssanner/IPPC_2011/RDDL.pdf)
* RDDL Tutorial Slides uploaded on Quercus
* [RDDL Tutorial Website](https://sites.google.com/site/rddltutorial/) -- a step-by-step guide to building the Wildfire domain, simulating it in RDDLSim, and using the [PROST](https://bitbucket.org/tkeller/prost/wiki/Home) planner with it.

The mark allocation for this project is as follows:

- Question 1: 2.5 pts
- Question 2: 7.5 pts
- Total: 10 pts

## [2.5 pts] Question 1. Design an ambulance domain in RDDL

#### Problem Description: the Ambulance Domain

There is a network of roads and locations where some locations are hospitals. 
For simplicity, assume a simple (n x m) grid of locations. 
Other than where a hospital is, emergency calls randomly occur at each location specified by its x and y position. 
Each location can have one emergency call at the most at a given time step. 
When there is no active call at a location, the arrival of a new emergency call 
follows the Bernoulli distribution with some location-dependent probability. 

One or more ambulances move along this road network. 
When an empty ambulance arrives at a location with an active emergency call, 
the ambulance can *choose* to pick up the patient (assume that a single ambulance can carry only one patient at a time).
Once an ambulance carrying a patient gets to a hospital, 
the patient is automatically dropped and 
the ambulance is freed up for taking in another patient from other locations. 

Ambulances can move to any of four directions on the grid network 
(i.e., east, west, north, and south), which comprises four out of five available actions. 
The last action is to pick up a patient (regardless of whether there is one).

There is a large negative reward for unanswered emergency calls in the network 
and a small negative reward for patients on ambulances who have not made it to a hospital yet.

**[Question 1]** Complete the *ambulance_mdp.rddl* file in [files/mie369_project4/rddl/](files/mie369_project4/rddl), 
which encodes the ambulance domain of any size (any number of hospitals, locations, and ambulances).

In the [ambulance_mdp.rddl](files/mie369_project4/rddl/ambulance_mdp.rddl) file, 
you are given the names of non-fluents, state-fluents, and action-fluents. 
Use this as a template (don't modify names or types) and complete the following:

<ol start="1">
  <li> The cpfs block</li>
  <br>
  In this block, you should complete the next state transitions 
  depending on the current state and action. 
</ol>

<ol start="2">
  <li> The reward function</li>
  <br>
  In this part (located just below the cpfs block), 
  you should specify the reward function given the current state.  
</ol>

<ol start="3">
  <li> The state-action-constraints block</li>
  <br>
  In this part (located below the reward definition), 
  add any action preconditions as necessary.  
</ol>

**Note 1**: all non-fluents and fluents used in this example are boolean variables. 
Do not attempt to modify the types of these variables.

**Note 2**: when more than one ambulances with no patients arrive at the same location with an emergency call,
only a single ambulance should respond to the call. You can use the  _state-action-constraints_  block to enforce this condition.
Take a look at the solution of the PacPeople domain for reference.  

**Note 3**: an ambulance stays at the same location if it chooses to move towards outside of the given map of locations. 
For example, if an ambulance is at the far-right edge of the map, it will not move if it chooses the *moveEast* action.

**Note 4**: an ambulance can take only one action at a time. This should be enforced in the  *state-action-constraints* block as well.  

**[Grading]**

This question will not be auto-graded. 
We will go through the rddl file to check if the domain is encoded correctly. 
We may use the animation to double-check your encoding.

**[Animating the simulation]**

Once you complete the ambulance_mdp.rddl file, 
you need to write an instance file to run a simulation. 
We have already provided some examples in [ambulance_inst.rddl](files/mie369_project4/rddl/ambulance_inst.rddl), 
so you can consult the file to see how you can define a specific instance of the domain. 
Note that when you define the *location* non-fluents, 
you should **strictly** follow the naming convention used in the examples. 
That is, (1, 1) in the grid will be *(x1, y1)*, while (2, 1) will be *(x2, y1)*, and so on. 

With the domain file and an instance file, 
you should be able to run simulations while animating the progress. 
The arguments you need to give when running *rddl.sim.Simulator* on Eclipse to view the animation are: 

```
[path_to_directory_with_rddl_files] [policy] [instance_name] [display_class] [random_seed_for_simulation] [random_seed_for_policy]
```

To run on your command line tool, you simply need to add ./run rddl.sim.Simulator at the beginning:

```
./run rddl.sim.Simulator [directory_with_rddl_files] [policy] [instance_name] [display_class] [random_seed_for_simulation] [random_seed_for_policy]
```

For example, with the following arguments you can run the simulation of the 1 x 3 ambulance instance (assuming you have implemented the ambulance domain):

```
files/mie369_project4/rddl rddl.policy.RandomBoolPolicy ambulance_inst_1x3 rddl.viz.AmbulanceDisplay 0 0
```

Again, remember that you are running the simulation with rddl.sim.Simulator. 

## [7.5 pts] Question 2. Improve the MCTS solver and compete on the elevators domain

In this part of the project, you will make some enhancements to the provided MCTS planner and compete on the **Elevators domain** with your enhanced MCTS planner. The RDDL encoding of the domain can be found in [elevators_mdp.rddl](files/mie369_project4/rddl/elevators_mdp.rddl).

**In the file [my_mcts.txt](files/mie369_project4/my_mcts.txt), explain the rationale behind the enhancements you make for Q2(b) and Q2(c) and cite any papers you have consulted (the file is at [files/mie369_project4/](files/mie369_project4)).**

**[Running simulations with the MCTS solver]**

To run the handout MCTS code, run rddl.sim.Simulator with the following arguments on Eclipse:

```
files/mie369_project4/rddl rddl.solver.mdp.mcts.MCTS elevators_inst_mdp__5 rddl.viz.ElevatorDisplay
```

If you want to run the code from command, just prepend ./run rddl.sim.Simulator at the beginning:

```
./run rddl.sim.Simulator files/mie369_project4/rddl rddl.solver.mdp.mcts.MCTS elevators_inst_mdp__5 rddl.viz.ElevatorDisplay
```

The instance file you are going to use is [elevators_inst_mdp__5](files/mie369_project4/rddl/elevators_inst_mdp__5.rddl). 
As you implement Q2(a), (b), and (c), you will want to replace the default MCTS solver with your implemented solver
for simulation. For example, to use your CompetitiveMCTS solver, you can replace *rddl.solver.mdp.mcts.MCTS* with *rddl.solver.mdp.mcts.CompetitiveMCTS*
in the above command.

(a) **[0.5 pt]** Rollout policy.

Firstly, implement the rollout policy in the [RolloutPolicy](src/rddl/solver/mdp/mcts/RolloutPolicy.java) class file. 
The rollout policy is a policy in which you only take one action selection via a tree policy 
(e.g. UCB1 as in UCT), followed by random rollouts until the end of the horizon or some specific steps. 
The correct implementation will get you the full point (it's either 0 or 0.5).

(Hint: By examining the provided [MCTS](src/rddl/solver/mdp/mcts/MCTS.java) class, 
you will be able to see that you can implement the rollout policy by simply adjusting 
a parameter given to the constructor of the RolloutPolicy class.)

(b) **[2.5 pts]** Selection strategy

Implement your own selection strategy in [MySelectionMCTS](src/rddl/solver/mdp/mcts/MySelectionMCTS.java).
You can copy-paste the code in MCTS.java and update parts of the code to implement your chosen strategy. 
Alternatively, you can choose to simply override the *evaluateStateActionNode* method. 

Either way, you need to make sure that we can use your MySelectionMCTS class as the policy for running a simulation. 
Also, note that you should handle the case when (greedy = true) separately in the method. 

You will be graded for what you describe in [my_mcts.txt](files/mie369_project4/my_mcts.txt) 
and what you have actually implemented. A clear explanation of the implemented method with 
proper citations will get the full points. Points will be deducted for mismatches between 
the explanation and the implementation (we will manually grade). 

(c) **[2.5 pts]** Back-propagation strategy

Implement your own backpropagation strategy in [MyBackpropMCTS](src/rddl/solver/mdp/mcts/MyBackpropMCTS.java).
You can copy-paste the code in MCTS.java and update parts of the code to implement your chosen backpropagation strategy.
Alternatively, you can choose to simply override the *backPropagate* method.

Either way, you need to make sure that we can use your MyBackpropMCTS class as the policy for running a simulation.

You will be graded for what you describe in [my_mcts.txt](files/mie369_project4/my_mcts.txt) 
and what you have actually implemented. A clear explanation of the implemented method with 
proper citations will get the full points. Points will be deducted for mismatches between 
the explanation and the implementation. 

(d) **[2pts]** Competitive portion

Make your MCTS planner beat others' on the Elevators domain! 
The instance file we are going to use for the competition is 
[files/mie369_project4/rddl/elevators_inst_mdp__5.rddl](files/mie369_project4/rddl/elevators_inst_mdp__5.rddl). 
In this Elevator instance, there are 2 elevators moving up and down in a 5-floor building. 
The horizon of this instance is set to 40, and the total time allocated to action selections 
in each simulation run is 10 seconds (the actual total running time will be longer). 

Put the selection and backpropagation strategies that you will use for this competitive 
portion into [CompetitiveMCTS.java](src/rddl/solver/mdp/mcts/CompetitiveMCTS.java).
You can choose to override some select methods of the MCTS class or copy-paste the whole file 
and make modifications as needed. Either way, make sure we can use your CompetitiveMCTS class as the policy
when running a simulation. 

You must also describe the improvements you made in [my_mcts.txt](files/mie369_project4/my_mcts.txt). 
If you make no changes from the given MCTS class then you will receive 0 points for this question.
  
As your score, we will use the average of the cumulative rewards obtained from 60 runs with different random seeds. 

Your competitive portion grade will be in the range [0, 2] scaled linearly with 0 points 
for obtaining the average cumulative reward that is smaller than or equal to *-200* 
(cf. the handout code produces roughly -180) and 2 points for getting the greatest average cumulative 
reward among correct submissions. 

To run this Elevators instance with your CompetitiveMCTS implementation, 
use the following arguments on Eclipse:

```
files/mie369_project4/rddl rddl.solver.mdp.mcts.CompetitiveMCTS elevators_inst_mdp__5 rddl.viz.ElevatorDisplay
```

You can also play with the [elevators_inst_mdp__3.rddl](files/mie369_project4/rddl/elevators_inst_mdp__3.rddl) file
which should run faster.

**Note 5**: your MCTS should also work with the ambulance domain of size up to 3 x 3. 
This size limitation is due to computational inefficiency in the compilation step of the RDDL instance.

**Note 6**: we have modified the original Elevators domain from a previous competition such that an agent receives 
(1) a positive reward for delivering people to their destination and 
(2) a negative reward for people waiting on floors. 

**Note 7**: in the current domain design, the agent receives a fixed reward when
an elevator delivers people to their destination, regardless of how many people are delivered. 
Hence, a reasonable MCTS policy may deliver people one by one,
rather than picking up a few people from different floors and delivering them as a group
in order to receive more rewards.  

**Note 8**: the MCTS algorithm has the anytime property. Thus, in other words, the longer it runs,
it generally returns a better plan. Therefore, for a fair evaluation, it is critical that the same
amount of total planning time is spent for each student submission. 
We enforce this by checking the total cumulative time spent on planning is capped from above by 10 seconds (or 10^10 nanoseconds).
Your CompetitiveMCTS class should implement the *getTimePerAction* method which returns how much time in nanoseconds
is allocated on planning for a single time step. The sum of the return values of this method over 40 horizon should be below
10^10 nanoseconds. Otherwise, you'll get 0 grade for the competitive portion. 

**Example resources you can consult:**

- [A Survey of Monte Carlo Tree Search Methods](http://www.incompleteideas.net/609%20dropbox/other%20readings%20and%20resources/MCTS-survey.pdf)
- [Using Monte Carlo Tree Search to Solve Planning Problems in Transportation Domains](http://www.ms.mff.cuni.cz/~truno7am/clanky/download/2013micai/paper.pdf)
- [Anytime Optimal MDP Planning with Trial-based Heuristic Tree Search](https://ai.dmi.unibas.ch/papers/keller-dissertation.pdf)
- [Trial-based Heuristic Tree Search for Finite Horizon MDPs](https://gki.informatik.uni-freiburg.de/papers/keller-helmert-icaps2013.pdf)
