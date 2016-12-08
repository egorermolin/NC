package ru.ncapital.gateways.micexfast

import ru.ncapital.gateways.moexfast.OrderDepthEngine
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction
import ru.ncapital.gateways.moexfast.domain.impl.BBO
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel
import ru.ncapital.gateways.moexfast.domain.intf.IPublicTrade

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

            @Override
            protected boolean updateInRecovery(BBO<String> previousBBO, BBO<String> newBBO) {
                return false
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
        List<IDepthLevel> toSendDL = new ArrayList<>()
        List<IPublicTrade> toSendPT = new ArrayList<>()

        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 5.0, null, true), new ArrayList<IDepthLevel>(), new ArrayList<IPublicTrade>())
        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 10.0, 0.0, "001", true), new ArrayList<IDepthLevel>(), new ArrayList<IPublicTrade>())
        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 10.0, 5.0, "001", false), toSendDL, toSendPT)
        assert toSendDL.size() == 1
        assert toSendDL[0].isBid == false
        assert toSendDL[0].mdUpdateAction == MdUpdateAction.INSERT
        assert toSendDL[0].mdEntryPx == 10.0
        assert toSendDL[0].mdEntrySize == 5.0
    }

    void testOnDepthLevelTradedPartially2() {
        OrderDepthEngine de = getOrderDepthEngine()
        List<IDepthLevel> toSendDL = new ArrayList<>()
        List<IPublicTrade> toSendPT = new ArrayList<>()

        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<IDepthLevel>(), new ArrayList<IPublicTrade>())
        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.UPDATE, "entry1", 10.0, 5.0, "001", true), toSendDL, toSendPT)
        assert toSendDL.size() == 1
        assert toSendDL[0].isBid == true
        assert toSendDL[0].mdUpdateAction == MdUpdateAction.UPDATE
        assert toSendDL[0].mdEntryPx == 10.0
        assert toSendDL[0].mdEntrySize == 5.0
    }

    void testOnDepthLevelTradedFully() {
        OrderDepthEngine de = getOrderDepthEngine()
        List<IDepthLevel> toSendDL = new ArrayList<>()
        List<IPublicTrade> toSendPT = new ArrayList<>()

        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<IDepthLevel>(), new ArrayList<IPublicTrade>())
        de.onDepthLevel(getDepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 10.0, 0.0, "001", true), toSendDL, toSendPT)
        assert toSendDL.size() == 1
        assert toSendDL[0].isBid == true
        assert toSendDL[0].mdUpdateAction == MdUpdateAction.DELETE
        assert toSendDL[0].mdEntryPx == 10.0
        assert toSendDL[0].mdEntrySize == 0.0
    }
}
