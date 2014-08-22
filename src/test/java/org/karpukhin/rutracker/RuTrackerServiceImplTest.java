package org.karpukhin.rutracker;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Karpukhin
 * @since 14.07.14
 */
public class RuTrackerServiceImplTest {

    private RuTrackerService service;
    private String username;
    private String password;

    @Before
    public void setup() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream("/rutracker.properties"));
        username = properties.getProperty("username");
        password = properties.getProperty("password");
        service = new RuTrackerServiceImpl();
    }

    @Test(expected = AuthorizationException.class)
    public void testLoginWithWrongCredentials() {
        service.login("abcdef", "123456");
    }

    @Test
    public void testLoginWithCorrectCredentials() throws IOException{
        boolean result = service.login(username, password);
        assertThat(result, is(equalTo(true)));
    }

    @Test
    public void testIsLoggedInWhenNotLoggedIn() {
        boolean result = service.isLoggedIn();
        assertThat(result, is(equalTo(false)));
    }

    @Test
    public void testIsLoggedInWhenLoggedIn() {
        boolean logged = service.login(username, password);
        assertThat(logged, is(equalTo(true)));
        boolean result = service.isLoggedIn();
        assertThat(result, is(equalTo(true)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetTopicsWithWrongForumId() {
        service.getTopics(173778979, 70);
    }

    @Test
    public void testGetTopics() {
        List<Topic> result = service.getTopics(1737, 70);
        assertThat(result, is(not(nullValue())));
        assertThat(result.size(), is(equalTo(70)));
    }

    @Test(expected = AuthorizationException.class)
    public void testGetTorrentWhenNotLoggedIn() throws IOException {
        service.getTorrent(4770508);
    }

    @Test
    public void testGetTorrent() throws IOException {
        boolean logged = service.login(username, password);
        assertThat(logged, is(equalTo(true)));
        byte[] result = service.getTorrent(4770508);
        assertThat(result, is(not(nullValue())));
        assertTrue(result.length > 0);
    }
}
