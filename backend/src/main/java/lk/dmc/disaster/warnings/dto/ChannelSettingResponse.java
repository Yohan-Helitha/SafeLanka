package lk.dmc.disaster.warnings.dto;

import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.ChannelSetting;

/** A channel and its two demo switches. */
public record ChannelSettingResponse(Channel channel, boolean enabled, boolean simulateFailure) {

  public static ChannelSettingResponse from(ChannelSetting setting) {
    return new ChannelSettingResponse(
        setting.getChannel(), setting.isEnabled(), setting.isSimulateFailure());
  }
}
