package ru.ncapital.gateways.micexfast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.domain.BBO;
import ru.ncapital.gateways.micexfast.domain.DepthLevel;
import ru.ncapital.gateways.micexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.micexfast.domain.PublicTrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by egore on 12/17/15.
 */
public class OrderDepthEngine {

    private Logger logger = LoggerFactory.getLogger("OrderDepthEngine");

    private Map<String, OrderDepth> bids;

    private Map<String, OrderDepth> offers;

    private Map<String, PublicTrade> publicTrades;

    public OrderDepthEngine() {
        this.bids = new HashMap<>();
        this.offers = new HashMap<>();
        this.publicTrades = new HashMap<>();
    }

    private OrderDepth getOrderDepth(String securityId, boolean isBid) {
        OrderDepth depth = isBid ? bids.get(securityId) : offers.get(securityId);
        if (depth == null) {
            if (isBid) {
                bids.put(securityId, depth = new OrderDepth(true));
                offers.put(securityId, new OrderDepth(false));
            } else {
                bids.put(securityId, new OrderDepth(true));
                offers.put(securityId, depth = new OrderDepth(false));
            }
        }
        return depth;
    }

    public void onPublicTrade(PublicTrade publicTrade) {
    }

    public void onDepthLevel(DepthLevel depthLevel, List<DepthLevel> depthLevelsToSend) {
        if (depthLevel.getMdUpdateAction() == MdUpdateAction.SNAPSHOT) {
            getOrderDepth(depthLevel.getSecurityId(), true).clearDepth();
            getOrderDepth(depthLevel.getSecurityId(), false).clearDepth();
        } else {
            getOrderDepth(depthLevel.getSecurityId(), depthLevel.isBid()).onDepthLevel(depthLevel);
        }
        depthLevelsToSend.add(depthLevel);
    }

    public void onDepthLevels(DepthLevel[] depthLevels, List<DepthLevel> depthLevelsToSend) {
        for (DepthLevel depthLevel : depthLevels)
            onDepthLevel(depthLevel, depthLevelsToSend);
    }

    public BBO getBBO(String securityId) {
        return getBBO(new BBO(securityId));
    }

    public BBO getBBO(BBO bbo) {
        OrderDepth bidDepth = bids.get(bbo.getSecurityId());
        OrderDepth offerDepth = offers.get(bbo.getSecurityId());

        bidDepth.extractBBO(bbo);
        offerDepth.extractBBO(bbo);

        return bbo;
    }

    public boolean[] updateBBO(BBO previousBBO, BBO newBBO) {
        if (logger.isTraceEnabled())
            logger.trace("Updating BBO: " + previousBBO + " from " + newBBO);

        boolean[] changed = new boolean[] {false, false, false};
        if (Double.compare(newBBO.getBidPx(), 0.0) != 0 && Double.compare(newBBO.getBidPx(), previousBBO.getBidPx()) != 0) {
            changed[0] = true;
            previousBBO.setBidPx(newBBO.getBidPx());
        }
        if (Double.compare(newBBO.getOfferPx(), 0.0) != 0 && Double.compare(newBBO.getOfferPx(), previousBBO.getOfferPx()) != 0) {
            changed[0] = true;
            previousBBO.setOfferPx(newBBO.getOfferPx());
        }
        if (Double.compare(newBBO.getBidSize(), 0.0) != 0 && Double.compare(newBBO.getBidSize(), previousBBO.getBidSize()) != 0) {
            changed[0] = true;
            previousBBO.setBidSize(newBBO.getBidSize());
        }
        if (Double.compare(newBBO.getOfferSize(), 0.0) != 0 && Double.compare(newBBO.getOfferSize(), previousBBO.getOfferSize()) != 0) {
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
        for (int i : new int[] {0, 1}) {
            if (newBBO.isInRecoverySet(i) && newBBO.isInRecovery(i) != previousBBO.isInRecovery(i)) {
                changed[2] = true;
                previousBBO.setInRecovery(newBBO.isInRecovery(i), i);
            }
        }
        if (newBBO.getPerformanceData() != null && !newBBO.getPerformanceData().equals(previousBBO.getPerformanceData())) {
            previousBBO.getPerformanceData().updateFrom(newBBO.getPerformanceData());
        }
        if (logger.isTraceEnabled())
            logger.trace("Updated BBO: " + previousBBO);

        return changed;
    }

    public void getDepthLevels(String securityId, List<DepthLevel> depthLevels) {
        OrderDepth bidDepth = bids.get(securityId);
        OrderDepth offerDepth = offers.get(securityId);

        if (bidDepth != null)
            bidDepth.extractDepthLevels(depthLevels);

        if (offerDepth != null)
            offerDepth.extractDepthLevels(depthLevels);
    }
}
