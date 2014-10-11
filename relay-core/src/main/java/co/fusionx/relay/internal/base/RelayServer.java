package co.fusionx.relay.internal.base;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import javax.inject.Inject;

import co.fusionx.relay.constant.Capability;
import co.fusionx.relay.configuration.SessionConfiguration;
import co.fusionx.relay.event.Event;
import co.fusionx.relay.event.server.ServerEvent;
import co.fusionx.relay.internal.core.Postable;
import co.fusionx.relay.internal.core.InternalServer;
import co.fusionx.relay.sender.ServerSender;

public class RelayServer extends AbstractConversation<ServerEvent>
        implements InternalServer {

    private final SessionConfiguration mConfiguration;

    private final Set<Capability> mCapabilities;

    private final ServerSender mServerSender;

    @Inject
    public RelayServer(final Postable<Event> sessionBus,
            final SessionConfiguration configuration, final ServerSender serverSender,
            final Set<Capability> capCapabilities) {
        super(sessionBus);

        mConfiguration = configuration;
        mCapabilities = capCapabilities;
        mServerSender = serverSender;
    }

    // Internal methods
    @Override
    public void addCapability(final Capability capability) {
        mCapabilities.add(capability);
    }

    // Conversation Interface
    @Override
    public String getId() {
        return getTitle();
    }

    // Server Interface
    @Override
    public String getTitle() {
        return mConfiguration.getConnectionConfiguration().getTitle();
    }

    @Override
    public SessionConfiguration getConfiguration() {
        return mConfiguration;
    }

    @Override
    public ImmutableSet<Capability> getCapabilities() {
        return ImmutableSet.copyOf(mCapabilities);
    }

    // Equals and hashcode
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InternalServer)) {
            return false;
        }

        final InternalServer server = (InternalServer) o;
        return getTitle().equals(server.getTitle());
    }

    @Override
    public int hashCode() {
        return getTitle().hashCode();
    }

    // ServerSender interface
    @Override
    public void sendJoin(final String channelName) {
        mServerSender.sendJoin(channelName);
    }

    @Override
    public void sendNick(final String newNick) {
        mServerSender.sendNick(newNick);
    }

    @Override
    public void sendWhois(final String nick) {
        mServerSender.sendWhois(nick);
    }

    @Override
    public void sendRawLine(final String rawLine) {
        mServerSender.sendRawLine(rawLine);
    }
}