-- Owner: Herath (analytics module). History data for the Kalu and Kelani events.

-- Event 1: Kalu Flood May 2026 (CLOSED, 0007-0002)
-- Hazard: FLOOD (0003-0001), Kalu basin (0002-0002), source SENSOR, RESOLVED
INSERT INTO hazards (id, event_id, hazard_type_id, severity, river_basin_id, description, source, sensor_id, status, detected_at) VALUES
 ('00000000-0000-0000-0012-000000000001', '00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0003-000000000001', 4, '00000000-0000-0000-0002-000000000002', 'Kalu Ganga severe flooding', 'SENSOR', '00000000-0000-0000-0008-000000000004', 'RESOLVED', '2026-05-14 00:10:00+00:00');

-- hazard_evidence linking Yohan's verified Ratnapura report
INSERT INTO hazard_evidence (hazard_id, report_id, linked_at) VALUES
 ('00000000-0000-0000-0012-000000000001', 'a0000000-0000-0000-0000-000000000001', '2026-05-14 00:15:00+00:00');

-- readings for Ratnapura gauge (0008-0004) rising above 7.5m
INSERT INTO sensor_readings (sensor_id, value, recorded_at) VALUES
 ('00000000-0000-0000-0008-000000000004', 5.5, '2026-05-13 22:00:00+00:00'),
 ('00000000-0000-0000-0008-000000000004', 6.8, '2026-05-13 23:00:00+00:00'),
 ('00000000-0000-0000-0008-000000000004', 7.6, '2026-05-14 00:10:00+00:00');

-- warnings WATCH -> WARNING -> EVACUATE targeting Kalu basin
INSERT INTO warnings (id, hazard_id, event_id, level, status, target_type, title, message, sms_text, instructions, issued_by, issued_at) VALUES
 ('00000000-0000-0000-0013-000000000001', '00000000-0000-0000-0012-000000000001', '00000000-0000-0000-0007-000000000002', 'WATCH', 'ESCALATED', 'RIVER_BASIN', 'Kalu Ganga Watch', 'Water levels rising', 'Water levels rising', 'Be prepared', '00000000-0000-0000-0006-000000000001', '2026-05-14 01:30:00+00:00'),
 ('00000000-0000-0000-0013-000000000002', '00000000-0000-0000-0012-000000000001', '00000000-0000-0000-0007-000000000002', 'WARNING', 'ESCALATED', 'RIVER_BASIN', 'Kalu Ganga Warning', 'Major flooding expected', 'Major flooding expected', 'Move to higher ground', '00000000-0000-0000-0006-000000000001', '2026-05-14 20:30:00+00:00'),
 ('00000000-0000-0000-0013-000000000003', '00000000-0000-0000-0012-000000000001', '00000000-0000-0000-0007-000000000002', 'EVACUATE', 'EXPIRED', 'RIVER_BASIN', 'Kalu Ganga Evacuation', 'Evacuate immediately', 'Evacuate immediately', 'Evacuate', '00000000-0000-0000-0006-000000000001', '2026-05-15 14:30:00+00:00');

UPDATE warnings SET supersedes_id = '00000000-0000-0000-0013-000000000001' WHERE id = '00000000-0000-0000-0013-000000000002';
UPDATE warnings SET supersedes_id = '00000000-0000-0000-0013-000000000002' WHERE id = '00000000-0000-0000-0013-000000000003';

INSERT INTO warning_target_areas (warning_id, river_basin_id) VALUES
 ('00000000-0000-0000-0013-000000000001', '00000000-0000-0000-0002-000000000002'),
 ('00000000-0000-0000-0013-000000000002', '00000000-0000-0000-0002-000000000002'),
 ('00000000-0000-0000-0013-000000000003', '00000000-0000-0000-0002-000000000002');

-- notification_deliveries for citizen and volunteer in Ratnapura (0001-0004) and Kalutara (0001-0003)
DO $$
DECLARE
    u_id UUID;
    w1 UUID := '00000000-0000-0000-0013-000000000001';
    w2 UUID := '00000000-0000-0000-0013-000000000002';
    w3 UUID := '00000000-0000-0000-0013-000000000003';
    i INT := 0;
