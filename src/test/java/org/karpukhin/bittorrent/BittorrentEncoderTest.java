package org.karpukhin.bittorrent;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Pavel Karpukhin
 * @since 10.07.14
 */
public class BittorrentEncoderTest {

    private BittorrentEncoder encoder = new BittorrentEncoder();
    private BittorrentDecoder decoder = new BittorrentDecoder();

    @Test(expected = IllegalArgumentException.class)
    public void testAssertTrueWhenConditionIsFalse() {
        BittorrentEncoder.assertTrue(false, "message");
    }

    @Test
    public void testAssertTrueWhenConditionIsTrue() {
        BittorrentEncoder.assertTrue(true, "message");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeWhenObjIsNull() {
        encoder.encode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeWhenClassIsUnknown() {
        encoder.encode(true);
    }

    @Test
    public void testEncodeInteger() {
        byte[] result = encoder.encode(-355);
        assertThat(result, is("i-355e".getBytes()));
    }

    @Test
    public void testEncodeLong() {
        byte[] result = encoder.encode(-256L);
        assertThat(result, is("i-256e".getBytes()));
    }

    @Test
    public void testEncodeString() {
        byte[] result = encoder.encode("string");
        assertThat(result, is("6:string".getBytes()));
    }

    @Test
    public void testEncodeBytes() {
        byte[] result = encoder.encode("string".getBytes());
        assertThat(result, is("6:string".getBytes()));
    }

    @Test
    public void testEncodeList() {
        byte[] result = encoder.encode(Arrays.asList(-25L, "string".getBytes()));
        assertThat(result, is("li-25e6:stringe".getBytes()));
    }

    @Test
    public void testEncodeDictionary() {
        Map map = new HashMap<>();
        map.put(-25L, "string");
        byte[] result = encoder.encode(map);
        assertThat(result, is("di-25e6:stringe".getBytes()));
    }

    @Test
    public void testDecodingEncoding() throws IOException {
        InputStream stream = this.getClass().getResourceAsStream("/some.torrent");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int res;
        while ((res = stream.read(buffer)) != -1) {
            out.write(buffer, 0, res);
        }
        byte[] content = out.toByteArray();

        stream = new ByteArrayInputStream(content);
        Object obj = decoder.parse(stream);

        byte[] result = encoder.encode(obj);
        assertThat(result, is(content));
    }
}
