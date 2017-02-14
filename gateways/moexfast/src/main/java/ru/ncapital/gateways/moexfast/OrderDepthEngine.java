package ru.ncapital.gateways.moexfast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;
import ru.ncapital.gateways.moexfast.domain.intf.IPublicTrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by egore on 12/17/15.
 */
public abstract class OrderDepthEngine<T> {

    private Logger logger = LoggerFactory.getLogger("OrderDepthEngine");

    private Map<T, OrderDepth> bids;

    private Map<T, OrderDepth> offers;

    private IGatewayConfiguration configuration;

    public OrderDepthEngine(IGatewayConfiguration configuration) {
        this.bids = new HashMap<>();
        this.offers = new HashMap<>();
        this.configuration = configuration;
    }

    private OrderDepth getOrderDepth(T exchangeSecurityId, boolean isBid) {
        OrderDepth depth = isBid ? bids.get(exchangeSecurityId) : offers.get(exchangeSecurityId);
        if (depth == null) {
            if (isBid) {
                bids.put(exchangeSecurityId, depth = new OrderDepth(true, configuration));
                offers.put(exchangeSecurityId, new OrderDepth(false, configuration));
            } else {
                bids.put(exchangeSecurityId, new OrderDepth(true, configuration));
                offers.put(exchangeSecurityId, depth = new OrderDepth(false, configuration));
            }
        }
        return depth;
    }

    public void onDepthLevel(DepthLevel<T> depthLevel, List<IDepthLevel> depthLevelsToSend, List<IPublicTrade> publicTradesToSend) {
        if (depthLevel.getMdUpdateAction() == MdUpdateAction.SNAPSHOT) {
            getOrderDepth(depthLevel.getExchangeSecurityId(), true).clearDepth();
            getOrderDepth(depthLevel.getExchangeSecurityId(), false).clearDepth();
        } else {
            getOrderDepth(depthLevel.getExchangeSecurityId(), depthLevel.getIsBid()).onDepthLevel(depthLevel);
        }
        depthLevelsToSend.add(depthLevel);
        if (depthLevel.getPublicTrade() != null)
            publicTradesToSend.add(depthLevel.getPublicTrade());
    }

    public void onDepthLevels(DepthLevel<T>[] depthLevels, List<IDepthLevel> depthLevelsToSend, List<IPublicTrade> publicTradesToSend) {
        for (DepthLevel<T> depthLevel : depthLevels)
            onDepthLevel(depthLevel, depthLevelsToSend, publicTradesToSend);
    }

