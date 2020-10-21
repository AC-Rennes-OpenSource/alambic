package fr.gouv.education.acrennes.alambic.utils;

import java.util.Properties;

public class Config {
    private static Properties props = new Properties();

    public static void setProperties(Properties properties) {
        props = properties;
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }
}
