import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class JdbcTest {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String user = "postgres.qnrjoswvyrzpnbplerah";
        String pass = "Yohan$hx15!";
        
        System.out.println("Connecting...");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connected!");
            String sql = "WITH target_basins AS ( " +
                "  SELECT warning_id, river_basin_id FROM warning_target_areas WHERE river_basin_id IS NOT NULL " +
                "), target_districts AS ( " +
                "  SELECT warning_id, district_id FROM warning_target_areas WHERE district_id IS NOT NULL " +
                "  UNION " +
                "  SELECT tb.warning_id, drb.district_id FROM target_basins tb JOIN district_river_basins drb ON tb.river_basin_id = drb.river_basin_id " +
                ") " +
                "SELECT w.id AS warningId, w.level, w.status, w.issued_at AS issuedAt, w.supersedes_id AS supersedesId, " +
                "COALESCE((SELECT array_agg(DISTINCT td.district_id) FROM target_districts td WHERE td.warning_id = w.id), CAST('{}' AS uuid[])) AS resolvedDistrictIds " +
                "FROM warnings w " +
                "WHERE w.event_id = ? " +
                "AND EXISTS (SELECT 1 FROM target_districts td WHERE td.warning_id = w.id AND td.district_id = ANY(?)) " +
                "ORDER BY w.issued_at ASC";
                
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, UUID.fromString("00000000-0000-0000-0007-000000000002"));
                
                UUID[] districtIds = new UUID[]{
                    UUID.fromString("00000000-0000-0000-0001-000000000004"), // Ratnapura
                    UUID.fromString("00000000-0000-0000-0001-000000000003")  // Kalutara
                };
                
                java.sql.Array array = conn.createArrayOf("uuid", districtIds);
                stmt.setArray(2, array);
                
                System.out.println("Executing query...");
                try (ResultSet rs = stmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        System.out.println("Found warning: " + rs.getObject("warningId"));
                        count++;
                    }
                    System.out.println("Total warnings: " + count);
                }
            }
        }
    }
}
