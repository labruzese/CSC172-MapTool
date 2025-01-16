# Map Tool
This project implements a street mapping system using Java.
Featuring both a graphical interface and an interactive console. 
* [Components](https://github.com/labruzese/CSC172-MapTool/new/master?filename=README.md#components)
* [Features](https://github.com/labruzese/CSC172-MapTool/new/master?filename=README.md#features)
* [A* Algorithm](https://github.com/labruzese/CSC172-MapTool/new/master?filename=README.md#a-algorithm)
* [Interactive Console](https://github.com/labruzese/CSC172-MapTool/new/master?filename=README.md#interactive-console)
* [How to Run](https://github.com/labruzese/CSC172-MapTool/new/master?filename=README.md#how-to-run)

# Components
## Graph Implementation
* Custom Graph interface with ALGraph (Adjacency List) implementation
* Vertex class (Intersection) and Edge class (Road) for representing map elements
* Support for weighted edges and Dijkstra's/A* pathfinding
## Data Structures
* Custom HashTable implementation using chaining
* IndexedPriorityQueue for efficient Dijkstra's algorithm
* Custom PriorityQueue implementation
## UI Components
* MapPanel for visualization using Java AWT
* Interactive console with command handling
* Support for map navigation and path highlighting

# Features
## Adjacency List Graph
Having previously implemented a similar project in high school using an adjacency matrix, I opted for an adjacency list representation this time. 
This choice significantly improves the runtime complexity for Dijkstra's algorithm, especially for sparse graphs like street networks. 
The adjacency list provides O(V + E) space complexity compared to O(V^2) for an adjacency matrix, and more efficient edge traversal during pathfinding.
## Buffered Graphics
The initial implementation re-rendered the entire graph for each pan or zoom operation, which was highly inefficient as it required recalculating and redrawing every vertex and edge. 
The current implementation uses a buffered approach:
* Maintains a larger buffer than the visible area
* Only redraws when the view approaches buffer boundaries
* Uses viewport calculations to display the correct portion of the buffer
* Implements smooth panning by moving the viewport window
* Scales the view by adjusting the viewport dimensions rather than redrawing
Of course when the viewport moves too far or zooms too much the buffered image in the background will redraw itself
# A* Algorithm
I initially was satisfied with Dijkstra's algorithm, but after testing a rather long path in new york, I wanted to try my hand at the A* algorithm as I haven't really worked with it before. 
To improve performance for long-distance routing, I implemented A* pathfinding as an optimization over Dijkstra's algorithm, it will only be used at over 200km. 
The initial implementation was suboptimal as it operated outside the graph structure, requiring frequent traversal operations. The improved version:
 * Integrates directly into the ALGraph class
 * Uses coordinate map(yes my own hashtable) to get the locations associated with each vertex
 * Employs the Haversine formula for distance estimation <- which I basically copied from Wikipedia I don't really know how it works that well
# Interactive Console
One of my favorite things that I got to do this project was the console interface. 
I didn't want you to have to rerun the java file every single time you wanted to do something and I also did not have the patience to get a proper side panel or anything to work within the GUI directly, so instead I have a console that can interact with the gui through kind of an api, basically just telling it to highlight things. 
As such you can type in commands to the console as follows:

```java Console map.txt [--show] [--directions startIntersection endIntersection]```

`--show` - will turn on the GUI and put you into interactive mode where you can do the following
`--directions` - will show the directions immediately before launching the interactive mode.

`search <intersection>` - Shows details about a specific intersection
	Latitude/longitude coordinates
	Connected intersections
	Adjacent roads and distances

`search road <roadID>` - Shows information about a specific road
	Connected intersections
	Total distance
	Location on map (when GUI is enabled)

`directions <start> <end>` - Shows path between two intersections
`directions <int1> <int2> <int3>...` - Creates a multi-stop route with:
  * Turn-by-turn navigation
  * Total distance
  * Estimated walking/driving times
  * Visual path on map (when GUI is enabled)

### Highlight Controls (Available after running a directions command)

`highlight <step>` - Highlights a specific step
`highlight <start>-<end>` - Highlights a range of steps
`highlight all` - Highlights entire path
`highlight clear` - Removes all highlights


From the GUI you can also right click on a spot and if you're within 10 pixels of a intersection or road it will run a search command in the console for that intersection or road. 
Perfect for if you have a general area you want to get a path between, you can see the intersection id about where your cursor is.

One other feature that was actually more easy than I thought to get working was the directions. 
I wanted specific directions like sharp-left and what not, I did do some research online beforehand, but it was very easy to debug and get working thankfully.

# HOW TO RUN
Clone Repository.

Compile the Java source files by running the following command **in the src directory**:

```javac abruzese/console/Console.java```

Run the Console program by executing the following command:

```java abruzese.console.Console <map.txt> [--show] [--directions startIntersection endIntersection]```

To run the default options you can use the main class instead
