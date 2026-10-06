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
   * Switches a channel on or off.
   *
   * @throws AppException NOT_FOUND when the channel has no setting row
   */
  @Transactional
  public ChannelSetting setEnabled(Channel channel, boolean on) {
    ChannelSetting setting = find(channel);
    setting.switchEnabled(on);
    log.info("Channel {} enabled={}", channel, on);
    return setting;
  }

  /**
   * Makes a channel's gateway fail, or work again.
   *
   * @throws AppException NOT_FOUND when the channel has no setting row
   */
  @Transactional
  public ChannelSetting setSimulatedFailure(Channel channel, boolean on) {
    ChannelSetting setting = find(channel);
    setting.switchSimulatedFailure(on);
    log.info("Channel {} simulateFailure={}", channel, on);
    return setting;
  }

  private ChannelSetting find(Channel channel) {
    return settings
        .findById(channel)
        .orElseThrow(
            () -> new AppException(ErrorCode.NOT_FOUND, "No setting for channel " + channel + "."));
  }
}
