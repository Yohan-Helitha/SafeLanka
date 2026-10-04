package lk.dmc.disaster.auth;

import static org.assertj.core.api.Assertions.assertThat;

import lk.dmc.disaster.auth.domain.Nic;
import lk.dmc.disaster.auth.domain.PasswordPolicy;
import lk.dmc.disaster.auth.domain.PhoneNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdentityRulesTest {

  @ParameterizedTest
  @ValueSource(
      strings = {"0771234567", "771234567", "+94771234567", "077 123 4567", "077-123-4567"})
  void acceptsEveryWrittenFormOfAMobileNumberAndStoresE164(String input) {
    assertThat(PhoneNumber.parse(input)).map(PhoneNumber::e164).contains("+94771234567");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0112345678", "077123456", "+9477123456789", "abc", "", "0671234567"})
  void rejectsAnythingThatIsNotASriLankanMobile(String input) {
    assertThat(PhoneNumber.parse(input)).isEmpty();
  }

  @Test
  void rejectsNull() {
    assertThat(PhoneNumber.parse(null)).isEmpty();
  }

  @Test
  void masksTheMiddleOfTheNumber() {
    assertThat(PhoneNumber.parse("0771234567").orElseThrow().masked()).isEqualTo("+94 77 *** 4567");
  }

  @ParameterizedTest
  @ValueSource(strings = {"199012345V", "199012345v", "199012345X", "199012345678"})
  void acceptsOldAndNewNic(String nic) {
    assertThat(nic.matches(Nic.PATTERN)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"19901234V", "1990123456789", "abcdefghiV", ""})
  void rejectsMalformedNic(String nic) {
    assertThat(nic.matches(Nic.PATTERN)).isFalse();
  }

  @Test
  void upperCasesNic() {
    assertThat(Nic.normalise(" 199012345v ")).isEqualTo("199012345V");
  }

  @ParameterizedTest
  @ValueSource(strings = {"Demo@1234", "abcdefg1", "12345678a"})
  void acceptsPasswordsWithALetterAndANumber(String password) {
    assertThat(PasswordPolicy.isValid(password)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"short1", "onlyletters", "12345678", ""})
  void rejectsWeakPasswords(String password) {
    assertThat(PasswordPolicy.isValid(password)).isFalse();
  }

  @Test
  void rejectsOverlongPassword() {
    assertThat(PasswordPolicy.isValid("a1".repeat(33))).isFalse();
  }
}
