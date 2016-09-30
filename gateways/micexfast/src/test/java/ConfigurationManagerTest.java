import org.junit.Assert;
import org.junit.Test;
import ru.ncapital.gateways.micexfast.ConfigurationManager;
import ru.ncapital.gateways.micexfast.NullGatewayConfiguration;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;

/**
 * Created by egore on 10.02.2016.
 */
public class ConfigurationManagerTest {
    @Test
    public void testConfigurationManager() {
        ConfigurationManager cm = new ConfigurationManager().configure(new NullGatewayConfiguration() {
            @Override
            public String getFastTemplatesFile() {
                return "src/main/resources/fast_templates.xml";
            }

            @Override
            public String getNetworkInterface() {
                return "eth0";
            }

            @Override
            public String getConnectionsFile() {
                return "src/main/resources/config_test_internet.xml";
            }
        });

        for (ConnectionId id : ConnectionId.values())
            assert cm.getConnection(id) != null;

        assert cm.getFastTemplatesFile() == "src/main/resources/fast_templates.xml";

        Assert.assertEquals("eth0", cm.getPrimaryNetworkInterface());
        Assert.assertEquals("eth0", cm.getSecondaryNetworkInterface());
    }

    @Test
    public void testConfigurationManager2Interfaces() {
        ConfigurationManager cm = new ConfigurationManager().configure(new NullGatewayConfiguration() {
            @Override
            public String getFastTemplatesFile() {
                return "src/main/resources/fast_templates.xml";
            }

            @Override
            public String getNetworkInterface() {
                return "eth0;eth1";
            }

            @Override
            public String getConnectionsFile() {
                return "src/main/resources/config_test_internet.xml";
            }
        });

        Assert.assertEquals("eth0", cm.getPrimaryNetworkInterface());
        Assert.assertEquals("eth1", cm.getSecondaryNetworkInterface());
    }
}
