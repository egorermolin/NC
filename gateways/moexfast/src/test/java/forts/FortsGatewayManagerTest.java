package forts;

import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.ncapital.gateways.fortsfast.*;
import ru.ncapital.gateways.moexfast.DefaultMarketDataHandler;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.IMarketDataHandler;

/**
 * Created by egore on 2/2/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class FortsGatewayManagerTest {

    IFortsGatewayConfiguration configuration = new FortsNullGatewayConfiguration() {
        @Override
        public IMarketDataHandler getMarketDataHandler() {
            return new DefaultMarketDataHandler();
        }

        @Override
        public String getFastTemplatesFile() {
            return "src/main/resources/forts/fast_templates.xml";
        }

        @Override
        public String getNetworkInterface() {
            return "lo";
        }

        @Override
        public String getConnectionsFile() {
            return "src/main/resources/forts/config_test_internet.xml";
        }
    };

    @Before
    public void setup() {
        FortsGatewayManager.create(configuration);
    }

    @Test
    public void testCreate() {
        FortsMarketDataManager md = Guice.createInjector(new FortsGatewayModule()).getInstance(FortsMarketDataManager.class);
        md.configure(configuration);
        assert md.getHeartbeatProcessor() != null;
    }
}
