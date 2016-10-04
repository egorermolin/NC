import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import ru.ncapital.gateways.micexfast.*;
import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
import ru.ncapital.gateways.moexfast.DefaultMarketDataHandler;
import ru.ncapital.gateways.moexfast.IMarketDataHandler;

/**
 * Created by egore on 2/2/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class MicexGatewayManagerTest {

    @Before
    public void setup() {
        MicexGatewayManager.create(new MicexNullGatewayConfiguration() {
            @Override
            public IMarketDataHandler getMarketDataHandler() {
                return new DefaultMarketDataHandler();
            }

            @Override
            public String getFastTemplatesFile() {
                return "src/main/resources/micex/fast_templates.xml";
            }

            @Override
            public String getNetworkInterface() {
                return "lo";
            }

            @Override
            public String getConnectionsFile() {
                return "src/main/resources/micex/config_test_internet.xml";
            }

            @Override
            public TradingSessionId[] getAllowedTradingSessionIds() {
                return new TradingSessionId[] {TradingSessionId.CETS};
            }

            @Override
            public ProductType[] getAllowedProductTypes() {
                return new ProductType[]{ProductType.CURRENCY};
            }
        });
    }

    @Test
    public void testCreate() {
        MicexMarketDataManager md = Guice.createInjector(new MicexGatewayModule()).getInstance(MicexMarketDataManager.class);
        assert md.getHeartbeatProcessor() != null;
    }
}
