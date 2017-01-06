package ru.ncapital.gateways.moexfast.domain.intf;

import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 10/15/16.
 */
public interface IDepthLevel extends Comparable<IDepthLevel>{
    String getSecurityId();

    String getMdEntryId();

    MdUpdateAction getMdUpdateAction();

    double getMdEntryPx();

    double getMdEntrySize();

    String getTradeId();

    boolean getIsBid();

    PerformanceData getPerformanceData();

    String toShortString();
}
