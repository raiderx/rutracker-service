package org.karpukhin.bittorrent;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

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
        assertThat(result, is(instanceOf(Long.class)));
        assertThat((Long)result, is(-255L));
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
        assertThat(result, is(instanceOf(String.class)));
        assertThat((String)result, is(""));
    }

    @Test
    public void testDecodeString() throws IOException {
        InputStream stream = new ByteArrayInputStream("5:abcde".getBytes());
        Object result = parser.parse(stream);
        assertThat(result, is(instanceOf(byte[].class)));
        assertThat((byte[]) result, is("abcde".getBytes()));
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
        assertThat(result, is(instanceOf(List.class)));
        List list = (List)result;
        assertThat(list.size(), is(2));
        assertThat((byte[])list.get(0), is("abcde".getBytes()));
        assertThat((Long)list.get(1), is(-4L));
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
        assertThat(result, is(instanceOf(Map.class)));
        Map map = (Map)result;
        assertThat(map.size(), is(1));
        assertThat((Long)map.get("abcde"), is(-67L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeWrongStream() throws IOException {
        InputStream stream = new ByteArrayInputStream("c4:45e".getBytes());
        parser.parse(stream);
    }
}
