package co.fusionx.relay.event.server;

import co.fusionx.relay.base.Server;

public class GenericServerEvent extends ServerEvent {

    public final String message;

    public GenericServerEvent(final Server server, final String message) {
        super(server);

        this.message = message;
    }
}