package forts;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.*;
import org.openfast.codec.Coder;
import ru.ncapital.gateways.fortsfast.FortsGatewayManager;
import ru.ncapital.gateways.fortsfast.FortsInstrumentManager;
import ru.ncapital.gateways.fortsfast.FortsMarketDataManager;
import ru.ncapital.gateways.fortsfast.IFortsGatewayConfiguration;
import ru.ncapital.gateways.fortsfast.domain.FortsInstrument;
import ru.ncapital.gateways.moexfast.IMarketDataHandler;
import ru.ncapital.gateways.moexfast.InstrumentManager;
import ru.ncapital.gateways.moexfast.connection.ConnectionManager;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.intf.IInstrument;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.*;

/**
 * Created by egore on 4/26/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class FortsInstrumentManagerTest {
    @Mock
    private Context context;

    @Mock
    private Coder coder;

    private FortsInstrumentManager instrumentManager;

    @Mock
    private IFortsGatewayConfiguration configuration;

    @Mock
    private FortsMarketDataManager marketDataManager;

    @Mock
    private ConnectionManager connectionManager;

    @Mock
    private IMarketDataHandler marketDataHandler;

    @Mock
    private FortsGatewayManager gatewayManager;

    @Captor
    private ArgumentCaptor<BBO<Long>> bboCapture;

    @Captor
    private ArgumentCaptor<BBO<Long>> bboCaptureSecurityStatus;

    @Before
    public void setup() {
        when(configuration.getAllowedUnderlyings()).thenReturn(new String[] {"Si", "Eu"});
        when(configuration.getMarketDataHandler()).thenReturn(marketDataHandler);

        instrumentManager = new FortsInstrumentManager();
        instrumentManager.setMarketDataManager(marketDataManager);
        instrumentManager.setGatewayManager(gatewayManager);
        instrumentManager.configure(configuration);

        when(marketDataManager.createBBO(anyLong())).thenCallRealMethod();
        when(marketDataManager.getInstrumentManager()).thenReturn(instrumentManager);
    }

    private void initUnderlyingAndTradingStatus(Message messageMock, String underlying) {
        SequenceValue underlyingSeq = mock(SequenceValue.class);
        GroupValue underlyingGrp = mock(GroupValue.class);

        when(messageMock.getSequence("Underlyings")).thenReturn(underlyingSeq);
        when(underlyingSeq.getLength()).thenReturn(1);
        when(underlyingSeq.get(0)).thenReturn(underlyingGrp);

        when(messageMock.getValue("TradingSessionID")).thenReturn(mock(FieldValue.class));
        when(messageMock.getInt("TradingSessionID")).thenReturn(1);
        when(messageMock.getValue("SecurityTradingStatus")).thenReturn(mock(FieldValue.class));
        when(messageMock.getInt("SecurityTradingStatus")).thenReturn(17);
        when(underlyingGrp.getValue("UnderlyingSymbol")).thenReturn(mock(FieldValue.class));
        when(underlyingGrp.getString("UnderlyingSymbol")).thenReturn(underlying);
    }

    private Message getSecurityDefinitionMessageMock(int num) {
        Message message = Mockito.mock(Message.class);

        when(message.getString("MessageType")).thenReturn("d");
        when(message.getInt("MsgSeqNum")).thenReturn(num);
        when(message.getLong("SendingTime")).thenReturn(System.currentTimeMillis());
        when(message.getInt("TotNumReports")).thenReturn(4);

        switch (num) {
            // added
            case 1:
                when(message.getString("Symbol")).thenReturn("EuZ6");
                when(message.getLong("SecurityID")).thenReturn(101342534L);
                initUnderlyingAndTradingStatus(message, "Eu");
                break;

            case 2:
                when(message.getString("Symbol")).thenReturn("SiZ6");
                when(message.getLong("SecurityID")).thenReturn(99587654L);
                initUnderlyingAndTradingStatus(message, "Si");
                break;

            //ignored
            case 3:
                when(message.getString("Symbol")).thenReturn("VBZ6");
                when(message.getLong("SecurityID")).thenReturn(99586118L);
                initUnderlyingAndTradingStatus(message, "VTBR");
                break;

            case 4:
                when(message.getString("Symbol")).thenReturn("RRX6");
                when(message.getLong("SecurityID")).thenReturn(101378886L);
                initUnderlyingAndTradingStatus(message, "RUONIA");
                break;
        }

        return message;
    }

    private Message getSecurityStatusMessageMock(int num) {
        Message message = Mockito.mock(Message.class);

        when(message.getString("MessageType")).thenReturn("f");
        when(message.getInt("MsgSeqNum")).thenReturn(num);
        when(message.getLong("SendingTime")).thenReturn(System.currentTimeMillis());

        switch (num) {
            // added
            case 1:
                when(message.getString("Symbol")).thenReturn("EuZ6");
                when(message.getLong("SecurityID")).thenReturn(101342534L);
                when(message.getValue("SecurityTradingStatus")).thenReturn(mock(FieldValue.class));
                when(message.getInt("SecurityTradingStatus")).thenReturn(18);
                break;

            case 2:
                when(message.getString("Symbol")).thenReturn("SiZ6");
                when(message.getLong("SecurityID")).thenReturn(99587654L);
                when(message.getValue("SecurityTradingStatus")).thenReturn(mock(FieldValue.class));
                when(message.getInt("SecurityTradingStatus")).thenReturn(18);
                break;

            //ignored
            case 3:
                when(message.getString("Symbol")).thenReturn("VBZ6");
                when(message.getLong("SecurityID")).thenReturn(99586118L);
                when(message.getValue("SecurityTradingStatus")).thenReturn(mock(FieldValue.class));
                when(message.getInt("SecurityTradingStatus")).thenReturn(18);
                break;

            case 4:
                when(message.getString("Symbol")).thenReturn("RRX6");
                when(message.getLong("SecurityID")).thenReturn(101378886L);
                when(message.getValue("SecurityTradingStatus")).thenReturn(mock(FieldValue.class));
                when(message.getInt("SecurityTradingStatus")).thenReturn(18);
                break;
        }

        return message;
    }

    @Test
    public void isAllowedInstrument() {
        testInstrumentAddAndFinish();

        assertTrue(instrumentManager.isAllowedInstrument(FortsInstrument.getExchangeSecurityId(101342534L)));
        assertTrue(instrumentManager.isAllowedInstrument(FortsInstrument.getExchangeSecurityId(99587654L)));
        assertFalse(instrumentManager.isAllowedInstrument(FortsInstrument.getExchangeSecurityId(99586118L)));
        assertFalse(instrumentManager.isAllowedInstrument(FortsInstrument.getExchangeSecurityId(101378886L)));
    }


    @Test
    public void testInstrumentStatus() {
        testInstrumentAddAndFinish();

        Mockito.reset(marketDataManager);
        when(marketDataManager.createBBO(anyLong())).thenCallRealMethod();
        when(marketDataManager.getInstrumentManager()).thenReturn(instrumentManager);

        for (int i : new int [] {1, 1, 2, 2, 3, 3, 4, 4})
            instrumentManager.handleMessage(getSecurityStatusMessageMock(i), context, coder);

        verify(marketDataManager, times(2)).onBBO(bboCaptureSecurityStatus.capture());
        assertEquals("EuZ6", bboCaptureSecurityStatus.getAllValues().get(0).getSecurityId());
        assertEquals("18", bboCaptureSecurityStatus.getAllValues().get(0).getTradingStatus());
        assertEquals("SiZ6", bboCaptureSecurityStatus.getAllValues().get(1).getSecurityId());
        assertEquals("18", bboCaptureSecurityStatus.getAllValues().get(1).getTradingStatus());
    }

    @Test
    public void testInstrumentAddAndFinish() {
        for (int i : new int [] {1, 1, 2, 2, 3, 3, 4, 4})
            instrumentManager.handleMessage(getSecurityDefinitionMessageMock(i), context, coder);

        verify(gatewayManager, times(1)).onInstrumentDownloadFinished();

        verify(marketDataManager, times(2)).onBBO(bboCapture.capture());
        List<BBO<Long>> values = bboCapture.getAllValues();
        Collections.sort(values, new Comparator<BBO>() {
            @Override
            public int compare(BBO o1, BBO o2) {
                return o1.getSecurityId().compareTo(o2.getSecurityId());
            }
        });
        assertEquals("EuZ6", values.get(0).getSecurityId());
        assertEquals("17", values.get(0).getTradingStatus());
        assertEquals("SiZ6", values.get(1).getSecurityId());
        assertEquals("17", values.get(1).getTradingStatus());

        assertEquals(395869L, (long) instrumentManager.getExchangeSecurityId("EuZ6"));
        assertEquals(389014L, (long) instrumentManager.getExchangeSecurityId("SiZ6"));
        assertEquals("EuZ6", instrumentManager.getSecurityId(395869L));
        assertEquals("SiZ6", instrumentManager.getSecurityId(389014L));

        ArgumentCaptor<IInstrument[]> instrumentCapture = ArgumentCaptor.forClass(IInstrument[].class);
        verify(marketDataHandler, times(1)).onInstruments(instrumentCapture.capture());
        assertEquals(2, instrumentCapture.getValue().length);
        Arrays.sort(instrumentCapture.getValue(), new Comparator<IInstrument>() {
            @Override
            public int compare(IInstrument o1, IInstrument o2) {
                return o1.getSecurityId().compareTo(o2.getSecurityId());
            }
        });
        assertEquals("EuZ6", instrumentCapture.getValue()[0].getSecurityId());
        assertEquals("Eu", instrumentCapture.getValue()[0].getUnderlying());
        assertEquals("SiZ6", instrumentCapture.getValue()[1].getSecurityId());
        assertEquals("Si", instrumentCapture.getValue()[1].getUnderlying());
    }

    @Test
    public void testInstrumentAddAndNotFinished() {
        for (int i : new int [] {1, 1, 3, 3, 4, 4})
            instrumentManager.handleMessage(getSecurityDefinitionMessageMock(i), context, coder);

        verify(connectionManager, times(0)).stopInstrument();

        for (int i : new int [] {1, 1, 2, 2, 3, 3, 4, 4})
            instrumentManager.handleMessage(getSecurityDefinitionMessageMock(i), context, coder);

        verify(marketDataManager, times(2)).onBBO(any(BBO.class));
        verify(gatewayManager, times(1)).onInstrumentDownloadFinished();
        ArgumentCaptor<IInstrument[]> instrumentCapture = ArgumentCaptor.forClass(IInstrument[].class);
        verify(marketDataHandler, times(1)).onInstruments(instrumentCapture.capture());
        assertEquals(2, instrumentCapture.getValue().length);
    }

    @Test
    public void testInstrumentAddAndNotFinished2() {
        for (int i : new int [] {1, 1, 2, 2, 4, 4})
            instrumentManager.handleMessage(getSecurityDefinitionMessageMock(i), context, coder);

        verify(gatewayManager, times(0)).onInstrumentDownloadFinished();

        for (int i : new int [] {1, 1, 2, 2, 3, 3, 4, 4})
            instrumentManager.handleMessage(getSecurityDefinitionMessageMock(i), context, coder);

        verify(marketDataManager, times(2)).onBBO(any(BBO.class));
        verify(gatewayManager, times(1)).onInstrumentDownloadFinished();
        ArgumentCaptor<IInstrument[]> instrumentCapture = ArgumentCaptor.forClass(IInstrument[].class);
        verify(marketDataHandler, times(1)).onInstruments(instrumentCapture.capture());
        assertEquals(2, instrumentCapture.getValue().length);
    }

    @Test
    public void testInstrumentAddAndNotFinished3() {
        for (int i : new int [] {1, 1, 3, 3, 4, 4})
            instrumentManager.handleMessage(getSecurityDefinitionMessageMock(i), context, coder);

        verify(gatewayManager, times(0)).onInstrumentDownloadFinished();

        for (int i : new int [] {1, 1, 2, 2, 4, 4})
            instrumentManager.handleMessage(getSecurityDefinitionMessageMock(i), context, coder);

        verify(marketDataManager, times(2)).onBBO(any(BBO.class));
        verify(gatewayManager, times(1)).onInstrumentDownloadFinished();
        ArgumentCaptor<IInstrument[]> instrumentCapture = ArgumentCaptor.forClass(IInstrument[].class);
        verify(marketDataHandler, times(1)).onInstruments(instrumentCapture.capture());
        assertEquals(2, instrumentCapture.getValue().length);
    }

}
