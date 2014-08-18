package co.fusionx.relay.base.relay;

import com.google.common.collect.FluentIterable;

import android.os.Handler;
import android.util.Pair;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import co.fusionx.relay.base.ConnectionManager;
import co.fusionx.relay.base.ConnectionStatus;
import co.fusionx.relay.base.Server;
import co.fusionx.relay.base.ServerConfiguration;
import co.fusionx.relay.interfaces.RelayConfiguration;
import co.fusionx.relay.misc.RelayConfigurationProvider;
import gnu.trove.map.hash.THashMap;

public class RelayConnectionManager implements ConnectionManager {

    private static ConnectionManager sConnectionManager;

    private final Map<String, RelayIRCConnection> mConnectionMap = new THashMap<>();

    private RelayConnectionManager() {
    }

    /**
     * Returns a singleton connection manager which is lazily created
     *
     * @param preferences a concrete implementation of the
     *                    {@link co.fusionx.relay.interfaces.RelayConfiguration} interface
     * @return the connection manager which was created
     */
    public static ConnectionManager getConnectionManager(final RelayConfiguration preferences) {
        if (sConnectionManager == null) {
            sConnectionManager = new RelayConnectionManager();
            RelayConfigurationProvider.onInterfaceReceived(preferences);
        }
        return sConnectionManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<Boolean, ? extends Server> requestConnection(final ServerConfiguration
            configuration, final Collection<String> ignoreList, final Handler errorHandler) {
        RelayIRCConnection connection = mConnectionMap.get(configuration.getTitle());

        final boolean exists = connection != null;
        if (!exists) {
            connection = new RelayIRCConnection(configuration, errorHandler, ignoreList);
            connection.startConnection();
            mConnectionMap.put(configuration.getTitle(), connection);
        }
        return new Pair<>(exists, connection.getServer());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestReconnection(final Server server) {
        final RelayIRCConnection connection = mConnectionMap.get(server.getTitle());

        if (connection == null) {
            throw new IllegalArgumentException("Server not managed by this manager");
        }

        if (connection.getStatus() != ConnectionStatus.DISCONNECTED) {
            throw new IllegalArgumentException("Server not in disconnected state");
        }

        connection.startConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requestStoppageAndRemoval(final String serverName) {
        final RelayIRCConnection connection = mConnectionMap.get(serverName);
        if (connection != null) {
            connection.stopConnection();
            mConnectionMap.remove(serverName);
        }
        return mConnectionMap.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestDisconnectAll() {
        for (final RelayIRCConnection connection : mConnectionMap.values()) {
            connection.stopConnection();
        }
        mConnectionMap.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Server getServerIfExists(final String serverName) {
        if (mConnectionMap.containsKey(serverName)) {
            return mConnectionMap.get(serverName).getServer();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getServerCount() {
        return mConnectionMap.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<? extends Server> getImmutableServerSet() {
        return FluentIterable.from(mConnectionMap.values())
                .transform(RelayIRCConnection::getServer)
                .toSet();
    }
}