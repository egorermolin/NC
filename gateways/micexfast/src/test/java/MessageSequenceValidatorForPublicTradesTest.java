import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import ru.ncapital.gateways.micexfast.GatewayModule;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;

/**
 * Created by egore on 17.02.2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageSequenceValidatorForPublicTradesTest {
    IMessageSequenceValidator sequenceValidator = Guice.createInjector(new GatewayModule()).getInstance(MessageSequenceValidatorFactory.class).createMessageSequenceValidatorForPublicTrades();

    @Before
    public void setup() {
    }

    @Test
    public void testPublicTradesOutOfOrder() {
        assert !sequenceValidator.isRecovering("AAA", false);
        assert sequenceValidator.onIncrementalSeq("AAA", 100);
        assert sequenceValidator.onIncrementalSeq("AAA", 102);
        assert sequenceValidator.onIncrementalSeq("AAA", 101);
        assert sequenceValidator.onIncrementalSeq("AAA", 103);
        assert sequenceValidator.onIncrementalSeq("AAA", 105);
        assert !sequenceValidator.isRecovering("AAA", false);
    }
}
