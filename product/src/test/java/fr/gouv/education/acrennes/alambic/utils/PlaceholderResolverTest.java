package fr.gouv.education.acrennes.alambic.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Properties;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({System.class, PlaceholderResolver.class})
public class PlaceholderResolverTest {

    @Test
    public void testResolvedFromMockedEnv() {
        // Arrange: mock System.getenv()
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv("MY_VAR")).thenReturn("mockedValue");
        PowerMockito.when(System.getenv("MY_VAR_2")).thenReturn("mockedValue2");

        Properties props = new Properties();
        props.setProperty("key", "${MY_VAR}");
        props.setProperty("key2", "${MY_VAR_2}");
        props.setProperty("host", "localhost");
        props.setProperty("port", "${UNDEFINED_ENV:1234}");
        props.setProperty("env", "${MISSING_ENV}");

        Properties resolved = PlaceholderResolver.resolvePlaceholders(props);

        assertEquals("mockedValue", resolved.getProperty("key"));
        assertEquals("mockedValue2", resolved.getProperty("key2"));
        assertEquals("localhost", resolved.getProperty("host"));
        assertEquals("1234", resolved.getProperty("port"));
        assertEquals("${MISSING_ENV}", resolved.getProperty("env"));
    }

    @Test
    public void testResolvedFromEnv() {
        // Simulate env variable manually (read-only in real JVM, but we can test against actual env if set)
        String envVar = System.getenv("HOME"); // Choose a commonly defined env var
        if (envVar == null) {
            System.out.println("Skipping testResolvedFromEnv: HOME not set in environment");
            return;
        }

        Properties props = new Properties();
        props.setProperty("path", "${HOME}");

        Properties resolved = PlaceholderResolver.resolvePlaceholders(props);

        assertEquals(envVar, resolved.getProperty("path"));
    }

    @Test
    public void testDefaultUsedIfEnvMissing() {
        Properties props = new Properties();
        props.setProperty("port", "${UNDEFINED_ENV:1234}");

        Properties resolved = PlaceholderResolver.resolvePlaceholders(props);

        assertEquals("1234", resolved.getProperty("port"));
    }

    @Test
    public void testUnresolvedPlaceholderRemains() {
        Properties props = new Properties();
        props.setProperty("host", "${MISSING_ENV}");

        Properties resolved = PlaceholderResolver.resolvePlaceholders(props);

        assertEquals("${MISSING_ENV}", resolved.getProperty("host"));
    }

    @Test
    public void testFixedValueRemainsUnchanged() {
        Properties props = new Properties();
        props.setProperty("env", "production");
        props.setProperty("host", "localhost");

        Properties resolved = PlaceholderResolver.resolvePlaceholders(props);

        assertEquals("production", resolved.getProperty("env"));
        assertEquals("localhost", resolved.getProperty("host"));
    }

    @Test
    public void testMixedPlaceholderAndLiteral() {
        Properties props = new Properties();
        props.setProperty("url", "http://${UNDEFINED_ENV:localhost}:8080/api");

        Properties resolved = PlaceholderResolver.resolvePlaceholders(props);

        assertEquals("http://localhost:8080/api", resolved.getProperty("url"));
    }
}
