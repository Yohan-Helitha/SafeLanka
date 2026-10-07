package lk.dmc.disaster.shared.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lk.dmc.disaster.shared.error.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GeoPointTest {

  @Test
  void of_colomboIsAccepted() {
    GeoPoint p = GeoPoint.of(6.9391, 79.8921);

    assertThat(p.latitude()).isEqualTo(6.9391);
    assertThat(p.longitude()).isEqualTo(79.8921);
  }

  @ParameterizedTest
  @CsvSource({"5.8,79.5", "9.9,82.0", "5.8,82.0", "9.9,79.5"})
  void of_boundaryCornersAreInside(double lat, double lng) {
    assertThat(GeoPoint.of(lat, lng)).isEqualTo(new GeoPoint(lat, lng));
  }

  @ParameterizedTest
  @CsvSource({"5.79,80", "9.91,80", "7,79.49", "7,82.01", "51.5,-0.12"})
  void of_outsideSriLankaIsRejected(double lat, double lng) {
    assertThatThrownBy(() -> GeoPoint.of(lat, lng)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void of_nanIsRejected() {
    assertThatThrownBy(() -> GeoPoint.of(Double.NaN, 80)).isInstanceOf(BusinessRuleException.class);
  }
}
