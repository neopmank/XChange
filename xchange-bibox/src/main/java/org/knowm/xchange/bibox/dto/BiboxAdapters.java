package org.knowm.xchange.bibox.dto;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knowm.xchange.bibox.dto.account.BiboxCoin;
import org.knowm.xchange.bibox.dto.marketdata.BiboxMarket;
import org.knowm.xchange.bibox.dto.marketdata.BiboxTicker;
import org.knowm.xchange.bibox.dto.trade.BiboxOrder;
import org.knowm.xchange.bibox.dto.trade.BiboxOrders;
import org.knowm.xchange.bibox.dto.trade.BiboxOrderBook;
import org.knowm.xchange.bibox.dto.trade.BiboxOrderBookEntry;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trades.TradeSortType;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.dto.trade.UserTrades;

/**
 * @author odrotleff
 */
public class BiboxAdapters {

  public static String toBiboxPair(CurrencyPair pair) {

    return pair.base.getCurrencyCode() + "_" + pair.counter.getCurrencyCode();
  }

  public static Ticker adaptTicker(BiboxTicker ticker, CurrencyPair currencyPair) {
    return new Ticker.Builder().currencyPair(currencyPair).ask(ticker.getSell())
        .bid(ticker.getBuy()).high(ticker.getHigh()).low(ticker.getLow()).last(ticker.getLast())
        .volume(ticker.getVol()).timestamp(new Date(ticker.getTimestamp())).build();
  }

  public static AccountInfo adaptAccountInfo(List<BiboxCoin> coins) {
    Wallet wallet = adaptWallet(coins);
    return new AccountInfo(wallet);
  }

  private static Wallet adaptWallet(List<BiboxCoin> coins) {
    List<Balance> balances =
        coins.stream().map(BiboxAdapters::adaptBalance).collect(Collectors.toList());
    return new Wallet(balances);
  }

  private static Balance adaptBalance(BiboxCoin coin) {
    return new Balance.Builder()
        .currency(Currency.getInstance(coin.getSymbol()))
        .available(coin.getBalance())
        .frozen(coin.getFreeze())
        .total(coin.getTotalBalance())
        .build();
  }

  public static OrderBook adaptOrderBook(BiboxOrderBook orderBook, CurrencyPair currencyPair) {
    return new OrderBook(
        new Date(orderBook.getUpdateTime()),
        orderBook.getAsks().stream().map(e -> adaptOrderBookOrder(e, OrderType.ASK, currencyPair)).collect(Collectors.toList()),
        orderBook.getBids().stream().map(e -> adaptOrderBookOrder(e, OrderType.BID, currencyPair)).collect(Collectors.toList()));
  }

  public static LimitOrder adaptOrderBookOrder(BiboxOrderBookEntry entry, OrderType orderType, CurrencyPair currencyPair) {
    return new LimitOrder.Builder(orderType, currencyPair)
        .limitPrice(entry.getPrice())
        .originalAmount(entry.getVolume())
        .build();
  }

  public static OpenOrders adaptOpenOrders(BiboxOrders biboxOpenOrders) {
    return new OpenOrders(biboxOpenOrders.getItems().stream()
        .map(BiboxAdapters::adaptLimitOpenOrder)
        .collect(Collectors.toList()));
  }

  private static LimitOrder adaptLimitOpenOrder(BiboxOrder biboxOrder) {
    CurrencyPair currencyPair = new CurrencyPair(biboxOrder.getCoinSymbol(), biboxOrder.getCurrencySymbol());
    return new LimitOrder.Builder(biboxOrder.getOrderSide().getOrderType(), currencyPair)
        .id(String.valueOf(biboxOrder.getId()))
        .timestamp(new Date(biboxOrder.getCreatedAt()))
        .limitPrice(biboxOrder.getPrice())
        .originalAmount(biboxOrder.getAmount())
        .cumulativeAmount(biboxOrder.getDealAmount())
        .remainingAmount(biboxOrder.getUnexecuted())
        .orderStatus(biboxOrder.getStatus().getOrderStatus())
        .build();
  }

  public static ExchangeMetaData adaptMetadata(List<BiboxMarket> markets) {
    Map<CurrencyPair, CurrencyPairMetaData> pairMeta = new HashMap<>();
    for (BiboxMarket biboxMarket : markets) {
      pairMeta.put(new CurrencyPair(biboxMarket.getCoinSymbol(), biboxMarket.getCurrencySymbol()), new CurrencyPairMetaData(null, null, null, null));
    }
    return new ExchangeMetaData(pairMeta, null, null, null, null);
  }

  public static UserTrades adaptUserTrades(BiboxOrders biboxOrderHistory) {
    List<UserTrade> trades = biboxOrderHistory.getItems().stream()
        .map(BiboxAdapters::adaptUserTrade)
        .collect(Collectors.toList());
    return new UserTrades(trades, TradeSortType.SortByID);
  }
  
  private static UserTrade adaptUserTrade(BiboxOrder order) {
    return new UserTrade.Builder()
        .id(Long.toString(order.getId()))
        .currencyPair(new CurrencyPair(order.getCoinSymbol(), order.getCurrencySymbol()))
        .price(order.getPrice())
        .originalAmount(order.getAmount())
        .timestamp(new Date(order.getCreatedAt()))
        .type(order.getOrderSide().getOrderType())
        .feeCurrency(new Currency(order.getFeeSymbol()))
        .feeAmount(order.getFee())
        .build();
  }
}
