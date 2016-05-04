// Copyright 2016 Orc Software AB All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.ncapital.gateways.micexfast.ConfigurationManager;
import ru.ncapital.gateways.micexfast.InstrumentManager;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.connection.Connection;
import ru.ncapital.gateways.micexfast.connection.ConnectionId;
import ru.ncapital.gateways.micexfast.connection.multicast.MessageReader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;

import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by egore on 5/4/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageReaderTest {
    private MessageReader messageReaderAsynch;

    private MessageReader messageReaderSynch;

    @Mock
    private ConfigurationManager configurationManager;

    @Mock
    private InstrumentManager instrumentManager;

    @Mock
    private MarketDataManager marketDataManager;

    @Mock
    private Connection connection;

    @Mock
    private DatagramChannel channel;

    @Mock
    private MembershipKey membershipKey;

    @Before
    public void setup() throws IOException {
        when(configurationManager.getConnection(any(ConnectionId.class))).thenReturn(connection);
        when(configurationManager.getPrimaryNetworkInterface()).thenReturn("localhost");
        when(configurationManager.getSecondaryNetworkInterface()).thenReturn("localhost");
        when(configurationManager.getFastTemplatesFile()).thenReturn("src/main/resources/fast_templates.xml");
        when(channel.join(any(InetAddress.class), any(NetworkInterface.class), any(InetAddress.class))).thenReturn(membershipKey);

        when(configurationManager.isAsynchChannelReader()).thenReturn(true);
        messageReaderAsynch = new MessageReader(ConnectionId.CURR_INSTRUMENT_SNAP_A, configurationManager, marketDataManager, instrumentManager) {
            @Override
            public DatagramChannel openChannel() throws IOException {
                return channel;
            }

            @Override
            public NetworkInterface getNetworkInterface(String name) throws SocketException {
                return NetworkInterface.getNetworkInterfaces().nextElement();
            }
        };

        when(configurationManager.isAsynchChannelReader()).thenReturn(false);
        messageReaderSynch = new MessageReader(ConnectionId.CURR_INSTRUMENT_SNAP_A, configurationManager, marketDataManager, instrumentManager) {
            @Override
            public DatagramChannel openChannel() throws IOException {
                return channel;
            }

            @Override
            public NetworkInterface getNetworkInterface(String name) throws SocketException {
                return NetworkInterface.getNetworkInterfaces().nextElement();
            }
        };

    }

    @Test
    public void testInitSynch() {
        try {
            messageReaderSynch.init("debug");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testInitAsynch() {
        try {
            messageReaderAsynch.init("debug");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testStart() {
        testInitSynch();

        messageReaderSynch.start();
    }
}
