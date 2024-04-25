# Paxos
*Adapted from the [MIT 6.824
Labs](http://nil.csail.mit.edu/6.824/2015/labs/lab-3.html)*


You need to read papers (2 in particular) and follow their ideas to implement
your PAXOS in this one. The original paper for PAXOS, [The Part-Time
Parliament](https://courses.cs.washington.edu/courses/csep590/04wi/papers/lamport-part-time-parliament.pdf),
proposed by Leslie Lamport in the 1990s, is somehow hard to understand and there
is, in general, a huge gap between the implementation and illustration (while
there is little doubt around the efficiency of PAXOS, due to its proved least
rounds of messaging). This is also the reason that lots of industrial products
such as Kubernetes and Redis would turn their back to PAXOS and choose Raft.
However, some enhanced papers with clearer illustrations come out as time goes
by, and we find them much easier for students to follow. Once you understanding
these papers, you will find their ideas are the same as the original ones. As
for add-ons, we will ask you to implement more features than merely PAXOS, i.e.
stable leaders and garbage collection. We will discuss them in detail in the
following sections. There are also some optimizations you can perform, such as
request batching, in your PAXOS implementation.

## PAXOS
We will follow [Paxos Made Moderately Complex](http://www.cs.cornell.edu/courses/cs7412/2011sp/paxos.pdf)
for the PAXOS implementation. Before starting, you should also read
[Paxos Made Simple](https://lamport.azurewebsites.net/pubs/paxos-simple.pdf)
for the general ideas of PAXOS.

Your system will consist of `PaxosServer`s and `PaxosClient`s. The clients send
`PaxosRequest`s to the servers, and servers respond with `PaxosReply`s. Each
node has the list of servers in the system. You are only provided the
aforementioned messages as well as the `ClientTimer`; you will have to define
the rest of the messages and timers your implementation uses.

Your system should guarantee *linearizability* of clients' commands. That is,
from the perspective of the callers of clients' functions and the results they
see, your implementation should be indistinguishable from a single, correct
entity processing clients' commands in sequence (e.g., your `SimpleServer` from
lab 1). Furthermore, your implementation should be able to process incoming
commands and return results as long as a majority of servers can communicate
with each other with "reasonable" message delay (and as long as the client can
send a command to these servers). This means that it should be robust to dropped
messages, as long as network connectivity is eventually restored.

You should achieve this by implementing the multi-instance Paxos algorithm.
Multi-instance Paxos can be viewed as a way for servers to agree on a shared log
of clients' commands. Agreement for each "slot" in the log is reached separately
by a different instance of Paxos, and servers can then play the log forward in
order, executing commands on their own local copy of the application, as long as
each command they execute is "stable" (i.e., agreement has been reached for that
command and all preceding commands). As long as no two servers ever decide
different values for a log slot (and as long as the underlying application is
deterministic), this approach will guarantee linearizability.

Your Paxos-based replicated state machine will have some limitations that would
need to be fixed in order for it to be a serious system. It won't cope with
crashes, since it stores neither the key/value database nor the Paxos state on
disk. Also, It requires the set of servers to be fixed, so one cannot replace
old servers. These problems can be fixed.

You might find the Paxos lecture notes, [Paxos Made
Simple](https://lamport.azurewebsites.net/pubs/paxos-simple.pdf), and [Paxos
Made Moderately
Complex](http://www.cs.cornell.edu/courses/cs7412/2011sp/paxos.pdf), and
[Viewstamped Replication](https://dl.acm.org/citation.cfm?id=62549) useful.

The Paxos Made Moderately Complex (PMMC) protocol divides the nodes into different
"roles" (e.g., replica, acceptor, leader), while your implementation only has one
role, `PaxosServer`. It will play all of the aforementioned roles simultaneously.
This could be done by keeping all of the states for each sub-role
entirely separate, but you'll find that there are opportunities for optimization
on the naive approach. You should not try to "spawn" scouts or commanders;
instead, simply keep the necessary state on your `PaxosServer`.


### Stable Leaders
In addition to the base PMMC protocol, as Google Spanner, we are expecting a mechanism
for checking and maintaining a stable leader (sometimes called the distinguished proposer).
When a leader is preempted (receives a ballot larger than its own), instead of immediately
starting phase 1 of PAXOS (spawning a scout) again, it should transition to
"follower mode" and stay inactive.

Every `PaxosServer` should have a `HeartbeatCheckTimer` which ticks, firing
periodically, exactly like the `PingCheckTimer` in lab 2. While in follower
mode, if a node sees two of these timers in a row without receiving a message
from the node it thinks is the active leader (the one with the largest ballot
it's seen), it should then attempt to become active and start phase 1 of Paxos.

To prevent itself from being preempted unnecessarily, while an active leader, a
`PaxosServer` should periodically broadcast `Heartbeat` messages to the other
nodes. Simply have another timer which fires periodically.

While you can use the AIMD (additive increase, multiplicative decrease) like the
one described in section 3 of the PMMC paper, we recommend keeping the timer
lengths fixed for simplicity. The timer lengths from lab 2 are a good starting
point, but you are free to tune them as you see fit.

One important note: this approach to leader election should only be seen as a
performance optimization. There could still be nodes which simultaneously
believe themselves to be an active leader – who believe their ballot is
acceptable to a majority. The Paxos protocol ensures safety in this case.


## Garbage Collection
A long-running Paxos deployment must forget about log slots that are no longer
needed and free the memory storing information about those slots. While PMMC
proposes one mechanism for garbage collection, for this lab we will do something
slightly simpler.

For our purposes, we will say that a command in a log slot is needed if it has
not been processed on all servers; it is not okay to delete commands as soon as
they are processed on the local server. In order to implement this log
compaction, each server will have to inform the other servers about the latest
point in its own log that is "stable." The easiest approach is to piggyback this
information on the periodic heartbeat protocol. Each follower server responds to
heartbeats with a message containing the latest slot they have executed
(`slot_out` in PMMC terms). Then, the active leader should be able to figure out
the latest log slot which has been executed on all nodes; it can then include
that information in subsequent heartbeats. Once a node learns that all nodes
have executed all slots up to some slot `i`, it can then safely discard all
information for slots less than or equal to `i`.

If one of your servers falls behind (i.e. does not receive the decision for some
instance), it will later need to find out what (if anything) was agreed to. A
simple way to bring a follower node up to date is by having the active leader
send it missing decisions when the follower sends its latest executed slot in
the above protocol.

Your garbage collection mechanism should be able to free memory from old log
slots when all Paxos servers can communicate with each other; it does not need
to make progress when only a majority can communicate. This is one weakness of
the simplified approach we describe. It is possible to do garbage collection of
slots that only a majority have executed and discovered the values for. In that
case, however, bringing lagging nodes up-to-date requires a complete *state
transfer*, which can get tricky. Additionally, doing state transfer is not as
modular; you will see what we mean by that when you use Paxos as a part of a
larger protocol in lab 4.


## Paxos Interface Methods
In order for the tests to more efficiently check your implementation, you'll
need to implement four methods in your `PaxosServer`: `status(logSlotNum)`,
`command(logSlotNum)`, `firstNonCleared()`, and `lastNonEmpty()`. These methods
simply return information about the *local state* of a server. Implementing
them should be straightforward, but be sure to pay attention their requirements
and implement them correctly.

We have provided you the implementation of ballot and pvalue. The `PaxosRequest`,
`PaxosReply`, and `PaxosDecision` are also ready to use. They are shortly discussed
in following section about future Project 5.

