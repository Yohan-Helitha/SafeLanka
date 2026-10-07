package lk.dmc.disaster.shared.storage;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.error.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link FileStorage} in a private Supabase Storage bucket, shared by everyone on the same project.
 * Uses the secret key, so the bucket needs no public access and no policies. Selected with {@code
 * STORAGE_PROVIDER=supabase}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "supabase")
class SupabaseFileStorage implements FileStorage {

  private static final Pattern BUCKET = Pattern.compile("[A-Za-z0-9_-]{1,63}");
  private static final Pattern STORED_PATH =
      Pattern.compile("[a-z0-9-]{1,30}/[0-9a-f-]{36}\\.(jpg|png)");

  private final RestClient client;
  private final String bucket;

  @Autowired
  SupabaseFileStorage(
      @Value("${app.storage.supabase.url:}") String url,
      @Value("${app.storage.supabase.service-key:}") String serviceKey,
      @Value("${app.storage.supabase.bucket:evidence}") String bucket,
      RestClient.Builder builder) {
    this(builder.requestFactory(withTimeouts()), url, serviceKey, bucket);
  }

  /** Lets tests supply a builder wired to a mock server. */
  SupabaseFileStorage(RestClient.Builder builder, String url, String serviceKey, String bucket) {
    if (url == null || url.isBlank() || serviceKey == null || serviceKey.isBlank()) {
      throw new IllegalStateException(
          "STORAGE_PROVIDER=supabase needs SUPABASE_URL and SUPABASE_SERVICE_KEY in backend/.env");
    }
    if (!BUCKET.matcher(bucket).matches()) {
      throw new IllegalArgumentException("Invalid storage bucket name: " + bucket);
    }
    this.bucket = bucket;
    this.client =
        builder
            .baseUrl(storageApiUrl(url))
            .defaultHeader("Authorization", "Bearer " + serviceKey)
            .defaultHeader("apikey", serviceKey)
            .build();
  }

  /**
   * The storage API root for a project URL. The project URL is often copied as {@code
   * https://ref.supabase.co/rest/v1/}; only the host part matters.
   */
  static String storageApiUrl(String projectUrl) {
    String root = projectUrl.trim().replaceAll("/+$", "").replaceAll("/rest/v1$", "");
    return root + "/storage/v1";
  }

  @Override
  public StoredFile store(String folder, String contentType, byte[] content) {
    ImageRules.validate(folder, contentType, content);
    String path = folder + "/" + UUID.randomUUID() + "." + ImageRules.extensionOf(contentType);
    try {
      client
          .post()
          .uri(objectUri(path))
          .contentType(MediaType.parseMediaType(contentType))
          .body(content)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException e) {
      throw unavailable("upload", e);
    }
    return new StoredFile(path, contentType, content.length);
  }

  @Override
  public byte[] load(String path) {
    if (!STORED_PATH.matcher(path).matches()) {
      throw new NotFoundException("File not found.");
    }
    try {
      byte[] bytes =
          client.get().uri("/object/authenticated/" + bucket + "/" + path).retrieve().body(byte[].class);
      if (bytes == null) {
        throw new NotFoundException("File not found.");
      }
      return bytes;
    } catch (RestClientResponseException e) {
      HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
      if (status == HttpStatus.NOT_FOUND || status == HttpStatus.BAD_REQUEST) {
        throw new NotFoundException("File not found.");
      }
      throw unavailable("download", e);
    } catch (RestClientException e) {
      throw unavailable("download", e);
    }
  }

  @Override
  public void delete(String path) {
    if (!STORED_PATH.matcher(path).matches()) {
      return;
    }
    try {
      client.delete().uri(objectUri(path)).retrieve().toBodilessEntity();
    } catch (RestClientException e) {
      log.warn("Could not delete {} from the {} bucket: {}", path, bucket, e.getMessage());
    }
  }

  private String objectUri(String path) {
    return "/object/" + bucket + "/" + path;
  }

  private static AppException unavailable(String action, RestClientException cause) {
    log.warn("Supabase storage {} failed: {}", action, cause.getMessage());
    return new AppException(
        ErrorCode.INTERNAL_ERROR, "Photo storage is unavailable. Try again in a moment.");
  }

  private static JdkClientHttpRequestFactory withTimeouts() {
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    factory.setReadTimeout(Duration.ofSeconds(15));
    return factory;
  }
}
