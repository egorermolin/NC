package ru.ncapital.gateways.micexfast

import ru.ncapital.gateways.moexfast.OrderDepthEngine
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel

/**
 * Created by egore on 12/19/15.
 */
class MicexOrderDepthEngineTest extends GroovyTestCase {
    
    static OrderDepthEngine<String> getOrderDepthEngine() {
        return new OrderDepthEngine<String>() {
            @Override
            DepthLevel<String> createSnapshotDepthLevel(String exchangeSecurityId) {
                return new DepthLevel<String>(exchangeSecurityId, exchangeSecurityId) {
                    { setMdUpdateAction(MdUpdateAction.SNAPSHOT); }
                };
            }
        }
    }

    static DepthLevel<String> getDepthLevel(String securityId, MdUpdateAction action, String id, double px, double size, String tradeId, boolean isBid) {
        return new DepthLevel<String>(securityId, securityId) {
            {
                setMdUpdateAction(action);
                setMdEntryId(id);
                setMdEntryPx(px);
                setMdEntrySize(size);
                setTradeId(tradeId);
                setIsBid(isBid);
            }
        }
    }
    
    void testOnDepthLevelTradedPartially() {
        OrderDepthEngine de = getOrderDepthEngine()
        List<DepthLevel> toSend = new ArrayList<>()

        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 5.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 10.0, 0.0, "001", true), new ArrayList<DepthLevel>())
        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 10.0, 5.0, "001", false), toSend)
        assert toSend.size() == 1
        assert toSend[0].isBid == false
        assert toSend[0].mdUpdateAction == MdUpdateAction.INSERT
        assert toSend[0].mdEntryPx == 10.0
        assert toSend[0].mdEntrySize == 5.0
    }

    void testOnDepthLevelTradedPartially2() {
        OrderDepthEngine de = getOrderDepthEngine()
        List<DepthLevel> toSend = new ArrayList<>()

        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.UPDATE, "entry1", 10.0, 5.0, "001", true), toSend)
        assert toSend.size() == 1
        assert toSend[0].isBid == true
        assert toSend[0].mdUpdateAction == MdUpdateAction.UPDATE
        assert toSend[0].mdEntryPx == 10.0
        assert toSend[0].mdEntrySize == 5.0
    }

    void testOnDepthLevelTradedFully() {
        OrderDepthEngine de = getOrderDepthEngine()
        List<DepthLevel> toSend = new ArrayList<>()

        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 10.0, 0.0, "001", true), toSend)
        assert toSend.size() == 1
        assert toSend[0].isBid == true
        assert toSend[0].mdUpdateAction == MdUpdateAction.DELETE
        assert toSend[0].mdEntryPx == 10.0
        assert toSend[0].mdEntrySize == 0.0
    }
}
