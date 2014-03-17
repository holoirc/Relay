package com.fusionx.relay.parser.command;

import com.fusionx.relay.Channel;
import com.fusionx.relay.ChannelSnapshot;
import com.fusionx.relay.Server;
import com.fusionx.relay.WorldUser;
import com.fusionx.relay.event.channel.ChannelEvent;
import com.fusionx.relay.event.channel.WorldJoinEvent;
import com.fusionx.relay.event.server.JoinEvent;
import com.fusionx.relay.event.server.ServerEvent;

import java.util.List;

class JoinParser extends CommandParser {

    public JoinParser(final Server server) {
        super(server);
    }

    @Override
    public void onParseCommand(final List<String> parsedArray, final String rawSource) {
        final String channelName = parsedArray.get(2);

        final ChannelSnapshot snapshot = mServer.getUser().getChannelSnapshot(channelName);
        final WorldUser user = mUserChannelInterface.getUserFromRaw(rawSource);
        final Channel channel = mUserChannelInterface.getChannelFromSnapshot(channelName, snapshot);
        mUserChannelInterface.coupleUserAndChannel(user, channel);

        if (user.isUserNickEqual(mServer.getUser())) {
            final ServerEvent event = new JoinEvent(channel);
            mServerEventBus.post(event);
        } else {
            final ChannelEvent event = new WorldJoinEvent(channel, user);
            mServerEventBus.postAndStoreEvent(event, channel);
        }
    }
}