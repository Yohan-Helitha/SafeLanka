package lk.dmc.disaster.warnings.repository;

import lk.dmc.disaster.warnings.entity.Channel;
import lk.dmc.disaster.warnings.entity.ChannelSetting;
import org.springframework.data.jpa.repository.JpaRepository;

/** Demo switches, one row per channel. */
public interface ChannelSettingRepository extends JpaRepository<ChannelSetting, Channel> {}
