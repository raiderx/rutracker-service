package org.karpukhin.rutracker;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

    @Test
    public void testLoginWithWrongCredentials() {
        boolean result = service.login("abcdef", "123456");
        assertFalse(result);
    }

    @Test
    public void testLoginWithCorrectCredentials() throws IOException{
        boolean result = service.login(username, password);
        assertTrue(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetTopicsWithWrongForumId() {
        service.getTopics(173778979, 70);
    }

    @Test
    public void testGetTopics() {
        List<Topic> result = service.getTopics(1737, 70);
        assertNotNull(result);
        assertEquals(70, result.size());
    }

    @Test(expected = ApplicationException.class)
    public void testGetTorrentWhenNotLoggedIn() throws IOException {
        service.getTorrent(4770508);
    }

    @Test
    public void testGetTorrent() throws IOException {
        boolean logged = service.login(username, password);
        assertTrue(logged);
        byte[] result = service.getTorrent(4770508);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
