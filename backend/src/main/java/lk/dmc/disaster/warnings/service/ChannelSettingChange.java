package lk.dmc.disaster.warnings.service;

import java.util.Map;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;

/**
 * A partial update of a channel setting. A null flag means "leave as it is", and at least one flag
 * must be given.
 */
public record ChannelSettingChange(Boolean enabled, Boolean simulateFailure) {

  public ChannelSettingChange {
    if (enabled == null && simulateFailure == null) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "Give enabled, simulateFailure, or both.",
          Map.of("field", "enabled"));
    }
  }
}
