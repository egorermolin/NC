package ru.ncapital.gateways.moexfast;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.domain.BBO;
import ru.ncapital.gateways.moexfast.domain.DepthLevel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by egore on 12/17/15.
 */
public class OrderDepth {
    private Map<String, DepthLevel> depthLevels;

    private SortedMultiset<DepthLevel> depthLevelsSorted;

    private boolean isBid;

    private Logger logger;

    public OrderDepth(boolean isBid) {
        this.isBid = isBid;
        if (isBid)
        this.depthLevelsSorted = TreeMultiset.create(new Comparator<DepthLevel>() {
            @Override
            public int compare(DepthLevel depthLevel, DepthLevel depthLevel1) {
                return depthLevel1.compareTo(depthLevel);
            }
        });
        else
        this.depthLevelsSorted = TreeMultiset.create(new Comparator<DepthLevel>() {
            @Override
            public int compare(DepthLevel depthLevel, DepthLevel depthLevel1) {
                return depthLevel.compareTo(depthLevel1);
            }
        });
        this.depthLevels = new HashMap<>();
        this.logger = LoggerFactory.getLogger((isBid ? "Bid" : "Offer") + "OrderDepth");
    }

    public void onDepthLevel(DepthLevel depthLevel) {
        if (logger.isTraceEnabled())
            logger.trace("onDepthLevel: " + depthLevel.toString());

        DepthLevel previousDepthLevel;
        switch (depthLevel.getMdUpdateAction()) {
            case INSERT:
                previousDepthLevel = depthLevels.get(depthLevel.getMdEntryId());
                if (previousDepthLevel != null) {
                    logger.error("Entry found for dl insert " + depthLevel.getMdEntryId() + " (Received dl: " + depthLevel + ") (Previous dl: " + previousDepthLevel + ")");

                    depthLevelsSorted.remove(previousDepthLevel);
                    depthLevels.remove(previousDepthLevel.getMdEntryId());
                }

                depthLevelsSorted.add(depthLevel);
                depthLevels.put(depthLevel.getMdEntryId(), depthLevel);
                break;
            case UPDATE:
                previousDepthLevel = depthLevels.get(depthLevel.getMdEntryId());
                if (previousDepthLevel == null) {
                    logger.error("Entry not found for dl update " + depthLevel.getMdEntryId() + " (Received dl: " + depthLevel + ") (Previous dl: " + previousDepthLevel + ")");
                } else {
                    depthLevelsSorted.remove(previousDepthLevel);
                    depthLevels.remove(previousDepthLevel.getMdEntryId());
                }
                depthLevelsSorted.add(depthLevel);
                depthLevels.put(depthLevel.getMdEntryId(), depthLevel);
                break;
            case DELETE:
                previousDepthLevel = depthLevels.get(depthLevel.getMdEntryId());
                if (previousDepthLevel == null || Double.compare(previousDepthLevel.getMdEntryPx(), depthLevel.getMdEntryPx()) != 0) {
                    logger.error("Entry not found for dl delete " + depthLevel.getMdEntryId() + " (Received dl: " + depthLevel + ") (Previous dl: " + previousDepthLevel + ")");
                } else {
                    depthLevelsSorted.remove(previousDepthLevel);
                    depthLevels.remove(previousDepthLevel.getMdEntryId());
                }
                break;
        }
    }

    public void extractBBO(BBO bbo) {
        if (isBid) {
            if (depthLevelsSorted.size() > 0) {
                bbo.setBidPx(depthLevelsSorted.firstEntry().getElement().getMdEntryPx());
                bbo.setBidSize(0.0);
                for (DepthLevel dl : depthLevelsSorted) {
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
                for (DepthLevel dl : depthLevelsSorted) {
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

    public void extractDepthLevels(List<DepthLevel> depthLevels) {
        depthLevels.addAll(this.depthLevels.values());
    }

    public void clearDepth() {
        depthLevels.clear();
        depthLevelsSorted.clear();
    }
}
