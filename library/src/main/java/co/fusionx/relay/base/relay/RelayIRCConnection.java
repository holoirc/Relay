package co.fusionx.relay.base.relay;

import com.google.common.base.Function;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.Collection;

import co.fusionx.relay.base.ConnectionStatus;
import co.fusionx.relay.base.Server;
import co.fusionx.relay.base.ServerConfiguration;
import co.fusionx.relay.event.channel.ChannelConnectEvent;
import co.fusionx.relay.event.channel.ChannelDisconnectEvent;
import co.fusionx.relay.event.channel.ChannelEvent;
import co.fusionx.relay.event.channel.ChannelStopEvent;
import co.fusionx.relay.event.query.QueryConnectEvent;
import co.fusionx.relay.event.query.QueryDisconnectEvent;
import co.fusionx.relay.event.query.QueryEvent;
import co.fusionx.relay.event.query.QueryStopEvent;
import co.fusionx.relay.event.server.ConnectEvent;
import co.fusionx.relay.event.server.ConnectingEvent;
import co.fusionx.relay.event.server.DisconnectEvent;
import co.fusionx.relay.event.server.ReconnectEvent;
import co.fusionx.relay.event.server.ServerEvent;
import co.fusionx.relay.event.server.StopEvent;
import co.fusionx.relay.parser.connection.ServerConnectionParser;
import co.fusionx.relay.parser.main.ServerLineParser;
import co.fusionx.relay.sender.relay.RelayCapSender;
import co.fusionx.relay.sender.relay.RelayInternalSender;
import co.fusionx.relay.sender.relay.RelayServerLineSender;
import co.fusionx.relay.util.SocketUtils;
import co.fusionx.relay.util.Utils;

import static co.fusionx.relay.misc.RelayConfigurationProvider.getPreferences;

public class RelayIRCConnection {

    private final Handler mUiThreadHandler;

    private final Handler mCallHandler;

    private final ServerConfiguration mServerConfiguration;

    private final RelayServerLineSender mRelayServerLineSender;

    private final RelayServer mServer;

    private final RelayCapSender mCapSender;

    private final RelayInternalSender mInternalSender;

    private Thread mMainThread;

    private ConnectionStatus mStatus = ConnectionStatus.DISCONNECTED;

    private Socket mSocket;

    private int mReconnectAttempts;

    private ServerLineParser mLineParser;

    private boolean mStopped;

    RelayIRCConnection(final ServerConfiguration serverConfiguration, final Handler handler,
            final Collection<String> ignoreList) {
        mServerConfiguration = serverConfiguration;
        mUiThreadHandler = handler;

        final HandlerThread handlerThread = new HandlerThread("ServerCalls");
        handlerThread.start();
        mCallHandler = new Handler(handlerThread.getLooper());

        mServer = new RelayServer(serverConfiguration, this, mCallHandler, ignoreList);
        mRelayServerLineSender = mServer.getRelayServerLineSender();
        mCapSender = new RelayCapSender(mRelayServerLineSender);
        mInternalSender = new RelayInternalSender(mRelayServerLineSender);
    }

    void startConnection() {
        mMainThread = new Thread(() -> {
            try {
                connectToServer();
            } catch (final Exception ex) {
                mUiThreadHandler.post(() -> {
                    throw new RuntimeException(getCurrentLine(), ex);
                });
            }
        });
        mMainThread.start();
    }

    void stopConnection() {
        mCallHandler.post(() -> {
            // Send the stop events and set the status before we talk to the server - ensures
            // that we don't get concurrent modifications
            onStopped();

            if (mStatus == ConnectionStatus.CONNECTED) {
                mStopped = true;
                mInternalSender.quitServer(getPreferences().getQuitReason());
            } else if (mMainThread.isAlive()) {
                mMainThread.interrupt();
            }
            closeSocket();
            mServer.onConnectionTerminated();
        });
    }

    RelayServer getServer() {
        return mServer;
    }

    ConnectionStatus getStatus() {
        return mStatus;
    }

    public String getCurrentLine() {
        if (mLineParser != null) {
            return mLineParser.getCurrentLine();
        }
        return "";
    }

