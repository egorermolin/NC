package micex;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.micexfast.MicexMarketDataManager;
import ru.ncapital.gateways.micexfast.MicexNullGatewayConfiguration;
import ru.ncapital.gateways.micexfast.messagehandlers.MicexStatisticsMessageHandler;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by egore on 11/6/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class MicexStatisticsMessageHandlerTest {
    @Captor
    ArgumentCaptor<BBO<String>> bbo;

    @Mock
    private MicexMarketDataManager marketDataManager;

    private MicexStatisticsMessageHandler messageHandler;

    @Before
    public void setup() {
        messageHandler = new MicexStatisticsMessageHandler(marketDataManager, new MicexNullGatewayConfiguration());
        when(marketDataManager.createBBO(anyString())).thenCallRealMethod();
    }

    private Message getMessage(int mdEntriesLength, String symbol, String tradingSessionId) {
        Message readMessage = mock(Message.class);
        when(readMessage.getString("MessageType")).thenReturn("W");
        when(readMessage.getString("Symbol")).thenReturn(symbol);
        when(readMessage.getString("TradingSessionID")).thenReturn(tradingSessionId);

        SequenceValue mdEntries = mock(SequenceValue.class);
        when(readMessage.getSequence("GroupMDEntries")).thenReturn(mdEntries);

        when(mdEntries.getLength()).thenReturn(mdEntriesLength);
        for (int i = 0; i < mdEntriesLength; ++i)
            when(mdEntries.get(i)).thenReturn(mock(GroupValue.class));

        return readMessage;
    }

    @Test
    public void testStatisticsEmptySnapshot() {
        Message readMessage = getMessage(1, "SYMB", "CETS");
        when(readMessage.getSequence("GroupMDEntries").get(0).getString("MDEntryType")).thenReturn("J");

        messageHandler.onSnapshot(readMessage);

        verify(marketDataManager).onBBO(bbo.capture());
        assert bbo.getValue().isEmpty();
    }
}
