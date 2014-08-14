package com.fusionx.relay.call.channel;

import com.fusionx.relay.call.Call;

public class ChannelJoinCall extends Call {

    private final String channelName;

    public ChannelJoinCall(String channelName) {
        this.channelName = channelName;
    }

    @Override
    public String getLineToSendServer() {
        return "JOIN " + channelName;
    }
}