BEGIN
    FOR u_id IN SELECT id FROM users WHERE district_id IN ('00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0001-000000000004') AND role IN ('CITIZEN', 'VOLUNTEER') LOOP
        i := i + 1;
        -- w1 PUSH and SMS
        INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at) VALUES (w1, u_id, 'PUSH', 'DELIVERED', '2026-05-14 01:31:00+00:00');
        IF i % 12 = 0 THEN
            INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at, failure_reason) VALUES (w1, u_id, 'SMS', 'FAILED', '2026-05-14 01:31:00+00:00', 'Simulated gateway timeout');
        ELSE
            INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at) VALUES (w1, u_id, 'SMS', 'DELIVERED', '2026-05-14 01:31:00+00:00');
        END IF;

        -- w2 PUSH, SMS, AUDIBLE
        INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at) VALUES (w2, u_id, 'PUSH', 'DELIVERED', '2026-05-14 20:31:00+00:00');
        INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at) VALUES (w2, u_id, 'AUDIBLE', 'DELIVERED', '2026-05-14 20:31:00+00:00');
        IF (i+1) % 12 = 0 THEN
            INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at, failure_reason) VALUES (w2, u_id, 'SMS', 'FAILED', '2026-05-14 20:31:00+00:00', 'Simulated gateway timeout');
        ELSE
            INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at) VALUES (w2, u_id, 'SMS', 'DELIVERED', '2026-05-14 20:31:00+00:00');
        END IF;

        -- w3 PUSH, SMS, AUDIBLE
        INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at) VALUES (w3, u_id, 'PUSH', 'DELIVERED', '2026-05-15 14:31:00+00:00');
        INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at) VALUES (w3, u_id, 'AUDIBLE', 'DELIVERED', '2026-05-15 14:31:00+00:00');
        IF (i+2) % 12 = 0 THEN
            INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at, failure_reason) VALUES (w3, u_id, 'SMS', 'FAILED', '2026-05-15 14:31:00+00:00', 'Simulated gateway timeout');
        ELSE
            INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at) VALUES (w3, u_id, 'SMS', 'DELIVERED', '2026-05-15 14:31:00+00:00');
        END IF;
    END LOOP;
END $$;

