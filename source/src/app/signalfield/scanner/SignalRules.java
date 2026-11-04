package app.signalfield.scanner;

public final class SignalRules {
    private SignalRules() {}
    public static String strength(int rssi) {
        return rssi >= -55 ? "STRONG" : rssi >= -70 ? "MODERATE" : "WEAK";
    }
    public static String hex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) out.append(String.format(java.util.Locale.US, "%02X", b & 255));
        return out.toString();
    }
    public static boolean freshFix(long nowNanos, long fixNanos) {
        long age = nowNanos - fixNanos;
        return age >= 0 && age <= 60000000000L;
    }
    public static boolean freshWifi(long nowMicros, long scanMicros) {
        long age = nowMicros - scanMicros;
        return scanMicros > 0 && age >= 0 && age <= 15000000L;
    }
}
