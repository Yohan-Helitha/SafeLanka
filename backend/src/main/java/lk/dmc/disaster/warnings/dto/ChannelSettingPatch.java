package lk.dmc.disaster.warnings.dto;

import lk.dmc.disaster.warnings.service.ChannelSettingChange;

/** Body of a request to change a channel. A missing flag is left as it is. */
public record ChannelSettingPatch(Boolean enabled, Boolean simulateFailure) {

  public ChannelSettingChange toChange() {
    return new ChannelSettingChange(enabled, simulateFailure);
  }
}
