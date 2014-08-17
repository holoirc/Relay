package co.fusionx.relay.event.channel;

import com.google.common.base.Optional;

import co.fusionx.relay.base.Channel;
import co.fusionx.relay.base.ChannelUser;
import co.fusionx.relay.base.Nick;
import co.fusionx.relay.base.relay.RelayChannelUser;

public class ChannelWorldKickEvent extends ChannelWorldUserEvent {

    public final Nick kickingNick;

    public final String kickingNickString;

    public final String reason;

    public ChannelWorldKickEvent(final Channel channel, final ChannelUser kickedUser,
            final Optional<RelayChannelUser> optKickingUser, final String kickingNickString,
            final String reason) {
        super(channel, kickedUser.getNick());

        this.kickingNick = optKickingUser.transform(ChannelUser::getNick).orNull();
        this.kickingNickString = kickingNickString;
        this.reason = reason;
    }
}