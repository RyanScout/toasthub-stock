package org.toasthub.stock.algorithm;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.stock.model.AssetDay;
import org.toasthub.stock.model.AssetMinute;
import org.toasthub.stock.model.Configuration;
import org.toasthub.stock.model.LBB;
import org.toasthub.stock.model.SMA;
import org.toasthub.stock.model.Symbol;
import org.toasthub.stock.model.TechnicalIndicator;
import org.toasthub.stock.model.TradeConstant;
import org.toasthub.stock.model.TradeSignalCache;
import org.toasthub.stock.model.UBB;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.common.enums.Exchange;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.bar.CryptoBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarFeed;

@Service("TAAlgorithmCruncherSvc")
public class AlgorithmCruncherSvcImpl implements AlgorithmCruncherSvc {

	@Autowired
	protected AlpacaAPI alpacaAPI;

	@Autowired
	@Qualifier("TAAlgorithmCruncherDao")
	protected AlgorithmCruncherDao algorithmCruncherDao;

	@Autowired
	private TradeSignalCache tradeSignalCache;

	public final int START_OF_2022 = 1640998860;

	@Override
	public void process(final RestRequest request, final RestResponse response) {
		final String action = (String) request.getParams().get("action");

		switch (action) {
			case "ITEM":
				item(request, response);
				break;
			case "LIST":
				break;
			case "SAVE":
				save(request, response);
				break;
			case "DELETE":
				delete(request, response);
				break;
			case "BACKLOAD":
				System.out.println("Starting!");
				backloadCryptoData(request, response);
				System.out.println("CryptoData Loaded");
				backloadStockData(request, response);
				System.out.println("StockData Loaded");
				break;
			case "BACKLOAD_ALG":
				backloadAlg(request, response);
				break;
			default:
				break;
		}

	}