    boolean[] updateBBO(BBO<T> previousBBO, BBO<T> newBBO) {
        if (logger.isTraceEnabled())
            logger.trace("Updating BBO: " + previousBBO + " from " + newBBO);

        boolean[] changed = new boolean[] {false, false, false};
        if ((Double.compare(newBBO.getBidPx(), 0.0) != 0 || newBBO.isEmpty()) && Double.compare(newBBO.getBidPx(), previousBBO.getBidPx()) != 0) {
            changed[0] = true;
            previousBBO.setBidPx(newBBO.getBidPx());
        }
        if ((Double.compare(newBBO.getOfferPx(), 0.0) != 0 || newBBO.isEmpty()) && Double.compare(newBBO.getOfferPx(), previousBBO.getOfferPx()) != 0) {
            changed[0] = true;
            previousBBO.setOfferPx(newBBO.getOfferPx());
        }
        if ((Double.compare(newBBO.getBidSize(), 0.0) != 0 || newBBO.isEmpty()) && Double.compare(newBBO.getBidSize(), previousBBO.getBidSize()) != 0) {
            changed[0] = true;
            previousBBO.setBidSize(newBBO.getBidSize());
        }
        if ((Double.compare(newBBO.getOfferSize(), 0.0) != 0 || newBBO.isEmpty()) && Double.compare(newBBO.getOfferSize(), previousBBO.getOfferSize()) != 0) {
            changed[0] = true;
            previousBBO.setOfferSize(newBBO.getOfferSize());
        }
        if (Double.compare(newBBO.getLastSize(), 0.0) != 0 && Double.compare(newBBO.getLastSize(), previousBBO.getLastSize()) != 0) {
            changed[1] = true;
            previousBBO.setLastSize(newBBO.getLastSize());
        }
        if (Double.compare(newBBO.getLastPx(), 0.0) != 0 && Double.compare(newBBO.getLastPx(), previousBBO.getLastPx()) != 0) {
            changed[1] = true;
            previousBBO.setLastPx(newBBO.getLastPx());
        }
        if (Double.compare(newBBO.getHighPx(), 0.0) != 0 && Double.compare(newBBO.getHighPx(), previousBBO.getHighPx()) != 0) {
            changed[1] = true;
            previousBBO.setHighPx(newBBO.getHighPx());
        }
        if (Double.compare(newBBO.getLowPx(), 0.0) != 0 && Double.compare(newBBO.getLowPx(), previousBBO.getLowPx()) != 0) {
            changed[1] = true;
            previousBBO.setLowPx(newBBO.getLowPx());
        }
        if (Double.compare(newBBO.getOpenPx(), 0.0) != 0 && Double.compare(newBBO.getOpenPx(), previousBBO.getOpenPx()) != 0) {
            changed[1] = true;
            previousBBO.setOpenPx(newBBO.getOpenPx());
        }
        if (Double.compare(newBBO.getClosePx(), 0.0) != 0 && Double.compare(newBBO.getClosePx(), previousBBO.getClosePx()) != 0) {
            changed[1] = true;
            previousBBO.setClosePx(newBBO.getClosePx());
        }
        if (newBBO.getTradingStatus() != null && !newBBO.getTradingStatus().equals(previousBBO.getTradingStatus())) {
            if (logger.isDebugEnabled())
                logger.debug("Updated Trading Status " + newBBO.getSecurityId() +
                    " " + previousBBO.getTradingStatus() +
                    " -> " + newBBO.getTradingStatus());

            changed[2] = true;
            previousBBO.setTradingStatus(newBBO.getTradingStatus());
        }
        if (updateInRecovery(previousBBO, newBBO)) {
            changed[2] = true;
        }
        if (newBBO.getPerformanceData() != null && !newBBO.getPerformanceData().equals(previousBBO.getPerformanceData())) {
            previousBBO.getPerformanceData().updateFrom(newBBO.getPerformanceData());
        }

        if (logger.isTraceEnabled())
            logger.trace("Updated BBO: " + previousBBO);

        return changed;
    }

    void getDepthLevels(T exchangeSecurityId, List<IDepthLevel> depthLevelsToSend) {
        depthLevelsToSend.add(createSnapshotDepthLevel(exchangeSecurityId));

        OrderDepth bidDepth = bids.get(exchangeSecurityId);
        OrderDepth offerDepth = offers.get(exchangeSecurityId);

        if (bidDepth != null)
            bidDepth.extractDepthLevels(depthLevelsToSend);

        if (offerDepth != null)
            offerDepth.extractDepthLevels(depthLevelsToSend);
    }

    protected void updateDepthLevelFromPublicTrade(PublicTrade<T> publicTrade) {
        if (publicTrade == null || publicTrade.getMdEntryId() == null)
            return;

        DepthLevel depthLevel = getOrderDepth(publicTrade.getExchangeSecurityId(), publicTrade.isBid())
                .getDepthLevel(publicTrade.getMdEntryId());

        if (depthLevel == null) {
            if (logger.isTraceEnabled())
                logger.trace("DepthLevel not found for PublicTrade: " + publicTrade);

            return;
        }

        if (logger.isTraceEnabled())
            logger.trace("DepthLevel found for PublicTrade: " + publicTrade);

        depthLevel.setTradeId(publicTrade.getTradeId());
    }

    protected void onPublicTrade(PublicTrade<T> publicTrade) {
        // do nothing by default
    }

    protected abstract boolean updateInRecovery(BBO<T> previousBBO, BBO<T> newBBO);

    protected abstract DepthLevel<T> createSnapshotDepthLevel(T exchangeSecurityId);
}
