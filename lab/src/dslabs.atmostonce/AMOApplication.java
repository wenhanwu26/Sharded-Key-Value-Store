package dslabs.atmostonce;

import dslabs.framework.Address;
import dslabs.framework.Application;
import dslabs.framework.Command;
import dslabs.framework.Result;
import java.util.Collections;
import java.util.HashMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@RequiredArgsConstructor
public final class AMOApplication implements Application {
    @Getter @NonNull private final Application application;

    // Your code here...

    private HashMap<Address, HashMap<Integer,AMOResult>> AMOResults = new HashMap<>();

    @Override
    public AMOResult execute(Command command) {
        if (!(command instanceof AMOCommand)) {
            throw new IllegalArgumentException();
        }

        AMOCommand amoCommand = (AMOCommand) command;

        // Your code here...

        if(alreadyExecuted(amoCommand)){
            // if is the latest seq number (only the most recent result is stored), then return result, else ignore it
            if(AMOResults.get(amoCommand.clientAddress()).containsKey(amoCommand.sequenceNum())) {
                return AMOResults.get(amoCommand.clientAddress()).get(amoCommand.sequenceNum());
            }else {
                return null;
            }
        }
        AMOResult AMOResult = new AMOResult(application.execute(amoCommand.command()), amoCommand.sequenceNum());

        HashMap<Integer,AMOResult> seqNumberResultPair = new HashMap<>();
        seqNumberResultPair.put(amoCommand.sequenceNum(),AMOResult);

        AMOResults.put(amoCommand.clientAddress(),seqNumberResultPair);

        return AMOResult;
    }

    public Result executeReadOnly(Command command) {
        if (!command.readOnly()) {
            throw new IllegalArgumentException();
        }

        if (command instanceof AMOCommand) {
            return execute(command);
        }

        return application.execute(command);
    }

    public boolean alreadyExecuted(AMOCommand amoCommand) {
        // Your code here...
        return (AMOResults.containsKey(amoCommand.clientAddress()) && Collections.max(AMOResults.get(amoCommand.clientAddress()).keySet())>=amoCommand.sequenceNum());

    }
}
