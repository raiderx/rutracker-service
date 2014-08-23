package org.karpukhin.bittorrent;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Karpukhin
 * @since 08.07.14
 */
public class BittorrentDecoder {

    static final String UTF8 = "UTF-8";

    private DecoderFactory factory = new DecoderFactoryImpl();

    public Object parse(InputStream stream) throws IOException {
        assertTrue(stream != null, "Parameter 'stream' can not be null");

        int symbol = stream.read();
        if (symbol != -1) {
            Object result = factory.getDecoder(symbol).decoder(symbol, stream);
            if (stream.read() != -1) {
                System.out.println("There are some unread characters in stream");
            }
            return result;
        }
        return null;
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    interface Decoder {

        Object decoder(int firstChar, InputStream stream) throws IOException;
    }

    interface DecoderFactory {

        Decoder getDecoder(int firstChar);
    }

    static class IntegerDecoder implements Decoder {

        @Override
        public Object decoder(int firstChar, InputStream stream) throws IOException {
            assertTrue(firstChar == 'i', "Expected 'i' but got " + String.valueOf(Character.toChars(firstChar)));

            long result = 0;
            boolean isPositive = true;
            int nextChar = stream.read();
            while (nextChar != 'e') {
                if (nextChar == -1) {
                    System.err.println("Unexpected end of stream");
                    return null;
                }
                if (nextChar == '-' && isPositive) {
                    isPositive = false;
                } else if (nextChar >= '0' && nextChar <= '9') {
                    result = result * 10 + (nextChar - '0');
                } else {
                    throw new IllegalArgumentException("Expected digit but got " + String.valueOf(Character.toChars(nextChar)));
                }

                nextChar = stream.read();
            }
            return isPositive ? result : -result;
        }
    }

    static class StringDecoder implements Decoder {

        @Override
        public Object decoder(int firstChar, InputStream stream) throws IOException {
            assertTrue(firstChar >= '0' && firstChar <= '9', "Expected digit but got " + String.valueOf(Character.toChars(firstChar)));

            int length = firstChar - '0';
            int nextChar = stream.read();
            while (nextChar >= '0' && nextChar <= '9') {
                length = length * 10 + (nextChar - '0');
                nextChar = stream.read();
            }
            if (nextChar == -1) {
                System.err.println("Unexpected end of stream");
                return null;
            }
            assertTrue(nextChar == ':', "Expected ':' but got " + String.valueOf(Character.toChars(nextChar)));

            if (length > 0) {
                byte[] buffer = new byte[length];
                int c = 0;
                while (c < length) {
                    int res = stream.read(buffer, c, length - c);
                    if (res == -1) {
                        System.err.println("Unexpected end of stream");
                        return null;
                    }
                    c += res;
                }
                return buffer;
            }
            return "";
        }
    }

    static class ListDecoder implements Decoder {

        DecoderFactory factory;

        ListDecoder(DecoderFactory factory) {
            this.factory = factory;
        }

        @Override
        public Object decoder(int firstChar, InputStream stream) throws IOException {
            assertTrue(firstChar == 'l', "Expected 'l' but got " + String.valueOf(Character.toChars(firstChar)));

            List result = new ArrayList();
            int nextChar = stream.read();
            while (nextChar != 'e') {
                if (nextChar == -1) {
                    System.err.println("Unexpected end of stream");
                    return null;
                }
                Object item = factory.getDecoder(nextChar).decoder(nextChar, stream);
                result.add(item);
                nextChar = stream.read();
            }
            return result;
        }
    }

    static class DictionaryDecoder implements Decoder {

        DecoderFactory factory;

        DictionaryDecoder(DecoderFactory factory) {
            this.factory = factory;
        }

        @Override
        public Object decoder(int firstChar, InputStream stream) throws IOException {
            assertTrue(firstChar == 'd', "Expected 'd' but got " + String.valueOf(Character.toChars(firstChar)));

            Map result = new LinkedHashMap();
            boolean expectedKey = true;
            String key = null;
            Object value = null;
            int nextChar = stream.read();
            while (nextChar != 'e') {
                if (nextChar == -1) {
                    System.err.println("Unexpected end of stream");
                    return null;
                }
                if (expectedKey) {
                    key = new String((byte[])new StringDecoder().decoder(nextChar, stream), UTF8);
                } else {
                    value = factory.getDecoder(nextChar).decoder(nextChar, stream);
                    result.put(key, value);
                }
                expectedKey = !expectedKey;
                nextChar = stream.read();
            }
            return result;
        }
    }

    static class DecoderFactoryImpl implements DecoderFactory {

        final Decoder integerDecoder;
        final Decoder stringDecoder;
        final Decoder listDecoder;
        final Decoder dictionaryDecoder;

        DecoderFactoryImpl() {
            integerDecoder = new IntegerDecoder();
            stringDecoder = new StringDecoder();
            listDecoder = new ListDecoder(this);
            dictionaryDecoder = new DictionaryDecoder(this);
        }

        @Override
        public Decoder getDecoder(int firstChar) {
            if (firstChar == 'i') {
                return integerDecoder;
            }
            if (firstChar == 'l') {
                return listDecoder;
            }
            if (firstChar == 'd') {
                return dictionaryDecoder;
            }
            if (firstChar >= '0' && firstChar <= '9') {
                return stringDecoder;
            }
            throw new IllegalArgumentException("Unexpected symbol " + String.valueOf(Character.toChars(firstChar)));
        }
    }
}
