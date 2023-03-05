package dslabs.paxos;

import dslabs.framework.Address;
import dslabs.framework.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import lombok.Data;

// Your code here...
@Data
class P1A implements Message {
    private final Ballot ballot;
}

@Data
class P1B implements Message {
    private final Ballot ballot;
    private final ArrayList<Pvalue> acceptedLogs;
    private final int unChosenSlotBegin;
}

@Data
class P2A implements Message {
    private final Ballot ballot;
    private final ArrayList<Pvalue> acceptedLogs;
    private final int unChosenSlotBegin;
}

@Data
class P2B implements Message {
    private final Ballot ballot;
    private final ArrayList<Pvalue> acceptedLogs;
    private final int unChosenSlotBegin;
}

@Data
class HeartBeat implements Message {
    private final Ballot ballot;
}

@Data
class GarbageCar implements Message {
    private final int slotOfLastExecuted;
}
