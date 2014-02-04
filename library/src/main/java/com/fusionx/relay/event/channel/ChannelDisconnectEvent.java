package com.fusionx.relay.event.channel;

import com.fusionx.relay.Channel;

public class ChannelDisconnectEvent extends ChannelEvent {
    public final String message;

    public ChannelDisconnectEvent(final Channel channel, final String message) {
        super(channel);
        this.message = message;
    }
}