import java.io.PrintWriter;

public class GenSql {
    public static void main(String[] args) throws Exception {
        try (PrintWriter w = new PrintWriter("backend/src/main/resources/db/migration/V1_0_2__seed_reference.sql")) {
            w.println("-- Seed reference data");
            // 5 districts
            w.println("INSERT INTO districts (id, code, name, province) VALUES");
            for (int i=1; i<=5; i++) {
                w.printf("  ('00000000-0000-0000-0001-%012d', 'D%d', 'District %d', 'Province')%s\n", i, i, i, i==5?";":",");
            }
            
            // 2 river basins
            w.println("INSERT INTO river_basins (id, code, name) VALUES");
            for (int i=1; i<=2; i++) {
                w.printf("  ('00000000-0000-0000-0002-%012d', 'RB%d', 'Basin %d')%s\n", i, i, i, i==2?";":",");
            }
            
            // district_river_basins mapping
            w.println("INSERT INTO district_river_basins (district_id, river_basin_id) VALUES");
            w.println("  ('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0002-000000000001'),");
            w.println("  ('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0002-000000000001');");
            
            // 3 hazard types
            w.println("INSERT INTO hazard_types (id, code, name, onset_speed, report_categories) VALUES");
            w.println("  ('00000000-0000-0000-0003-000000000001', 'FLOOD', 'Flood', 'RAPID', '{\"WATER_LEVEL\", \"OTHER\"}'),");
            w.println("  ('00000000-0000-0000-0003-000000000002', 'LANDSLIDE', 'Landslide', 'RAPID', '{\"SOIL_SLIP\", \"OTHER\"}'),");
            w.println("  ('00000000-0000-0000-0003-000000000003', 'DROUGHT', 'Drought', 'SLOW', '{\"WATER_SHORTAGE\", \"OTHER\"}');");
            
            // 9 organisations
            w.println("INSERT INTO organisations (id, name, type) VALUES");
            for (int i=1; i<=9; i++) {
                w.printf("  ('00000000-0000-0000-0004-%012d', 'Org %d', 'NGO')%s\n", i, i, i==9?";":",");
            }
            
            // 4 relief items
            w.println("INSERT INTO relief_items (id, code, name, unit, category) VALUES");
            for (int i=1; i<=4; i++) {
                w.printf("  ('00000000-0000-0000-0005-%012d', 'ITM%d', 'Item %d', 'kg', 'FOOD')%s\n", i, i, i, i==4?";":",");
            }
            
            // 49 users
            w.println("INSERT INTO users (id, role, full_name, district_id) VALUES");
            for (int i=1; i<=49; i++) {
                w.printf("  ('00000000-0000-0000-0006-%012d', 'CITIZEN', 'User %d', '00000000-0000-0000-0001-000000000001')%s\n", i, i, i==49?";":",");
            }
            
            // 2 events
            w.println("INSERT INTO disaster_events (id, name, hazard_type_id, status, started_at) VALUES");
            w.println("  ('00000000-0000-0000-0007-000000000001', 'Event 1', '00000000-0000-0000-0003-000000000001', 'ACTIVE', now()),");
            w.println("  ('00000000-0000-0000-0007-000000000002', 'Event 2', '00000000-0000-0000-0003-000000000001', 'CLOSED', now());");

            w.println("INSERT INTO event_districts (event_id, district_id) VALUES");
            w.println("  ('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0001-000000000001');");

        }
    }
}
