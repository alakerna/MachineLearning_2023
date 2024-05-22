# Project 1: A* on OpenStreetMap for Cyclists

Original code developed by Yudong (William) Xu.

Route planning application on OpenStreetMap for cyclists.

## Questions (submission is via github, see syllabus for the due date)

* **[2 pts]** Implement an admissable AStarHeuristic.java (subclassing Heuristic.java) for CostFunctionAllFeatures.  Do not implement a 0 or constant heuristic -- it should be computed from properties of *both* the start and end nodes.
* **[4 pts]** Implement UniformCostPlanner.java (1 pt), GreedyBestFirstPlanner.java (1 pt), AStarPlanner.java (2 pts).  For simplicity, use the tree search (not graph search) version.  When a heuristic is needed, use your own AStarHeuristic.  All must subclass Planner.java so we can autograde them.
* **[2 pts]** Competitive part: assuming your AStar is correct, we will time it on 10 different start, end pairs and compute the average time.  Your competitive portion grade will be in the range [0,2] scaled linearly with 0 points for taking greater than or equal to the time of UniformCostSearch (uninformed) to 2 points for taking the minimum time of all student submissions and the instructor's solution.
* **[2 pts]** Compare BFSPlanner, DFSPlanner, UniformCostPlanner, GreedyBestFirstPlanner, and AStarPlanner for 5 pairs of start/end positions (you choose, but make sure they differ in distance from close to far) for "toronto_full.osm" in terms of running time (milliseconds), number of nodes expanded, and total path cost according to CostFunctionAllFeatures; the file should be cleanly formatted when viewing on github... align your columns and numbers.  You can choose a reasonable time out (e.g., 300 seconds) and report "DNT" if the algorithm did not terminate within that time.  You should choose **at least one start/end pair** close enough that **all** algorithms terminate within 300 seconds, though you should also choose some other start/end pairs that show significant differences in running time between the algorithms. Save your results to MyResults.txt in the root directory of the repo.

**Note 1:** Do not modify abstract classes or other classes we provide -- we rely on these exact class definitions for autograding.  You may add additional helper classes and you may add as many additional methods to your solution classes as you want.

**Note 2:** Make sure all of the above are viewable from your github repo URL on the web -- if you cannot see your files on the web at your github URL, we cannot see them either (i.e., remember to add new files to your repo when necessary, e.g., using "git add FILENAME").

## Data sources:
osm files can be exported from the official openStreetMap website
https://www.openstreetmap.org/export

Cycling safety data for toronto can be downloaded from https://data.torontopolice.on.ca/pages/traffic


## How to run:

Run *main.Demo.java* to start the application.

Within the application: 
* Use left mouse button to pan.
* Use mouse scroll wheel to zoom.
* Use right mouse button to set start/destination way point, and to clear existing way points.


## Map configuration options:

The application is set to run for a small snapshot of Toronto (near the U. of Toronto) in file "toronto.osm".
We have also included a much large map "toronto_full.osm", which is needed in the above assignment -- this will take
a few minutes to load.  To run for other OpenStreetMap locations in the world, 
download the area from the open street map website, put the .osm file under /data and 
change the osmFile variable in main method accordingly. 
(Note: the cyclist dataset is only relevant for toronto.)