    /**
     * Method which keeps trying to reconnect to the server the number of times specified and if
     * the user has not explicitly tried to disconnect
     */
    private void connectToServer() {
        connect();

        for (mReconnectAttempts = 0; !mStopped && isReconnectNeeded(); mReconnectAttempts++) {
            onReconnecting();

            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                // This interrupt will *should* only ever occur if the user explicitly kills
                // reconnection
                return;
            }
            connect();
        }
        if (mStopped) {
            return;
        }
        onDisconnected("Disconnected from server (no reconnect pending).", false);
    }

    /**
     * Closes the socket if it is not already closed
     */
    private void closeSocket() {
        if (mSocket == null || mSocket.isClosed()) {
            mSocket = null;
            return;
        }

        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSocket = null;
    }

    private void connect() {
        String disconnectMessage = "";
        try {
            mSocket = SocketUtils.openSocketConnection(mServerConfiguration);

            final BufferedWriter socketWriter = SocketUtils.getSocketBufferedWriter(mSocket);
            mServer.onOutputStreamCreated(socketWriter);

            // We are now in the phase where we can say we are connecting to the server
            onConnecting();

            if (mServerConfiguration.isSaslAvailable()) {
                // By sending this line, the server *should* wait until we end the CAP stuff with
                // CAP END
                mCapSender.sendSupportedCAP();
            }

            if (Utils.isNotEmpty(mServerConfiguration.getServerPassword())) {
                mInternalSender.sendServerPassword(mServerConfiguration.getServerPassword());
            }

            // Send NICK and USER lines to the server
            mServer.sendNick(mServerConfiguration.getNickStorage().getFirstChoiceNick());
            mInternalSender.sendUser(mServerConfiguration.getServerUserName(),
                    Utils.returnNonEmpty(mServerConfiguration.getRealName(), "RelayUser"));

            final BufferedReader reader = SocketUtils.getSocketBufferedReader(mSocket);
            final ServerConnectionParser parser = new ServerConnectionParser(mServer,
                    mServerConfiguration, reader, mRelayServerLineSender);
            final String nick = parser.parseConnect();

            // This nick may well be different from any of the nicks in storage - get the
            // *official* nick from the server itself and use it
            // If the nick is null then we have no hope of progressing
            if (!TextUtils.isEmpty(nick)) {
                onStartParsing(nick, reader);
            }
        } catch (final IOException ex) {
            // Usually occurs when WiFi/3G is turned off on the device - usually fruitless to try
            // to reconnect but hey ho
            disconnectMessage = ex.getMessage();
        }

        // If it was stopped then this cleanup would have already been performed
        if (mStopped) {
            return;
        }
        onDisconnected(disconnectMessage, isReconnectNeeded());
        closeSocket();
        mServer.onConnectionTerminated();
    }

    private void onStartParsing(final String nick, final BufferedReader reader) throws IOException {
        // Since we are now connected, reset the reconnect attempts
        mReconnectAttempts = 0;
        mServer.getUser().setNick(nick);
        onConnected();

        // Identifies with NickServ if the password exists
        if (Utils.isNotEmpty(mServerConfiguration.getNickservPassword())) {
            mInternalSender.sendNickServPassword(mServerConfiguration.getNickservPassword());
        }

        final Collection<RelayChannel> channels = mServer.getUser().getChannels();
        if (channels.isEmpty()) {
            // Automatically join the channels specified in the configuration
            for (final String channelName : mServerConfiguration.getAutoJoinChannels()) {
                mServer.sendJoin(channelName);
            }
        } else {
            for (final RelayChannel channel : channels) {
                mServer.sendJoin(channel.getName());
            }
        }

        // Initialise the parser used to parse any lines from the server
        mLineParser = new ServerLineParser(mServer);
        // Loops forever until broken
        mLineParser.parseMain(reader, mRelayServerLineSender);
    }

    private void onConnecting() {
        mStatus = ConnectionStatus.CONNECTING;

        mServer.postAndStoreEvent(new ConnectingEvent(mServer));
    }

    private void onReconnecting() {
        mStatus = ConnectionStatus.RECONNECTING;

        mServer.postAndStoreEvent(new ReconnectEvent(mServer));
    }

    private void onConnected() {
        onStatusChanged(ConnectionStatus.CONNECTED,
                ChannelConnectEvent::new,
                QueryConnectEvent::new,
                server -> new ConnectEvent(server, mServerConfiguration.getUrl()));
    }

    private void onDisconnected(final String serverMessage, final boolean retryPending) {
        onStatusChanged(ConnectionStatus.DISCONNECTED,
                channel -> new ChannelDisconnectEvent(channel, serverMessage),
                user -> new QueryDisconnectEvent(user, serverMessage),
                server -> new DisconnectEvent(server, serverMessage, retryPending));
    }

    private void onStopped() {
        mStatus = ConnectionStatus.STOPPED;

        for (final RelayChannel channel : mServer.getUser().getChannels()) {
            channel.postAndStoreEvent(new ChannelStopEvent(channel));
            channel.markInvalid();
        }

        for (final RelayQueryUser user : mServer.getUserChannelInterface().getQueryUsers()) {
            user.postAndStoreEvent(new QueryStopEvent(user));
            user.markInvalid();
        }

        mServer.postAndStoreEvent(new StopEvent(mServer));
        mServer.markInvalid();
    }

    private void onStatusChanged(final ConnectionStatus status,
            final Function<RelayChannel, ChannelEvent> channelFunction,
            final Function<RelayQueryUser, QueryEvent> queryFunction,
            final Function<Server, ServerEvent> serverFunction) {
        mStatus = status;

        for (final RelayChannel channel : mServer.getUser().getChannels()) {
            channel.postAndStoreEvent(channelFunction.apply(channel));
        }

        for (final RelayQueryUser user : mServer.getUserChannelInterface().getQueryUsers()) {
            user.postAndStoreEvent(queryFunction.apply(user));
        }

        mServer.postAndStoreEvent(serverFunction.apply(mServer));
    }

    private boolean isReconnectNeeded() {
        return mReconnectAttempts < getPreferences().getReconnectAttemptsCount();
    }
}