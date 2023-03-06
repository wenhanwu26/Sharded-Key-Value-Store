package dslabs.paxos;

import dslabs.atmostonce.AMOApplication;
import dslabs.atmostonce.AMOCommand;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Address;
import dslabs.framework.Application;
import dslabs.framework.Command;
import dslabs.framework.Node;
import java.util.ArrayList;
import java.util.HashMap;
import jdk.swing.interop.SwingInterOpUtils;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static dslabs.paxos.GarbageCarTimer.GARBAGE_CAR_MILLIS;
import static dslabs.paxos.HeartbeatCheckTimer.HEART_BEAT_CHECK_MILLIS;
import static dslabs.paxos.HeartbeatTimer.HEART_BEAT_MILLIS;


@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PaxosServer extends Node {
    /** All servers in the Paxos group, including this one. */
    private final Address[] servers;

    // Your code here...
    private AMOApplication app;
    private Ballot largestBallot = new Ballot(0, address());
    private ArrayList<Pvalue> acceptedLogs = new ArrayList<>();
    private HashMap<Address, Integer> slotOfLastExecutedAllServers =
            new HashMap<>();
    private boolean isActive = false;
    //private int slotOfLastExecuted = 0;
    private int unChosenSlotBegin = 1;
    private int P2BCount = 0;
    private HashMap<Address, Integer> P2BCountMap = new HashMap<>();
    private int P1BCount = 0;
    private HashMap<Address, Integer> P1BCountMap = new HashMap<>();
    // private P1A messageComing;

    /* -------------------------------------------------------------------------
        Construction and Initialization
       -----------------------------------------------------------------------*/
    public PaxosServer(Address address, Address[] servers, Application app) {
        super(address);
        this.servers = servers;
        this.app = new AMOApplication(app);
        // Your code here...
    }


    @Override
    public void init() {
        // Your code here...
        sendP1A();
        set(new HeartbeatCheckTimer(), HEART_BEAT_CHECK_MILLIS);
        set(new HeartbeatTimer(), HEART_BEAT_MILLIS);
        // set(new GarbageCarTimer(), GARBAGE_CAR_MILLIS);
        slotOfLastExecutedAllServers.put(address(), 0);
    }

    /* -------------------------------------------------------------------------
        Interface Methods

        Be sure to implement the following methods correctly. The test code uses
        them to check correctness more efficiently.
       -----------------------------------------------------------------------*/

    /**
     * Return the status of a given slot in the server's local log.
     *
     * If this server has garbage-collected this slot, it should return {@link
     * PaxosLogSlotStatus#CLEARED} even if it has previously accepted or chosen
     * command for this slot. If this server has both accepted and chosen a
     * command for this slot, it should return {@link PaxosLogSlotStatus#CHOSEN}.
     *
     * Log slots are numbered starting with 1.
     *
     * @param logSlotNum
     *         the index of the log slot
     * @return the slot's status
     *
     * @see PaxosLogSlotStatus
     */
    public PaxosLogSlotStatus status(int logSlotNum) {
        // Your code here...
        // TODO: garbage collected
        if (acceptedLogs.size() > 0 &&
                logSlotNum < acceptedLogs.get(0).slot_num()) {
            return PaxosLogSlotStatus.CLEARED;
        } else if (logSlotNum < unChosenSlotBegin) {
            return PaxosLogSlotStatus.CHOSEN;
        } else if (acceptedLogs.size() > 0 && logSlotNum <=
                acceptedLogs.get(acceptedLogs.size() - 1).slot_num()) {
            return PaxosLogSlotStatus.ACCEPTED;
        } else {
            return PaxosLogSlotStatus.EMPTY;
        }
        //return null;
    }

    /**
     * Return the command associated with a given slot in the server's local
     * log.
     *
     * If the slot has status {@link PaxosLogSlotStatus#CLEARED} or {@link
     * PaxosLogSlotStatus#EMPTY}, this method should return {@code null}.
     * Otherwise, return the command this server has chosen or accepted,
     * according to {@link PaxosServer#status}.
     *
     * If clients wrapped commands in {@link dslabs.atmostonce.AMOCommand}, this
     * method should unwrap them before returning.
     *
     * Log slots are numbered starting with 1.
     *
     * @param logSlotNum
     *         the index of the log slot
     * @return the slot's contents or {@code null}
     *
     * @see PaxosLogSlotStatus
     */
    public Command command(int logSlotNum) {
        // Your code here...
        for (Pvalue pvalue : acceptedLogs) {
            if (pvalue.slot_num() == logSlotNum) {
                return pvalue.paxosRequest().command().command();
            }
        }
        return null;
    }

    /**
     * Return the index of the first non-cleared slot in the server's local log.
     * The first non-cleared slot is the first slot which has not yet been
     * garbage-collected. By default, the first non-cleared slot is 1.
     *
     * Log slots are numbered starting with 1.
     *
     * @return the index in the log
     *
     * @see PaxosLogSlotStatus
     */
    public int firstNonCleared() {
        // Your code here...
        return acceptedLogs.size() > 0 ? acceptedLogs.get(0).slot_num() : 1;
        //return 1;
    }

    /**
     * Return the index of the last non-empty slot in the server's local log,
     * according to the defined states in {@link PaxosLogSlotStatus}. If there
     * are no non-empty slots in the log, this method should return 0.
     *
     * Log slots are numbered starting with 1.
     *
     * @return the index in the log
     *
     * @see PaxosLogSlotStatus
     */
    public int lastNonEmpty() {
        // Your code here...
        if (acceptedLogs.size() > 0) {
            return acceptedLogs.get(acceptedLogs.size() - 1).slot_num();
        }
        return 0;
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private void handlePaxosRequest(PaxosRequest m, Address sender) {
        // Your code here...
        // isActive, then can send P2A
        // drop if inactive
        if (isActive) {
            // System.out.println(address() + " receive Paxos Request "+m);
            // inefficient, may need optimize
            for (Pvalue pvalue : acceptedLogs) {
                if (pvalue.paxosRequest().equals(m)) {
                    if (pvalue.slot_num() < unChosenSlotBegin) {
                        AMOResult AMOResult =
                                app.execute(pvalue.paxosRequest().command());
                        if (AMOResult != null) {
                            send(new PaxosReply(AMOResult), sender);
                        }
                    } else {
                        sendP2A(acceptedLogs.get(acceptedLogs.size()-1).slot_num());
                    }
                    return;
                }
            }

            int slot_num = acceptedLogs.size() > 0 ?
                    acceptedLogs.get(acceptedLogs.size() - 1).slot_num() + 1 :
                    1;
            acceptedLogs.add(new Pvalue(slot_num, largestBallot, m));
            sendP2A(slot_num);
        }
    }


    // Your code here...
    private void handleP1A(P1A m, Address sender) {
        if (largestBallot.compareTo(m.ballot()) <= 0) {
            // System.out.println(address() + " receive P1A with " + m +" from "+sender);
            // messageComing = m;
            if (largestBallot.compareTo(m.ballot()) < 0) {
                isActive = false;
                largestBallot = m.ballot();
                sendP1B(sender);
            }
            // reset count when try to become leader
            //            if (largestBallot.compareTo(m.ballot()) == 0) {
            //                P1BCount = 0;
            //                //sendP1B(sender);
            //            }
        }
    }

    private void handleP1B(P1B m, Address sender) {
        if (largestBallot.compareTo(m.ballot()) <= 0 && !isActive) {
            // // // System.out.println(address() + " receive P1B with " + m +" from "+sender);
            if (largestBallot.compareTo(m.ballot()) < 0) {
                // a bit weird if getting here, send to this leader with ballot higher than this leader
//                isActive = false;
//                largestBallot = m.ballot();
            } else if (largestBallot.compareTo(m.ballot()) == 0) {
                // System.out.println(address() + " receive P1B with " + m +" from "+sender);
                updateAcceptedLogs(m.acceptedLogs(), m.unChosenSlotBegin());
                P1BCount++;
                P1BCountMap.put(sender, 1);
                // // // System.out.println(address());
                // // // System.out.println(P1BCountMap);
                if (P1BCountMap.size() + 1 > servers.length /
                        2) { // didnt send p1b to itself so +1 to include itself
                    isActive = true;
                    //P1BCount = 0;
                    P1BCountMap = new HashMap<>();
                    // sendP2A();
                }
            }
        }

    }

    private void handleP2A(P2A m, Address sender) {
        if (largestBallot.compareTo(m.ballot()) == 0) {
            if (acceptedLogs.size() > 0 &&
                    acceptedLogs.get(acceptedLogs.size() - 1).slot_num() >
                            m.slot_num()) {
                // filter old p2a message with same ballot #
                return;
            }
            // System.out.println(address() + " receive P2A with " + m+" from "+sender);
            //            if (largestBallot.compareTo(m.ballot()) < 0) {
            //                isActive = false;
            //            }
            //            largestBallot = m.ballot();
            acceptedLogs = m.acceptedLogs();
            unChosenSlotBegin =
                    Math.max(m.unChosenSlotBegin(), unChosenSlotBegin);
            //P2BCount = 0;
            P2BCountMap = new HashMap<>();
            // // // // System.out.println(address() + " Sending P2B "+ acceptedLogs);
            sendP2B(m.slot_num());
        }
    }

    private void handleP2B(P2B m, Address sender) {
        //        if (largestBallot.compareTo(m.ballot()) < 0) {
        //              // // // System.out.println(address() + " receive P2B with " + m+" from "+sender);
        //            isActive = false;
        //            largestBallot = m.ballot();
        //            acceptedLogs = m.acceptedLogs();
        //            unChosenSlotBegin =
        //                    Math.max(m.unChosenSlotBegin(), unChosenSlotBegin);
        //            //P2BCount = 0;
        //            P2BCountMap = new HashMap<>();
        //        } else if (largestBallot.compareTo(m.ballot()) == 0) {
        //             // // // System.out.println(address() + " receive P2B with " + m+" from "+sender);
        //            P2BCountMap.put(sender,1);
        //            //P2BCount++;
        //            if (P2BCountMap.size() > servers.length / 2) {
        //                if (acceptedLogs.size() > 0) {
        //                    executeCommand();
        //                    unChosenSlotBegin =
        //                            acceptedLogs.get(acceptedLogs.size() - 1)
        //                                        .slot_num() + 1;
        ////                    // // // System.out.println(address()+ " "+ acceptedLogs);
        ////                    // // // System.out.println(unChosenSlotBegin);
        //                }
        //                //P2BCount = 0;
        //                P2BCountMap = new HashMap<>();
        //            }
        //        }
        if (largestBallot.compareTo(m.ballot()) == 0 && isActive) {
              // System.out.println(address() + " receive P2B with " + m+" from "+sender);
            if (acceptedLogs.size() > 0 &&
                    acceptedLogs.get(acceptedLogs.size() - 1).slot_num() !=
                            m.slot_num()) {
                return;
            }
            // // // System.out.println(address() + " receive P2B with " + m+" from "+sender);
            P2BCountMap.put(sender, 1);
            //P2BCount++;
            if (P2BCountMap.size() > servers.length / 2) {
                if (acceptedLogs.size() > 0) {
                    executeCommand();
                    unChosenSlotBegin =
                            acceptedLogs.get(acceptedLogs.size() - 1)
                                        .slot_num() + 1;
                    //                    // // // System.out.println(address()+ " "+ acceptedLogs);
                    //                    // // // System.out.println(unChosenSlotBegin);
                }
                //P2BCount = 0;
                P2BCountMap = new HashMap<>();
                // System.out.println(address() +" state after received P2B "+acceptedLogs + " "+unChosenSlotBegin);
            }
        }
    }

    public void executeCommand() {
        for (int i = 0; i < acceptedLogs.size(); i++) {
            if (acceptedLogs.get(i).slot_num() > slotOfLastExecutedAllServers.get(address())) {
                AMOCommand AMOCommand =
                        acceptedLogs.get(i).paxosRequest().command();
                AMOResult AMOResult = app.execute(AMOCommand);
                if (AMOResult != null && isActive) {
                    send(new PaxosReply(AMOResult), AMOCommand.clientAddress());
                }
                slotOfLastExecutedAllServers.put(address(),
                        acceptedLogs.get(i).slot_num());
            }
        }
    }


    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    // Your code here...
    private int count = 0;

    private void onHeartbeatCheckTimer(HeartbeatCheckTimer t) {
        if (count >= 2) {
            sendP1A();
            count = 0;
        } else {
            count++;
        }
        set(t, HEART_BEAT_CHECK_MILLIS);
    }

    private void onHeartbeatTimer(HeartbeatTimer t) {
        if (isActive) {
            for (Address serverAddress : servers) {
                if (serverAddress.equals(address())) {
                    handleHeartBeat(new HeartBeat(largestBallot), address());
                } else {
                    send(new HeartBeat(largestBallot), serverAddress);
                }
            }
        }
        set(t, HEART_BEAT_MILLIS);
    }

    private void handleHeartBeat(HeartBeat m, Address sender) {
        // // // // System.out.println(address()+ " received heartbeat from "+sender);
        if (largestBallot.compareTo(m.ballot()) <= 0) {
            count = 0;
        }
    }

    private void onGarbageCarTimer(GarbageCarTimer t) {
        for (Address serverAddress : servers) {
            if (serverAddress.equals(address())) {
                handleGarbageCar(new GarbageCar(
                                slotOfLastExecutedAllServers.get(address())),
                        address());
            } else {
                send(new GarbageCar(
                                slotOfLastExecutedAllServers.get(address())),
                        serverAddress);
            }
        }

        set(t, GARBAGE_CAR_MILLIS);
    }

    private void handleGarbageCar(GarbageCar m, Address sender) {
        // // // // System.out.println(address() + " received garbage from " + sender);
        slotOfLastExecutedAllServers.put(sender, m.slotOfLastExecuted());
        if (slotOfLastExecutedAllServers.keySet().size() == servers.length) {
            int smallestSlotFinishExecuted = Integer.MAX_VALUE;
            for (Address serverAddress : slotOfLastExecutedAllServers.keySet()) {
                smallestSlotFinishExecuted =
                        Math.min(smallestSlotFinishExecuted,
                                slotOfLastExecutedAllServers.get(
                                        serverAddress));
            }
            if (smallestSlotFinishExecuted != 0) {
                while (acceptedLogs.size() > 0 &&
                        acceptedLogs.get(0).slot_num() <=
                                smallestSlotFinishExecuted) {
                    acceptedLogs.remove(0);
                }
            }
        }
    }

    /* -------------------------------------------------------------------------
        Utils
       -----------------------------------------------------------------------*/
    // Your code here...
    // for leader in P1B
    private void updateAcceptedLogs(ArrayList<Pvalue> LogsToCompare,
                                    int unChosenSlotBeginToCompare) {

        if (LogsToCompare.size() == 0) {
            return;
        }

        if (acceptedLogs.size() == 0) {
            // LogsToCompare should be a new arraylist if get it from message so no need deep copy constructor
            acceptedLogs = LogsToCompare;
        }

        // acceptedLogs may have different length, maybe one hasn't finished garbage collected
        int index1 = 0, index2 = 0;
        while (acceptedLogs.get(index1).slot_num() !=
                LogsToCompare.get(index2).slot_num()) {
            int slot_num1 = acceptedLogs.get(index1).slot_num();
            int slot_num2 = LogsToCompare.get(index2).slot_num();
            if (slot_num1 > slot_num2) {
                index2++;
            } else {
                index1++;
            }
        }

        // copy value of slot before max of unchosenSlot and unChosenSlotBeginToCompare
        int maxUnChosenSlot =
                Math.max(unChosenSlotBegin, unChosenSlotBeginToCompare);
        //        while (acceptedLogs.get(index1).slot_num() < maxUnChosenSlot) {
        //            if (index1 < acceptedLogs.size()) {
        //                acceptedLogs.set(index1, LogsToCompare.get(index2));
        //            } else {
        //                acceptedLogs.add(LogsToCompare.get(index2));
        //            }
        //            index1++;
        //            index2++;
        //        }

            while (acceptedLogs.size() > index1 && acceptedLogs.get(index1).slot_num() < unChosenSlotBegin){
                index1++;
                index2++;
            }

        //
        //        if(unChosenSlotBegin <= unChosenSlotBeginToCompare){
        //            while(acceptedLogs.get(index1).slot_num()<unChosenSlotBegin){
        //                index1++;
        //                index2++;
        //            }
        //        }else {
        //            if(acceptedLogs.get(acceptedLogs.size()-1).slot_num() < unChosenSlotBeginToCompare){
        //
        //            }
        //
        //        }

        //        if (unChosenSlotBegin < unChosenSlotBeginToCompare) {
        //            unChosenSlotBegin = unChosenSlotBeginToCompare;
        //        }
        // get the value with the largest ballot number for each slot not in chosenIndex
        while (index1 < acceptedLogs.size() && index2 < LogsToCompare.size()) {

            // null, no-op case
            // why there is hole ?

            if (acceptedLogs.get(index1).ballot()
                            .compareTo(LogsToCompare.get(index2).ballot()) <
                    0) {
                acceptedLogs.set(index1, LogsToCompare.get(index2));

            }
            //            // // // System.out.println(acceptedLogs);
            //            // // // System.out.println(maxUnChosenSlot);
            index1++;
            index2++;
        }

        // if have something remaining in LogsToCompare, give it to acceptedLogs
        while (index2 < LogsToCompare.size()) {
            acceptedLogs.add(LogsToCompare.get(index2));
            index2++;
        }

    }

    private void sendP1A() {
        largestBallot = new Ballot(largestBallot.sequenceNum() + 1, address());
        for (Address serverAddress : servers) {
            if (serverAddress.equals(address())) {
                handleP1A(new P1A(largestBallot), address());
            } else {
                send(new P1A(largestBallot), serverAddress);
            }
        }
    }

    private void sendP1B(Address leader) {
        send(new P1B(largestBallot, acceptedLogs, unChosenSlotBegin), leader);
    }

    private void sendP2A(int slot_num) {
        // only the leader can call this method
        // may need to have extra seq# to differentiate P2A with same ballot
        // largestBallot = new Ballot(largestBallot.sequenceNum() + 1, address());
        for (Address serverAddress : servers) {
            if (serverAddress.equals(address())) {
                handleP2A(
                        new P2A(largestBallot, acceptedLogs, unChosenSlotBegin,
                                slot_num), address());
            } else {
                send(new P2A(largestBallot, acceptedLogs, unChosenSlotBegin,
                        slot_num), serverAddress);
            }
        }
    }

    private void sendP2B(int slot_num) {
        for (Address serverAddress : servers) {
            if (serverAddress.equals(address())) {
                handleP2B(
                        new P2B(largestBallot, acceptedLogs, unChosenSlotBegin,
                                slot_num), address());
            } else {
                send(new P2B(largestBallot, acceptedLogs, unChosenSlotBegin,
                        slot_num), serverAddress);
            }
        }
    }
}
