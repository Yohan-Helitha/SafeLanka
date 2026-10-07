-- Owner: group. Fixed UUIDs: 00000000-0000-0000-TTTT-0000000000NN (see Section 8).

INSERT INTO districts (id, code, name, province) VALUES
 ('00000000-0000-0000-0001-000000000001', 'CMB', 'Colombo',   'Western'),
 ('00000000-0000-0000-0001-000000000002', 'GAM', 'Gampaha',   'Western'),
 ('00000000-0000-0000-0001-000000000003', 'KAL', 'Kalutara',  'Western'),
 ('00000000-0000-0000-0001-000000000004', 'RAT', 'Ratnapura', 'Sabaragamuwa'),
 ('00000000-0000-0000-0001-000000000005', 'KEG', 'Kegalle',   'Sabaragamuwa');

INSERT INTO river_basins (id, code, name) VALUES
 ('00000000-0000-0000-0002-000000000001', 'KELANI', 'Kelani Ganga'),
 ('00000000-0000-0000-0002-000000000002', 'KALU',   'Kalu Ganga');

INSERT INTO district_river_basins (district_id, river_basin_id) VALUES
 ('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0002-000000000001'),  -- Colombo   / Kelani
 ('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0002-000000000001'),  -- Gampaha   / Kelani
 ('00000000-0000-0000-0001-000000000005', '00000000-0000-0000-0002-000000000001'),  -- Kegalle   / Kelani
 ('00000000-0000-0000-0001-000000000004', '00000000-0000-0000-0002-000000000002'),  -- Ratnapura / Kalu
 ('00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0002-000000000002');  -- Kalutara  / Kalu

-- DROUGHT is inactive: enabling it is a data change, not a code change (Phase 2 flexibility).
INSERT INTO hazard_types (id, code, name, onset_speed, report_categories, active) VALUES
 ('00000000-0000-0000-0003-000000000001', 'FLOOD',     'Flood',     'RAPID',
  ARRAY['RISING_WATER', 'BLOCKED_ROAD', 'OTHER'], TRUE),
 ('00000000-0000-0000-0003-000000000002', 'LANDSLIDE', 'Landslide', 'RAPID',
  ARRAY['LANDSLIDE_CRACK', 'BLOCKED_ROAD', 'OTHER'], TRUE),
 ('00000000-0000-0000-0003-000000000003', 'DROUGHT',   'Drought',   'SLOW',
  ARRAY['WATER_SHORTAGE', 'OTHER'], FALSE);

INSERT INTO organisations (id, name, type) VALUES
 ('00000000-0000-0000-0004-000000000001', 'Disaster Management Centre',  'GOVERNMENT'),
 ('00000000-0000-0000-0004-000000000002', 'Ministry of Health',          'GOVERNMENT'),
 ('00000000-0000-0000-0004-000000000003', 'Sri Lanka Army',              'ARMED_FORCES'),
 ('00000000-0000-0000-0004-000000000004', 'Sri Lanka Navy',              'ARMED_FORCES'),
 ('00000000-0000-0000-0004-000000000005', 'Sri Lanka Air Force',         'ARMED_FORCES'),
 ('00000000-0000-0000-0004-000000000006', 'Sri Lanka Police',            'POLICE'),
 ('00000000-0000-0000-0004-000000000007', 'Sri Lanka Red Cross Society', 'NGO'),
 ('00000000-0000-0000-0004-000000000008', 'Sarvodaya',                   'NGO'),
 ('00000000-0000-0000-0004-000000000009', 'Private Donor Network',       'PRIVATE_DONOR');

INSERT INTO relief_items (id, code, name, unit, category) VALUES
 ('00000000-0000-0000-0005-000000000001', 'DRY_RATION',  'Dry ration pack',    'packs',   'FOOD'),
 ('00000000-0000-0000-0005-000000000002', 'WATER_5L',    'Drinking water 5 L', 'bottles', 'WATER'),
 ('00000000-0000-0000-0005-000000000003', 'FIRST_AID',   'First aid kit',      'kits',    'MEDICINE'),
 ('00000000-0000-0000-0005-000000000004', 'HYGIENE_KIT', 'Hygiene kit',        'kits',    'HYGIENE');

