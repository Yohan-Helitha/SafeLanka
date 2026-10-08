package lk.dmc.disaster.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Arrays;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Against a mock server: no real Supabase call is ever made. */
class SupabaseFileStorageTest {

  private static final String ROOT = "https://abc.supabase.co/storage/v1";
  private static final String KEY = "sb_secret_test";
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
  private static final String PATH = "reports/" + UUID.randomUUID() + ".jpg";

  private MockRestServiceServer server;
  private SupabaseFileStorage storage;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    storage = new SupabaseFileStorage(builder, "https://abc.supabase.co/rest/v1/", KEY, "evidence");
  }

  // ---- store --------------------------------------------------------------------------------

  @Test
  void store_uploadsToThePrivateBucketWithTheSecretKey() {
    server
        .expect(requestTo(matchesPattern(ROOT + "/object/evidence/reports/[0-9a-f-]{36}\\.jpg")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer " + KEY))
        .andExpect(header("apikey", KEY))
        .andExpect(header("Content-Type", "image/jpeg"))
        .andExpect(content().bytes(JPEG))
        .andRespond(withSuccess("{\"Key\":\"evidence/x\"}", MediaType.APPLICATION_JSON));

    StoredFile stored = storage.store("reports", "image/jpeg", JPEG);

    assertThat(stored.path()).matches("reports/[0-9a-f-]{36}\\.jpg");
    assertThat(stored.contentType()).isEqualTo("image/jpeg");
    assertThat(stored.sizeBytes()).isEqualTo(JPEG.length);
    server.verify();
  }

  @Test
  void store_pngGetsPngExtension() {
    byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1};
    server
        .expect(requestTo(matchesPattern(ROOT + "/object/evidence/reports/[0-9a-f-]{36}\\.png")))
        .andRespond(withSuccess());

    assertThat(storage.store("reports", "image/png", png).path()).endsWith(".png");
  }

  @Test
  void store_invalidImageIsRejectedWithoutAnyRequest() {
    assertThatThrownBy(() -> storage.store("reports", "image/gif", JPEG))
        .isInstanceOf(AppException.class);
    assertThatThrownBy(() -> storage.store("reports", "image/jpeg", new byte[] {1, 2}))
        .isInstanceOf(AppException.class);
    assertThatThrownBy(
            () ->
                storage.store(
                    "reports", "image/jpeg", Arrays.copyOf(JPEG, ImageRules.MAX_BYTES + 1)))
        .hasMessageContaining("5 MB");
    server.verify();
  }

  @Test
  void store_serverErrorBecomesAnUnavailableError() {
    server
        .expect(requestTo(matchesPattern(ROOT + "/object/evidence/.*")))
        .andRespond(withServerError());

    assertThatThrownBy(() -> storage.store("reports", "image/jpeg", JPEG))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INTERNAL_ERROR))
        .hasMessageContaining("unavailable");
  }

  // ---- load ---------------------------------------------------------------------------------

  @Test
  void load_downloadsThroughTheAuthenticatedRoute() {
    server
        .expect(requestTo(ROOT + "/object/authenticated/evidence/" + PATH))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + KEY))
        .andRespond(withSuccess(JPEG, MediaType.IMAGE_JPEG));

    assertThat(storage.load(PATH)).isEqualTo(JPEG);
    server.verify();
  }

  @Test
  void load_missingObjectIsNotFound() {
    server
        .expect(requestTo(ROOT + "/object/authenticated/evidence/" + PATH))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThatThrownBy(() -> storage.load(PATH)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void load_supabaseReportsAMissingObjectAsBadRequestToo() {
    server
        .expect(requestTo(ROOT + "/object/authenticated/evidence/" + PATH))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    assertThatThrownBy(() -> storage.load(PATH)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void load_serverErrorIsUnavailableNotNotFound() {
    server
        .expect(requestTo(ROOT + "/object/authenticated/evidence/" + PATH))
        .andRespond(withServerError());

    assertThatThrownBy(() -> storage.load(PATH))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INTERNAL_ERROR));
  }

  @Test
  void load_pathsThatWereNeverStoredAreNotFoundWithoutAnyRequest() {
    assertThatThrownBy(() -> storage.load("../secrets/key.jpg"))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> storage.load("reports/not-a-uuid.jpg"))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> storage.load("other/" + UUID.randomUUID() + ".gif"))
        .isInstanceOf(NotFoundException.class);
    server.verify();
  }

  // ---- delete -------------------------------------------------------------------------------

  @Test
  void delete_removesTheObject() {
    server
        .expect(requestTo(ROOT + "/object/evidence/" + PATH))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withSuccess());

    storage.delete(PATH);

    server.verify();
  }

  @Test
  void delete_failuresAreSwallowed() {
    server.expect(requestTo(ROOT + "/object/evidence/" + PATH)).andRespond(withServerError());

    assertThatCode(() -> storage.delete(PATH)).doesNotThrowAnyException();
  }

  @Test
  void delete_aPathThatWasNeverStoredIsIgnoredWithoutAnyRequest() {
    storage.delete("../../etc/passwd");

    server.verify();
  }

  // ---- configuration ------------------------------------------------------------------------

  @Test
  void storageApiUrl_dropsTheRestSuffixAndTrailingSlashes() {
    assertThat(SupabaseFileStorage.storageApiUrl("https://abc.supabase.co/rest/v1/"))
        .isEqualTo(ROOT);
    assertThat(SupabaseFileStorage.storageApiUrl("https://abc.supabase.co/")).isEqualTo(ROOT);
    assertThat(SupabaseFileStorage.storageApiUrl(" https://abc.supabase.co ")).isEqualTo(ROOT);
  }

  @Test
  void construction_withoutUrlOrKeyFailsWithAClearMessage() {
    assertThatThrownBy(() -> new SupabaseFileStorage(RestClient.builder(), "", KEY, "evidence"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SUPABASE_URL");
    assertThatThrownBy(
            () ->
                new SupabaseFileStorage(
                    RestClient.builder(), "https://abc.supabase.co", " ", "evidence"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SUPABASE_SERVICE_KEY");
    assertThatThrownBy(() -> new SupabaseFileStorage(RestClient.builder(), null, null, "evidence"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void construction_withAnUnsafeBucketNameIsRejected() {
    assertThatThrownBy(
            () ->
                new SupabaseFileStorage(
                    RestClient.builder(), "https://abc.supabase.co", KEY, "a/b"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
