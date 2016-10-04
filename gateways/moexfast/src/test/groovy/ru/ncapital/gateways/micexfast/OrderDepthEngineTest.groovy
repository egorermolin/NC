package ru.ncapital.gateways.micexfast

import ru.ncapital.gateways.moexfast.OrderDepthEngine
import ru.ncapital.gateways.moexfast.domain.BBO
import ru.ncapital.gateways.moexfast.domain.DepthLevel
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction
import ru.ncapital.gateways.moexfast.domain.PublicTrade

/**
 * Created by egore on 12/19/15.
 */
class OrderDepthEngineTest extends GroovyTestCase {
    void testOnDepthLevelTradedPartially() {
        OrderDepthEngine de = new OrderDepthEngine()
        List<DepthLevel> toSend = new ArrayList<>()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 5.0, null, true), new ArrayList<DepthLevel>())
        de.onPublicTrade(new PublicTrade("AAA", "001", 10.0, 5.0, false))
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 10.0, 0.0, "001", true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 10.0, 5.0, "001", false), toSend)
        assert toSend.size() == 1
        assert !toSend[0].isBid
        assert toSend[0].mdUpdateAction == MdUpdateAction.INSERT
        assert toSend[0].mdEntryPx == 10.0
        assert toSend[0].mdEntrySize == 5.0
    }

    void testOnDepthLevelTradedPartially2() {
        OrderDepthEngine de = new OrderDepthEngine()
        List<DepthLevel> toSend = new ArrayList<>()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onPublicTrade(new PublicTrade("AAA", "001", 10.0, 5.0, false))

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.UPDATE, "entry1", 10.0, 5.0, "001", true), toSend)
        assert toSend.size() == 1
        assert toSend[0].isBid
        assert toSend[0].mdUpdateAction == MdUpdateAction.UPDATE
        assert toSend[0].mdEntryPx == 10.0
        assert toSend[0].mdEntrySize == 5.0
    }

    void testOnDepthLevelTradedFully() {
        OrderDepthEngine de = new OrderDepthEngine()
        List<DepthLevel> toSend = new ArrayList<>()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onPublicTrade(new PublicTrade("AAA", "001", 10.0, 10.0, false))
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 10.0, 0.0, "001", true), toSend)
        assert toSend.size() == 1
        assert toSend[0].isBid
        assert toSend[0].mdUpdateAction == MdUpdateAction.DELETE
        assert toSend[0].mdEntryPx == 10.0
        assert toSend[0].mdEntrySize == 0.0
    }

    void testOnDepthLevelInsert() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 11.0, 10.0, null, false), new ArrayList<DepthLevel>())

        BBO bbo = de.getBBO("AAA")

        assert bbo.getBidPx() == 10.0
        assert bbo.getOfferPx() == 11.0
        assert bbo.getBidSize() == 10.0
        assert bbo.getOfferSize() == 10.0
    }

    void testOnDepthLevelInsertAndDeleteAll() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 11.0, 10.0, null, false), new ArrayList<DepthLevel>())

        BBO bbo = de.getBBO("AAA")

        assert bbo.getBidPx() == 10.0
        assert bbo.getOfferPx() == 11.0
        assert bbo.getBidSize() == 10.0
        assert bbo.getOfferSize() == 10.0

        List<DepthLevel> dlDeleted = new ArrayList<DepthLevel>()
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.SNAPSHOT), dlDeleted);

        bbo = de.getBBO("AAA")

        assert bbo.getBidPx() == 0.0
        assert bbo.getOfferPx() == 0.0
        assert bbo.getBidSize() == 0.0
        assert bbo.getOfferSize() == 0.0

        assert dlDeleted[0].getMdUpdateAction() == MdUpdateAction.SNAPSHOT

        List<DepthLevel> dl = new ArrayList<DepthLevel>()
        de.getDepthLevels("AAA", dl)
        assert dl.size() == 0
    }

    void testOnDepthLevelInsertBid() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())

        BBO bbo = de.getBBO("AAA")

        assert bbo.getBidPx() == 10.0
        assert bbo.getOfferPx() == 0.0
        assert bbo.getBidSize() == 10.0
        assert bbo.getOfferSize() == 0.0
    }

    void testOnDepthLevelInsertOffer() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, false), new ArrayList<DepthLevel>())

        BBO bbo = de.getBBO("AAA")

        assert bbo.getBidPx() == 0.0
        assert bbo.getOfferPx() == 10.0
        assert bbo.getBidSize() == 0.0
        assert bbo.getOfferSize() == 10.0
    }

    void testOnDepthLevelInsertTwoLevelsDifferentPrice() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 9.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry3", 12.0, 10.0, null, false), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry4", 11.0, 10.0, null, false), new ArrayList<DepthLevel>())

        BBO bbo = de.getBBO("AAA")

        assert bbo.getBidPx() == 10.0
        assert bbo.getOfferPx() == 11.0
        assert bbo.getBidSize() == 10.0
        assert bbo.getOfferSize() == 10.0
    }

    void testOnDepthLevelInsertTwoLevelsDifferentPrice2() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 9.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry3", 12.0, 10.0, null, false), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry4", 11.0, 10.0, null, false), new ArrayList<DepthLevel>())

        BBO bbo = de.getBBO("AAA")

        assert bbo.getBidPx() == 10.0
        assert bbo.getOfferPx() == 11.0
        assert bbo.getBidSize() == 10.0
        assert bbo.getOfferSize() == 10.0
    }


    void testOnDepthLevelInsertTwoLevelsSamePrice() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry3", 11.0, 10.0, null, false), new ArrayList<DepthLevel>())
        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry4", 11.0, 10.0, null, false), new ArrayList<DepthLevel>())

        BBO bbo = de.getBBO("AAA")

        assert bbo.getBidPx() == 10.0
        assert bbo.getOfferPx() == 11.0
        assert bbo.getBidSize() == 20.0
        assert bbo.getOfferSize() == 20.0
    }


    void testOnDepthLevelInsertUpdateDeleteBid() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        BBO bbo = de.getBBO("AAA")
        assert bbo.getBidPx() == 10.0
        assert bbo.getBidSize() == 10.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.UPDATE, "entry1", 11.0, 10.0, null, true), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getBidPx() == 11.0
        assert bbo.getBidSize() == 10.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.UPDATE, "entry1", 11.0, 20.0, null, true), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getBidPx() == 11.0
        assert bbo.getBidSize() == 20.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 11.0, 0.0, null, true), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getBidPx() == 0.0
        assert bbo.getBidSize() == 0.0

        List<DepthLevel> dl = new ArrayList<DepthLevel>()
        de.getDepthLevels("AAA", dl)
        assert dl.size() == 0
    }

    void testOnDepthLevelInsertUpdateDeleteBidTwoLevels() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        BBO bbo = de.getBBO("AAA")
        assert bbo.getBidPx() == 10.0
        assert bbo.getBidSize() == 10.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 10.0, 10.0, null, true), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA")
        assert bbo.getBidPx() == 10.0
        assert bbo.getBidSize() == 20.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.UPDATE, "entry1", 11.0, 10.0, null, true), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getBidPx() == 11.0
        assert bbo.getBidSize() == 10.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.UPDATE, "entry2", 11.0, 20.0, null, true), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getBidPx() == 11.0
        assert bbo.getBidSize() == 30.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 11.0, 0.0, null, true), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getBidPx() == 11.0
        assert bbo.getBidSize() == 20.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.DELETE, "entry2", 11.0, 0.0, null, true), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getBidPx() == 0.0
        assert bbo.getBidSize() == 0.0

        List<DepthLevel> dl = new ArrayList<DepthLevel>()
        de.getDepthLevels("AAA", dl)
        assert dl.size() == 0
    }


    void testOnDepthLevelInsertUpdateDeleteOffer() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 12.0, 10.0, null, false), new ArrayList<DepthLevel>())
        BBO bbo = de.getBBO("AAA")
        assert bbo.getOfferPx() == 12.0
        assert bbo.getOfferSize() == 10.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.UPDATE, "entry1", 11.0, 10.0, null, false), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getOfferPx() == 11.0
        assert bbo.getOfferSize() == 10.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.UPDATE, "entry1", 11.0, 20.0, null, false), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getOfferPx() == 11.0
        assert bbo.getOfferSize() == 20.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 11.0, 0.0, null, false), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getOfferPx() == 0.0
        assert bbo.getOfferSize() == 0.0

        List<DepthLevel> dl = new ArrayList<DepthLevel>()
        de.getDepthLevels("AAA", dl)
        assert dl.size() == 0
    }

    void testOnDepthLevelInsertUpdateDeleteOfferTwoLevels() {
        OrderDepthEngine de = new OrderDepthEngine()

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry1", 12.0, 10.0, null, false), new ArrayList<DepthLevel>())
        BBO bbo = de.getBBO("AAA")
        assert bbo.getOfferPx() == 12.0
        assert bbo.getOfferSize() == 10.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.INSERT, "entry2", 12.0, 10.0, null, false), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA")
        assert bbo.getOfferPx() == 12.0
        assert bbo.getOfferSize() == 20.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.UPDATE, "entry1", 11.0, 10.0, null, false), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getOfferPx() == 11.0
        assert bbo.getOfferSize() == 10.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.UPDATE, "entry2", 11.0, 20.0, null, false), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getOfferPx() == 11.0
        assert bbo.getOfferSize() == 30.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.DELETE, "entry1", 11.0, 0.0, null, false), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getOfferPx() == 11.0
        assert bbo.getOfferSize() == 20.0

        de.onDepthLevel(new DepthLevel("AAA", MdUpdateAction.DELETE, "entry2", 11.0, 0.0, null, false), new ArrayList<DepthLevel>())
        bbo = de.getBBO("AAA");
        assert bbo.getOfferPx() == 0.0
        assert bbo.getOfferSize() == 0.0

        List<DepthLevel> dl = new ArrayList<DepthLevel>()
        de.getDepthLevels("AAA", dl)
        assert dl.size() == 0
    }



}
