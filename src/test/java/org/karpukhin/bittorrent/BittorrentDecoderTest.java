package org.karpukhin.bittorrent;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Karpukhin
 * @since 09.07.14
 */
public class BittorrentDecoderTest {

    private BittorrentDecoder parser = new BittorrentDecoder();

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIncorrectInteger() throws IOException {
        InputStream stream = new ByteArrayInputStream("i-2-55e".getBytes());
        parser.parse(stream);
    }

    @Test
    public void testDecodeInteger() throws IOException {
        InputStream stream = new ByteArrayInputStream("i-255e".getBytes());
        Object result = parser.parse(stream);
        assertNotNull(result);
        assertTrue(result instanceof Long);
        assertEquals(-255L, result);
    }

    @Test
    public void testDecodeString() throws IOException {
        InputStream stream = new ByteArrayInputStream("5:abcde".getBytes());
        Object result = parser.parse(stream);
        assertNotNull(result);
        assertTrue(result instanceof byte[]);
        assertArrayEquals("abcde".getBytes(), (byte[])result);
    }

    @Test
    public void testDecodeList() throws IOException {
        InputStream stream = new ByteArrayInputStream("l5:abcdei-4ee".getBytes());
        Object result = parser.parse(stream);
        assertNotNull(result);
        assertTrue(result instanceof List);
        List list = (List)result;
        assertEquals(2, list.size());
        assertArrayEquals("abcde".getBytes(), (byte[])list.get(0));
        assertEquals(-4L, list.get(1));
    }

    @Test
    public void testDecodeDictionary() throws IOException {
        InputStream stream = new ByteArrayInputStream("d5:abcdei-67ee".getBytes());
        Object result = parser.parse(stream);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map map = (Map)result;
        assertEquals(1, map.size());
        assertEquals(-67L, map.get("abcde"));
    }
}
