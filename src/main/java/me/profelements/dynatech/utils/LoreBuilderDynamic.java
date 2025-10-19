package me.profelements.dynatech.utils;

public class LoreBuilderDynamic {
    private LoreBuilderDynamic() {}

    public static String powerBuffer(double power) {
        return power(power, " Buffer");
    }

    public static String powerPerSecond(double power) {
        return power(power * 20.0, "/s");
    }

    public static String powerPerTick(double power) {
        return power(power, "/t");
    }

    public static String power(double power, String suffix) {
        return "&8\u21E8 &e\u26A1 &7" + format(power) + " J" + suffix;
    }

    private static String format(double value) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", value);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}
