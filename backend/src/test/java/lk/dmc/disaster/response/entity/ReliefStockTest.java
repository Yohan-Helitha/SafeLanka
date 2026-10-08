package lk.dmc.disaster.response.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReliefStockTest {

  @Test
  void create_initializesWithGivenQuantityAndDefaults() {
    UUID itemId = UUID.randomUUID();
    UUID orgId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();

    ReliefStock stock = ReliefStock.create(itemId, orgId, districtId, 500);

    assertThat(stock.getId()).isNotNull();
    assertThat(stock.getItemId()).isEqualTo(itemId);
    assertThat(stock.getOrganisationId()).isEqualTo(orgId);
    assertThat(stock.getDistrictId()).isEqualTo(districtId);
    assertThat(stock.getQuantityAvailable()).isEqualTo(500);
    assertThat(stock.getVersion()).isEqualTo(0L);
  }

  @Test
  void decreaseAndIncrease_adjustQuantityCorrectly() {
    ReliefStock stock =
        ReliefStock.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 100);

    stock.decrease(30);
    assertThat(stock.getQuantityAvailable()).isEqualTo(70);

    stock.increase(50);
    assertThat(stock.getQuantityAvailable()).isEqualTo(120);
  }

  @Test
  void decrease_throwsWhenQuantityExceedsAvailable() {
    ReliefStock stock =
        ReliefStock.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 40);

    assertThatThrownBy(() -> stock.decrease(41))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Insufficient stock");
  }
}
