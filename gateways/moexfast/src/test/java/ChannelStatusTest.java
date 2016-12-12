import org.junit.Before;
import org.junit.Test;
import ru.ncapital.gateways.moexfast.domain.impl.ChannelStatus;
import ru.ncapital.gateways.moexfast.domain.intf.IChannelStatus;

import static org.junit.Assert.assertTrue;

/**
 * Created by egore on 12/12/2016.
 */
public class ChannelStatusTest {
    private ChannelStatus channelStatus;

    @Before
    public void setup() {
        channelStatus = new ChannelStatus();
    }

    @Test
    public void testAllUpFUT() {
        // A
        channelStatus.addChannel(IChannelStatus.ChannelType.BBO);
        channelStatus.addChannel(IChannelStatus.ChannelType.Statistics);
        channelStatus.addChannel(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannel(IChannelStatus.ChannelType.Instrument);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.BBO);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.Statistics);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.Instrument);

        // B
        channelStatus.addChannel(IChannelStatus.ChannelType.BBO);
        channelStatus.addChannel(IChannelStatus.ChannelType.Statistics);
        channelStatus.addChannel(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannel(IChannelStatus.ChannelType.Instrument);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.BBO);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.Statistics);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.Instrument);

        ChannelStatus result = channelStatus.checkAll();
        assertTrue(result.isChannelUp(IChannelStatus.ChannelType.All) == 1);
    }

    @Test
    public void testAllPartialFUT() {
        // A
        channelStatus.addChannel(IChannelStatus.ChannelType.BBO);
        channelStatus.addChannel(IChannelStatus.ChannelType.Statistics);
        channelStatus.addChannel(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannel(IChannelStatus.ChannelType.Instrument);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.BBO);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.Statistics);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.Instrument);

        ChannelStatus result = channelStatus.checkAll();
        assertTrue(result.isChannelUp(IChannelStatus.ChannelType.All) == 0);
    }

    @Test
    public void testAllDownFUT() {
        // A
        channelStatus.addChannel(IChannelStatus.ChannelType.BBO);
        channelStatus.addChannel(IChannelStatus.ChannelType.Statistics);
        channelStatus.addChannel(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannel(IChannelStatus.ChannelType.Instrument);

        // B
        channelStatus.addChannel(IChannelStatus.ChannelType.BBO);
        channelStatus.addChannel(IChannelStatus.ChannelType.Statistics);
        channelStatus.addChannel(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannel(IChannelStatus.ChannelType.Instrument);

        ChannelStatus result = channelStatus.checkAll();
        assertTrue(result.isChannelUp(IChannelStatus.ChannelType.All) == -1);
    }

    @Test
    public void testAllUpCURR() {
        // A
        channelStatus.addChannel(IChannelStatus.ChannelType.BBOAndStatistics);
        channelStatus.addChannel(IChannelStatus.ChannelType.PublicTrade);
        channelStatus.addChannel(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannel(IChannelStatus.ChannelType.Instrument);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.BBOAndStatistics);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.PublicTrade);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.Instrument);

        // B
        channelStatus.addChannel(IChannelStatus.ChannelType.BBOAndStatistics);
        channelStatus.addChannel(IChannelStatus.ChannelType.PublicTrade);
        channelStatus.addChannel(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannel(IChannelStatus.ChannelType.Instrument);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.BBOAndStatistics);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.PublicTrade);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.Instrument);

        ChannelStatus result = channelStatus.checkAll();
        assertTrue(result.isChannelUp(IChannelStatus.ChannelType.All) == 1);
    }

    @Test
    public void testAllPartialCURR() {
        // A
        channelStatus.addChannel(IChannelStatus.ChannelType.BBOAndStatistics);
        channelStatus.addChannel(IChannelStatus.ChannelType.PublicTrade);
        channelStatus.addChannel(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannel(IChannelStatus.ChannelType.Instrument);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.BBOAndStatistics);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.PublicTrade);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannelUp(IChannelStatus.ChannelType.Instrument);

        ChannelStatus result = channelStatus.checkAll();
        assertTrue(result.isChannelUp(IChannelStatus.ChannelType.All) == 0);
    }

    @Test
    public void testAllDownCURR() {
        // A
        channelStatus.addChannel(IChannelStatus.ChannelType.BBOAndStatistics);
        channelStatus.addChannel(IChannelStatus.ChannelType.PublicTrade);
        channelStatus.addChannel(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannel(IChannelStatus.ChannelType.Instrument);

        // B
        channelStatus.addChannel(IChannelStatus.ChannelType.BBOAndStatistics);
        channelStatus.addChannel(IChannelStatus.ChannelType.PublicTrade);
        channelStatus.addChannel(IChannelStatus.ChannelType.OrderList);
        channelStatus.addChannel(IChannelStatus.ChannelType.Instrument);

        ChannelStatus result = channelStatus.checkAll();
        assertTrue(result.isChannelUp(IChannelStatus.ChannelType.All) == -1);
    }
}
