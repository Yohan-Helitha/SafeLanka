-- Owner: response module. Seed newly activated emergency shelters and incoming headcount update notifications.

-- Newly added emergency shelters in Colombo district
INSERT INTO shelters (id, name, district_id, address, latitude, longitude, capacity, current_occupancy,
                      status, coordinator_id) VALUES
 ('00000000-0000-0000-0009-000000000007', 'Sedawatta Siddhartha Maha Vidyalaya', '00000000-0000-0000-0001-000000000001',
  'Sedawatta, Kelani Bank Road', 6.9480, 79.8820, 250, 110, 'OPEN', '00000000-0000-0000-0006-000000000006'),
 ('00000000-0000-0000-0009-000000000008', 'Kotikawatta Rajasinghe Maha Vidyalaya', '00000000-0000-0000-0001-000000000001',
  'Kotikawatta Junction', 6.9290, 79.9140, 350, 290, 'OPEN', NULL),
 ('00000000-0000-0000-0009-000000000009', 'Grandpass Community Centre', '00000000-0000-0000-0001-000000000001',
  'Grandpass North', 6.9535, 79.8700, 180,  45, 'OPEN', NULL),
 ('00000000-0000-0000-0009-000000000010', 'Mulleriyawa Central Relief Shelter', '00000000-0000-0000-0001-000000000001',
  'Mulleriyawa South', 6.9210, 79.9320, 220,   0, 'OPEN', NULL);

-- Initial occupancy logs for the newly added shelters
INSERT INTO occupancy_logs (shelter_id, event_id, occupancy, delta, recorded_by, recorded_at) VALUES
 ('00000000-0000-0000-0009-000000000007', '00000000-0000-0000-0007-000000000001', 110, 110,
  '00000000-0000-0000-0006-000000000006', now() - interval '3 hours'),
 ('00000000-0000-0000-0009-000000000008', '00000000-0000-0000-0007-000000000001', 290, 290,
  '00000000-0000-0000-0006-000000000002', now() - interval '2 hours'),
 ('00000000-0000-0000-0009-000000000009', '00000000-0000-0000-0007-000000000001',  45,  45,
  '00000000-0000-0000-0006-000000000002', now() - interval '2 hours');

-- Incoming shelter headcount update notifications awaiting District Officer review/update
INSERT INTO shelter_headcount_updates (id, shelter_id, district_id, reported_occupancy, previous_occupancy,
                                       reported_by_name, reported_by_role, message, status, reported_at) VALUES
 ('00000000-0000-0000-0018-000000000001', '00000000-0000-0000-0009-000000000007', '00000000-0000-0000-0001-000000000001',
  145, 110, 'Dilani Gunasekara', 'SHELTER_COORDINATOR',
  'Rapid influx of families displaced by rising water near Nagalagam street. Verified count at 145 evacuees.',
  'PENDING', now() - interval '25 minutes'),

 ('00000000-0000-0000-0018-000000000002', '00000000-0000-0000-0009-000000000008', '00000000-0000-0000-0001-000000000001',
  320, 290, 'Tharindu Silva', 'VOLUNTEER',
  '30 additional evacuees arrived from Kotikawatta Low-Line canal sector. Approaching 91% capacity limit.',
  'PENDING', now() - interval '40 minutes'),

 ('00000000-0000-0000-0018-000000000003', '00000000-0000-0000-0009-000000000009', '00000000-0000-0000-0001-000000000001',
  95, 45, 'Grama Niladhari Unit - Grandpass', 'DISTRICT_FIELD_OFFICER',
  'Evacuation transport wave 2 completed. 50 new occupants admitted. Total headcount is now 95.',
  'PENDING', now() - interval '1 hour'),

 ('00000000-0000-0000-0018-000000000004', '00000000-0000-0000-0009-000000000001', '00000000-0000-0000-0001-000000000001',
  190, 140, 'Red Cross Medical Team', 'NGO',
  'Boat rescue team delivered 50 evacuees from Salamulla. Current headcount verified at 190.',
  'PENDING', now() - interval '2 hours');

-- Activity logs for newly added shelters and incoming headcount update alerts
INSERT INTO activity_logs (id, district_id, event_id, type, message, occurred_at) VALUES
 (gen_random_uuid(), '00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0007-000000000001',
  'SHELTER', 'New emergency shelter activated: Kotikawatta Rajasinghe Maha Vidyalaya (Capacity: 350)', now() - interval '3 hours'),

 (gen_random_uuid(), '00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0007-000000000001',
  'SHELTER', 'New emergency shelter activated: Sedawatta Siddhartha Maha Vidyalaya (Capacity: 250)', now() - interval '3 hours'),

 (gen_random_uuid(), '00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0007-000000000001',
  'SHELTER', 'Headcount update alert: 320 evacuees reported at Kotikawatta Rajasinghe Maha Vidyalaya (91% full)', now() - interval '40 minutes'),

 (gen_random_uuid(), '00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0007-000000000001',
  'SHELTER', 'Headcount update alert: 145 evacuees reported at Sedawatta Siddhartha Maha Vidyalaya', now() - interval '25 minutes');