-- One named user per role (the frontend role switcher uses these).
INSERT INTO users (id, role, full_name, phone, nic, home_address, district_id, river_basin_id,
                   preferred_language, organisation_id) VALUES
 ('00000000-0000-0000-0006-000000000001', 'DMC_OFFICER',         'Nimal Perera',           '+94771000001', NULL, NULL,
  '00000000-0000-0000-0001-000000000001', NULL, 'en', '00000000-0000-0000-0004-000000000001'),
 ('00000000-0000-0000-0006-000000000002', 'DISTRICT_OFFICER',    'Kasun Jayawardena',      '+94771000002', NULL, NULL,
  '00000000-0000-0000-0001-000000000001', NULL, 'en', '00000000-0000-0000-0004-000000000001'),
 ('00000000-0000-0000-0006-000000000003', 'DISTRICT_OFFICER',    'Sanduni Wickramasinghe', '+94771000003', NULL, NULL,
  '00000000-0000-0000-0001-000000000004', NULL, 'en', '00000000-0000-0000-0004-000000000001'),
 ('00000000-0000-0000-0006-000000000004', 'CITIZEN',             'Ruwan Fernando',         '+94771000004', '199012345678',
  '45 Station Road, Kolonnawa', '00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0002-000000000001', 'si', NULL),
 ('00000000-0000-0000-0006-000000000005', 'VOLUNTEER',           'Tharindu Silva',         '+94771000005', '199534567890',
  '12 Temple Lane, Biyagama', '00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0002-000000000001', 'si', NULL),
 ('00000000-0000-0000-0006-000000000006', 'SHELTER_COORDINATOR', 'Dilani Gunasekara',      '+94771000006', NULL, NULL,
  '00000000-0000-0000-0001-000000000001', NULL, 'si', '00000000-0000-0000-0004-000000000001'),
 ('00000000-0000-0000-0006-000000000007', 'RESCUE_MEMBER',       'Asanka Bandara',         '+94771000007', NULL, NULL,
  '00000000-0000-0000-0001-000000000001', NULL, 'si', '00000000-0000-0000-0004-000000000003'),
 ('00000000-0000-0000-0006-000000000008', 'RESCUE_MEMBER',       'Mahesh Kumara',          '+94771000008', NULL, NULL,
  '00000000-0000-0000-0001-000000000001', NULL, 'si', '00000000-0000-0000-0004-000000000004'),
 ('00000000-0000-0000-0006-000000000009', 'CITIZEN',             'Priya Shanmugam',        '+94771000009', '199278901234',
  '8 River View, Kalutara', '00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0002-000000000002', 'ta', NULL);

-- 40 demo residents (ids ...0006-000000000101 to ...140) across the 5 districts; every 4th is a volunteer.
INSERT INTO users (id, role, full_name, phone, nic, home_address, district_id, river_basin_id, preferred_language)
SELECT ('00000000-0000-0000-0006-' || lpad(n::text, 12, '0'))::uuid,
       CASE WHEN n % 4 = 0 THEN 'VOLUNTEER' ELSE 'CITIZEN' END,
       'Demo Resident ' || n,
       '+9477' || lpad((2000000 + n)::text, 7, '0'),
       '1985' || lpad(n::text, 8, '0'),
       'Demo address ' || n,
       d.id,
       drb.river_basin_id,
       (ARRAY['si', 'ta', 'en'])[1 + n % 3]
FROM generate_series(101, 140) AS n
JOIN districts d ON d.code = (ARRAY['CMB', 'GAM', 'KAL', 'RAT', 'KEG'])[1 + n % 5]
JOIN district_river_basins drb ON drb.district_id = d.id;

INSERT INTO disaster_events (id, name, hazard_type_id, status, started_at, ended_at) VALUES
 ('00000000-0000-0000-0007-000000000001', 'Kelani Flood October 2026', '00000000-0000-0000-0003-000000000001',
  'ACTIVE', '2026-10-01 06:00:00+05:30', NULL),
 ('00000000-0000-0000-0007-000000000002', 'Kalu Flood May 2026',       '00000000-0000-0000-0003-000000000001',
  'CLOSED', '2026-05-14 04:00:00+05:30', '2026-05-21 18:00:00+05:30');

INSERT INTO event_districts (event_id, district_id) VALUES
 ('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0001-000000000001'),
 ('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0001-000000000002'),
 ('00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0001-000000000004'),
 ('00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0001-000000000003');
