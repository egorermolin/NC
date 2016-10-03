package ru.ncapital.gateways.micexfast;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import ru.ncapital.gateways.micexfast.messagehandlers.MicexOrderListMessageHandler;
import ru.ncapital.gateways.micexfast.messagehandlers.MicexPublicTradesMessageHandler;
import ru.ncapital.gateways.micexfast.messagehandlers.MicexStatisticsMessageHandler;
import ru.ncapital.gateways.moexfast.ConfigurationManager;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.*;
import ru.ncapital.gateways.moexfast.messagehandlers.*;

/**
 * Created by egore on 02.02.2016.
 */
public class GatewayModule extends AbstractModule {

    @Override
    public void configure() {
        install(new FactoryModuleBuilder()
                .implement(IMessageHandler.class, Names.named("orderlist"), MicexOrderListMessageHandler.class)
                .implement(IMessageHandler.class, Names.named("publictrades"), MicexPublicTradesMessageHandler.class)
                .implement(IMessageHandler.class, Names.named("statistics"), MicexStatisticsMessageHandler.class)
                .build(MessageHandlerFactory.class));

        install(new FactoryModuleBuilder()
                .implement(IMessageSequenceValidator.class, Names.named("orderlist"), MessageSequenceValidatorForOrderList.class)
                .implement(IMessageSequenceValidator.class, Names.named("publictrades"), MessageSequenceValidatorForPublicTrades.class)
                .implement(IMessageSequenceValidator.class, Names.named("statistics"), MessageSequenceValidatorForStatistics.class)
                .build(MessageSequenceValidatorFactory.class));

        bind(ConfigurationManager.class).to(MicexConfigurationManager.class).in(Singleton.class);
    }
}
