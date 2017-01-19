package ru.ncapital.gateways.moexfast;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;

import java.util.*;

/**
 * Created by egore on 12/17/15.
 */
public class OrderDepth {
    private Map<String, IDepthLevel> depthLevels;

    private SortedMultiset<IDepthLevel> depthLevelsSorted;

    private boolean isBid;

    private Logger logger;

    public OrderDepth(boolean isBid) {
        this.isBid = isBid;
        this.depthLevelsSorted = TreeMultiset.create(getComparator(isBid));
        this.depthLevels = new HashMap<>();
        this.logger = LoggerFactory.getLogger((isBid ? "Bid" : "Offer") + "OrderDepth");
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

    public void onDepthLevel(DepthLevel depthLevel) {
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
                    depthLevel.getPublicTrade().setLastSize(depthLevel.getMdEntrySize());
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
                    // keep original price of the order
                    depthLevel.setMdEntryPx(previousDepthLevel.getMdEntryPx());
                    if (depthLevel.getPublicTrade() != null)
                        depthLevel.getPublicTrade().setLastSize(previousDepthLevel.getMdEntrySize() - depthLevel.getMdEntrySize());
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
                    // keep original price of the order
                    depthLevel.setMdEntryPx(previousDepthLevel.getMdEntryPx());
                    if (depthLevel.getPublicTrade() != null)
                        depthLevel.getPublicTrade().setLastSize(previousDepthLevel.getMdEntrySize());
                }
                break;
        }
    }

    public void extractBBO(BBO bbo) {
        if (isBid) {
            if (depthLevelsSorted.size() > 0) {
                bbo.setBidPx(depthLevelsSorted.firstEntry().getElement().getMdEntryPx());
                bbo.setBidSize(0.0);
                for (IDepthLevel dl : depthLevelsSorted) {
                    if (Double.compare(dl.getMdEntryPx(), bbo.getBidPx()) != 0)
                        break;

                    bbo.setBidSize(bbo.getBidSize() + dl.getMdEntrySize());
                }
            } else {
                bbo.setBidPx(0.0);
                bbo.setBidSize(0.0);
            }
        } else {
            if (depthLevelsSorted.size() > 0) {
                bbo.setOfferPx(depthLevelsSorted.firstEntry().getElement().getMdEntryPx());
                bbo.setOfferSize(0.0);
                for (IDepthLevel dl : depthLevelsSorted) {
                    if (Double.compare(dl.getMdEntryPx(), bbo.getOfferPx()) != 0)
                        break;

                    bbo.setOfferSize(bbo.getOfferSize() + dl.getMdEntrySize());
                }
            } else {
                bbo.setOfferPx(0.0);
                bbo.setOfferSize(0.0);
            }
        }
    }

    public void extractDepthLevels(List<IDepthLevel> depthLevelsToSend) {
        List<IDepthLevel> depthLevelsTmp = new ArrayList<>(depthLevels.values());
        Collections.sort(depthLevelsTmp, getComparator(isBid));
        depthLevelsToSend.addAll(depthLevelsTmp);
    }

    public void clearDepth() {
        depthLevels.clear();
        depthLevelsSorted.clear();
    }
}
