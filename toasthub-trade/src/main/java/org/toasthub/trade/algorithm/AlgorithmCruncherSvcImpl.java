package org.toasthub.trade.algorithm;

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

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.AssetDay;
import org.toasthub.trade.model.AssetMinute;
import org.toasthub.trade.model.Configuration;
import org.toasthub.trade.model.ExpectedException;
import org.toasthub.trade.model.InsufficientDataException;
import org.toasthub.trade.model.LBB;
import org.toasthub.trade.model.SMA;
import org.toasthub.trade.model.Symbol;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TradeConstant;
import org.toasthub.trade.model.TradeSignalCache;
import org.toasthub.trade.model.UBB;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.common.enums.Exchange;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.bar.CryptoBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarFeed;

@Service("TAAlgorithmCruncherSvc")
public class AlgorithmCruncherSvcImpl implements ServiceProcessor, AlgorithmCruncherSvc {

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
		try {
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

					if (request.getParam(GlobalConstant.ITEMID) == null) {
						throw new ExpectedException("Item Id is null");
					}

					if (request.getParam("startTime") == null) {
						throw new ExpectedException("Start time is null");
					}
					final long itemId = Long.valueOf(String.valueOf(request.getParam(GlobalConstant.ITEMID)));

					final long startTime = Long.valueOf(String.valueOf(request.getParam("startTime")));

					final long endTime = Long.valueOf(String.valueOf(request.getParam("startTime")));

					backloadAlgorithm(itemId, startTime, endTime);

					System.out.println("Algorithms Backloaded !");

					break;

				default:
					throw new Exception("Action : " + action + "is not recognized");
			}
		} catch (final Exception e) {
			response.setStatus("Exception : " + e.getMessage());
			e.printStackTrace();
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
			for (final String stockName : Symbol.STOCK_SYMBOLS) {

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

						final StopWatch timer = new StopWatch();

						timer.start();
						algorithmCruncherDao.saveObject(stockDay);
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

			for (final String cryptoName : Symbol.CRYPTO_SYMBOLS) {

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

						final StopWatch timer = new StopWatch();

						timer.start();
						algorithmCruncherDao.saveObject(cryptoDay);
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
			for (final String stockName : Symbol.STOCK_SYMBOLS) {
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

				request.addParam(TradeConstant.EPOCH_SECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				request.addParam(TradeConstant.SYMBOL, stockName);
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

			for (final String cryptoName : Symbol.CRYPTO_SYMBOLS) {
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

				request.addParam(TradeConstant.EPOCH_SECONDS, today.truncatedTo(ChronoUnit.DAYS).toEpochSecond());
				request.addParam(TradeConstant.SYMBOL, cryptoName);
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
		final Set<SMA> smaSet = algorithmCruncherDao.getSMAPrototypes();
		final Set<LBB> lbbSet = algorithmCruncherDao.getLBBPrototypes();
		final Set<UBB> ubbSet = algorithmCruncherDao.getUBBPrototypes();

		Symbol.SYMBOLS.stream().forEach(symbol -> {
			final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));

			final long startingEpochSecondsDay = now.minusDays(1000).toEpochSecond();
			final long endingEpochSecondsDay = now.plusDays(1000).toEpochSecond();

			final List<AssetDay> assetDays = algorithmCruncherDao
					.getAssetDays(symbol, startingEpochSecondsDay, endingEpochSecondsDay).stream()
					.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds())).toList();

			if (assetDays.size() == 0)
				return;

			final long startingEpochSecondsMinute = now.minusMinutes(1000).toEpochSecond();
			final long endingEpochSecondsMinute = now.plusMinutes(1000).toEpochSecond();

			final List<AssetMinute> assetMinutes = algorithmCruncherDao
					.getAssetMinutes(symbol, startingEpochSecondsMinute, endingEpochSecondsMinute).stream()
					.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds())).toList();

			if (assetMinutes.size() == 0)
				return;

			final AssetMinute latestAssetMinute = assetMinutes.get(assetMinutes.size() - 1);

			final List<Object> saveList = new ArrayList<Object>();

			smaSet.stream()
					.filter(sma -> sma.getSymbol().equals(symbol))
					.forEach(sma -> {
						try {
							switch (sma.getEvaluationPeriod().toUpperCase()) {
								case "DAY":
									final long itemCountDay = algorithmCruncherDao.getSMAItemCount(symbol,
											sma.getEvaluationPeriod(), sma.getEvaluationDuration(),
											latestAssetMinute.getEpochSeconds());

									if (itemCountDay >= 1) {
										// throw new ExpectedException("Entity already exists");
										return;
									}

									final SMA configuredDaySMA = sma.configureSMA(assetDays, latestAssetMinute);

									saveList.add(configuredDaySMA);

									break;

								case "MINUTE":
									final long itemCountMinute = algorithmCruncherDao.getSMAItemCount(symbol,
											sma.getEvaluationPeriod(), sma.getEvaluationDuration(),
											latestAssetMinute.getEpochSeconds());

									if (itemCountMinute >= 1) {
										// throw new ExpectedException("Entity already exists");
										return;
									}

									final SMA configuredMinuteSMA = sma.configureSMA(assetMinutes);

									saveList.add(configuredMinuteSMA);

									break;
								default:
									throw new ExpectedException("Invalid evaluationPeriod");
							}

						} catch (final ExpectedException e) {
							e.printStackTrace();
						} catch (final InsufficientDataException e) {
							return;
						}

					});

			lbbSet.stream()
					.filter(lbb -> lbb.getSymbol().equals(symbol))
					.forEach(lbb -> {

						try {
							switch (lbb.getEvaluationPeriod().toUpperCase()) {
								case "DAY":

									final long itemCountDay = algorithmCruncherDao.getLBBItemCount(symbol,
											lbb.getEvaluationPeriod(), lbb.getEvaluationDuration(),
											latestAssetMinute.getEpochSeconds(), lbb.getStandardDeviations());

									if (itemCountDay >= 1) {
										// throw new ExpectedException("Entity already exists");
										return;
									}

									final LBB configuredLBBDay = lbb.configureLBB(assetDays,
											assetMinutes.get(assetMinutes.size() - 1));

									saveList.add(configuredLBBDay);

									break;
								case "MINUTE":

									final long itemCountMinute = algorithmCruncherDao.getLBBItemCount(symbol,
											lbb.getEvaluationPeriod(), lbb.getEvaluationDuration(),
											latestAssetMinute.getEpochSeconds(), lbb.getStandardDeviations());

									if (itemCountMinute >= 1) {
										// throw new ExpectedException("Entity already exists");
										return;
									}

									final LBB configuredLBBMinute = lbb.configureLBB(assetMinutes);
									saveList.add(configuredLBBMinute);
									break;
								default:
									throw new ExpectedException("Invalid evaluationPeriod");
							}

						} catch (final ExpectedException e) {
							e.printStackTrace();
						} catch (final InsufficientDataException e) {
							return;
						}

					});

			ubbSet.stream()
					.filter(ubb -> ubb.getSymbol().equals(symbol))
					.forEach(ubb -> {

						try {
							switch (ubb.getEvaluationPeriod().toUpperCase()) {
								case "DAY":

									final long itemCountDay = algorithmCruncherDao.getUBBItemCount(symbol,
											ubb.getEvaluationPeriod(), ubb.getEvaluationDuration(),
											latestAssetMinute.getEpochSeconds(), ubb.getStandardDeviations());

									if (itemCountDay >= 1) {
										// throw new ExpectedException("Entity already exists");
										return;
									}

									final UBB configuredUBBDay = ubb.configureUBB(assetDays,
											assetMinutes.get(assetMinutes.size() - 1));

									saveList.add(configuredUBBDay);

									break;
								case "MINUTE":

									final long itemCountMinute = algorithmCruncherDao.getUBBItemCount(symbol,
											ubb.getEvaluationPeriod(), ubb.getEvaluationDuration(),
											latestAssetMinute.getEpochSeconds(), ubb.getStandardDeviations());

									if (itemCountMinute >= 1) {
										// throw new ExpectedException("Entity already exists");
										return;
									}

									final UBB configuredUBBMinute = ubb.configureUBB(assetMinutes);
									saveList.add(configuredUBBMinute);
									break;
								default:
									throw new ExpectedException("Invalid evaluationPeriod");
							}

						} catch (final ExpectedException e) {
							e.printStackTrace();
						} catch (final InsufficientDataException e) {
							return;
						}
					});
			algorithmCruncherDao.saveList(saveList);
		});
	}

	@Override
	public void backloadAlgorithm(final long itemId, final long startTime, final long endTime) throws Exception {

		final TechnicalIndicator t = algorithmCruncherDao.findTechnicalIndicatorById(itemId);

		if (t.getEvaluationPeriod().toUpperCase().equals("MINUTE")) {
			throw new Exception("Backloading minute data is not supported");
		}

		algorithmCruncherDao.saveObject(t);

		tradeSignalCache.insertTechnicalIndicator(t);

		switch (t.getTechnicalIndicatorType()) {
			case TechnicalIndicator.GOLDENCROSS:
				backloadSMAValues(t, startTime, endTime);
				break;
			case TechnicalIndicator.LOWERBOLLINGERBAND:
				backloadLBBValues(t, startTime, endTime);
				break;
			case TechnicalIndicator.UPPERBOLLINGERBAND:
				backloadUBBValues(t, startTime, endTime);
				break;
			default:
				throw new ExpectedException("Invalid Technical Indicator");
		}
	}

	public void backloadSMAValues(final TechnicalIndicator technicalIndicator, final long startTime, final long endTime)
			throws ExpectedException {

		if (!technicalIndicator.getTechnicalIndicatorType().equals(TechnicalIndicator.GOLDENCROSS)) {
			throw new ExpectedException("Technical Indicator Type must be Lower Bollinger Band");
		}

		final Set<SMA> smaSet = new HashSet<SMA>();

		final String evaluationPeriod = technicalIndicator.getEvaluationPeriod();

		final int shortSMAEvaluationDuration = technicalIndicator.getShortSMAEvaluationDuration();
		final int longSMAEvaluationDuration = technicalIndicator.getLongSMAEvaluationDuration();

		final String symbol = technicalIndicator.getSymbol();

		final SMA shortSMA = new SMA();
		shortSMA.setEvaluationPeriod(evaluationPeriod);
		shortSMA.setEvaluationDuration(shortSMAEvaluationDuration);
		shortSMA.setSymbol(symbol);
		smaSet.add(shortSMA);

		final SMA longSMA = new SMA();
		longSMA.setEvaluationPeriod(evaluationPeriod);
		longSMA.setEvaluationDuration(longSMAEvaluationDuration);
		longSMA.setSymbol(symbol);
		smaSet.add(longSMA);

		final ForkJoinPool customThreadPool = new ForkJoinPool(2);

		try {
			customThreadPool.submit(() -> smaSet.stream()
					.parallel()
					.forEachOrdered(sma -> {

						final long endingEpochSecondsDay = endTime;

						final long startingEpochSecondsDay = startTime - (60 * 60 * 24 * sma.getEvaluationDuration());

						final List<AssetDay> assetDays = algorithmCruncherDao.getAssetDays(symbol,
								startingEpochSecondsDay, endingEpochSecondsDay);

						assetDays.stream()
								.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
								.distinct()
								.forEachOrdered(assetDay -> {

									final StopWatch timer = new StopWatch();

									timer.start();

									final List<Object> saveList = new ArrayList<Object>();

									final List<AssetDay> trimmedAssetDays = assetDays.stream()
											.filter(a -> a.getEpochSeconds() <= assetDay.getEpochSeconds())
											.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
											.collect(Collectors.toCollection(ArrayList::new));

									final long startingEpochSecondsMinute = assetDay.getEpochSeconds();
									final long endingEpochSecondsMinute = assetDay.getEpochSeconds() + (60 * 60 * 24);

									// only queries assetMinutes without a proper corresponding lbb value
									final List<AssetMinute> assetMinutes = algorithmCruncherDao
											.getAssetMinutesWithoutSma(symbol,
													startingEpochSecondsMinute, endingEpochSecondsMinute,
													sma.getEvaluationPeriod(),
													sma.getEvaluationDuration());

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
												try {
													final SMA configuredSMA = sma
															.configureSMA(trimmedAssetDays, assetMinute);
													saveList.add(configuredSMA);
												} catch (final InsufficientDataException e) {
													return;
												} catch (final Exception e) {
													e.printStackTrace();
												}
											});

									algorithmCruncherDao.saveList(saveList);

									timer.stop();

									System.out.println("Saved " + saveList.size() + " new entities");
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

	public void backloadLBBValues(final TechnicalIndicator technicalIndicator, final long startTime, final long endTime)
			throws ExpectedException {

		if (!technicalIndicator.getTechnicalIndicatorType().equals(TechnicalIndicator.LOWERBOLLINGERBAND)) {
			throw new ExpectedException("Technical Indicator Type must be Lower Bollinger Band");
		}

		final String evaluationPeriod = technicalIndicator.getEvaluationPeriod();

		final int lbbEvaluationDuration = technicalIndicator.getLbbEvaluationDuration();

		final BigDecimal standardDeviations = technicalIndicator.getStandardDeviations();

		final String symbol = technicalIndicator.getSymbol();

		final LBB lbb = new LBB();
		lbb.setEvaluationPeriod(evaluationPeriod);
		lbb.setEvaluationDuration(lbbEvaluationDuration);
		lbb.setStandardDeviations(standardDeviations);
		lbb.setSymbol(symbol);

		final long endingEpochSecondsDay = endTime;

		final long startingEpochSecondsDay = startTime - (60 * 60 * 24 * lbbEvaluationDuration);

		final List<AssetDay> assetDays = algorithmCruncherDao.getAssetDays(symbol,
				startingEpochSecondsDay, endingEpochSecondsDay);

		final ForkJoinPool customThreadPool = new ForkJoinPool(2);

		try {
			customThreadPool.submit(() -> assetDays.stream()
					.parallel()
					.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
					.distinct()
					.forEachOrdered(assetDay -> {

						final StopWatch timer = new StopWatch();

						timer.start();

						final List<Object> saveList = new ArrayList<Object>();

						final List<AssetDay> trimmedAssetDays = assetDays.stream()
								.filter(a -> a.getEpochSeconds() <= assetDay.getEpochSeconds())
								.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
								.collect(Collectors.toCollection(ArrayList::new));

						final long startingEpochSecondsMinute = assetDay.getEpochSeconds();
						final long endingEpochSecondsMinute = assetDay.getEpochSeconds() + (60 * 60 * 24);

						// only queries assetMinutes without a proper corresponding lbb value
						final List<AssetMinute> assetMinutes = algorithmCruncherDao.getAssetMinutesWithoutLbb(symbol,
								startingEpochSecondsMinute, endingEpochSecondsMinute, lbb.getEvaluationPeriod(),
								lbb.getEvaluationDuration(), lbb.getStandardDeviations());

						assetMinutes.stream()
								.filter(assetMinute -> assetDay.getEpochSeconds() == ZonedDateTime
										.ofInstant(Instant.ofEpochSecond(assetMinute.getEpochSeconds()),
												ZoneId.of("America/New_York"))
										.truncatedTo(ChronoUnit.DAYS).toEpochSecond())
								.distinct()
								.forEach(assetMinute -> {
									try {
										final LBB configuredLBB = lbb
												.configureLBB(trimmedAssetDays, assetMinute);
										saveList.add(configuredLBB);
									} catch (final InsufficientDataException e) {
										return;
									} catch (final Exception e) {
										e.printStackTrace();
									}
								});

						algorithmCruncherDao.saveList(saveList);

						timer.stop();

						System.out.println("Saved " + saveList.size() + " new entities");
						System.out
								.println("Loading LBB day took " + timer.getLastTaskTimeMillis()
										+ "milliseconds");

					})).get();

			customThreadPool.shutdown();

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void backloadUBBValues(final TechnicalIndicator technicalIndicator, final long startTime, final long endTime)
			throws ExpectedException {
		if (!technicalIndicator.getTechnicalIndicatorType().equals(TechnicalIndicator.UPPERBOLLINGERBAND)) {
			throw new ExpectedException("Technical Indicator Type must be Upper Bollinger Band");
		}

		final String evaluationPeriod = technicalIndicator.getEvaluationPeriod();

		final int ubbEvaluationDuration = technicalIndicator.getUbbEvaluationDuration();

		final BigDecimal standardDeviations = technicalIndicator.getStandardDeviations();

		final String symbol = technicalIndicator.getSymbol();

		final UBB ubb = new UBB();
		ubb.setEvaluationPeriod(evaluationPeriod);
		ubb.setEvaluationDuration(ubbEvaluationDuration);
		ubb.setStandardDeviations(standardDeviations);
		ubb.setSymbol(symbol);

		final long endingEpochSecondsDay = endTime;

		final long startingEpochSecondsDay = startTime - (60 * 60 * 24 * ubb.getEvaluationDuration());

		final List<AssetDay> assetDays = algorithmCruncherDao.getAssetDays(symbol,
				startingEpochSecondsDay, endingEpochSecondsDay);

		final ForkJoinPool customThreadPool = new ForkJoinPool(2);

		try {
			customThreadPool.submit(() -> assetDays.stream()
					.parallel()
					.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
					.distinct()
					.forEachOrdered(assetDay -> {

						final StopWatch timer = new StopWatch();

						timer.start();

						final List<Object> saveList = new ArrayList<Object>();

						final List<AssetDay> trimmedAssetDays = assetDays.stream()
								.filter(a -> a.getEpochSeconds() <= assetDay.getEpochSeconds())
								.sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
								.collect(Collectors.toCollection(ArrayList::new));

						final long startingEpochSecondsMinute = assetDay.getEpochSeconds();
						final long endingEpochSecondsMinute = assetDay.getEpochSeconds() + (60 * 60 * 24);

						// only queries assetMinutes without a proper corresponding lbb value
						final List<AssetMinute> assetMinutes = algorithmCruncherDao.getAssetMinutesWithoutUbb(symbol,
								startingEpochSecondsMinute, endingEpochSecondsMinute, ubb.getEvaluationPeriod(),
								ubb.getEvaluationDuration(), ubb.getStandardDeviations());

						assetMinutes.stream()
								.filter(assetMinute -> assetDay.getEpochSeconds() == ZonedDateTime
										.ofInstant(Instant.ofEpochSecond(assetMinute.getEpochSeconds()),
												ZoneId.of("America/New_York"))
										.truncatedTo(ChronoUnit.DAYS).toEpochSecond())
								.distinct()
								.forEach(assetMinute -> {
									try {
										final UBB configuredUBB = ubb
												.configureUBB(trimmedAssetDays, assetMinute);
										saveList.add(configuredUBB);
									} catch (final InsufficientDataException e) {
										return;
									} catch (final Exception e) {
										e.printStackTrace();
									}
								});

						algorithmCruncherDao.saveList(saveList);

						timer.stop();

						System.out.println("Saved " + saveList.size() + " new entities");
						System.out
								.println("Loading UBB day took " + timer.getLastTaskTimeMillis()
										+ "milliseconds");

					})).get();

			customThreadPool.shutdown();

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
