package ru.ncapital.gateways.micexfast;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import ru.ncapital.gateways.micexfast.messagehandlers.MicexOrderListMessageHandler;
import ru.ncapital.gateways.micexfast.messagehandlers.MicexPublicTradesMessageHandler;
import ru.ncapital.gateways.micexfast.messagehandlers.MicexStatisticsMessageHandler;
import ru.ncapital.gateways.moexfast.ConfigurationManager;
import ru.ncapital.gateways.moexfast.IGatewayManager;
import ru.ncapital.gateways.moexfast.InstrumentManager;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.*;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerFactory;

/**
 * Created by egore on 02.02.2016.
 */
public class MicexGatewayModule extends AbstractModule {

    @Override
    public void configure() {
        install(new FactoryModuleBuilder()
                .implement(new TypeLiteral<IMessageHandler<String>>(){}, Names.named("orderlist"), MicexOrderListMessageHandler.class)
                .implement(new TypeLiteral<IMessageHandler<String>>(){}, Names.named("publictrades"), MicexPublicTradesMessageHandler.class)
                .implement(new TypeLiteral<IMessageHandler<String>>(){}, Names.named("statistics"), MicexStatisticsMessageHandler.class)
                .build(new TypeLiteral<MessageHandlerFactory<String>>(){}));

        install(new FactoryModuleBuilder()
                .implement(new TypeLiteral<IMessageSequenceValidator<String>>(){}, Names.named("orderlist"), new TypeLiteral<MessageSequenceValidatorForOrderList<String>>(){})
                .implement(new TypeLiteral<IMessageSequenceValidator<String>>(){}, Names.named("publictrades"), new TypeLiteral<MessageSequenceValidatorForPublicTrades<String>>(){})
                .implement(new TypeLiteral<IMessageSequenceValidator<String>>(){}, Names.named("statistics"), new TypeLiteral<MessageSequenceValidatorForStatistics<String>>(){})
                .build(new TypeLiteral<MessageSequenceValidatorFactory<String>>(){}));

        bind(new TypeLiteral<MarketDataManager<String>>(){}).to(MicexMarketDataManager.class).in(Singleton.class);
        bind(new TypeLiteral<InstrumentManager<String>>(){}).to(MicexInstrumentManager.class).in(Singleton.class);

        bind(ConfigurationManager.class).to(MicexConfigurationManager.class).in(Singleton.class);
        bind(MarketDataManager.class).to(MicexMarketDataManager.class).in(Singleton.class);
        bind(InstrumentManager.class).to(MicexInstrumentManager.class).in(Singleton.class);
        bind(IGatewayManager.class).to(MicexGatewayManager.class).in(Singleton.class);
    }
}