-- occupancy_logs for Ratnapura Sivali Central College (0009-0006)
-- 20, 85, 160, 240, 285, 290, 260, 180, 110, 60, 20, 0
INSERT INTO occupancy_logs (shelter_id, event_id, occupancy, delta, recorded_by, recorded_at) VALUES
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 20, 20, '00000000-0000-0000-0006-000000000006', '2026-05-14 22:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 85, 65, '00000000-0000-0000-0006-000000000006', '2026-05-15 10:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 160, 75, '00000000-0000-0000-0006-000000000006', '2026-05-15 22:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 240, 80, '00000000-0000-0000-0006-000000000006', '2026-05-16 10:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 285, 45, '00000000-0000-0000-0006-000000000006', '2026-05-16 22:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 290, 5, '00000000-0000-0000-0006-000000000006', '2026-05-17 10:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 260, -30, '00000000-0000-0000-0006-000000000006', '2026-05-17 22:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 180, -80, '00000000-0000-0000-0006-000000000006', '2026-05-18 10:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 110, -70, '00000000-0000-0000-0006-000000000006', '2026-05-18 22:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 60, -50, '00000000-0000-0000-0006-000000000006', '2026-05-19 10:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 20, -40, '00000000-0000-0000-0006-000000000006', '2026-05-19 22:00:00+00:00'),
 ('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 0, -20, '00000000-0000-0000-0006-000000000006', '2026-05-20 10:00:00+00:00');

-- allocations: 250 dry ration (stock 0011-0010) and 180 water (stock 0011-0011), both DISTRIBUTED
INSERT INTO resource_allocations (id, stock_id, shelter_id, event_id, quantity, status, allocated_by, allocated_at) VALUES
 ('00000000-0000-0000-0014-000000000001', '00000000-0000-0000-0011-000000000010', '00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 250, 'DISTRIBUTED', '00000000-0000-0000-0006-000000000001', '2026-05-15 08:00:00+00:00'),
 ('00000000-0000-0000-0014-000000000002', '00000000-0000-0000-0011-000000000011', '00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0007-000000000002', 180, 'DISTRIBUTED', '00000000-0000-0000-0006-000000000001', '2026-05-16 09:00:00+00:00');

INSERT INTO relief_distributions (allocation_id, quantity_distributed, distributed_by, distributed_at, synced_at) VALUES
 ('00000000-0000-0000-0014-000000000001', 250, '00000000-0000-0000-0006-000000000006', '2026-05-15 14:00:00+00:00', '2026-05-15 14:00:00+00:00'),
 ('00000000-0000-0000-0014-000000000002', 180, '00000000-0000-0000-0006-000000000006', '2026-05-16 15:00:00+00:00', '2026-05-16 15:00:00+00:00');

UPDATE relief_stocks SET quantity_available = quantity_available - 250 WHERE id = '00000000-0000-0000-0011-000000000010';
UPDATE relief_stocks SET quantity_available = quantity_available - 180 WHERE id = '00000000-0000-0000-0011-000000000011';

-- two COMPLETED rescue assignments
INSERT INTO rescue_assignments (id, event_id, team_id, created_by, latitude, longitude, location_text, task, priority, status, assigned_at, completed_at) VALUES
 ('00000000-0000-0000-0015-000000000001', '00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0010-000000000006', '00000000-0000-0000-0006-000000000001', 6.68, 80.40, 'Ratnapura Town', 'Evacuate stranded family', 1, 'COMPLETED', '2026-05-14 10:00:00+00:00', '2026-05-14 12:00:00+00:00'),
 ('00000000-0000-0000-0015-000000000002', '00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0010-000000000006', '00000000-0000-0000-0006-000000000001', 6.69, 80.41, 'Ratnapura North', 'Medical transport', 2, 'COMPLETED', '2026-05-15 08:00:00+00:00', '2026-05-15 11:00:00+00:00');

INSERT INTO team_status_logs (team_id, assignment_id, from_status, to_status, changed_by, changed_at, synced_at) VALUES
 ('00000000-0000-0000-0010-000000000006', '00000000-0000-0000-0015-000000000001', 'AVAILABLE', 'ACTIVE', '00000000-0000-0000-0006-000000000001', '2026-05-14 10:00:00+00:00', '2026-05-14 10:00:00+00:00'),
 ('00000000-0000-0000-0010-000000000006', '00000000-0000-0000-0015-000000000001', 'ACTIVE', 'AVAILABLE', '00000000-0000-0000-0006-000000000001', '2026-05-14 12:00:00+00:00', '2026-05-14 12:00:00+00:00');

-- Event 2: Kelani Flood October 2026 (ACTIVE, 0007-0001)
-- Hazards: Kelani flood (0003-0001, SENSOR Nagalagam Street 0008-0001, WARNED, severity 3), 
-- Gampaha flood (0003-0001, MANUAL, MONITORING), 
-- Kegalle landslide (0003-0002, REPORT, UNDER_ASSESSMENT)
INSERT INTO hazards (id, event_id, hazard_type_id, severity, district_id, river_basin_id, description, source, sensor_id, status, detected_at) VALUES
 ('00000000-0000-0000-0012-000000000002', '00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0003-000000000001', 3, NULL, '00000000-0000-0000-0002-000000000001', 'Kelani basin flood', 'SENSOR', '00000000-0000-0000-0008-000000000001', 'WARNED', '2026-10-05 08:00:00+00:00'),
 ('00000000-0000-0000-0012-000000000003', '00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0003-000000000001', 2, '00000000-0000-0000-0001-000000000002', NULL, 'Gampaha local flooding', 'MANUAL', NULL, 'MONITORING', '2026-10-06 09:00:00+00:00'),
 ('00000000-0000-0000-0012-000000000004', '00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0003-000000000002', 2, '00000000-0000-0000-0001-000000000005', NULL, 'Kegalle landslide risk', 'REPORT', NULL, 'UNDER_ASSESSMENT', '2026-10-06 12:00:00+00:00');

-- evidence to verified reports
INSERT INTO hazard_evidence (hazard_id, report_id, linked_at) VALUES
 ('00000000-0000-0000-0012-000000000002', 'a0000000-0000-0000-0000-000000000002', '2026-10-05 08:30:00+00:00'),
 ('00000000-0000-0000-0012-000000000004', 'a0000000-0000-0000-0000-000000000003', '2026-10-06 12:30:00+00:00');

-- active warning 20 hours ago
INSERT INTO warnings (id, hazard_id, event_id, level, status, target_type, title, message, sms_text, instructions, issued_by, issued_at) VALUES
 ('00000000-0000-0000-0013-000000000004', '00000000-0000-0000-0012-000000000002', '00000000-0000-0000-0007-000000000001', 'WARNING', 'ACTIVE', 'RIVER_BASIN', 'Kelani Warning', 'Flooding in Kelani', 'Flooding in Kelani', 'Be alert', '00000000-0000-0000-0006-000000000001', now() - interval '20 hours');
INSERT INTO warning_target_areas (warning_id, river_basin_id) VALUES
 ('00000000-0000-0000-0013-000000000004', '00000000-0000-0000-0002-000000000001');

DO $$
DECLARE
    u_id UUID;
BEGIN
    FOR u_id IN SELECT id FROM users WHERE district_id IN ('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0001-000000000002') AND role IN ('CITIZEN', 'VOLUNTEER') LOOP
        INSERT INTO notification_deliveries (warning_id, citizen_id, channel, status, attempted_at) VALUES ('00000000-0000-0000-0013-000000000004', u_id, 'PUSH', 'DELIVERED', now() - interval '20 hours');
    END LOOP;
END $$;

-- Navy team (0010-0002) DISPATCHED with PENDING_ACK to Wellampitiya (0009-0002) (12 people)
INSERT INTO rescue_assignments (id, event_id, team_id, created_by, latitude, longitude, location_text, task, priority, people_estimated, destination_shelter_id, status, assigned_at) VALUES
 ('00000000-0000-0000-0015-000000000003', '00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0010-000000000002', '00000000-0000-0000-0006-000000000001', 6.93, 79.89, 'Wellampitiya area', 'Rescue 12 people', 1, 12, '00000000-0000-0000-0009-000000000002', 'PENDING_ACK', now() - interval '2 hours');
UPDATE rescue_teams SET status = 'DISPATCHED' WHERE id = '00000000-0000-0000-0010-000000000002';

-- one UNASSIGNED assignment at Sedawatte
INSERT INTO rescue_assignments (id, event_id, created_by, latitude, longitude, location_text, task, priority, status) VALUES
 ('00000000-0000-0000-0015-000000000004', '00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0006-000000000001', 6.94, 79.88, 'Sedawatte', 'Rescue 5 people', 2, 'UNASSIGNED');

-- one COMPLETED Army assignment (0010-0001)
INSERT INTO rescue_assignments (id, event_id, team_id, created_by, latitude, longitude, location_text, task, priority, status, assigned_at, completed_at) VALUES
 ('00000000-0000-0000-0015-000000000005', '00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0010-000000000001', '00000000-0000-0000-0006-000000000001', 6.95, 79.87, 'Colombo North', 'Evacuation', 1, 'COMPLETED', now() - interval '1 day', now() - interval '20 hours');

-- allocation 200 bottles water to Wellampitiya Community Hall (0009-0002) (reduce Army stock to 600). Army stock is 0011-0004.
INSERT INTO resource_allocations (id, stock_id, shelter_id, event_id, quantity, status, allocated_by, allocated_at) VALUES
 ('00000000-0000-0000-0014-000000000003', '00000000-0000-0000-0011-000000000004', '00000000-0000-0000-0009-000000000002', '00000000-0000-0000-0007-000000000001', 200, 'PARTIALLY_DISTRIBUTED', '00000000-0000-0000-0006-000000000001', now() - interval '5 hours');
INSERT INTO relief_distributions (allocation_id, quantity_distributed, distributed_by, distributed_at, synced_at) VALUES
 ('00000000-0000-0000-0014-000000000003', 120, '00000000-0000-0000-0006-000000000006', now() - interval '2 hours', now() - interval '2 hours');
UPDATE relief_stocks SET quantity_available = 600 WHERE id = '00000000-0000-0000-0011-000000000004';

-- allocation 150 dry ration packs to Kolonnawa (0009-0001). Stock 0011-0001 (DMC).
INSERT INTO resource_allocations (id, stock_id, shelter_id, event_id, quantity, status, allocated_by, allocated_at) VALUES
 ('00000000-0000-0000-0014-000000000004', '00000000-0000-0000-0011-000000000001', '00000000-0000-0000-0009-000000000001', '00000000-0000-0000-0007-000000000001', 150, 'ALLOCATED', '00000000-0000-0000-0006-000000000001', now() - interval '4 hours');
UPDATE relief_stocks SET quantity_available = quantity_available - 150 WHERE id = '00000000-0000-0000-0011-000000000001';

-- recent activity logs rows for Colombo and Gampaha
INSERT INTO activity_logs (district_id, event_id, type, message, occurred_at) VALUES
 ('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0007-000000000001', 'WARNING', 'Warning issued for Kelani', now() - interval '20 hours'),
 ('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0007-000000000001', 'SHELTER', 'Biyagama shelter opened', now() - interval '2 days');
