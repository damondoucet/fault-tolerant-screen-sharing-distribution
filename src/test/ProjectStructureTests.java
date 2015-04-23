package test;

import org.junit.Test;
import static com.google.common.base.Preconditions.*;

/**
 * Tests that the project is setup correctly.
 *
 * Tested things:
 *      - JUnit
 *      - Guava
 *      - Lambda support
 */
public class ProjectStructureTests {
    @Test
    public void testGuava() {
        checkArgument(1 == 1);
    }

    @Test
    public void testLambda() {
        Runnable r = () -> {
            int x = 1;
        };
        r.run();
    }
}
