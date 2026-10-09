package lk.dmc.disaster.response.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.entity.ActivityLog;
import lk.dmc.disaster.response.entity.ActivityType;
import lk.dmc.disaster.response.entity.ReliefStock;
import lk.dmc.disaster.response.repository.ActivityLogRepository;
import lk.dmc.disaster.response.repository.ReliefStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service to replenish relief stocks automatically each day.
 * Increases stock quantities by 100 daily and records system activity logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReliefStockReplenishmentService {

  private static final int DAILY_INCREMENT_AMOUNT = 100;

  private final ReliefStockRepository reliefStockRepository;
  private final ActivityLogRepository activityLogRepository;

  /**
   * Runs daily at midnight (00:00:00).
   * Also can be invoked manually or upon replenishment triggers.
   */
  @Scheduled(cron = "${app.relief.daily-replenishment-cron:0 0 0 * * *}")
  @Transactional
  public int replenishStocksDaily() {
    log.info("Starting automatic daily relief stock replenishment (+{} units per item)", DAILY_INCREMENT_AMOUNT);

    List<ReliefStock> allStocks = reliefStockRepository.findAll();
    if (allStocks.isEmpty()) {
      log.info("No relief stock lines found to replenish.");
      return 0;
    }

    int updatedCount = 0;
    Instant now = Instant.now();

    for (ReliefStock stock : allStocks) {
      stock.increase(DAILY_INCREMENT_AMOUNT);
      updatedCount++;
    }

    reliefStockRepository.saveAll(allStocks);

    // Record activity logs per affected district
    allStocks.stream()
        .map(ReliefStock::getDistrictId)
        .distinct()
        .forEach(
            districtId -> {
              ActivityLog logEntry =
                  ActivityLog.create(
                      districtId,
                      null,
                      ActivityType.RELIEF,
                      "Daily stock replenishment: relief stocks increased by " + DAILY_INCREMENT_AMOUNT + " units",
                      now);
              activityLogRepository.save(logEntry);
            });

    log.info("Completed automatic daily relief stock replenishment for {} stock lines across districts.", updatedCount);
    return updatedCount;
  }
}

