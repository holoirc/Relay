package co.fusionx.relay.internal.base;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.os.Parcel;

import co.fusionx.relay.base.ServerConfiguration;

import static co.fusionx.relay.base.ServerConfiguration.Builder.CREATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@Config(sdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ServerConfigurationTest {

    // Builder tests
    @Test
    public void testBuilderParcelling() {
        final Parcel parcel = Parcel.obtain();
        final ServerConfiguration.Builder expected = TestUtils.getFreenodeBuilder();
        expected.writeToParcel(parcel, 0);

        // done writing, now reset parcel for reading
        parcel.setDataPosition(0);

        final ServerConfiguration.Builder actual = CREATOR.createFromParcel(parcel);

        assertThat(actual)
                .isNotNull()
                .isEqualToComparingFieldByField(expected);
    }

    // Configuration tests
    @Test
    public void testBuild() {
        final ServerConfiguration.Builder builder = TestUtils.getFreenodeBuilder();
        final ServerConfiguration configuration = TestUtils.getFreenodeConfiguration();
        assertEquals(builder.getTitle(), configuration.getTitle());
    }

    @Test
    public void testParcelling() {
        final Parcel parcel = Parcel.obtain();
        final ServerConfiguration expected = TestUtils.getFreenodeConfiguration();
        expected.writeToParcel(parcel, 0);

        // done writing, now reset parcel for reading
        parcel.setDataPosition(0);

        final ServerConfiguration actual = ServerConfiguration.CREATOR.createFromParcel(parcel);

        assertThat(actual)
                .isNotNull()
                .isEqualToComparingFieldByField(expected);
    }
}