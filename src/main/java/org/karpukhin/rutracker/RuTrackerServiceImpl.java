package org.karpukhin.rutracker;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.PrettyHtmlSerializer;
import org.karpukhin.util.AssertUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * @author Pavel Karpukhin
 * @since 14.07.14
 */
public class RuTrackerServiceImpl implements RuTrackerService {

    static final int BUFFER_SIZE = 8192;

    static final String CP1251 = "CP1251";

    static final String CONTENT_TYPE = "Content-Type";
    static final String CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded";
    static final String USER_AGENT = "User-Agent";
    static final String USER_AGENT_VALUE = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.125 Safari/537.36";
    static final String ACCEPT = "Accept";
    static final String ACCEPT_VALUE = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
    static final String ACCEPT_ENCODING = "Accept-Encoding";
    static final String ACCEPT_ENCODING_VALUE = "gzip,deflate,sdch";
    static final String ACCEPT_LANGUAGE = "Accept-Language";
    static final String ACCEPT_LANGUAGE_VALUE = "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4,ms;q=0.2";
    static final String REFERER = "Referer";
    static final String REFERER_VALUE = "http://rutracker.org/forum/index.php";
    static final String COOKIE = "Cookie";

    static final String LOCATION = "Location";
    static final String CONTENT_ENCODING = "Content-Encoding";
    static final String SET_COOKIE = "Set-Cookie";

    static final String PROFILE_XPATH = "//div[@class=\"topmenu\"]/table/tbody/tr/td";
    static final String TOPIC_XPATH = "//table[@class=\"forumline forum\"]/tbody/tr[@id]";
    static final String MESSAGE_XPATH = "//table[@class=\"forumline message\"]/tbody/tr/td/div";
    static final String LOGIN_MESSAGE_XPATH = "//form[@id=\"login-form\"]/table[@class=\"forumline\"]/tbody/tr/td/h4";

    static final String INDEX_URL = "http://rutracker.org/forum/index.php";
    static final String LOGIN_URL = "http://login.rutracker.org/forum/login.php";
    static final String FORUM_URL_FORMAT = "http://rutracker.org/forum/viewforum.php?f=%d";
    static final String FORUM_URL_FORMAT_WITH_START = "http://rutracker.org/forum/viewforum.php?f=%d&start=%d";
    static final String TOPIC_URL_FORMAT = "http://rutracker.org/forum/viewtopic.php?t=%d";
    static final String TORRENT_URL_FORMAT = "http://dl.rutracker.org/forum/dl.php?t=%d";

    private final HtmlCleaner cleaner = new HtmlCleaner();
    private String cookies;

    @Override
    public boolean login(String username, String password) {
        AssertUtils.assertTrue(username != null, "Parameter 'username' is required");
        AssertUtils.assertTrue(password != null, "Parameter 'password' is required");

        String query = new StringBuilder()
                .append("login_username=").append(username)
                .append("&login_password=").append(password)
                .append("&login=%C2%F5%EE%E4").toString();
        URL url = getUrl(LOGIN_URL);
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
            connection.setRequestProperty(CONTENT_TYPE, CONTENT_TYPE_VALUE);
            connection.setRequestProperty(ACCEPT, ACCEPT_VALUE);
            connection.setRequestProperty(ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE);
            connection.setRequestProperty(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE);
            connection.setRequestProperty(REFERER, REFERER_VALUE);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(query.getBytes(CP1251));
            outputStream.close();

            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                cookies = connection.getHeaderField(SET_COOKIE);
                return true;
            }
            TagNode root = cleaner.clean(getInputStream(connection), CP1251);

