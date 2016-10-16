package ru.ncapital.gateways.fortsfast;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import ru.ncapital.gateways.fortsfast.domain.FortsInstrument;
import ru.ncapital.gateways.fortsfast.messagehandlers.FortsOrderListMessageHandler;
import ru.ncapital.gateways.fortsfast.messagehandlers.FortsPublicTradesMessageHandler;
import ru.ncapital.gateways.fortsfast.messagehandlers.FortsStatisticsMessageHandler;
import ru.ncapital.gateways.micexfast.MicexMarketDataManager;
import ru.ncapital.gateways.moexfast.ConfigurationManager;
import ru.ncapital.gateways.moexfast.IGatewayManager;
import ru.ncapital.gateways.moexfast.InstrumentManager;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.*;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerFactory;

/**
 * Created by Egor on 04-Oct-16.
 */
public class FortsGatewayModule extends AbstractModule {

    @Override
    public void configure() {
        install(new FactoryModuleBuilder()
                .implement(IMessageHandler.class, Names.named("orderlist"), FortsOrderListMessageHandler.class)
                .implement(IMessageHandler.class, Names.named("publictrades"), FortsPublicTradesMessageHandler.class)
                .implement(IMessageHandler.class, Names.named("statistics"), FortsStatisticsMessageHandler.class)
                .build(MessageHandlerFactory.class));

        install(new FactoryModuleBuilder()
                .implement(IMessageSequenceValidator.class, Names.named("orderlist"), new TypeLiteral<MessageSequenceValidatorForPublicTrades<Long>>(){})
                .implement(IMessageSequenceValidator.class, Names.named("publictrades"), new TypeLiteral<MessageSequenceValidatorForPublicTrades<Long>>(){})
                .implement(IMessageSequenceValidator.class, Names.named("statistics"), new TypeLiteral<MessageSequenceValidatorForPublicTrades<Long>>(){})
                .build(MessageSequenceValidatorFactory.class));

        bind(ConfigurationManager.class).to(FortsConfigurationManager.class).in(Singleton.class);

        bind(new TypeLiteral<MarketDataManager<Long>>(){}).to(FortsMarketDataManager.class).in(Singleton.class);
        bind(MarketDataManager.class).to(FortsMarketDataManager.class).in(Singleton.class);

        bind(new TypeLiteral<InstrumentManager<Long>>(){}).to(FortsInstrumentManager.class).in(Singleton.class);
        bind(InstrumentManager.class).to(FortsInstrumentManager.class).in(Singleton.class);

        bind(IGatewayManager.class).to(FortsGatewayManager.class).in(Singleton.class);
    }
}
