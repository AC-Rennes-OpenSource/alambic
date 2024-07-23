package fr.gouv.education.acrennes.alambic.utils;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.nullable;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Variables.class)
@PowerMockIgnore("javax.management.*")
public class VariablesWithEnvTest {

    Map<String, String> env = new HashMap<>();

    @Before
    public void setUp() {
        env.put("env_var_1", "testVar$1");
        env.put("env_var_2", "testVar2");
        PowerMockito.mockStatic(System.class);

        PowerMockito.when(System.getenv()).thenReturn(env);
        PowerMockito.when(System.getenv(nullable(String.class))).thenAnswer(invocationOnMock -> env.get(invocationOnMock.getArgument(0,
                String.class)));
    }

    @Test
    public void testVariablePriority() throws AlambicException {
        final Variables variables = new Variables();
        variables.put("VAL", "1");
        variables.put("env_var_1", "varTest1");
        String resolvString = variables.resolvString("1 : %env_var_%VAL%%, 2 : %env_var_2%");
        Assert.assertEquals("1 : varTest1, 2 : testVar2", resolvString);
    }

    @Test
    public void testReplaceWithSpecialChar() throws AlambicException {
        final Variables variables = new Variables();
        variables.put("VAL", "1");
        String resolvString = variables.resolvString("1 : %env_var_%VAL%%, 2 : %env_var_2%");
        Assert.assertEquals("1 : testVar$1, 2 : testVar2", resolvString);
    }
}
