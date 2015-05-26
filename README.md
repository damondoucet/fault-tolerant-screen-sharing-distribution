# Fault-Tolerant Screen-Sharing Distribution
6.824 Final Project -- A computer's screen is broadcasted through a fault-tolerant distribution network.

Modern screen-sharing applications fall into one of two patterns. One pattern is the broadcaster sending its screen to some distributer (e.g., Twitch or Azubu), which requires trusting that central authority. The other requires each client to open a connection to the broadcaster, a clearly non-scalable protocol that forces too much load onto the broadcaster's CPU and network and has no tolerance of network failures. This project solves the lack of fault tolerance and provides mechanisms for solving the performance issues as well.

Our goal is to maintain the invariant that as long as a client could have heard about another node that receives screen sharing data, it should be able to receive data as well. The only input a client receives is the address of the broadcaster; it should hear about everything else through the network.

## Setup
##### Requirements

IntelliJ Idea, Java 8

##### Instructions

Clone the project, then import it into Idea and follow the steps. It should automatically include the libraries.

Next, to set the project language level, go to File -> Project Structure -> Project, and change the `Project Language Level` to `8 - Lambdas, type annotations, etc`.

The `ProjectStructureTests` test that JUnit and Guava were correctly included and that the language version is correct.

## Software Structure

The project is structured as three major components: the deliverable (`main.deliverable`), the network protocols (`main.network.protocols`), and the tests ( `test.benchmarks` and `test.unit`). It has been designed around the idea that different protocols should be tested and prove their own merits (through the benchmarks). We currently have a single fault-tolerant protocol (the tree protocol) whose fault tolerance is shown through unit tests, but we have benchmarks set up for performance testing of new protocols.

##### `main.deliverable`
Windowed applications demonstrating the project. The broadcaster and client are both runnable applications.

`ImageDisplay.java` creates and shows the window that the broadcaster and client both use.

`ScreenGrabber.java` grabs snapshots of the screen at a desired resolution and frames per second.

The broadcaster (`Broadcaster.java`) prints an IP address (this only works on MIT networks; feel free to comment it out and use other means of finding an IP address of the server). It broadcasts to port `5567`.

The client (`Client.java`) asks for an IP and port for the broadcaster, then connects. While it's connected, it polls for IP/port combinations to which it should no longer connect (to demonstrate fault tolerance). There is currently no mechanism for reallowing those connections, but that's not a hard change to make.

##### `main.network.connections`

A `Connection` represents a means of sending and receiving bytes between two nodes (a node, of course, being either a broadcaster or a client). To simplify testing, a `Connection` can either be based around a network socket (the `socket` subpackage), as the deliverable uses, or around a byte buffer (the `test` subpackage) so that the network isn't used in testing. The test connections provide methods for rate-limiting and creating arbitrary network blockages.

##### `main.network.protocols`

A network protocol defines the way a broadcaster and client work. Callers create the broadcaster and clients (possibly on different machines), gives the clients the broadcaster's address, and expects the clients to handle fault tolerance from there (by receiving information from the broadcaster and other clients about the state of the network).

The `basic` protocol simply has each client connect to the broadcaster and receive snapshots from it. This represents modern screen-sharing applications and has no fault tolerance. Additionally, its performance can be lacking.

The `tree` protocol allows clients to connect to other clients in the event of a failure between it and the broadcaster. The network acts a tree rooted at the broadcaster, in which a parent sends screen data to its children (which it received from its children, or received by being the broadcaster). Additionally, a parent sends information about the state of the tree not including the client, and the client sends information about its subtree to its parent. When a node's connection to its parent fails, it uses the information it received about the other nodes in the tree to find a new parent. It is susceptible to slow network connections.

##### `test.benchmarks`

This package provides mechanisms for performance testing in the form of unit tests.

The entry point is `InputSuite.java`. Currently it only has a few small tests, but it's relatively simple to extend the testing suite to include much larger tests. Each unit test runs a set of inputs on a specific protocol.

An `Input` is defined as a name, a number of clients, a number of rounds, and a `RateLimitSchedule`. The `RateLimitSchedule` is a series of failures in the network (either completely removing the network connection between two nodes or simply lowering the speed to some amount).

To run an input, the test creates a `Runner`. The `Runner` takes broadcaster and client factory methods, a connection manager (for rate-limiting and closing connections), and an input. The `Runner` then spawns a number of clients as described by the input, then runs a series of rounds, where the `i`th round corresponds to waiting for the `i`th frame to reach all clients. It tracks how long each client takes to receive each frame through the `ResultSetBuilder` and returns a `ResultSet` when the testing has completed.

## Future Work

* Create protocols based around higher performance (i.e., switching parents also on the basis of receiving frames faster rather than just in the case of failure).
* Sending adaptive resolutions and frames per second based on connection properties.
* Skipping (or dropping) certain frames so that the connection doesn't get backlogged.
* Limiting bandwidth of a given node. Right now, specific connections can be limited, but one way you might expect the basic protocol to fail in the real world is that machines only have so much network throughput; this would prevent a <i>node</i> rather than a <i>connection</i> from sending more than `X` bytes per time period.

