package org.tasknavi.service;

import org.tasknavi.model.User;
import org.tasknavi.persistence.UserDao;

/**
 * ログイン中の人。アプリ全体で1人だけ持つ。
 */
public class UserSession {

    private static final Object SESSION_LOCK = new Object();
    private static final UserDao userDao = new UserDao();
    private static volatile User currentUser = null;

    public static void login(User user) {
        synchronized (SESSION_LOCK) {
            currentUser = user;
        }
    }

    public static void login(String username) {
        if (username == null || username.trim().isEmpty()) {
            logout();
            return;
        }
        User user = userDao.findByUsername(username);
        login(user);
    }

    public static void logout() {
        synchronized (SESSION_LOCK) {
            currentUser = null;
        }
    }

    public static User getCurrentUser() {
        synchronized (SESSION_LOCK) {
            return currentUser;
        }
    }

    public static String getCurrentUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    public static boolean isLoggedIn() {
        synchronized (SESSION_LOCK) {
            return currentUser != null;
        }
    }
}
