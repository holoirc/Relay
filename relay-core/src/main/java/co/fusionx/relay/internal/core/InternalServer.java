package co.fusionx.relay.internal.core;

import co.fusionx.relay.constant.Capability;
import co.fusionx.relay.conversation.Server;
import co.fusionx.relay.event.server.ServerEvent;

public interface InternalServer extends InternalConversation<ServerEvent>, Server {

    public void addCapability(Capability capability);
}