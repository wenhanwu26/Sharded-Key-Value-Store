DSLabs was developed as the educational framework for  **CSE452 Distributed
Systems** from the *University of Washington* and it was written by Ellis
Michael. It is a framework for creating, testing, model checking, visualizing, and debugging
distributed systems. While all information required for projects
is included, students can still refer to the original GitHub links to see the
general ideas in [dslabs repo](https://github.com/emichael/dslabs). 

A sharded key/value store out of multiple replica groups, with each of them using PAXOS for requests ordering internally, is built. Besides, a two-phase commit protocol is introduced for transactions


## Programming Model

The DSLabs framework is built around message-passing state machines (also known as I/O automata or distributed actors), which we call nodes. These basic units of a distributed system consist of a set of message and timer handlers; these handlers define how the node updates its internal state, sends messages, and sets timers in response to an incoming message or timer. These nodes are run in single-threaded event loops, which take messages from network and timers from the node's timer queue and call the node's handlers for those events.

