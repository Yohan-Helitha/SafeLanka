package lk.dmc.disaster.support;

import java.util.UUID;

/** Fixed UUIDs used in seed data and tests. */
public final class TestIds {
    private TestIds() {}

    public static UUID district(int n) { return id("0001", n); }
    public static UUID basin(int n) { return id("0002", n); }
    public static UUID hazardType(int n) { return id("0003", n); }
    public static UUID organisation(int n) { return id("0004", n); }
    public static UUID reliefItem(int n) { return id("0005", n); }
    public static UUID user(int n) { return id("0006", n); }
    public static UUID event(int n) { return id("0007", n); }

    private static UUID id(String t, int n) {
        return UUID.fromString(String.format("00000000-0000-0000-%s-%012d", t, n));
    }
}
