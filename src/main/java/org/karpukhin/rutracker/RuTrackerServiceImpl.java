package org.karpukhin.rutracker;

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

/**
 * @author Pavel Karpukhin
 * @since 14.07.14
 */
public class RuTrackerServiceImpl implements RuTrackerService {

    static final String CP1251 = "CP1251";
    static final String CONTENT_TYPE = "Content-Type";
    static final String CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded";
    static final String USER_AGENT = "User-Agent";
    static final String USER_AGENT_VALUE = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.125 Safari/537.36";
    static final String ACCEPT = "Accept";
    //static final String ACCEPT_VALUE = "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2";
    static final String ACCEPT_VALUE = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
    static final String ACCEPT_ENCODING = "Accept-Encoding";
    static final String ACCEPT_ENCODING_VALUE = "gzip,deflate,sdch\n";
    static final String ACCEPT_LANGUAGE = "Accept-Language";
    static final String ACCEPT_LANGUAGE_VALUE = "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4,ms;q=0.2\n";
    static final String REFERER = "Referer";
    static final String REFERER_VALUE = "http://rutracker.org/forum/index.php";

    static final String TOPIC_XPATH = "//table[@class=\"forumline forum\"]/tbody/tr[@id]";
    static final String MESSAGE_XPATH = "//table[@class=\"forumline message\"]/tbody/tr/td/div";
    static final String CAPTCHA_XPATH = "//form[@id=\"login-form\"]/table[@class=\"forumline\"]";

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
        URL url;
        try {
            url = new URL(LOGIN_URL);
        } catch (MalformedURLException e) {
            throw new ApplicationException("Bad URL " + LOGIN_URL, e);
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
                cookies = connection.getHeaderField("Set-Cookie");
                return true;
            }
            TagNode root = cleaner.clean(connection.getInputStream(), CP1251);
            Object[] tables;
            try {
                tables = root.evaluateXPath(CAPTCHA_XPATH);
            } catch (XPatherException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
            if (tables.length > 0) {
                throw new ApplicationException("Captcha is required");
            }
            System.err.println(root.getText());
            throw new ApplicationException("Unknown state");
        } catch (IOException e) {
            throw new ApplicationException("Error while reading", e);
        }
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public List<Topic> getTopics(int forumId, int maxCount) {
        AssertUtils.assertTrue(forumId > 0, "Parameter 'forumId' is wrong: " + forumId);
        AssertUtils.assertTrue(maxCount >= 0, "Parameter 'maxCount' is wrong: " + maxCount);

        URL url;
        List<Topic> topics = new ArrayList<>();
        while (topics.size() < maxCount) {
            try {
                url = new URL(getForumUrl(forumId, topics.size()));
            } catch (MalformedURLException e) {
                throw new ApplicationException("Bad URL " + getForumUrl(forumId, topics.size()), e);
            }
            TagNode root;
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
                connection.setRequestProperty(ACCEPT, ACCEPT_VALUE);
                connection.setRequestProperty(ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE);
                connection.setRequestProperty(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE);
                connection.setRequestProperty(REFERER, REFERER_VALUE);
                InputStream stream = connection.getInputStream();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    System.err.println(connection.getResponseMessage());
                    log(stream, System.err);
                    break;
                }
                root = cleaner.clean(stream, CP1251);
            } catch (IOException e) {
                throw new ApplicationException("Error while reading", e);
            }
            Object[] divs;
            try {
                divs = root.evaluateXPath(MESSAGE_XPATH);
            } catch (XPatherException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
            if (divs.length > 0) {
                if (!(divs[0] instanceof TagNode)) {
                    throw new ApplicationException("Expected TagNode but got " + divs[0].getClass());
                }
                throw new IllegalArgumentException(((TagNode)divs[0]).getText().toString());
            }
            Object[] trs;
            try {
                trs = root.evaluateXPath(TOPIC_XPATH);
            } catch (XPatherException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
            for (Object tr : trs) {
                if (!(tr instanceof TagNode)) {
                    throw new ApplicationException("Expected TagNode but got " + tr.getClass());
                }
                TagNode[] tds = ((TagNode)tr).getElementsByName("td", false);
                if (tds.length != 5) {
                    throw new ApplicationException("'tr' tag element contain less than 5 'td' tags");
                }
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
        URL url;
        try {
            url = new URL(getTorrentUrl(topicId));
        } catch (MalformedURLException e) {
            throw new ApplicationException("Bad URL " + getTorrentUrl(topicId), e);
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
            connection.setRequestProperty(CONTENT_TYPE, CONTENT_TYPE_VALUE);
            connection.setRequestProperty(ACCEPT, ACCEPT_VALUE);
            connection.setRequestProperty(ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE);
            connection.setRequestProperty(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE);
            connection.setRequestProperty(REFERER, getTopicUrl(topicId));
            if (cookies != null) {
                connection.setRequestProperty("Cookie", cookies);
            }
            connection.setInstanceFollowRedirects(false);

            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                if (LOGIN_URL.equals(connection.getHeaderField("Location"))) {
                    throw new ApplicationException("Your are not logged in");
                }
                throw new ApplicationException("Redirected to " + connection.getHeaderField("Location"));
            }

            InputStream stream = connection.getInputStream();
            if (connection.getHeaderField(CONTENT_TYPE).contains("application/x-bittorrent")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int res;
                while ((res = stream.read(buffer)) != -1) {
                    out.write(buffer, 0, res);
                }
                return out.toByteArray();
            }
            TagNode root = cleaner.clean(stream, CP1251);
            Object[] divs;
            try {
                divs = root.evaluateXPath(MESSAGE_XPATH);
            } catch (XPatherException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
            if (divs.length > 0) {
                if (!(divs[0] instanceof TagNode)) {
                    throw new ApplicationException("Expected TagNode but got " + divs[0].getClass());
                }
                throw new IllegalArgumentException(((TagNode)divs[0]).getText().toString());
            }
            System.err.println(root.getText());
            throw new ApplicationException("Your are not logged in");
        } catch (IOException e) {
            throw new ApplicationException("Error while reading", e);
        }
    }

    String getForumUrl(int forumId) {
        return String.format(FORUM_URL_FORMAT, forumId);
    }

    String getForumUrl(int forumId, int start) {
        return String.format(FORUM_URL_FORMAT_WITH_START, forumId, start);
    }

    String getTopicUrl(int topicId) {
        return String.format(TOPIC_URL_FORMAT, topicId);
    }

    String getTorrentUrl(int topicId) {
        return String.format(TORRENT_URL_FORMAT, topicId);
    }

    Topic parseLoggedIn(TagNode[] tds) {
        AssertUtils.assertTrue(tds != null, "Parameter 'tds' is required");
        Topic topic = new Topic();
        return topic;
    }

    Topic parseLoggedOff(TagNode[] tds) {
        AssertUtils.assertTrue(tds != null, "Parameter 'tds' is required");
        Topic topic = new Topic();
        TagNode a = tds[1].getElementsByName("a", false)[0];
        topic.setId(Integer.valueOf(tds[0].getAttributeByName("id")));
        topic.setName(a.getText().toString().trim().replace("<wbr></wbr>", ""));
        topic.setUrl(a.getAttributeByName("href"));
        topic.setSize(tds[2].getText().toString().trim().replace("&nbsp;", " "));
        return topic;
    }

    static void log(InputStream in) {
        log(in, System.out);
    }

    static void log(InputStream in, PrintStream out) {
        AssertUtils.assertTrue(in != null, "Parameter 'stream' is required");

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
