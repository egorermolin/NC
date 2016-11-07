package micex;

import com.google.inject.Guice;
import com.google.inject.Key;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import ru.ncapital.gateways.micexfast.MicexGatewayModule;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;

/**
 * Created by egore on 17.02.2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class MicexMessageSequenceValidatorForPublicTradesTest {

    private IMessageSequenceValidator<String> sequenceValidator =
                     Guice.createInjector(new MicexGatewayModule())
                    .getInstance(new Key<MessageSequenceValidatorFactory<String>>(){})
                    .createMessageSequenceValidatorForPublicTrades();

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
