package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.ChannelSetting;
import lk.dmc.disaster.warnings.repository.ChannelSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChannelSettingsServiceTest {

  @Mock private ChannelSettingRepository repository;

  private ChannelSettingsService service;

  @BeforeEach
  void setUp() {
    service = new ChannelSettingsService(repository);
  }

  private static ChannelSetting setting(Channel channel, boolean enabled, boolean failing) {
    ChannelSetting setting = BeanUtils.instantiateClass(ChannelSetting.class);
    ReflectionTestUtils.setField(setting, "channel", channel);
    setting.switchEnabled(enabled);
    setting.switchSimulatedFailure(failing);
    return setting;
  }

  @Test
  void findAll_returnsSettingsInChannelOrder() {
    when(repository.findAll())
        .thenReturn(
            List.of(
                setting(Channel.AUDIBLE, true, false),
                setting(Channel.PUSH, true, false),
                setting(Channel.SMS, true, false)));

    assertThat(service.findAll())
        .extracting(ChannelSetting::getChannel)
        .containsExactly(Channel.PUSH, Channel.SMS, Channel.AUDIBLE);
  }

  @Test
  void isEnabled_andIsFailureSimulated_readTheStoredFlags() {
    when(repository.findById(Channel.SMS))
        .thenReturn(Optional.of(setting(Channel.SMS, false, true)));

    assertThat(service.isEnabled(Channel.SMS)).isFalse();
    assertThat(service.isFailureSimulated(Channel.SMS)).isTrue();
  }

  @Test
  void setEnabled_changesOnlyTheEnabledFlag() {
    ChannelSetting sms = setting(Channel.SMS, true, false);
    when(repository.findById(Channel.SMS)).thenReturn(Optional.of(sms));

    ChannelSetting result = service.setEnabled(Channel.SMS, false);

    assertThat(result.isEnabled()).isFalse();
    assertThat(result.isSimulateFailure()).isFalse();
  }

  @Test
  void setSimulatedFailure_changesOnlyTheFailureFlag() {
    ChannelSetting sms = setting(Channel.SMS, true, false);
    when(repository.findById(Channel.SMS)).thenReturn(Optional.of(sms));

    ChannelSetting result = service.setSimulatedFailure(Channel.SMS, true);

    assertThat(result.isSimulateFailure()).isTrue();
    assertThat(result.isEnabled()).isTrue();
  }

  @Test
  void unknownChannel_isNotFound() {
    when(repository.findById(Channel.PUSH)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.setEnabled(Channel.PUSH, true))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
    assertThatThrownBy(() -> service.isEnabled(Channel.PUSH)).isInstanceOf(AppException.class);
  }
}
