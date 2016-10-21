package ru.ncapital.gateways.micexfast;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
import ru.ncapital.gateways.moexfast.DefaultMarketDataHandler;
import ru.ncapital.gateways.moexfast.IGatewayManager;
import ru.ncapital.gateways.moexfast.IMarketDataHandler;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.connection.MarketType;
import ru.ncapital.gateways.moexfast.domain.intf.IBBO;
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;
import ru.ncapital.gateways.moexfast.domain.intf.IInstrument;
import ru.ncapital.gateways.moexfast.domain.intf.IPublicTrade;

import java.util.concurrent.CountDownLatch;

/**
 * Created by egore on 12/7/15.
 */
public class MainClass {

    private IInstrument[] instruments;

    private CountDownLatch waiter = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        MainClass mc = new MainClass();

        mc.run(args);
    }

    public void run(final String[] args) throws InterruptedException {
        MicexGatewayManager.addConsoleAppender("%d{HH:mm:ss.SSS} %c{1} - %m%n", Level.INFO);
        MicexGatewayManager.addFileAppender("log/log.out", "%d{HH:mm:ss.SSS} %c{1} - %m%n", Level.DEBUG);
        Logger logger = LoggerFactory.getLogger("MainClass");
        if (args.length < 3) {
            System.err.println("Usage MainClass <fast_templates> <interface[s]> <connections_file>");
            return;
        }

        final IGatewayManager gwManager = MicexGatewayManager.create(new MicexNullGatewayConfiguration() {
            @Override
            public IMarketDataHandler getMarketDataHandler() {
                return new DefaultMarketDataHandler() {

                    @Override
                    public void onBBO(IBBO bbo) {
                    }

                    @Override
                    public void onDepthLevels(IDepthLevel[] depthLevels) {
                    }

                    @Override
                    public void onStatistics(IBBO bbo) {
                    }

                    @Override
                    public void onPublicTrade(IPublicTrade publicTrade) {
                    }

                    @Override
                    public void onInstruments(IInstrument[] _instruments) {
                        instruments = _instruments;
                        waiter.countDown();
                    }
                };
            }

            @Override
            public String getFastTemplatesFile() {
                return args[0];
            }

            @Override
            public String getNetworkInterface() {
                return args[1];
            }

            @Override
            public String getConnectionsFile() {
                return args[2];
            }

            @Override
            public MarketType getMarketType() { return MarketType.CURR; }

            @Override
            public TradingSessionId[] getAllowedTradingSessionIds() {
                if (getMarketType() == MarketType.FOND)
                  return new TradingSessionId[] {TradingSessionId.TQBR, TradingSessionId.TQBD, TradingSessionId.TQDE,
                                                TradingSessionId.TQIF,
                                                TradingSessionId.TQTF, TradingSessionId.TQTD,
                                                TradingSessionId.TQOB, TradingSessionId.TQOD,
                                                TradingSessionId.TQTC,
                            };
                else
                  return new TradingSessionId[] {TradingSessionId.CETS};
            }

            @Override
            public ProductType[] getAllowedProductTypes() {
                if (getMarketType() == MarketType.FOND)
                  return new ProductType[] {ProductType.EQUITY, ProductType.INDEX};
                else
                  return new ProductType[] {ProductType.CURRENCY};
            }

            @Override
            public String[] getAllowedSecurityIds() {
                // return new String[] {"*"};
                return new String[] {"USD000UTSTOM;CETS"};
            }

            @Override
            public boolean isAsynchChannelReader() {
                return true;
            }

            @Override
            public boolean isListenSnapshotChannelOnlyIfNeeded() {
                return true;
            }
        });

        gwManager.start();

        waiter.await();
        logger.info("TOTAL " + instruments.length + " INSTRUMENTS");

        for (IInstrument instrument : instruments) {
            logger.info(instrument.toString());
            // gwManager.subscribeForMarketData(instrument.getSecurityId());
        }

        try {
            Thread.sleep(1200000);
        } catch (InterruptedException e) {
            Utils.printStackTrace(e, logger, "InterruptedException occurred..");
        }

        gwManager.stop();
    }
}
