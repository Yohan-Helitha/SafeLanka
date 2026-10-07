package lk.dmc.disaster.warnings.service;

import java.util.Comparator;
import java.util.List;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.ChannelSetting;
import lk.dmc.disaster.warnings.integration.GatewayFailureSwitch;
import lk.dmc.disaster.warnings.repository.ChannelSettingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and changes the demo switches: which channels are on and which should fail. */
@Slf4j
@Service
public class ChannelSettingsService implements GatewayFailureSwitch {

  private final ChannelSettingRepository settings;

  public ChannelSettingsService(ChannelSettingRepository settings) {
    this.settings = settings;
  }

  /** All channel settings, in channel order. */
  @Transactional(readOnly = true)
  public List<ChannelSetting> findAll() {
    return settings.findAll().stream()
        .sorted(Comparator.comparing(ChannelSetting::getChannel))
        .toList();
  }

  /**
   * True when the channel is switched on.
   *
   * @throws AppException NOT_FOUND when the channel has no setting row
   */
  @Transactional(readOnly = true)
  public boolean isEnabled(Channel channel) {
    return find(channel).isEnabled();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isFailureSimulated(Channel channel) {
    return find(channel).isSimulateFailure();
  }

  /**
   * Applies the flags that were given and leaves the others as they are.
   *
   * @throws AppException NOT_FOUND when the channel has no setting row
   */
  @Transactional
  public ChannelSetting change(Channel channel, ChannelSettingChange change) {
    ChannelSetting setting = find(channel);
    if (change.enabled() != null) {
      setting.switchEnabled(change.enabled());
    }
    if (change.simulateFailure() != null) {
      setting.switchSimulatedFailure(change.simulateFailure());
    }
    log.info(
        "Channel {} now enabled={}, simulateFailure={}",
        channel,
        setting.isEnabled(),
        setting.isSimulateFailure());
    return setting;
  }

  private ChannelSetting find(Channel channel) {
    return settings
        .findById(channel)
        .orElseThrow(
            () -> new AppException(ErrorCode.NOT_FOUND, "No setting for channel " + channel + "."));
  }
}
