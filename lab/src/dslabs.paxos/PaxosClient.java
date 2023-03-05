package dslabs.paxos;

import com.google.common.base.Objects;
import dslabs.atmostonce.AMOCommand;
import dslabs.framework.Address;
import dslabs.framework.Client;
import dslabs.framework.Command;
import dslabs.framework.Node;
import dslabs.framework.Result;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static dslabs.paxos.ClientTimer.CLIENT_RETRY_MILLIS;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class PaxosClient extends Node implements Client {
    private final Address[] servers;

    // Your code here...
    private Result result;
    private PaxosRequest request;
    private int sequenceNum = 0;
    /* -------------------------------------------------------------------------
        Construction and Initialization
       -----------------------------------------------------------------------*/
    public PaxosClient(Address address, Address[] servers) {
        super(address);
        this.servers = servers;
    }

    @Override
    public synchronized void init() {
        // No need to initialize
    }

    /* -------------------------------------------------------------------------
        Public methods
       -----------------------------------------------------------------------*/
    @Override
    public synchronized void sendCommand(Command operation) {
        // Your code here...
        AMOCommand AMOCommand = new AMOCommand(operation,sequenceNum,this.address());
        request = new PaxosRequest(AMOCommand);
        sequenceNum++;
        result = null;
        for(Address serverAddress: servers){
            send(request, serverAddress);
        }
        set(new ClientTimer(request), CLIENT_RETRY_MILLIS);
    }

    @Override
    public synchronized boolean hasResult() {
        // Your code here...
        return result!=null;
    }

    @Override
    public synchronized Result getResult() throws InterruptedException {
        // Your code here...
        while(result==null){
            wait();
        }

        return result;
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private synchronized void handlePaxosReply(PaxosReply m, Address sender) {
        // Your code here...
        if(m.result().sequenceNum() == request.command().sequenceNum()) {
            result = m.result().result();
            notify();
        }
    }

    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    private synchronized void onClientTimer(ClientTimer t) {
        // Your code here...
        if (Objects.equal(request, t.request()) && result == null) {
            for(Address serverAddress: servers){
                send(request, serverAddress);
            }
            set(t, CLIENT_RETRY_MILLIS);
        }
    }
}
