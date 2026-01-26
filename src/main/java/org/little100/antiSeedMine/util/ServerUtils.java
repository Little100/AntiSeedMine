package org.little100.antiSeedMine.util;

public class ServerUtils {
    
    private static Boolean isFolia = null;
    private static Boolean isPaper = null;

    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    public static boolean isPaper() {
        if (isPaper == null) {
            try {
                Class.forName("com.destroystokyo.paper.PaperConfig");
                isPaper = true;
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("io.papermc.paper.configuration.Configuration");
                    isPaper = true;
                } catch (ClassNotFoundException e2) {
                    isPaper = false;
                }
            }
        }
        return isPaper;
    }

    public static String getServerType() {
        if (isFolia()) {
            return "Folia";
        } else if (isPaper()) {
            return "Paper";
        } else {
            return "Spigot";
        }
    }
}
