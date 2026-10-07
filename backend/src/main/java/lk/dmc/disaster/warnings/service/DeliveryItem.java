package lk.dmc.disaster.warnings.service;

import java.util.UUID;
import lk.dmc.disaster.warnings.entity.NotificationDelivery;

/**
 * One delivery with the district of the person it went to.
 *
 * @param districtId the person's home district, or null if the person is no longer known
 */
public record DeliveryItem(NotificationDelivery delivery, UUID districtId) {}
