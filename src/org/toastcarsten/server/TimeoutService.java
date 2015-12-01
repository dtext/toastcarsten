package org.toastcarsten.server;

public class TimeoutService {
    /**
     * Starts a thread that periodically checks whether or not to remove the specified user.
     * It only wakes up when the supposed timeout of the user should happen.
     * @param u The user to check
     */
    public static Thread start(User u) {
        Thread thread = new Thread(() -> {
            long t = Long.MAX_VALUE;
            while (t > System.currentTimeMillis()) {
                t = u.getTimeout();
                try { Thread.sleep(t - System.currentTimeMillis() + 1); } catch (Exception e) { return; }
            }
            User.remove(u);
        });
        thread.start();
        return thread;
    }
}
