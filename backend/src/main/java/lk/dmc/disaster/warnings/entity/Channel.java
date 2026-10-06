package lk.dmc.disaster.warnings.entity;

/** Delivery channel for a warning. Values match the CHECK on notification_deliveries.channel. */
public enum Channel {
  PUSH,
  SMS,
  AUDIBLE
}
