package lk.dmc.disaster.warnings.integration;

import lk.dmc.disaster.warnings.entity.Channel;

/**
 * The demo switch that makes a gateway fail. The simulators depend on this one-method interface,
 * not on where the setting is stored.
 */
public interface GatewayFailureSwitch {

  boolean isFailureSimulated(Channel channel);
}
