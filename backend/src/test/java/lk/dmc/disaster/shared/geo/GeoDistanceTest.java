package lk.dmc.disaster.shared.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoDistanceTest {

  private static final GeoPoint COLOMBO = GeoPoint.of(6.9271, 79.8612);
  private static final GeoPoint KANDY = GeoPoint.of(7.2906, 80.6337);

  @Test
  void metresBetween_samePointIsZero() {
    assertThat(GeoDistance.metresBetween(COLOMBO, COLOMBO)).isZero();
  }

  @Test
  void metresBetween_colomboToKandyIsAboutNinetyFourKm() {
    assertThat(GeoDistance.metresBetween(COLOMBO, KANDY)).isCloseTo(94_000, within(3_000.0));
  }

  @Test
  void metresBetween_isSymmetric() {
    assertThat(GeoDistance.metresBetween(COLOMBO, KANDY))
        .isEqualTo(GeoDistance.metresBetween(KANDY, COLOMBO));
  }

  @Test
  void metresBetween_oneThousandthOfADegreeLatitudeIsAbout111Metres() {
    GeoPoint north = GeoPoint.of(6.9281, 79.8612);

    assertThat(GeoDistance.metresBetween(COLOMBO, north)).isCloseTo(111.2, within(0.5));
  }
}
