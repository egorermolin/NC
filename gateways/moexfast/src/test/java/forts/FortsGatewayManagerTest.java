package forts;

import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import ru.ncapital.gateways.fortsfast.FortsGatewayManager;
import ru.ncapital.gateways.fortsfast.FortsGatewayModule;
import ru.ncapital.gateways.fortsfast.FortsMarketDataManager;
import ru.ncapital.gateways.fortsfast.FortsNullGatewayConfiguration;
import ru.ncapital.gateways.moexfast.DefaultMarketDataHandler;
import ru.ncapital.gateways.moexfast.IMarketDataHandler;

/**
 * Created by egore on 2/2/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class FortsGatewayManagerTest {

    @Before
    public void setup() {
        FortsGatewayManager.create(new FortsNullGatewayConfiguration() {
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
        });
    }

    @Test
    public void testCreate() {
        FortsMarketDataManager md = Guice.createInjector(new FortsGatewayModule()).getInstance(FortsMarketDataManager.class);
        assert md.getHeartbeatProcessor() != null;
    }
}
