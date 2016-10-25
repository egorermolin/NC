package ru.ncapital.gateways.fortsfast;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import ru.ncapital.gateways.fortsfast.messagehandlers.FortsOrderBookMessageHandler;
import ru.ncapital.gateways.fortsfast.messagehandlers.FortsOrderListMessageHandler;
import ru.ncapital.gateways.fortsfast.messagehandlers.FortsStatisticsMessageHandler;
import ru.ncapital.gateways.moexfast.ConfigurationManager;
import ru.ncapital.gateways.moexfast.IGatewayManager;
import ru.ncapital.gateways.moexfast.InstrumentManager;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.*;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerFactory;
import ru.ncapital.gateways.moexfast.messagehandlers.NullMessageHandler;

/**
 * Created by Egor on 04-Oct-16.
 */
public class FortsGatewayModule extends AbstractModule {

    @Override
    public void configure() {
        install(new FactoryModuleBuilder()
                .implement(new TypeLiteral<IMessageHandler<Long>>(){}, Names.named("orderlist"), FortsOrderListMessageHandler.class)
                .implement(new TypeLiteral<IMessageHandler<Long>>(){}, Names.named("statistics"), FortsStatisticsMessageHandler.class)
                .implement(new TypeLiteral<IMessageHandler<Long>>(){}, Names.named("publictrades"), new TypeLiteral<NullMessageHandler<Long>>(){})
                .implement(new TypeLiteral<IMessageHandler<Long>>(){}, Names.named("orderbook"), FortsOrderBookMessageHandler.class)
                .build(new TypeLiteral<MessageHandlerFactory<Long>>(){}));

        install(new FactoryModuleBuilder()
                .implement(new TypeLiteral<IMessageSequenceValidator<Long>>(){}, Names.named("orderlist"), new TypeLiteral<MessageSequenceValidatorForOrderList<Long>>(){})
                .implement(new TypeLiteral<IMessageSequenceValidator<Long>>(){}, Names.named("statistics"), new TypeLiteral<MessageSequenceValidatorForStatistics<Long>>(){})
                .implement(new TypeLiteral<IMessageSequenceValidator<Long>>(){}, Names.named("publictrades"), new TypeLiteral<MessageSequenceValidatorForPublicTrades<Long>>(){})
                .implement(new TypeLiteral<IMessageSequenceValidator<Long>>(){}, Names.named("orderbook"), new TypeLiteral<MessageSequenceValidatorForOrderBook<Long>>(){})
                .build(new TypeLiteral<MessageSequenceValidatorFactory<Long>>(){}));

        bind(new TypeLiteral<MarketDataManager<Long>>(){}).to(FortsMarketDataManager.class).in(Singleton.class);
        bind(new TypeLiteral<InstrumentManager<Long>>(){}).to(FortsInstrumentManager.class).in(Singleton.class);

        bind(ConfigurationManager.class).to(FortsConfigurationManager.class).in(Singleton.class);
        bind(MarketDataManager.class).to(FortsMarketDataManager.class).in(Singleton.class);
        bind(InstrumentManager.class).to(FortsInstrumentManager.class).in(Singleton.class);
        bind(IGatewayManager.class).to(FortsGatewayManager.class).in(Singleton.class);
    }
}