	@Override
	public void delete(final RestRequest request, final RestResponse response) {
		try {
			algorithmCruncherDao.delete(request, response);
			algorithmCruncherDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				algorithmCruncherDao.items(request, response);
			}
			response.setStatus(RestResponse.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(RestResponse.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	@Override
	public void item(final RestRequest request, final RestResponse response) {
		try {
			algorithmCruncherDao.item(request, response);
			response.setStatus(RestResponse.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(RestResponse.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	@Override
	public void items(final RestRequest request, final RestResponse response) {
		try {
			algorithmCruncherDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				algorithmCruncherDao.items(request, response);
			}
			response.setStatus(RestResponse.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(RestResponse.ACTIONFAILED);
			e.printStackTrace();
		}
	}

	@Override
	public void initializeDatabase() {
		final RestRequest request = new RestRequest();
		final RestResponse response = new RestResponse();
		request.addParam(TradeConstant.IDENTIFIER, "CONFIGURATION");
		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		Configuration config = new Configuration();

		if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 1) {

			try {
				algorithmCruncherDao.item(request, response);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			config = Configuration.class.cast(response.getParam(GlobalConstant.ITEM));
		}

		if (!config.isBackloaded()) {
			request.addParam("action", "BACKLOAD");
			process(request, response);
		}

		config.setBackloaded(true);
		request.addParam(GlobalConstant.ITEM, config);
		try {
			algorithmCruncherDao.save(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void save(final RestRequest request, final RestResponse response) {
	}

	public void backloadStockData(final RestRequest request, final RestResponse response) {
		try {
			for (final String stockName : Symbol.STOCKSYMBOLS) {

				System.out.println("Backloading stock data - " + stockName);

				final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York")).truncatedTo(ChronoUnit.DAYS);
				ZonedDateTime first = ZonedDateTime
						.ofInstant(Instant.ofEpochSecond(START_OF_2022), ZoneId.of("America/New_York"))
						.truncatedTo(ChronoUnit.DAYS);
				ZonedDateTime second = first.plusDays(1).minusMinutes(1);
				while (second.isBefore(now)) {
					List<StockBar> stockBars = null;
					List<StockBar> stockBar = null;

					try {
						stockBars = alpacaAPI.stockMarketData().getBars(stockName,
								first,
								second,
								null,
								null,
								1,
								BarTimePeriod.MINUTE,
								BarAdjustment.SPLIT,
								BarFeed.SIP).getBars();
					} catch (final Exception e) {
						System.out.println("Caught!");
						Thread.sleep(1200 * 60);
						System.out.println("Resuming!");
						stockBars = alpacaAPI.stockMarketData().getBars(stockName,
								first,
								second,
								null,
								null,
								1,
								BarTimePeriod.MINUTE,
								BarAdjustment.SPLIT,
								BarFeed.SIP).getBars();
					}

					try {
						stockBar = alpacaAPI.stockMarketData().getBars(stockName,
								first,
								second,
								null,
								null,
								1,
								BarTimePeriod.DAY,
								BarAdjustment.SPLIT,
								BarFeed.SIP).getBars();
					} catch (final Exception e) {
						System.out.println("Caught!");
						Thread.sleep(1200 * 60);
						System.out.println("Resuming!");
						stockBar = alpacaAPI.stockMarketData().getBars(stockName,
								first,
								second,
								null,
								null,
								1,
								BarTimePeriod.DAY,
								BarAdjustment.SPLIT,
								BarFeed.SIP).getBars();
					}

					if (stockBar != null && stockBars != null) {

						final AssetDay stockDay = new AssetDay();
						stockDay.setSymbol(stockName);
						stockDay.setHigh(new BigDecimal(stockBar.get(0).getHigh()));
						stockDay.setEpochSeconds(first.toEpochSecond());
						stockDay.setClose(new BigDecimal(stockBar.get(0).getClose()));
						stockDay.setLow(new BigDecimal(stockBar.get(0).getLow()));
						stockDay.setOpen(new BigDecimal(stockBar.get(0).getOpen()));
						stockDay.setVolume(stockBar.get(0).getVolume());
						stockDay.setVwap(new BigDecimal(stockBar.get(0).getVwap()));
						final LinkedHashSet<AssetMinute> stockMinutes = new LinkedHashSet<AssetMinute>();

						for (int i = 0; i < stockBars.size(); i++) {
							final AssetMinute stockMinute = new AssetMinute();
							stockMinute.setAssetDay(stockDay);
							stockMinute.setSymbol(stockName);
							stockMinute.setEpochSeconds(stockBars.get(i).getTimestamp().toEpochSecond());
							stockMinute.setValue(new BigDecimal(stockBars.get(i).getClose()));
							stockMinute.setVolume(stockBars.get(i).getVolume());
							stockMinute.setVwap(new BigDecimal(stockBars.get(i).getVwap()));
							stockMinutes.add(stockMinute);
						}

						stockDay.setAssetMinutes(stockMinutes);
						request.addParam(GlobalConstant.ITEM, stockDay);

						final StopWatch timer = new StopWatch();

						timer.start();
						algorithmCruncherDao.save(request, response);
						timer.stop();
						System.out.println(
								"Saving this stock day took " + timer.getLastTaskTimeMillis() + " milliseconds");
					}

					first = first.plusDays(1);
					second = second.plusDays(1);
				}
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void backloadCryptoData(final RestRequest request, final RestResponse response) {
		try {
			final Collection<Exchange> exchanges = new ArrayList<Exchange>();
			exchanges.add(Exchange.COINBASE);

			for (final String cryptoName : Symbol.CRYPTOSYMBOLS) {

				System.out.println("Backloading crypyo data - " + cryptoName);

				final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
				ZonedDateTime first = ZonedDateTime
						.ofInstant(Instant.ofEpochSecond(START_OF_2022), ZoneId.of("America/New_York"))
						.truncatedTo(ChronoUnit.DAYS);
				ZonedDateTime second = first.plusDays(1);
				while (second.isBefore(now)) {
					List<CryptoBar> cryptoBars = null;
					List<CryptoBar> cryptoBar = null;
					try {
						cryptoBars = alpacaAPI.cryptoMarketData().getBars(cryptoName,
								exchanges,
								first,
								1500,
								null,
								1,
								BarTimePeriod.MINUTE).getBars();
					} catch (final Exception e) {
						System.out.println("Caught!");
						Thread.sleep(1200 * 60);
						System.out.println("Resuming!");
						cryptoBars = alpacaAPI.cryptoMarketData().getBars(cryptoName,
								exchanges,
								first,
								1500,
								null,
								1,
								BarTimePeriod.MINUTE).getBars();
					}

					for (int i = 0; i < cryptoBars.size(); i++) {
						if (!cryptoBars.get(i).getTimestamp().isBefore(second)) {
							cryptoBars = cryptoBars.subList(0, i);
							break;
						}
					}
					try {
						cryptoBar = alpacaAPI.cryptoMarketData().getBars(cryptoName,
								null,
								first,
								1,
								null,
								1,
								BarTimePeriod.DAY).getBars();
					} catch (final Exception e) {
						System.out.println("Caught!");
						Thread.sleep(1200 * 60);
						System.out.println("Resuming!");
						cryptoBar = alpacaAPI.cryptoMarketData().getBars(cryptoName,
								null,
								first,
								1,
								null,
								1,
								BarTimePeriod.DAY).getBars();
					}
					if (cryptoBar != null && cryptoBars != null) {

						final AssetDay cryptoDay = new AssetDay();
						cryptoDay.setSymbol(cryptoName);
						cryptoDay.setHigh(new BigDecimal(cryptoBar.get(0).getHigh()));
						cryptoDay.setEpochSeconds(first.toEpochSecond());
						cryptoDay.setClose(new BigDecimal(cryptoBar.get(0).getClose()));
						cryptoDay.setLow(new BigDecimal(cryptoBar.get(0).getLow()));
						cryptoDay.setOpen(new BigDecimal(cryptoBar.get(0).getOpen()));
						cryptoDay.setVolume(cryptoBar.get(0).getVolume().longValue());
						cryptoDay.setVwap(new BigDecimal(cryptoBar.get(0).getVwap()));
						final Set<AssetMinute> cryptoMinutes = new LinkedHashSet<AssetMinute>();

						for (int i = 0; i < cryptoBars.size(); i++) {
							final AssetMinute cryptoMinute = new AssetMinute();
							cryptoMinute.setAssetDay(cryptoDay);
							cryptoMinute.setSymbol(cryptoName);
							cryptoMinute.setEpochSeconds(cryptoBars.get(i).getTimestamp().toEpochSecond());
							cryptoMinute.setValue(new BigDecimal(cryptoBars.get(i).getClose()));
							cryptoMinute.setVolume(cryptoBars.get(i).getVolume().longValue());
							cryptoMinute.setVwap(new BigDecimal(cryptoBars.get(i).getVwap()));
							cryptoMinutes.add(cryptoMinute);
						}

						cryptoDay.setAssetMinutes(cryptoMinutes);

						request.addParam(GlobalConstant.ITEM, cryptoDay);

						final StopWatch timer = new StopWatch();

						timer.start();
						algorithmCruncherDao.save(request, response);
						timer.stop();
						System.out.println(
								"Saving this crypto day took " + timer.getLastTaskTimeMillis() + " milliseconds");
					}

					first = first.plusDays(1);
					second = second.plusDays(1);
				}
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadStockData(final RestRequest request, final RestResponse response) {
		try {
			for (final String stockName : Symbol.STOCKSYMBOLS) {
				final ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York")).minusSeconds(60 * 20);

				Set<AssetMinute> preExistingStockMinutes = new LinkedHashSet<AssetMinute>();

				final List<StockBar> stockBars = alpacaAPI.stockMarketData().getBars(stockName,
						today.truncatedTo(ChronoUnit.DAYS),
						today,
						null,
						null,
						1,
						BarTimePeriod.MINUTE,
						BarAdjustment.SPLIT,
						BarFeed.SIP).getBars();

				final List<StockBar> stockBar = alpacaAPI.stockMarketData().getBars(stockName,
						today.truncatedTo(ChronoUnit.DAYS),
						today,
						null,
						null,
						1,
						BarTimePeriod.DAY,
						BarAdjustment.SPLIT,
						BarFeed.SIP).getBars();

				if (stockBar == null) {
					return;
				}

				AssetDay stockDay = new AssetDay();
				final Set<AssetMinute> stockMinutes = new LinkedHashSet<AssetMinute>();

				request.addParam(TradeConstant.EPOCHSECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				request.addParam(TradeConstant.SYMBOL, stockName);
				request.addParam(TradeConstant.TYPE, "AssetDay");
				request.addParam(TradeConstant.IDENTIFIER, "AssetDay");

				try {
					algorithmCruncherDao.initializedAssetDay(request, response);
					stockDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
					preExistingStockMinutes = stockDay.getAssetMinutes();
				} catch (final NoResultException e) {
					stockDay.setSymbol(stockName);
					stockDay.setEpochSeconds(today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				}

				stockDay.setHigh(new BigDecimal(stockBar.get(0).getHigh()));
				stockDay.setClose(new BigDecimal(stockBar.get(0).getClose()));
				stockDay.setLow(new BigDecimal(stockBar.get(0).getLow()));
				stockDay.setOpen(new BigDecimal(stockBar.get(0).getOpen()));
				stockDay.setVolume(stockBar.get(0).getVolume());
				stockDay.setVwap(new BigDecimal(stockBar.get(0).getVwap()));

				for (int i = preExistingStockMinutes.size(); i < stockBars.size(); i++) {
					final AssetMinute stockMinute = new AssetMinute();
					stockMinute.setAssetDay(stockDay);
					stockMinute.setSymbol(stockName);
					stockMinute.setEpochSeconds(stockBars.get(i).getTimestamp().toEpochSecond());
					stockMinute.setValue(new BigDecimal(stockBars.get(i).getClose()));
					stockMinute.setVolume(stockBars.get(i).getVolume());
					stockMinute.setVwap(new BigDecimal(stockBars.get(i).getVwap()));
					stockMinutes.add(stockMinute);
				}

				stockMinutes.addAll(preExistingStockMinutes);

				stockDay.setAssetMinutes(stockMinutes);
				request.addParam(GlobalConstant.ITEM, stockDay);
				algorithmCruncherDao.save(request, response);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadCryptoData(final RestRequest request, final RestResponse response) {
		try {
			final Collection<Exchange> exchanges = new ArrayList<Exchange>();
			exchanges.add(Exchange.COINBASE);

			for (final String cryptoName : Symbol.CRYPTOSYMBOLS) {
				final ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York"));
				Set<AssetMinute> preExistingCryptoMinutes = new LinkedHashSet<AssetMinute>();

				final List<CryptoBar> cryptoBars = alpacaAPI.cryptoMarketData().getBars(cryptoName,
						exchanges,
						today.truncatedTo(ChronoUnit.DAYS),
						1500,
						null,
						1,
						BarTimePeriod.MINUTE).getBars();

				final List<CryptoBar> cryptoBar = alpacaAPI.cryptoMarketData().getBars(cryptoName,
						exchanges,
						today.truncatedTo(ChronoUnit.DAYS),
						1,
						null,
						1,
						BarTimePeriod.DAY).getBars();

				if (cryptoBar == null) {
					return;
				}

				AssetDay cryptoDay = new AssetDay();
				final Set<AssetMinute> cryptoMinutes = new LinkedHashSet<AssetMinute>();

				request.addParam(TradeConstant.EPOCHSECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				request.addParam(TradeConstant.SYMBOL, cryptoName);
				request.addParam(TradeConstant.TYPE, "AssetDay");
				request.addParam(TradeConstant.IDENTIFIER, "AssetDay");

				try {
					algorithmCruncherDao.initializedAssetDay(request, response);
					cryptoDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
					preExistingCryptoMinutes = cryptoDay.getAssetMinutes();
				} catch (final NoResultException e) {
					cryptoDay.setSymbol(cryptoName);
					cryptoDay.setEpochSeconds(today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				}

				cryptoDay.setHigh(BigDecimal.valueOf(cryptoBar.get(0).getHigh()));
				cryptoDay.setClose(BigDecimal.valueOf(cryptoBar.get(0).getClose()));
				cryptoDay.setLow(BigDecimal.valueOf(cryptoBar.get(0).getLow()));
				cryptoDay.setOpen(BigDecimal.valueOf(cryptoBar.get(0).getOpen()));
				cryptoDay.setVolume(cryptoBar.get(0).getVolume().longValue());
				cryptoDay.setVwap(BigDecimal.valueOf(cryptoBar.get(0).getVwap()));

				for (int i = preExistingCryptoMinutes.size(); i < cryptoBars.size(); i++) {
					final AssetMinute cryptoMinute = new AssetMinute();
					cryptoMinute.setAssetDay(cryptoDay);
					cryptoMinute.setSymbol(cryptoName);
					cryptoMinute.setEpochSeconds(cryptoBars.get(i).getTimestamp().toEpochSecond());
					cryptoMinute.setValue(BigDecimal.valueOf(cryptoBars.get(i).getClose()));
					cryptoMinute.setVolume(cryptoBars.get(i).getVolume().longValue());
					cryptoMinute.setVwap(BigDecimal.valueOf(cryptoBars.get(i).getVwap()));
					cryptoMinutes.add(cryptoMinute);
				}

				cryptoMinutes.addAll(preExistingCryptoMinutes);

				cryptoDay.setAssetMinutes(cryptoMinutes);
				request.addParam(GlobalConstant.ITEM, cryptoDay);
				algorithmCruncherDao.save(request, response);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadAlgorithmData(final RestRequest request, final RestResponse response) {

		request.addParam(TradeConstant.IDENTIFIER, "TECHNICAL_INDICATOR");

		try {
			algorithmCruncherDao.items(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		final Set<SMA> smaSet = new HashSet<SMA>();
		final Set<LBB> lbbSet = new HashSet<LBB>();
		final Set<UBB> ubbSet = new HashSet<UBB>();

		if (response.getParam("SMA_SET") != null) {
			for (final Object obj : Set.class.cast(response.getParam("SMA_SET"))) {
				smaSet.add((SMA) obj);
			}
		}

		if (response.getParam("LBB_SET") != null) {
			for (final Object obj : Set.class.cast(response.getParam("LBB_SET"))) {
				lbbSet.add((LBB) obj);
			}
		}

		if (response.getParam("UBB_SET") != null) {
			for (final Object obj : Set.class.cast(response.getParam("UBB_SET"))) {
				ubbSet.add((UBB) obj);
			}
		}

		Stream.of(Symbol.SYMBOLS).forEach(symbol -> {

			final List<AssetDay> assetDays = new ArrayList<AssetDay>();
			final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();

			final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));

			request.addParam("STARTING_EPOCH_SECONDS", now.minusDays(1000).toEpochSecond());
			request.addParam("ENDING_EPOCH_SECONDS", now.plusDays(1000).toEpochSecond());
			request.addParam(TradeConstant.IDENTIFIER, "AssetDay");
			request.addParam(TradeConstant.SYMBOL, symbol);

			try {
				algorithmCruncherDao.items(request, response);
			} catch (final Exception e) {
				e.printStackTrace();
			}

			for (final Object obj : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
				final AssetDay assetDay = AssetDay.class.cast(obj);
				assetDays.add(assetDay);
			}

			assetDays.sort((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()));

			if (assetDays.size() - 1 < 0)
				return;

			request.addParam("STARTING_EPOCH_SECONDS", now.minusMinutes(1000).toEpochSecond());
			request.addParam("ENDING_EPOCH_SECONDS", now.plusMinutes(1000).toEpochSecond());
			request.addParam(TradeConstant.IDENTIFIER, "AssetMinute");
			request.addParam(TradeConstant.SYMBOL, symbol);

			try {
				algorithmCruncherDao.items(request, response);
			} catch (final Exception e) {
				e.printStackTrace();
			}

			for (final Object obj : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
				final AssetMinute assetMinute = AssetMinute.class.cast(obj);
				assetMinutes.add(assetMinute);
			}

			assetMinutes.sort((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()));

			if (assetMinutes.size() - 1 < 0)
				return;

			final List<SMA> smaList = new ArrayList<SMA>();
			smaSet.stream()
					.filter(sma -> sma.getSymbol().equals(symbol))
					.forEach(sma -> {

						final String evaluationPeriod = sma.getType().substring(sma.getType().indexOf("-") + 1);

						request.addParam(GlobalConstant.ITEM, sma);
						request.addParam("SUCCESSFUL", false);

						switch (evaluationPeriod) {
							case "day":
								request.addParam("RECENT_ASSET_MINUTE", assetMinutes.get(assetMinutes.size() - 1));
								request.addParam(GlobalConstant.ITEMS, assetDays);
								configureSMADay(request, response);
								break;
							case "minute":
								request.addParam(GlobalConstant.ITEMS, assetMinutes);
								configureSMAMinute(request, response);
								break;
							default:
								System.out.println("Invalid evaluationPeriod");
								return;
						}

						if ((boolean) request.getParam("SUCCESSFUL")) {
							smaList.add(sma);
						}

					});

			request.addParam(GlobalConstant.ITEMS, smaList);
			algorithmCruncherDao.saveAll(request, response);

			final List<LBB> lbbList = new ArrayList<LBB>();
			lbbSet.stream()
					.filter(lbb -> lbb.getSymbol().equals(symbol))
					.forEach(lbb -> {

						final String evaluationPeriod = lbb.getType().substring(lbb.getType().indexOf("-") + 1);

						request.addParam(GlobalConstant.ITEM, lbb);
						request.addParam("SUCCESSFUL", false);

						switch (evaluationPeriod) {
							case "day":
								request.addParam("RECENT_ASSET_MINUTE", assetMinutes.get(assetMinutes.size() - 1));
								request.addParam(GlobalConstant.ITEMS, assetDays);
								configureLBBDay(request, response);
								break;
							case "minute":
								request.addParam(GlobalConstant.ITEMS, assetMinutes);
								configureLBBMinute(request, response);
								break;
							default:
								System.out.println("Invalid evaluationPeriod");
								return;
						}

						if ((boolean) request.getParam("SUCCESSFUL")) {
							lbbList.add(lbb);
						}
					});

			request.addParam(GlobalConstant.ITEMS, lbbList);
			algorithmCruncherDao.saveAll(request, response);

			final List<UBB> ubbList = new ArrayList<UBB>();
			ubbSet.stream()
					.filter(ubb -> ubb.getSymbol().equals(symbol))
					.forEach(ubb -> {

						final String evaluationPeriod = ubb.getType().substring(ubb.getType().indexOf("-") + 1);

						request.addParam(GlobalConstant.ITEM, ubb);
						request.addParam("SUCCESSFUL", false);

						switch (evaluationPeriod) {
							case "day":
								request.addParam("RECENT_ASSET_MINUTE", assetMinutes.get(assetMinutes.size() - 1));
								request.addParam(GlobalConstant.ITEMS, assetDays);
								configureUBBDay(request, response);
								break;
							case "minute":
								request.addParam(GlobalConstant.ITEMS, assetMinutes);
								configureUBBMinute(request, response);
								break;
							default:
								System.out.println("Invalid evaluationPeriod");
								return;
						}

						if ((boolean) request.getParam("SUCCESSFUL")) {
							ubbList.add(ubb);
						}
					});

			request.addParam(GlobalConstant.ITEMS, ubbList);
			algorithmCruncherDao.saveAll(request, response);
		});
	}

	@Override
	public void backloadAlg(final RestRequest request, final RestResponse response) {

		if (request.getParam(GlobalConstant.ITEMID) == null) {
			System.out.println("No item id");
			response.setStatus(RestResponse.ERROR);
			return;
		}

		algorithmCruncherDao.getTechicalIndicator(request, response);

		final TechnicalIndicator t = (TechnicalIndicator) response.getParam(GlobalConstant.ITEM);

		if (t.isUpdating()) {
			response.setStatus(RestResponse.ERROR);
			return;
		}

		t.setUpdating(true);

		request.addParam(GlobalConstant.ITEM, t);

		try {
			algorithmCruncherDao.save(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		tradeSignalCache.insertTechnicalIndicator(t);

		request.addParam("ENDING_EPOCH_SECONDS", t.getLastCheck());
		request.addParam(TradeConstant.SYMBOL, t.getSymbol());

		switch (t.getTechnicalIndicatorType()) {
			case TechnicalIndicator.GOLDENCROSS:
				request.addParam("SHORT_SMA_TYPE", t.getShortSMAType());
				request.addParam("LONG_SMA_TYPE", t.getLongSMAType());
				backloadSMA(request, response);
				break;
			case TechnicalIndicator.LOWERBOLLINGERBAND:
				request.addParam("LBB_TYPE", t.getLBBType());
				request.addParam("STANDARD_DEVIATIONS", t.getStandardDeviations());
				backloadLBB(request, response);
				break;
			case TechnicalIndicator.UPPERBOLLINGERBAND:
				request.addParam("UBB_TYPE", t.getLBBType());
				request.addParam("STANDARD_DEVIATIONS", t.getStandardDeviations());
				break;
			default:
				System.out.println("INVALID TECHINCAL INDICATOR TYPE AT ALGORITHMCRUCNHERSVC BACKLOADALG");
				response.setStatus(RestResponse.ERROR);
				return;
		}

		response.setStatus(RestResponse.SUCCESS);
	}

	public void backloadSMA(final RestRequest request, final RestResponse response) {
		final Set<SMA> smaSet = new HashSet<SMA>();

		final String shortSMAType = (String) request.getParam("SHORT_SMA_TYPE");
		final String longSMAType = (String) request.getParam("LONG_SMA_TYPE");
		final String symbol = (String) request.getParam(TradeConstant.SYMBOL);

		final int daysToBackload = (int) request.getParam("DAYS_TO_BACKLOAD");

		final SMA shortSMA = new SMA();
		shortSMA.setType(shortSMAType);
		shortSMA.setSymbol(symbol);
		smaSet.add(shortSMA);

		final SMA longSMA = new SMA();
		longSMA.setType(longSMAType);
		longSMA.setSymbol(symbol);
		smaSet.add(longSMA);

		final ForkJoinPool customThreadPool = new ForkJoinPool(2);

		try {
			customThreadPool.submit(() -> smaSet.stream()
					.parallel()
					.forEachOrdered(sma -> {

						final List<AssetDay> assetDays = new ArrayList<AssetDay>();
						final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();

						final String smaType = sma.getType();
						final int smaPeriod = Integer.valueOf(smaType.substring(0, smaType.indexOf("-")));

						request.addParam(TradeConstant.IDENTIFIER, "SMA");
						request.addParam(TradeConstant.TYPE, smaType);
						request.addParam(TradeConstant.SYMBOL, symbol);

						final long endingEpochSeconds = (long) request.getParam("ENDING_EPOCH_SECONDS");
						final long startingEpochSeconds = endingEpochSeconds
								- (60 * 60 * 24 * (daysToBackload + smaPeriod));

						request.addParam("STARTING_EPOCH_SECONDS", startingEpochSeconds);
						request.addParam("ENDING_EPOCH_SECONDS", endingEpochSeconds);

						request.addParam(TradeConstant.IDENTIFIER, "AssetDay");

						try {
							algorithmCruncherDao.items(request, response);
						} catch (final Exception e) {
							e.printStackTrace();
						}

						for (final Object o : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
							assetDays.add((AssetDay) o);
						}

						assetDays.stream()
								.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
								.distinct()
								.forEachOrdered(assetDay -> {

									final StopWatch timer = new StopWatch();

									timer.start();

									final List<SMA> smaList = new ArrayList<SMA>();

									final List<AssetDay> trimmedAssetDays = assetDays.stream()
											.filter(a -> a.getEpochSeconds() <= assetDay.getEpochSeconds())
											.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
											.collect(Collectors.toCollection(ArrayList::new));

									request.addParam(TradeConstant.IDENTIFIER, "AssetMinute");
									request.addParam("STARTING_EPOCH_SECONDS", assetDay.getEpochSeconds());
									request.addParam("ENDING_EPOCH_SECONDS",
											assetDay.getEpochSeconds() + (60 * 60 * 24));

									try {
										algorithmCruncherDao.items(request, response);
									} catch (final Exception e) {
										e.printStackTrace();
									}

									for (final Object o : ArrayList.class
											.cast(response.getParam(GlobalConstant.ITEMS))) {
										assetMinutes.add((AssetMinute) o);
									}

									assetMinutes.stream()
											.filter(assetMinute -> assetDay
													.getEpochSeconds() == ZonedDateTime
															.ofInstant(
																	Instant.ofEpochSecond(
																			assetMinute.getEpochSeconds()),
																	ZoneId.of("America/New_York"))
															.truncatedTo(ChronoUnit.DAYS).toEpochSecond())
											.distinct()
											.forEach(assetMinute -> {

												final SMA tempSMA = new SMA();
												tempSMA.setType(smaType);
												tempSMA.setSymbol(symbol);

												request.addParam(GlobalConstant.ITEM, tempSMA);

												request.addParam("SUCCESSFUL", false);

												request.addParam("RECENT_ASSET_MINUTE", assetMinute);

												request.addParam(GlobalConstant.ITEMS, trimmedAssetDays);

												configureSMADay(request, response);

												if ((boolean) request.getParam("SUCCESSFUL")) {
													smaList.add(tempSMA);
												}

											});

									request.addParam(GlobalConstant.ITEMS, smaList);

									algorithmCruncherDao.saveAll(request, response);

									timer.stop();

									System.out.println("Saved " + smaList.size() + " new entities");
									System.out
											.println("Loading SMA day took " + timer.getLastTaskTimeMillis()
													+ "milliseconds");

								});

					})).get();

			customThreadPool.shutdown();

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void backloadLBB(final RestRequest request, final RestResponse response) {

		final String lbbType = (String) request.getParam("LBB_TYPE");
		final String symbol = (String) request.getParam(TradeConstant.SYMBOL);

		final BigDecimal standardDeviations = (BigDecimal) request.getParam("STANDARD_DEVIATIONS");

		final int daysToBackload = (int) request.getParam("DAYS_TO_BACKLOAD");

		final LBB lbbPrototype = new LBB();
		lbbPrototype.setType(lbbType);
		lbbPrototype.setSymbol(symbol);
		lbbPrototype.setStandardDeviations(standardDeviations);

		final List<AssetDay> assetDays = new ArrayList<AssetDay>();
		final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();

		final int lbbPeriod = Integer.valueOf(lbbType.substring(0, lbbType.indexOf("-")));

		request.addParam(TradeConstant.IDENTIFIER, "LBB");
		request.addParam(TradeConstant.TYPE, lbbType);
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam("STANDARD_DEVIATIONS", standardDeviations);

		final long endingEpochSeconds = (long) request.getParam("ENDING_EPOCH_SECONDS");
		final long startingEpochSeconds = endingEpochSeconds
				- (60 * 60 * 24 * (daysToBackload + lbbPeriod));

		request.addParam("STARTING_EPOCH_SECONDS", startingEpochSeconds);
		request.addParam("ENDING_EPOCH_SECONDS", endingEpochSeconds);

		request.addParam(TradeConstant.IDENTIFIER, "AssetDay");

		try {
			algorithmCruncherDao.items(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		for (final Object o : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
			assetDays.add((AssetDay) o);
		}

		final ForkJoinPool customThreadPool = new ForkJoinPool(2);

		try {
			customThreadPool.submit(() -> assetDays.stream()
					.parallel()
					.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
					.distinct()
					.forEachOrdered(assetDay -> {

						final StopWatch timer = new StopWatch();

						timer.start();

						final List<LBB> lbbList = new ArrayList<LBB>();

						final List<AssetDay> trimmedAssetDays = assetDays.stream()
								.filter(a -> a.getEpochSeconds() <= assetDay.getEpochSeconds())
								.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
								.collect(Collectors.toCollection(ArrayList::new));

						request.addParam(TradeConstant.IDENTIFIER, "AssetMinute");
						request.addParam("STARTING_EPOCH_SECONDS", assetDay.getEpochSeconds());
						request.addParam("ENDING_EPOCH_SECONDS",
								assetDay.getEpochSeconds() + (60 * 60 * 24));

						try {
							algorithmCruncherDao.items(request, response);
						} catch (final Exception e) {
							e.printStackTrace();
						}

						for (final Object o : ArrayList.class
								.cast(response.getParam(GlobalConstant.ITEMS))) {
							assetMinutes.add((AssetMinute) o);
						}

						assetMinutes.stream()
								.filter(assetMinute -> assetDay
										.getEpochSeconds() == ZonedDateTime
												.ofInstant(
														Instant.ofEpochSecond(
																assetMinute.getEpochSeconds()),
														ZoneId.of("America/New_York"))
												.truncatedTo(ChronoUnit.DAYS).toEpochSecond())
								.distinct()
								.forEach(assetMinute -> {

									final LBB tempLBB = new LBB();

									tempLBB.setType(lbbType);
									tempLBB.setSymbol(symbol);
									tempLBB.setStandardDeviations(standardDeviations);

									request.addParam(GlobalConstant.ITEM, tempLBB);

									request.addParam("SUCCESSFUL", false);

									request.addParam("RECENT_ASSET_MINUTE", assetMinute);

									request.addParam(GlobalConstant.ITEMS, trimmedAssetDays);

									configureLBBDay(request, response);

									if ((boolean) request.getParam("SUCCESSFUL")) {
										lbbList.add(tempLBB);
									}

								});

						request.addParam(GlobalConstant.ITEMS, lbbList);

						algorithmCruncherDao.saveAll(request, response);

						timer.stop();

						System.out.println("Saved " + lbbList.size() + " new entities");
						System.out
								.println("Loading LBB day took " + timer.getLastTaskTimeMillis()
										+ "milliseconds");

					})).get();

			customThreadPool.shutdown();

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void configureSMAMinute(final RestRequest request, final RestResponse response) {
		final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();
		final List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();

		final SMA sma = (SMA) request.getParam(GlobalConstant.ITEM);
		final String symbol = sma.getSymbol();
		final String smaType = sma.getType();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetMinute assetMinute = AssetMinute.class.cast(obj);
			assetMinutes.add(assetMinute);
			assetMinuteValues.add(assetMinute.getValue());
		}

		final int i = assetMinutes.size() - 1;
		final int smaPeriod = Integer.valueOf(smaType.substring(0, smaType.indexOf("-")));

		if (i < smaPeriod) {
			return;
		}

		request.addParam(TradeConstant.IDENTIFIER, "SMA");
		request.addParam(TradeConstant.TYPE, smaType);
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam(TradeConstant.EPOCHSECONDS,
				assetMinutes.get(i).getEpochSeconds());

		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
			return;
		}

		sma.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		sma.setSymbol(symbol);
		sma.setType(smaType);

		sma.setValue(SMA.calculateSMA(assetMinuteValues.subList(i - (smaPeriod - 1), i + 1)));

		request.addParam("SUCCESSFUL", true);
	}

	public void configureLBBMinute(final RestRequest request, final RestResponse response) {
		final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();
		final List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();

		final LBB lbb = (LBB) request.getParam(GlobalConstant.ITEM);
		final String symbol = lbb.getSymbol();
		final String lbbType = lbb.getType();
		final BigDecimal standardDeviations = lbb.getStandardDeviations();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetMinute assetMinute = AssetMinute.class.cast(obj);
			assetMinutes.add(assetMinute);
			assetMinuteValues.add(assetMinute.getValue());
		}

		final int i = assetMinutes.size() - 1;
		final int lbbPeriod = Integer.valueOf(lbbType.substring(0, lbbType.indexOf("-")));

		if (i < lbbPeriod) {
			return;
		}

		request.addParam(TradeConstant.IDENTIFIER, "LBB");
		request.addParam(TradeConstant.TYPE, lbbType);
		request.addParam("STANDARD_DEVIATIONS", standardDeviations);
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam(TradeConstant.EPOCHSECONDS,
				assetMinutes.get(i).getEpochSeconds());
		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
			return;
		}

		lbb.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		lbb.setSymbol(symbol);
		lbb.setType(lbbType);
		lbb.setStandardDeviations(standardDeviations);

		request.addParam(TradeConstant.IDENTIFIER, "SMA");
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam(TradeConstant.TYPE, lbbType);
		request.addParam(TradeConstant.EPOCHSECONDS, assetMinutes.get(i).getEpochSeconds());

		try {
			algorithmCruncherDao.item(request, response);
			lbb.setValue(
					LBB.calculateLBB(
							assetMinuteValues.subList(i - (lbbPeriod - 1), i + 1),
							((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
							standardDeviations));
		} catch (final NoResultException e) {
			lbb.setValue(
					LBB.calculateLBB(
							assetMinuteValues.subList(i - (lbbPeriod - 1), i + 1),
							standardDeviations));
		}
		request.addParam("SUCCESSFUL", true);
	}

	public void configureUBBMinute(final RestRequest request, final RestResponse response) {

		final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();
		final List<BigDecimal> assetMinuteValues = new ArrayList<BigDecimal>();

		final UBB ubb = (UBB) request.getParam(GlobalConstant.ITEM);
		final String symbol = ubb.getSymbol();
		final String ubbType = ubb.getType();
		final BigDecimal standardDeviations = ubb.getStandardDeviations();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetMinute assetMinute = AssetMinute.class.cast(obj);
			assetMinutes.add(assetMinute);
			assetMinuteValues.add(assetMinute.getValue());
		}

		final int i = assetMinutes.size() - 1;
		final int ubbPeriod = Integer.valueOf(ubbType.substring(0, ubbType.indexOf("-")));

		if (i < ubbPeriod) {
			return;
		}

		request.addParam(TradeConstant.IDENTIFIER, "UBB");
		request.addParam(TradeConstant.TYPE, ubbType);
		request.addParam("STANDARD_DEVIATIONS", standardDeviations);
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam(TradeConstant.EPOCHSECONDS,
				assetMinutes.get(i).getEpochSeconds());
		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
			return;
		}
		ubb.setEpochSeconds(assetMinutes.get(i).getEpochSeconds());
		ubb.setSymbol(symbol);
		ubb.setType(ubbType);
		ubb.setStandardDeviations(standardDeviations);

		request.addParam(TradeConstant.IDENTIFIER, "SMA");
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam(TradeConstant.TYPE, ubbType);
		request.addParam(TradeConstant.EPOCHSECONDS, assetMinutes.get(i).getEpochSeconds());

		try {
			algorithmCruncherDao.item(request, response);
			ubb.setValue(
					UBB.calculateUBB(
							assetMinuteValues.subList(i - (ubbPeriod - 1), i + 1),
							((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
							standardDeviations));
		} catch (final NoResultException e) {
			ubb.setValue(
					UBB.calculateUBB(
							assetMinuteValues.subList(i - (ubbPeriod - 1), i + 1),
							standardDeviations));
		}
		request.addParam("SUCCESSFUL", true);
	}

	public void configureSMADay(final RestRequest request, final RestResponse response) {
		final List<AssetDay> assetDays = new ArrayList<AssetDay>();
		final List<BigDecimal> assetDayValues = new ArrayList<BigDecimal>();
		final AssetMinute assetMinute = (AssetMinute) request.getParam("RECENT_ASSET_MINUTE");

		final SMA sma = (SMA) request.getParam(GlobalConstant.ITEM);
		final String symbol = sma.getSymbol();
		final String smaType = sma.getType();
		final long epochSeconds = assetMinute.getEpochSeconds();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetDay assetDay = AssetDay.class.cast(obj);
			assetDays.add(assetDay);
			assetDayValues.add(assetDay.getClose());
		}

		final int i = assetDays.size() - 1;
		final int smaPeriod = Integer.valueOf(smaType.substring(0, smaType.indexOf("-")));

		if (i < smaPeriod) {
			return;
		}

		assetDayValues.set(assetDayValues.size() - 1, assetMinute.getValue());

		request.addParam(TradeConstant.IDENTIFIER, "SMA");
		request.addParam(TradeConstant.TYPE, smaType);
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam(TradeConstant.EPOCHSECONDS, epochSeconds);

		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
			return;
		}

		sma.setEpochSeconds(epochSeconds);
		sma.setSymbol(symbol);
		sma.setType(smaType);
		sma.setCorrespondingDay(assetDays.get(i).getEpochSeconds());

		sma.setValue(SMA.calculateSMA(assetDayValues.subList(i - (smaPeriod - 1), i + 1)));

		request.addParam("SUCCESSFUL", true);
	}

	public void configureLBBDay(final RestRequest request, final RestResponse response) {
		final List<AssetDay> assetDays = new ArrayList<AssetDay>();
		final List<BigDecimal> assetDayValues = new ArrayList<BigDecimal>();
		final AssetMinute assetMinute = (AssetMinute) request.getParam("RECENT_ASSET_MINUTE");

		final LBB lbb = (LBB) request.getParam(GlobalConstant.ITEM);
		final String symbol = lbb.getSymbol();
		final String lbbType = lbb.getType();
		final BigDecimal standardDeviations = lbb.getStandardDeviations();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetDay assetDay = AssetDay.class.cast(obj);
			assetDays.add(assetDay);
			assetDayValues.add(assetDay.getClose());
		}

		final int i = assetDays.size() - 1;
		final int lbbPeriod = Integer.valueOf(lbbType.substring(0, lbbType.indexOf("-")));

		if (i < lbbPeriod) {
			return;
		}

		assetDayValues.set(assetDayValues.size() - 1, assetMinute.getValue());

		request.addParam(TradeConstant.IDENTIFIER, "LBB");
		request.addParam(TradeConstant.TYPE, lbbType);
		request.addParam("STANDARD_DEVIATIONS", standardDeviations);
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam(TradeConstant.EPOCHSECONDS,
				assetMinute.getEpochSeconds());

		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
			return;
		}

		lbb.setEpochSeconds(assetMinute.getEpochSeconds());
		lbb.setSymbol(symbol);
		lbb.setType(lbbType);
		lbb.setStandardDeviations(standardDeviations);
		lbb.setCorrespondingDay(assetDays.get(i).getEpochSeconds());

		request.addParam(TradeConstant.IDENTIFIER, "SMA");
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam(TradeConstant.TYPE, lbbType);
		request.addParam(TradeConstant.EPOCHSECONDS, lbb.getEpochSeconds());

		try {
			algorithmCruncherDao.item(request, response);
			lbb.setValue(
					LBB.calculateLBB(
							assetDayValues.subList(i - (lbbPeriod - 1), i + 1),
							((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
							standardDeviations));
		} catch (final NoResultException e) {
			lbb.setValue(
					LBB.calculateLBB(
							assetDayValues.subList(i - (lbbPeriod - 1), i + 1),
							standardDeviations));
		}

		request.addParam("SUCCESSFUL", true);
	}

	public void configureUBBDay(final RestRequest request, final RestResponse response) {
		final List<AssetDay> assetDays = new ArrayList<AssetDay>();
		final List<BigDecimal> assetDayValues = new ArrayList<BigDecimal>();
		final AssetMinute assetMinute = (AssetMinute) request.getParam("RECENT_ASSET_MINUTE");

		final UBB ubb = (UBB) request.getParam(GlobalConstant.ITEM);
		final String symbol = ubb.getSymbol();
		final String ubbType = ubb.getType();
		final BigDecimal standardDeviations = ubb.getStandardDeviations();

		for (final Object obj : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
			final AssetDay assetDay = AssetDay.class.cast(obj);
			assetDays.add(assetDay);
			assetDayValues.add(assetDay.getClose());
		}

		assetDayValues.set(assetDayValues.size() - 1, assetMinute.getValue());

		final int i = assetDays.size() - 1;
		final int ubbPeriod = Integer.valueOf(ubbType.substring(0, ubbType.indexOf("-")));

		if (i < ubbPeriod) {
			return;
		}

		request.addParam(TradeConstant.IDENTIFIER, "UBB");
		request.addParam(TradeConstant.TYPE, ubbType);
		request.addParam("STANDARD_DEVIATIONS", standardDeviations);
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam(TradeConstant.EPOCHSECONDS,
				assetMinute.getEpochSeconds());

		try {
			algorithmCruncherDao.itemCount(request, response);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
			return;
		}

		ubb.setEpochSeconds(assetMinute.getEpochSeconds());
		ubb.setSymbol(symbol);
		ubb.setType(ubbType);
		ubb.setStandardDeviations(standardDeviations);
		ubb.setCorrespondingDay(assetDays.get(i).getEpochSeconds());

		request.addParam(TradeConstant.IDENTIFIER, "SMA");
		request.addParam(TradeConstant.SYMBOL, symbol);
		request.addParam(TradeConstant.TYPE, ubbType);
		request.addParam(TradeConstant.EPOCHSECONDS, ubb.getEpochSeconds());

		try {
			algorithmCruncherDao.item(request, response);
			ubb.setValue(
					UBB.calculateUBB(
							assetDayValues.subList(i - (ubbPeriod - 1), i + 1),
							((SMA) response.getParam(GlobalConstant.ITEM)).getValue(),
							standardDeviations));
		} catch (final NoResultException e) {
			ubb.setValue(
					UBB.calculateUBB(
							assetDayValues.subList(i - (ubbPeriod - 1), i + 1),
							standardDeviations));
		}

		request.addParam("SUCCESSFUL", true);
	}
}
