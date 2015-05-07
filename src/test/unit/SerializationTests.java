package test.unit;

import main.network.SocketInformation;
import main.util.Serialization;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import static org.junit.Assert.*;

/**
 * Tests reading and writing objects.
 */
public class SerializationTests {
    // Tests that writing the value and reading returns the correct value.
    private void testString(String value) throws Exception {
        byte[] bytes = Serialization.serialize(value);
        String actual = Serialization.deserialize(
                new ByteArrayInputStream(bytes), value.getClass());
        assertEquals(value, actual);
    }

    private void testSocketInfo(SocketInformation value) throws Exception {
        byte[] bytes = Serialization.serialize(value);
        SocketInformation actual = Serialization.deserialize(
                new ByteArrayInputStream(bytes), value.getClass());
        assertEquals(value, actual);
    }

    @Test
    public void testEmptyString() throws Exception {
        testString("");
    }

    @Test
    public void testString() throws Exception {
        testString("hello");
    }

    @Test
    public void testSocketInfo() throws Exception {
        testSocketInfo(new SocketInformation("18.1.1.1", 5555));
    }
}
