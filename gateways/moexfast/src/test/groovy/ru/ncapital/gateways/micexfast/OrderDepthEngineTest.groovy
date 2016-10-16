package ru.ncapital.gateways.micexfast

import ru.ncapital.gateways.micexfast.domain.MicexDepthLevel
import ru.ncapital.gateways.micexfast.domain.MicexPublicTrade
import ru.ncapital.gateways.moexfast.OrderDepthEngine
import ru.ncapital.gateways.moexfast.domain.impl.BBO
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade

/**
 * Created by egore on 12/19/15.
 */
class MicexOrderDepthEngineTest extends GroovyTestCase {
    
    static OrderDepthEngine<String> getOrderDepthEngine() {
        return new OrderDepthEngine<String>() {
            @Override
            DepthLevel<String> createSnapshotDepthLevel(String exchangeSecurityId) {
                return new MicexDepthLevel(exchangeSecurityId, MdUpdateAction.SNAPSHOT)
            }
        }
    }
    
    void testOnDepthLevelTradedPartially() {
        OrderDepthEngine de = getOrderDepthEngine()
        List<DepthLevel> toSend = new ArrayList<>()

        de.onDepthLevel(new MicexDepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 5.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new MicexDepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 10.0, 0.0, "001", true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new MicexDepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 10.0, 5.0, "001", false), toSend)
        assert toSend.size() == 1
        assert !toSend[0].isBid
        assert toSend[0].mdUpdateAction == MdUpdateAction.INSERT
        assert toSend[0].mdEntryPx == 10.0
        assert toSend[0].mdEntrySize == 5.0
    }

    void testOnDepthLevelTradedPartially2() {
        OrderDepthEngine de = getOrderDepthEngine()
        List<DepthLevel> toSend = new ArrayList<>()

        de.onDepthLevel(new MicexDepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new MicexDepthLevel("AAA", MdUpdateAction.UPDATE, "entry1", 10.0, 5.0, "001", true), toSend)
        assert toSend.size() == 1
        assert toSend[0].isBid
        assert toSend[0].mdUpdateAction == MdUpdateAction.UPDATE
        assert toSend[0].mdEntryPx == 10.0
        assert toSend[0].mdEntrySize == 5.0
    }

    void testOnDepthLevelTradedFully() {
        OrderDepthEngine de = getOrderDepthEngine()
        List<DepthLevel> toSend = new ArrayList<>()

        de.onDepthLevel(new MicexDepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new MicexDepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 10.0, 0.0, "001", true), toSend)
        assert toSend.size() == 1
        assert toSend[0].isBid
        assert toSend[0].mdUpdateAction == MdUpdateAction.DELETE
        assert toSend[0].mdEntryPx == 10.0
        assert toSend[0].mdEntrySize == 0.0
    }
}
