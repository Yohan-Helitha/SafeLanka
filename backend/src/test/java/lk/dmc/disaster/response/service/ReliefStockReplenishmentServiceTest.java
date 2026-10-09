package lk.dmc.disaster.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.entity.ReliefStock;
import lk.dmc.disaster.response.repository.ActivityLogRepository;
import lk.dmc.disaster.response.repository.ReliefStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReliefStockReplenishmentServiceTest {

  @Mock private ReliefStockRepository reliefStockRepository;
  @Mock private ActivityLogRepository activityLogRepository;

  private ReliefStockReplenishmentService service;

  @BeforeEach
  void setUp() {
    service = new ReliefStockReplenishmentService(reliefStockRepository, activityLogRepository);
  }

  @Test
  @DisplayName("replenishStocksDaily increases every stock line by 100 and logs activity")
  void replenishStocksDaily_success() {
    UUID districtColombo = UUID.randomUUID();
    UUID districtGampaha = UUID.randomUUID();

    ReliefStock stock1 = ReliefStock.create(UUID.randomUUID(), UUID.randomUUID(), districtColombo, 500);
    ReliefStock stock2 = ReliefStock.create(UUID.randomUUID(), UUID.randomUUID(), districtColombo, 300);
    ReliefStock stock3 = ReliefStock.create(UUID.randomUUID(), UUID.randomUUID(), districtGampaha, 200);

    when(reliefStockRepository.findAll()).thenReturn(List.of(stock1, stock2, stock3));

    int count = service.replenishStocksDaily();

    assertThat(count).isEqualTo(3);
    assertThat(stock1.getQuantityAvailable()).isEqualTo(600);
    assertThat(stock2.getQuantityAvailable()).isEqualTo(400);
    assertThat(stock3.getQuantityAvailable()).isEqualTo(300);

    verify(reliefStockRepository).saveAll(any());
    // 2 distinct districts -> 2 activity log entries
    verify(activityLogRepository, times(2)).save(any());
  }

  @Test
  @DisplayName("replenishStocksDaily returns 0 if no stock rows exist")
  void replenishStocksDaily_emptyList() {
    when(reliefStockRepository.findAll()).thenReturn(List.of());

    int count = service.replenishStocksDaily();

    assertThat(count).isEqualTo(0);
    verify(reliefStockRepository, times(0)).saveAll(any());
    verify(activityLogRepository, times(0)).save(any());
  }
}

