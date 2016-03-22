package ru.ncapital.gateways.micexfast.domain;

/**
 * Created by egore on 24.12.2015.
 */
public class Instrument {
/*
    2015-12-24 15:06:10 DEBUG MicexFastMulticastInputStream - Received 161 - 168 = A8 00 00 00 E0 10 C3 01 A8 23 65 6D 67 5B 7E 1C 8B 01 A9 55 53 44 52 55 42 5F 54 4F 4D 39 CD 80 80 85 87 4D 52 43 58 58 58 84 46 4F 52 80 09 4E 43 A1 80 80 80 80 80 80 94 54 4F 4D 5F 39 4D 20 53 57 41 50 20 55 53 44 2F 52 55 42 9B 54 4F 4D 5F 39 4D 20 D0 A1 D0 92 D0 9E D0 9F 20 55 53 44 2F D0 A0 D0 A3 D0 91 80 82 9B 82 34 55 53 C4 82 83 81 82 43 4E 47 C4 CE 92 80 52 55 C2 80 80 8A 55 53 44 5F 54 4F 4D 39 4D 85 43 55 52 52 FC 81 80 80 80 80 81 81 FC 2B 04 D5 80 80 80 80 80 80 80
    d
    MsgSeqNum: 01a8 -> 168(168)
    SendingTime: 23656d675b7e1c8b -> 20151224120610315(20151224120610315)
    TotNumReports: 01a9 -> 168(168)
    Symbol: 5553445255425f544f4d39cd -> USDRUB_TOM9M(USDRUB_TOM9M)
    SecurityID: 80 -> null(null)
    SecurityIDSource: 80 -> null(null)
    Product: 85 -> 4(4)
    CFICode: 874d5243585858 -> MRCXXX(MRCXXX)
    SecurityType: 84464f52 -> FOR(FOR)
    MaturityDate: 80 -> null(null)
    SettlDate: 094e43a1 -> 20160928(20160928)
    SettleType: 80 -> null(null)
    OrigIssueAmt: 80 -> null(null)
    CouponPaymentDate: 80 -> null(null)
    CouponRate: 80 -> null(null)
    SettlFixingDate: 80 -> null(null)
    DividendNetPx: 80 -> null(null)
    SecurityDesc: 94544f4d5f394d2053574150205553442f525542 -> TOM_9M SWAP USD/RUB(TOM_9M SWAP USD/RUB)
    QuoteText: 80 -> null(null)
    NoInstrAttrib: 82 -> 1(1)
    GroupInstrAttrib
    InstrAttribType: 9b -> 27(27)
    InstrAttribValue: 8234 -> 4(4)
    Currency: 5553c4 -> USD(USD)
    NoMarketSegments: 82 -> 1(1)
    MarketSegmentGrp
    RoundLot: 8381 -> 100(100)
    NoTradingSessionRules: 82 -> 1(1)
    TradingSessionRulesGrp
    TradingSessionID: 434e47c4 -> CNGD(CNGD)
    TradingSessionSubID: ce -> N(N)
    SecurityTradingStatus: 92 -> 17(17)
    OrderNote: 80 -> null(null)
    SettlCurrency: 5255c2 -> RUB(RUB)
    PriceType: 80 -> null(null)
    StateSecurityID: 80 -> null(null)
    EncodedShortSecurityDesc: 8a5553445f544f4d394d -> USD_TOM9M(USD_TOM9M)
    MarketCode: 8543555252 -> CURR(CURR)
    MinPriceIncrement: fc81 -> 0.0001(0.0001)
    MktShareLimit: 80 -> null(null)
    MktShareThreshold: 80 -> null(null)
    MaxOrdersVolume: 80 -> null(null)
    PriceMvmLimit: 80 -> null(null)
    FaceValue: 8181 -> 1(1)
    BaseSwapPx: fc2b04d5 -> 70.5109(70.5109)
    RepoToPx: 80 -> null(null)
    BuyBackPx: 80 -> null(null)
    BuyBackDate: 80 -> null(null)
    NoSharesIssued: 80 -> null(null)
    HighLimit: 80 -> null(null)
    LowLimit: 80 -> null(null)
    NumOfDaysToMaturity: 80 -> null(null)

    */
    private String securityId;

    private String currency;

    private String description;

    private int lotSize;

    private double tickSize;

    private String tradingStatus;

    private double multiplier;

    private String underlying;

    private ProductType productType;

    public Instrument(String securityId) {
        this.securityId = securityId;
    }

    public String getSecurityId() {
        return securityId;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public int getLotSize() {
        return lotSize;
    }

    public double getTickSize() {
        return tickSize;
    }

    public String getTradingStatus() {
        return tradingStatus;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public String getUnderlying() { return underlying; }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLotSize(int lotSize) {
        this.lotSize = lotSize;
    }

    public void setTickSize(double tickSize) {
        this.tickSize = tickSize;
    }

    public void setTradingStatus(String tradingStatus) {
        this.tradingStatus = tradingStatus;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public void setUnderlying(String underlying) {
        this.underlying = underlying;
    }

    public void setProductType(int productType) {
        this.productType = ProductType.convert(productType);
    }

    public ProductType getProductType() {
        return this.productType;
    }

    @Override
    public String toString() {
        return "Instrument{" +
                "securityId='" + securityId + '\'' +
                ", currency='" + currency + '\'' +
                ", description='" + description + '\'' +
                ", lotSize=" + lotSize +
                ", tickSize=" + tickSize +
                ", tradingStatus='" + tradingStatus + '\'' +
                ", multiplier=" + multiplier +
                ", underlying='" + underlying + '\'' +
                ", productType=" + (productType == null ? "UNKNOWN" : productType.getDescription()) +
                '}';
    }

}