            Object[] h4s = getTagNodes(root, LOGIN_MESSAGE_XPATH);
            if (h4s.length > 0) {
                assertTrue(h4s[0] instanceof TagNode, "Expected TagNode but got " + h4s[0].getClass());
                throw new AuthorizationException(((TagNode)h4s[0]).getText().toString());
            }
            System.err.println(new PrettyHtmlSerializer(new CleanerProperties()).getAsString(root));
            throw new ApplicationException("Unknown state");
        } catch (IOException e) {
            throw new ApplicationException("Error while reading", e);
        }
    }

    @Override
    public boolean isLoggedIn() {
        URL url = getUrl(INDEX_URL);
        TagNode root;
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
            connection.setRequestProperty(ACCEPT, ACCEPT_VALUE);
            connection.setRequestProperty(ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE);
            connection.setRequestProperty(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE);
            connection.setRequestProperty(REFERER, REFERER_VALUE);
            if (cookies != null) {
                connection.setRequestProperty(COOKIE, cookies);
            }
            InputStream stream = getInputStream(connection);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                System.err.println(connection.getResponseMessage());
                log(stream, System.err);
                throw new ApplicationException("Unexpected response code " + connection.getResponseCode());
            }
            root = cleaner.clean(stream, CP1251);
        } catch (IOException e) {
            throw new ApplicationException("Error while reading", e);
        }
        Object[] tds = getTagNodes(root, PROFILE_XPATH);
        assertTrue(tds.length == 1 || tds.length == 3, "Expected 1 or 3 tds but got " + tds.length);
        return tds.length == 3;
    }

    @Override
    public List<Topic> getTopics(int forumId, int maxCount) {
        AssertUtils.assertTrue(forumId > 0, "Parameter 'forumId' is wrong: " + forumId);
        AssertUtils.assertTrue(maxCount >= 0, "Parameter 'maxCount' is wrong: " + maxCount);

        URL url;
        List<Topic> topics = new ArrayList<>();
        while (topics.size() < maxCount) {
            url = getUrl(getForumUrl(forumId, topics.size()));
            TagNode root;
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setUseCaches(false);
                connection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
                connection.setRequestProperty(ACCEPT, ACCEPT_VALUE);
                connection.setRequestProperty(ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE);
                connection.setRequestProperty(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE);
                connection.setRequestProperty(REFERER, REFERER_VALUE);
                InputStream stream = getInputStream(connection);
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    System.err.println(connection.getResponseMessage());
                    log(stream, System.err);
                    break;
                }
                root = cleaner.clean(stream, CP1251);
            } catch (IOException e) {
                throw new ApplicationException("Error while reading", e);
            }
            Object[] divs = getTagNodes(root, MESSAGE_XPATH);
            if (divs.length > 0) {
                assertTrue(divs[0] instanceof TagNode, "Expected TagNode but got " + divs[0].getClass());
                throw new IllegalArgumentException(((TagNode)divs[0]).getText().toString());
            }
            Object[] trs = getTagNodes(root, TOPIC_XPATH);
            for (Object tr : trs) {
                assertTrue(tr instanceof TagNode, "Expected TagNode but got " + tr.getClass());

                TagNode[] tds = ((TagNode)tr).getElementsByName("td", false);

                assertTrue(tds.length == 5, "'tr' tag element contain less than 5 'td' tags");

                Topic topic = isLoggedIn() ? parseLoggedIn(tds) : parseLoggedOff(tds);
                topics.add(topic);
                if (topics.size() >= maxCount) {
                    break;
                }
            }
        }
        return topics;
    }

    @Override
    public byte[] getTorrent(int topicId) {
        URL url = getUrl(getTorrentUrl(topicId));
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
            connection.setRequestProperty(CONTENT_TYPE, CONTENT_TYPE_VALUE);
            connection.setRequestProperty(ACCEPT, ACCEPT_VALUE);
            connection.setRequestProperty(ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE);
            connection.setRequestProperty(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE);
            connection.setRequestProperty(REFERER, getTopicUrl(topicId));
            if (cookies != null) {
                connection.setRequestProperty(COOKIE, cookies);
            }
            connection.setInstanceFollowRedirects(false);

            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                String location = connection.getHeaderField(LOCATION);
                if (location != null && location.startsWith(LOGIN_URL)) {
                    throw new AuthorizationException("Your are not logged in");
                }
                throw new ApplicationException("Redirected to " + connection.getHeaderField(LOCATION));
            }

            InputStream stream = getInputStream(connection);
            String contentType = connection.getHeaderField(CONTENT_TYPE);
            if (contentType != null && contentType.contains("application/x-bittorrent")) {
                return byteArrayFromStream(stream);
            }
            TagNode root = cleaner.clean(stream, CP1251);
            Object[] divs = getTagNodes(root, MESSAGE_XPATH);
            if (divs.length > 0) {
                assertTrue(divs[0] instanceof TagNode, "Expected TagNode but got " + divs[0].getClass());
                throw new IllegalArgumentException(((TagNode)divs[0]).getText().toString());
            }
            System.err.println(new PrettyHtmlSerializer(new CleanerProperties()).getAsString(root));
            throw new ApplicationException("Your are not logged in");
        } catch (IOException e) {
            throw new ApplicationException("Error while reading", e);
        }
    }

    static String getForumUrl(int forumId) {
        return String.format(FORUM_URL_FORMAT, forumId);
    }

    static String getForumUrl(int forumId, int start) {
        return String.format(FORUM_URL_FORMAT_WITH_START, forumId, start);
    }

    static String getTopicUrl(int topicId) {
        return String.format(TOPIC_URL_FORMAT, topicId);
    }

    static String getTorrentUrl(int topicId) {
        return String.format(TORRENT_URL_FORMAT, topicId);
    }

    static URL getUrl(String url) {
        assertTrue(url != null, "Parameter 'url' is required");

        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new ApplicationException("Bad URL " + url, e);
        }
    }

    static InputStream getInputStream(HttpURLConnection connection) throws IOException {
        String contentEncoding = connection.getHeaderField(CONTENT_ENCODING);
        if (contentEncoding == null) {
            return connection.getInputStream();
        }
        if ("gzip".equals(contentEncoding)) {
            return new GZIPInputStream(connection.getInputStream());
        }
        throw new ApplicationException("Unexpected content encoding: " + contentEncoding);
    }

    static Object[] getTagNodes(TagNode root, String xPathExpression) {
        assertTrue(root != null, "Parameter 'root' is required");
        assertTrue(xPathExpression != null, "Parameter 'xPathExpression' is required");

        try {
            return root.evaluateXPath(xPathExpression);
        } catch (XPatherException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    static byte[] byteArrayFromStream(InputStream stream) throws IOException {
        assertTrue(stream != null, "Parameter 'stream' is required");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int res;
        while ((res = stream.read(buffer)) != -1) {
            out.write(buffer, 0, res);
        }
        return out.toByteArray();
    }

    Topic parseLoggedIn(TagNode[] tds) {
        assertTrue(tds != null, "Parameter 'tds' is required");

        Topic topic = new Topic();
        return topic;
    }

    Topic parseLoggedOff(TagNode[] tds) {
        assertTrue(tds != null, "Parameter 'tds' is required");

        Topic topic = new Topic();
        TagNode a = tds[1].getElementsByName("a", false)[0];
        topic.setId(Integer.valueOf(tds[0].getAttributeByName("id")));
        topic.setName(a.getText().toString().trim().replace("<wbr></wbr>", ""));
        topic.setUrl(a.getAttributeByName("href"));
        topic.setSize(tds[2].getText().toString().trim().replace("&nbsp;", " "));
        return topic;
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new ApplicationException(message);
        }
    }

    static void log(InputStream in) {
        log(in, System.out);
    }

    static void log(InputStream in, PrintStream out) {
        assertTrue(in != null, "Parameter 'in' is required");
        assertTrue(out != null, "Parameter 'out' is required");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, CP1251))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
        } catch (UnsupportedEncodingException e) {
            throw new ApplicationException("Unsupported encoding " + CP1251, e);
        } catch (IOException e) {
            throw new ApplicationException("Error while reading", e);
        }
    }
}
