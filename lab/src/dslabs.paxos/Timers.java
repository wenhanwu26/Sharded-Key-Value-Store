package dslabs.paxos;

import dslabs.framework.Timer;
import lombok.Data;

@Data
final class ClientTimer implements Timer {
    static final int CLIENT_RETRY_MILLIS = 40;
    private final PaxosRequest request;
    // Your code here...
}

@Data
final class HeartbeatCheckTimer implements Timer {
    static final int HEART_BEAT_CHECK_MILLIS = 50;
}

@Data
final class HeartbeatTimer implements Timer {
    static final int HEART_BEAT_MILLIS = 25;
}

@Data
final class GarbageCarTimer implements Timer {
    static final int GARBAGE_CAR_MILLIS = 100;
}

// Your code here...
