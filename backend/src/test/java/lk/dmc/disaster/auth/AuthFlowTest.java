package lk.dmc.disaster.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.ThreadLocalRandom;
import lk.dmc.disaster.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** End to end through the real security chain, the services and the database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthFlowTest {

  private static final String COLOMBO = "00000000-0000-0000-0001-000000000001";
  private static final String PASSWORD = "Sunrise2026";
  private static final String REFRESH_COOKIE = "refresh_token";

  @Autowired MockMvc mvc;

  // ---- sign-up and phone verification ----

  @Test
  void citizenSignsUpVerifiesByCodeAndIsSignedIn() throws Exception {
    Account account = signUp();

    // wrong code first: counted, with the tries left
    MvcResult wrong = verify(account.phone, "000000".equals(account.devCode) ? "111111" : "000000");
    assertThat(wrong.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(wrong, "$.error.code")).isEqualTo("CODE_INVALID");
    assertThat(json(wrong, "$.error.details.attemptsLeft")).isEqualTo(2);

    MvcResult ok = verify(account.phone, account.devCode);
    assertThat(ok.getResponse().getStatus()).isEqualTo(200);
    assertThat(json(ok, "$.data.user.role")).isEqualTo("CITIZEN");
    assertThat(ok.getResponse().getHeader("Set-Cookie"))
        .contains("HttpOnly")
        .contains("SameSite=Strict")
        .contains("Path=/api/auth");
    assertThat(ok.getResponse().getContentAsString()).doesNotContain("refresh");

    String token = text(ok, "$.data.accessToken");
    MvcResult me = mvc.perform(bearer(get("/api/auth/me"), token)).andReturn();
    assertThat(me.getResponse().getStatus()).isEqualTo(200);
    assertThat(json(me, "$.data.fullName")).isEqualTo("Test Citizen");
  }

  @Test
  void codeIsDeadAfterThreeWrongTries() throws Exception {
    Account account = signUp();
    String wrong = "000000".equals(account.devCode) ? "111111" : "000000";
    verify(account.phone, wrong);
    verify(account.phone, wrong);
    MvcResult third = verify(account.phone, wrong);
    assertThat(json(third, "$.error.details.attemptsLeft")).isEqualTo(0);

    // even the right code is refused now: the person must request a new one
    MvcResult correct = verify(account.phone, account.devCode);
    assertThat(correct.getResponse().getStatus()).isEqualTo(410);
    assertThat(json(correct, "$.error.code")).isEqualTo("CODE_EXPIRED");
  }

  @Test
  void duplicatePhoneIsAConflictNamingTheField() throws Exception {
    Account first = signUp();
    MvcResult second = postJson("/api/auth/signup", signupBody(first.phone, randomNic(), null));
    assertThat(second.getResponse().getStatus()).isEqualTo(409);
    assertThat(json(second, "$.error.details.field")).isEqualTo("phone");
  }

  @Test
  void invalidSignupListsEveryProblemByField() throws Exception {
    MvcResult result =
        postJson(
            "/api/auth/signup",
            "{\"fullName\":\"A\",\"phone\":\"123\",\"nic\":\"x\",\"homeAddress\":\"no\","
                + "\"districtId\":\""
                + COLOMBO
                + "\",\"preferredLanguage\":\"en\",\"password\":\"weak\"}");
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(result, "$.error.code")).isEqualTo("VALIDATION_ERROR");
    assertThat(result.getResponse().getContentAsString())
        .contains("fullName", "phone", "nic", "homeAddress", "password");
  }

  @Test
  void unverifiedCitizenLoggingInIsSentANewCode() throws Exception {
    Account account = signUpOnly();
    MvcResult login = login("PHONE", account.phone, PASSWORD);
    assertThat(login.getResponse().getStatus()).isEqualTo(403);
    assertThat(json(login, "$.error.code")).isEqualTo("PHONE_NOT_VERIFIED");
    assertThat(json(login, "$.error.details.phone")).isEqualTo(account.e164);
  }

  // ---- login ----

  @Test
  void seededStaffLogInWithEmailAndTheDemoPassword() throws Exception {
    MvcResult dmc = login("EMAIL", "Nimal.Perera@DMC.lk", "Demo@1234");
    assertThat(dmc.getResponse().getStatus()).isEqualTo(200);
    assertThat(json(dmc, "$.data.user.role")).isEqualTo("DMC_OFFICER");
    assertThat(json(dmc, "$.data.expiresIn")).isEqualTo(900);

    MvcResult rescue = login("PHONE", "077 100 0008", "Demo@1234");
    assertThat(json(rescue, "$.data.user.role")).isEqualTo("RESCUE_MEMBER");
    assertThat(json(rescue, "$.data.user.rescueTeamId")).isNotNull();
  }

  @Test
  void wrongPasswordAndUnknownAccountGetTheSameAnswer() throws Exception {
    MvcResult wrongPassword = login("EMAIL", "nimal.perera@dmc.lk", "Wrong-password1");
    MvcResult unknown = login("EMAIL", "nobody@dmc.lk", "Wrong-password1");
    assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
    assertThat(unknown.getResponse().getStatus()).isEqualTo(401);
    assertThat(wrongPassword.getResponse().getContentAsString())
        .isEqualTo(unknown.getResponse().getContentAsString());
  }

  @Test
  void fiveWrongPasswordsLockTheAccountEvenAgainstTheRightPassword() throws Exception {
    Account account = signUp();
    verify(account.phone, account.devCode);

    for (int i = 0; i < 4; i++) {
      assertThat(login("PHONE", account.phone, "Wrong-password1").getResponse().getStatus())
          .isEqualTo(401);
    }
    MvcResult fifth = login("PHONE", account.phone, "Wrong-password1");
    assertThat(fifth.getResponse().getStatus()).isEqualTo(423);
    assertThat(json(fifth, "$.error.code")).isEqualTo("ACCOUNT_LOCKED");

    MvcResult rightPassword = login("PHONE", account.phone, PASSWORD);
    assertThat(rightPassword.getResponse().getStatus()).isEqualTo(423);
  }

  // ---- refresh and logout ----

  @Test
  void refreshRotatesTheTokenAndReuseRevokesEverything() throws Exception {
    String first = refreshCookieOf(login("EMAIL", "kasun.jayawardena@dmc.lk", "Demo@1234"));

    MvcResult rotated = refresh(first);
    assertThat(rotated.getResponse().getStatus()).isEqualTo(200);
    String second = refreshCookieOf(rotated);
    assertThat(second).isNotEqualTo(first);

    // the first token was already used: this looks like theft, so all tokens die
    assertThat(refresh(first).getResponse().getStatus()).isEqualTo(401);
    assertThat(refresh(second).getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void refreshWithoutACookieIsUnauthenticated() throws Exception {
    MvcResult result = mvc.perform(post("/api/auth/refresh")).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(401);
    assertThat(json(result, "$.error.code")).isEqualTo("UNAUTHENTICATED");
  }

  @Test
  void logoutRevokesTheRefreshTokenAndClearsTheCookie() throws Exception {
    MvcResult login = login("EMAIL", "sanduni.wickramasinghe@dmc.lk", "Demo@1234");
    String token = text(login, "$.data.accessToken");
    String cookie = refreshCookieOf(login);

    MvcResult logout =
        mvc.perform(
                bearer(post("/api/auth/logout"), token).cookie(new Cookie(REFRESH_COOKIE, cookie)))
            .andReturn();
    assertThat(logout.getResponse().getStatus()).isEqualTo(204);
    assertThat(logout.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");
    assertThat(refresh(cookie).getResponse().getStatus()).isEqualTo(401);
  }

  // ---- access matrix ----

  @Test
  void urlRulesGateByRoleBeforeAnyController() throws Exception {
    String citizen = text(login("PHONE", "0771000004", "Demo@1234"), "$.data.accessToken");
    String dmc = text(login("EMAIL", "nimal.perera@dmc.lk", "Demo@1234"), "$.data.accessToken");

    // no controller exists for these paths yet: 404 means "allowed through", 401/403 means
    // "blocked"
    assertThat(status(get("/api/hazards"))).isEqualTo(401);
    assertThat(status(bearer(get("/api/hazards"), citizen))).isEqualTo(403);
    assertThat(status(bearer(get("/api/hazards"), dmc))).isEqualTo(404);
    assertThat(status(bearer(get("/api/reports"), citizen))).isEqualTo(404);
    assertThat(status(bearer(get("/api/warnings/active/mine"), citizen))).isEqualTo(404);
    assertThat(status(bearer(get("/api/warnings/active/mine"), dmc))).isEqualTo(403);
    assertThat(status(bearer(get("/api/assignments"), citizen))).isEqualTo(403);
    assertThat(status(get("/api/reference/hazard-types"))).isEqualTo(404);
  }

  @Test
  void districtsArePublicForTheSignupForm() throws Exception {
    MvcResult result = mvc.perform(get("/api/reference/districts")).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(json(result, "$.data.length()")).isEqualTo(5);
  }

  @Test
  void tamperedTokenIsRejected() throws Exception {
    String token = text(login("EMAIL", "nimal.perera@dmc.lk", "Demo@1234"), "$.data.accessToken");
    String forged = token.substring(0, token.length() - 4) + "AAAA";
    MvcResult result = mvc.perform(bearer(get("/api/auth/me"), forged)).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(401);
    assertThat(json(result, "$.error.code")).isEqualTo("UNAUTHENTICATED");
  }

  // ---- helpers ----

  private record Account(String phone, String e164, String devCode) {}

  private Account signUp() throws Exception {
    Account account = signUpOnly();
    return account;
  }

  private Account signUpOnly() throws Exception {
    String phone = randomPhone();
    MvcResult result = postJson("/api/auth/signup", signupBody(phone, randomNic(), null));
    assertThat(result.getResponse().getStatus())
        .as(result.getResponse().getContentAsString())
        .isEqualTo(201);
    return new Account(phone, "+94" + phone.substring(1), text(result, "$.data.devCode"));
  }

  private String signupBody(String phone, String nic, String email) {
    return "{\"fullName\":\"Test Citizen\",\"phone\":\""
        + phone
        + "\","
        + (email == null ? "" : "\"email\":\"" + email + "\",")
        + "\"nic\":\""
        + nic
        + "\",\"homeAddress\":\"12 Temple Road, Kolonnawa\",\"districtId\":\""
        + COLOMBO
        + "\",\"preferredLanguage\":\"si\",\"password\":\""
        + PASSWORD
        + "\"}";
  }

  private MvcResult verify(String phone, String code) throws Exception {
    return postJson(
        "/api/auth/verify-phone", "{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}");
  }

  private MvcResult login(String type, String identifier, String password) throws Exception {
    return postJson(
        "/api/auth/login",
        "{\"identifierType\":\""
            + type
            + "\",\"identifier\":\""
            + identifier
            + "\",\"password\":\""
            + password
            + "\"}");
  }

  private MvcResult refresh(String cookie) throws Exception {
    return mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, cookie)))
        .andReturn();
  }

  private MvcResult postJson(String url, String body) throws Exception {
    return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
  }

  private int status(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request).andReturn().getResponse().getStatus();
  }

  private static MockHttpServletRequestBuilder bearer(
      MockHttpServletRequestBuilder r, String token) {
    return r.header("Authorization", "Bearer " + token);
  }

  private static String refreshCookieOf(MvcResult result) {
    String header = result.getResponse().getHeader("Set-Cookie");
    assertThat(header).as("Set-Cookie header").startsWith(REFRESH_COOKIE + "=");
    return header.substring((REFRESH_COOKIE + "=").length(), header.indexOf(';'));
  }

  private static Object json(MvcResult result, String path) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), path);
  }

  private static String text(MvcResult result, String path) throws Exception {
    return (String) json(result, path);
  }

  private static String randomPhone() {
    return "07" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
  }

  private static String randomNic() {
    return String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L));
  }
}
