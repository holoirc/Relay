package com.fusionx.relay.parser.command;

import com.fusionx.relay.RelayChannel;
import com.fusionx.relay.RelayChannelUser;
import com.fusionx.relay.RelayQueryUser;
import com.fusionx.relay.RelayServer;
import com.fusionx.relay.RelayUserChannelInterface;
import com.fusionx.relay.Server;
import com.fusionx.relay.bus.ServerEventBus;
import com.fusionx.relay.call.server.ERRMSGResponseCall;
import com.fusionx.relay.call.server.FingerResponseCall;
import com.fusionx.relay.call.server.PingResponseCall;
import com.fusionx.relay.call.server.TimeResponseCall;
import com.fusionx.relay.call.server.VersionResponseCall;
import com.fusionx.relay.event.channel.ChannelEvent;
import com.fusionx.relay.event.channel.ChannelWorldActionEvent;
import com.fusionx.relay.event.channel.ChannelWorldMessageEvent;
import com.fusionx.relay.event.query.QueryActionWorldEvent;
import com.fusionx.relay.event.server.NewPrivateMessageEvent;
import com.fusionx.relay.event.server.VersionEvent;
import com.fusionx.relay.parser.MentionParser;
import com.fusionx.relay.util.IRCUtils;
import com.fusionx.relay.util.LogUtils;

import java.util.List;

import java8.util.Optional;

class CtcpParser {

    private final RelayServer mServer;

    private final ServerEventBus mEventBus;

    private final RelayUserChannelInterface mUserChannelInterface;

    public CtcpParser(RelayServer server) {
        mServer = server;
        mUserChannelInterface = server.getUserChannelInterface();
        mEventBus = server.getServerEventBus();
    }

    public static boolean isCtcp(final String message) {
        return message.startsWith("\u0001") && message.endsWith("\u0001");
    }

    // Commands start here
    public void onParseCommand(final List<String> parsedArray, final String rawSource) {
        final String normalMessage = parsedArray.get(3);
        final String message = normalMessage.substring(1, normalMessage.length() - 1);

        final String nick = IRCUtils.getNickFromRaw(rawSource);
        // TODO - THIS IS INCOMPLETE
        if (message.startsWith("ACTION")) {
            onAction(parsedArray, rawSource);
        } else if (message.startsWith("FINGER")) {
            getServer().getServerCallBus().post(new FingerResponseCall(nick, mServer));
        } else if (message.startsWith("VERSION")) {
            getServer().getServerCallBus().post(new VersionResponseCall(nick));
        } else if (message.startsWith("SOURCE")) {
        } else if (message.startsWith("USERINFO")) {
        } else if (message.startsWith("ERRMSG")) {
            final String query = message.replace("ERRMSG ", "");
            getServer().getServerCallBus().post(new ERRMSGResponseCall(nick, query));
        } else if (message.startsWith("PING")) {
            final String timestamp = message.replace("PING ", "");
            getServer().getServerCallBus().post(new PingResponseCall(nick, timestamp));
        } else if (message.startsWith("TIME")) {
            getServer().getServerCallBus().post(new TimeResponseCall(nick));
        }
    }

    private void onAction(final List<String> parsedArray, final String rawSource) {
        final String nick = IRCUtils.getNickFromRaw(rawSource);
        if (mUserChannelInterface.shouldIgnoreUser(nick)) {
            return;
        }
        final String action = parsedArray.get(3).replace("ACTION ", "");
        final String recipient = parsedArray.get(2);
        if (RelayChannel.isChannelPrefix(recipient.charAt(0))) {
            onParseChannelAction(recipient, nick, action);
        } else {
            onParseUserAction(nick, action);
        }
    }

    private void onParseUserAction(final String nick, final String action) {
        final Optional<RelayQueryUser> optional = mUserChannelInterface.getQueryUser(nick);
        if (optional.isPresent()) {
            final RelayQueryUser user = optional.get();
            getServerEventBus().postAndStoreEvent(new QueryActionWorldEvent(user, action), user);
        } else {
            final RelayQueryUser user = mUserChannelInterface
                    .addQueryUser(nick, action, true, false);
            getServerEventBus().postAndStoreEvent(new NewPrivateMessageEvent(user));
        }
    }

    private void onParseChannelAction(final String channelName, final String sendingNick,
            final String action) {
        final Optional<RelayChannel> optChannel = mUserChannelInterface.getChannel(channelName);

        LogUtils.logOptionalBug(optChannel);
        optChannel.ifPresent(channel -> {
            final Optional<RelayChannelUser> optUser = mUserChannelInterface.getUser(sendingNick);
            final boolean mention = MentionParser.onMentionableCommand(action,
                    getServer().getUser().getNick().getNickAsString());

            final ChannelEvent event;
            if (optUser.isPresent()) {
                event = new ChannelWorldActionEvent(channel, action, optUser.get(), mention);
            } else {
                event = new ChannelWorldMessageEvent(channel, action, sendingNick, mention);
            }
            getServerEventBus().postAndStoreEvent(event, channel);
        });
    }
    // Commands End Here

    // Replies start here
    public void onParseReply(final List<String> parsedArray, final String rawSource) {
        final String normalMessage = parsedArray.get(3);
        final String message = normalMessage.substring(1, normalMessage.length() - 1);

        // TODO - THIS IS INCOMPLETE
        if (message.startsWith("ACTION")) {
            // Nothing should be done for an action reply - it is technically invalid
        } else if (message.startsWith("FINGER")) {
        } else if (message.startsWith("VERSION")) {
            // Pass this on to the server
            final String nick = IRCUtils.getNickFromRaw(rawSource);
            final String version = message.replace("VERSION", "");
            getServerEventBus().postAndStoreEvent(new VersionEvent(nick, version));
        } else if (message.startsWith("SOURCE")) {
        } else if (message.startsWith("USERINFO")) {
        } else if (message.startsWith("ERRMSG")) {
        } else if (message.startsWith("PING")) {
        } else if (message.startsWith("TIME")) {
        }
    }
    // Replies end here

    private ServerEventBus getServerEventBus() {
        return mEventBus;
    }

    private Server getServer() {
        return mServer;
    }
}