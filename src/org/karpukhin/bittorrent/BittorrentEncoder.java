package org.karpukhin.bittorrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Karpukhin
 * @since 10.07.14
 */
public class BittorrentEncoder {

    static final String UTF8 = "UTF-8";

    private EncoderFactory factory = new EncoderFactoryImpl();

    public byte[] encode(Object obj) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        encode(obj, stream);
        return stream.toByteArray();
    }

    public void encode(Object obj, OutputStream stream) {
        try {
            factory.getEncoder(obj).encode(obj, stream);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    interface Encoder {

        void encode(Object obj, OutputStream stream) throws IOException;
    }

    interface EncoderFactory {

        Encoder getEncoder(Object obj);
    }

    static class EncoderFactoryImpl implements EncoderFactory {

        Map<Class, Encoder> encoders = new HashMap<>();

        EncoderFactoryImpl() {
            encoders.put(Integer.class, new IntegerEncoder());
            encoders.put(Long.class, new IntegerEncoder());
            encoders.put(byte[].class, new StringEncoder());
            encoders.put(String.class, new StringEncoder());
            encoders.put(List.class, new ListEncoder(this));
            encoders.put(Map.class, new DictionaryEncoder(this));
        }

        @Override
        public Encoder getEncoder(Object obj) {
            assertNotNull(obj, "Parameter 'obj' can not be null");
            Encoder encoder = getEncoder(obj.getClass());
            assertNotNull(encoder, "Unexpected object type " + obj.getClass());
            return encoder;
        }

        Encoder getEncoder(Class clazz) {
            if (encoders.containsKey(clazz)) {
                return encoders.get(clazz);
            }
            for (Class clazz2 : clazz.getInterfaces()) {
                if (encoders.containsKey(clazz2)) {
                    return encoders.get(clazz2);
                }
            }
            return clazz.getSuperclass() != null ? getEncoder(clazz.getSuperclass()) : null;
        }
    }

    static class IntegerEncoder implements Encoder {

        @Override
        public void encode(Object obj, OutputStream stream) throws IOException {
            assertNotNull(obj, "Parameter 'obj' can not be null");
            assertTrue(obj instanceof Integer || obj instanceof Long, "Expected 'Integer' or 'Long' but got " + obj.getClass());

            stream.write('i');
            stream.write(String.valueOf(obj).getBytes(UTF8));
            stream.write('e');
        }
    }

    static class StringEncoder implements Encoder {

        @Override
        public void encode(Object obj, OutputStream stream) throws IOException {
            assertNotNull(obj, "Parameter 'obj' can not be null");
            assertTrue(obj instanceof String || obj instanceof byte[], "Expected 'String' or 'byte[]' but got " + obj.getClass());

            if (obj instanceof String) {
                String str = (String) obj;
                stream.write(String.valueOf(str.length()).getBytes());
                stream.write(':');
                stream.write(str.getBytes(UTF8));
            } else {
                byte[] bytes = (byte[]) obj;
                stream.write(String.valueOf(bytes.length).getBytes());
                stream.write(':');
                stream.write(bytes);
            }
        }
    }

    static class ListEncoder implements Encoder {

        EncoderFactory factory;

        ListEncoder(EncoderFactory factory) {
            this.factory = factory;
        }

        @Override
        public void encode(Object obj, OutputStream stream) throws IOException {
            assertNotNull(obj, "Parameter 'obj' can not be null");
            assertTrue(obj instanceof List, "Expected 'List' but got " + obj.getClass());

            List list = (List)obj;
            stream.write('l');
            for (Object item : list) {
                factory.getEncoder(item).encode(item, stream);
            }
            stream.write('e');
        }
    }

    static class DictionaryEncoder implements Encoder {

        EncoderFactory factory;

        DictionaryEncoder(EncoderFactory factory) {
            this.factory = factory;
        }

        @Override
        public void encode(Object obj, OutputStream stream) throws IOException {
            assertNotNull(obj, "Parameter 'obj' can not be null");
            assertTrue(obj instanceof Map, "Expected 'Map' but got " + obj.getClass());

            Map map = (Map)obj;
            stream.write('d');
            for (Object entry : map.entrySet()) {
                factory.getEncoder(((Map.Entry)entry).getKey()).encode(((Map.Entry) entry).getKey(), stream);
                factory.getEncoder(((Map.Entry)entry).getValue()).encode(((Map.Entry)entry).getValue(), stream);
            }
            stream.write('e');
        }
    }
}
