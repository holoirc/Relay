package com.fusionx.relay;

import com.fusionx.relay.communication.ServerCallBus;
import com.fusionx.relay.communication.ServerEventBus;
import com.fusionx.relay.connection.ServerConnection;
import com.fusionx.relay.event.server.ServerEvent;
import com.fusionx.relay.writers.ChannelWriter;
import com.fusionx.relay.writers.ServerWriter;
import com.fusionx.relay.writers.UserWriter;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Server {

    private final ServerConnection mServerConnection;

    private final UserChannelInterface mUserChannelInterface;

    private final List<ServerEvent> mBuffer;

    private final ServerConfiguration mConfiguration;

    private final ServerEventBus mServerEventBus;

    private final ServerCallBus mServerCallBus;

    private AppUser mUser;

    private List<String> mIgnoreList;

    public Server(final ServerConfiguration configuration, final ServerConnection connection) {
        mServerConnection = connection;
        mConfiguration = configuration;
        mBuffer = new ArrayList<>();
        mServerEventBus = new ServerEventBus(this);
        mServerCallBus = new ServerCallBus(connection);
        mUserChannelInterface = new UserChannelInterface(this);
        mIgnoreList = new ArrayList<>();
    }

    public void onServerEvent(final ServerEvent event) {
        mBuffer.add(event);
    }

    public void onDisconnect() {
        // Null can occur if the connection does not occur on initial connect
        if (mUser != null) {
            mUser.onDisconnect();
        }
        mUserChannelInterface.onDisconnect();

        // Need to remove old writers as they would be using the old socket OutputStream if a
        // reconnection occurs
        mServerCallBus.onDisconnect();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Server) {
            final Server server = (Server) o;
            return server.getTitle().equals(getTitle());
        }
        return false;
    }

    /**
     * Sets up the writers based on the output stream passed into the method
     *
     * @param writer the which the writers will use
     * @return the server writer created from the OutputStreamWriter
     */
    public ServerWriter onOutputStreamCreated(final Writer writer) {
        final ServerWriter serverWriter = new ServerWriter(writer);
        mServerCallBus.register(serverWriter);
        mServerCallBus.register(new ChannelWriter(writer));
        mServerCallBus.register(new UserWriter(writer));
        return serverWriter;
    }

    public void setIgnoreList(final Collection<String> collection) {
        mIgnoreList = new ArrayList<>(collection);
    }

    public boolean shouldIgnoreUser(final String userNick) {
        return mIgnoreList.contains(userNick);
    }

    // Getters and Setters
    public List<ServerEvent> getBuffer() {
        return mBuffer;
    }

    public UserChannelInterface getUserChannelInterface() {
        return mUserChannelInterface;
    }

    public AppUser getUser() {
        return mUser;
    }

    public void setUser(final AppUser user) {
        mUser = user;
    }

    public String getTitle() {
        return mConfiguration.getTitle();
    }

    public ConnectionStatus getStatus() {
        return mServerConnection.getStatus();
    }

    public ServerCallBus getServerCallBus() {
        return mServerCallBus;
    }

    public ServerEventBus getServerEventBus() {
        return mServerEventBus;
    }
}