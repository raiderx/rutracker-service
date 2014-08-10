package org.karpukhin.bittorrent;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Pavel Karpukhin
 * @since 10.07.14
 */
public class BittorrentEncoderTest {

    private BittorrentEncoder encoder = new BittorrentEncoder();
    private BittorrentDecoder decoder = new BittorrentDecoder();

    @Test
    public void testEncodeInteger() {
        byte[] result = encoder.encode(-355);
        assertArrayEquals("i-355e".getBytes(), result);
    }

    @Test
    public void testEncodeLong() {
        byte[] result = encoder.encode(-256L);
        assertArrayEquals("i-256e".getBytes(), result);
    }

    @Test
    public void testEncodeString() {
        byte[] result = encoder.encode("string");
        assertArrayEquals("6:string".getBytes(), result);
    }

    @Test
    public void testEncodeBytes() {
        byte[] result = encoder.encode("string".getBytes());
        assertArrayEquals("6:string".getBytes(), result);
    }

    @Test
    public void testEncodeList() {
        byte[] result = encoder.encode(Arrays.asList(-25L, "string".getBytes()));
        assertArrayEquals("li-25e6:stringe".getBytes(), result);
    }

    @Test
    public void testEncodeDictionary() {
        Map map = new HashMap<>();
        map.put(-25L, "string");
        byte[] result = encoder.encode(map);
        assertArrayEquals("di-25e6:stringe".getBytes(), result);
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
        out = new ByteArrayOutputStream();
        encoder.encode(obj, out);

        byte[] result = out.toByteArray();
        assertArrayEquals(content, result);
    }
}
