package co.fusionx.relay.event.server;

import co.fusionx.relay.base.Server;
import co.fusionx.relay.dcc.pending.DCCPendingSendConnection;

public class DCCSendRequestEvent extends DCCRequestEvent {

    public DCCSendRequestEvent(final Server server,
            final DCCPendingSendConnection pendingConnection) {
        super(server, pendingConnection);
    }

    @Override
    public DCCPendingSendConnection getPendingConnection() {
        return (DCCPendingSendConnection) pendingConnection;
    }
}
