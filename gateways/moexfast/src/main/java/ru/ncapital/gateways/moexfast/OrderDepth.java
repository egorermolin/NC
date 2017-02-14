package ru.ncapital.gateways.moexfast;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.connection.MarketType;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;

import java.util.*;

/**
 * Created by egore on 12/17/15.
 */
class OrderDepth {
    private Map<String, IDepthLevel> depthLevels;

    private SortedMultiset<IDepthLevel> depthLevelsSorted;

    private boolean isBid;

    private Logger logger;

    private IGatewayConfiguration configuration;

    OrderDepth(boolean isBid, IGatewayConfiguration configuration) {
        this.isBid = isBid;
        this.depthLevelsSorted = TreeMultiset.create(getComparator(isBid));
        this.depthLevels = new HashMap<>();
        this.logger = LoggerFactory.getLogger((isBid ? "Bid" : "Offer") + "OrderDepth");
        this.configuration = configuration;
    }

    private Comparator<IDepthLevel> getComparator(boolean isBid) {
        if (isBid) {
            return new Comparator<IDepthLevel>() {
                @Override
                public int compare(IDepthLevel depthLevel, IDepthLevel depthLevel1) {
                    return depthLevel1.compareTo(depthLevel);
                }
            };
        } else {
            return new Comparator<IDepthLevel>() {
                @Override
                public int compare(IDepthLevel depthLevel, IDepthLevel depthLevel1) {
                    return depthLevel.compareTo(depthLevel1);
                }
            };
        }
    }

    void onDepthLevel(DepthLevel depthLevel) {
        if (logger.isTraceEnabled())
            logger.trace("onDepthLevel: " + depthLevel.toString());

        IDepthLevel previousDepthLevel;
        switch (depthLevel.getMdUpdateAction()) {
            case INSERT:
                previousDepthLevel = depthLevels.get(depthLevel.getMdEntryId());
                if (previousDepthLevel != null) {
                    logger.error("Entry found for dl insert " + depthLevel.getMdEntryId() + " (Received dl: " + depthLevel + ") (Previous dl: " + previousDepthLevel + ")");

                    depthLevelsSorted.remove(previousDepthLevel);
                    depthLevels.remove(previousDepthLevel.getMdEntryId());
                }

                if (depthLevel.getPublicTrade() != null) {
                    // technical trade
                    if (logger.isDebugEnabled())
                        logger.debug("Technical trade " + depthLevel);
                } else {
                    depthLevelsSorted.add(depthLevel);
                    depthLevels.put(depthLevel.getMdEntryId(), depthLevel);
                }
                break;
            case UPDATE:
                previousDepthLevel = depthLevels.get(depthLevel.getMdEntryId());
                if (previousDepthLevel == null) {
                    logger.error("Entry not found for dl update " + depthLevel.getMdEntryId() + " (Received dl: " + depthLevel + ")");
                } else {
                    depthLevelsSorted.remove(previousDepthLevel);
                    depthLevels.remove(previousDepthLevel.getMdEntryId());
                    if (configuration.getMarketType() == MarketType.FUT && configuration.getVersion() == IGatewayConfiguration.Version.V2016) {
                        // keep original price of the order
                        depthLevel.setMdEntryPx(previousDepthLevel.getMdEntryPx());
                        if (depthLevel.getPublicTrade() != null)
                           depthLevel.getPublicTrade().setLastSize(previousDepthLevel.getMdEntrySize() - depthLevel.getMdEntrySize());
                    }
                }
                depthLevelsSorted.add(depthLevel);
                depthLevels.put(depthLevel.getMdEntryId(), depthLevel);
                break;
            case DELETE:
                previousDepthLevel = depthLevels.get(depthLevel.getMdEntryId());
                if (previousDepthLevel == null) {
                    logger.error("Entry not found for dl delete " + depthLevel.getMdEntryId() + " (Received dl: " + depthLevel + ")");
                } else {
                    depthLevelsSorted.remove(previousDepthLevel);
                    depthLevels.remove(previousDepthLevel.getMdEntryId());
                    if (configuration.getMarketType() == MarketType.FUT && configuration.getVersion() == IGatewayConfiguration.Version.V2016) {
                        // keep original price of the order
                        depthLevel.setMdEntryPx(previousDepthLevel.getMdEntryPx());
                        if (depthLevel.getPublicTrade() != null)
                             depthLevel.getPublicTrade().setLastSize(previousDepthLevel.getMdEntrySize());
                    }
                }
                break;
        }
    }

    void extractDepthLevels(List<IDepthLevel> depthLevelsToSend) {
        List<IDepthLevel> depthLevelsTmp = new ArrayList<>(depthLevels.values());
        Collections.sort(depthLevelsTmp, getComparator(isBid));
        depthLevelsToSend.addAll(depthLevelsTmp);
    }

    void clearDepth() {
        depthLevels.clear();
        depthLevelsSorted.clear();
    }

    DepthLevel getDepthLevel(String mdEntryId) {
        return (DepthLevel) depthLevels.get(mdEntryId);
    }
}
