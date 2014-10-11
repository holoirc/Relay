package co.fusionx.relay.base;

import co.fusionx.relay.core.InternalDCCManager;

public class RelayRelayDCCPendingChatConnection extends RelayDCCPendingConnection {

    public RelayRelayDCCPendingChatConnection(final String dccRequestNick,
            final InternalDCCManager manager,
            final String ip, final int port, final String argument, final long size) {
        super(dccRequestNick, manager, ip, port, argument, size);
    }

    public void acceptConnection() {
        mManager.acceptDCCConnection(this);
    }
}