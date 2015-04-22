package test;

import org.junit.Test;
import static com.google.common.base.Preconditions.*;

/**
 * Tests that guava and junit are included properly.
 */
public class TestProjectStructure {
    @Test
    public void testGuava() {
        checkArgument(1 == 1);
    }
}
