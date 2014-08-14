package org.karpukhin.rutracker;

import java.util.List;

/**
 * @author Pavel Karpukhin
 * @since 14.07.14
 */
public interface RuTrackerService {

    boolean login(String username, String password);

    boolean isLoggedIn();

    List<Topic> getTopics(int forumId, int start);

    byte[] getTorrent(int topicId);
}
