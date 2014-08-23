package org.karpukhin.bittorrent;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Pavel Karpukhin
 * @since 09.07.14
 */
public class BittorrentDecoderTest {

    private BittorrentDecoder parser = new BittorrentDecoder();

    @Test(expected = IllegalArgumentException.class)
    public void testAssertTrueWhenConditionIsFalse() {
        BittorrentDecoder.assertTrue(false, "message");
    }

    @Test
    public void testAssertTrueWhenConditionIsTrue() {
        BittorrentDecoder.assertTrue(true, "message");
    }

    @Test
    public void testDecodeEmptyStream() throws IOException {
        InputStream stream = new ByteArrayInputStream("".getBytes());
        Object result = parser.parse(stream);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void testDecodeIncompleteInteger() throws IOException {
        InputStream stream = new ByteArrayInputStream("i".getBytes());
        Object result = parser.parse(stream);
        assertThat(result, is(nullValue()));
    }

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
    public void testDecodeIncompleteString() throws IOException {
        InputStream stream = new ByteArrayInputStream("5".getBytes());
        Object result = parser.parse(stream);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void testDecodeEmptyString() throws IOException {
        InputStream stream = new ByteArrayInputStream("0:".getBytes());
        Object result = parser.parse(stream);
        assertEquals(result, "");
    }

    @Test
    public void testDecodeString() throws IOException {
        InputStream stream = new ByteArrayInputStream("5:abcde".getBytes());
        Object result = parser.parse(stream);
        assertNotNull(result);
        assertTrue(result instanceof byte[]);
        assertArrayEquals("abcde".getBytes(), (byte[]) result);
    }

    @Test
    public void testDecodeIncompleteList() throws IOException {
        InputStream stream = new ByteArrayInputStream("l".getBytes());
        Object result = parser.parse(stream);
        assertThat(result, is(nullValue()));
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
    public void testDecodeIncompleteDictionary() throws IOException {
        InputStream stream = new ByteArrayInputStream("d".getBytes());
        Object result = parser.parse(stream);
        assertThat(result, is(nullValue()));
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

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeWrongStream() throws IOException {
        InputStream stream = new ByteArrayInputStream("c4:45e".getBytes());
        parser.parse(stream);
    }
}
