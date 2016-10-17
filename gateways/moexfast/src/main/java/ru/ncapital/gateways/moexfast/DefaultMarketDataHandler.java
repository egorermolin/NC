package ru.ncapital.gateways.moexfast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.domain.impl.Instrument;
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;
import ru.ncapital.gateways.moexfast.domain.intf.IBBO;
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;
import ru.ncapital.gateways.moexfast.domain.intf.IInstrument;
import ru.ncapital.gateways.moexfast.domain.intf.IPublicTrade;

/**
 * Created by egore on 12/7/15.
 */
public class DefaultMarketDataHandler implements IMarketDataHandler {

    private Logger logger = LoggerFactory.getLogger("DefaultMarketDataHandler");

    @Override
    public void onBBO(IBBO bbo) {
        StringBuilder sb = new StringBuilder();
        sb.append("onBBO: ");
        sb.append(bbo.getSecurityId()).append(" ");
        sb.append(bbo.getBidSize()).append("@").append(bbo.getBidPx()).append(" - ");
        sb.append(bbo.getOfferSize()).append("@").append(bbo.getOfferPx()).append(" ");
        sb.append(bbo.getPerformanceData().toString());

        logger.info(sb.toString());
    }

    @Override
    public void onTradingStatus(IBBO bbo) {
        StringBuilder sb = new StringBuilder();
        sb.append("onTradingStatus: ");
        sb.append(bbo.getSecurityId()).append(" ");
        sb.append(bbo.getTradingStatus()).append(" ");
        sb.append(bbo.getPerformanceData().toString());

        logger.info(sb.toString());
    }

    @Override
    public void onStatistics(IBBO bbo) {
        StringBuilder sb = new StringBuilder();
        sb.append("onStatistics: ");
        sb.append(bbo.getSecurityId());
        sb.append(" La:").append(bbo.getLastSize()).append("@").append(bbo.getLastPx());
        sb.append(" H:").append(bbo.getHighPx());
        sb.append(" L:").append(bbo.getLowPx());
        sb.append(" O:").append(bbo.getOpenPx());
        sb.append(" C:").append(bbo.getClosePx()).append(" ");
        sb.append(bbo.getPerformanceData().toString());

        logger.info(sb.toString());
    }

    @Override
    public void onDepthLevels(IDepthLevel[] depthLevels) {
        for (IDepthLevel depthLevel : depthLevels) {
            StringBuilder sb = new StringBuilder();
            sb.append("onDepthLevel: ");
            switch (depthLevel.getMdUpdateAction()) {
                case INSERT:
                    sb.append("Insert ");
                    break;
                case UPDATE:
                    sb.append("Update ");
                    break;
                case DELETE:
                    sb.append("Delete ");
                    break;
                case SNAPSHOT:
                    sb.append("Snapshot ");
                    break;
            }
            sb.append(depthLevel.getSecurityId()).append(" ");
            sb.append(depthLevel.getIsBid() ? "B" : "S").append(depthLevel.getMdEntryId()).append(" ");
            sb.append(depthLevel.getMdEntrySize()).append("@").append(depthLevel.getMdEntryPx()).append(" ");
            sb.append(depthLevel.getPerformanceData().toString());

            logger.info(sb.toString());
        }
    }

    @Override
    public void onPublicTrade(IPublicTrade publicTrade) {
        StringBuilder sb = new StringBuilder();
        sb.append("onPublicTrade: ");
        sb.append(publicTrade.getSecurityId()).append(" ");
        sb.append(publicTrade.isBid() ? "B" : "S");
        sb.append(publicTrade.getLastSize()).append("@").append(publicTrade.getLastPx()).append(" ");
        sb.append(publicTrade.getPerformanceData().toString());

        logger.info(sb.toString());
    }

    @Override
    public void onInstruments(IInstrument[] instruments) {

    }

    @Override
    public void onFeedStatus(boolean up, boolean all) {
        logger.info("onFeedStatus [" + (up ? "UP" : "DOWN") + "][" + (all ? "ALL" : "SOME") + "]");
    }
}
