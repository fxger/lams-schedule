package org.example.lamscommon.context;

public class BaseContext {

    public static ThreadLocal<Long> idThreadLocal = new ThreadLocal<>();
    public static ThreadLocal<String> usernameThreadLocal = new ThreadLocal<>();
    public static ThreadLocal<String> userTypeThreadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        idThreadLocal.set(id);
    }

    public static Long getCurrentId() {
        return idThreadLocal.get();
    }

    public static void setCurrentUsername(String username) {
        usernameThreadLocal.set(username);
    }

    public static String getCurrentUsername() {
        return usernameThreadLocal.get();
    }

    public static void setCurrentUserType(String userType) {
        userTypeThreadLocal.set(userType);
    }

    public static String getCurrentUserType() {
        return userTypeThreadLocal.get();
    }

    public static void removeCurrentId() {
        idThreadLocal.remove();
    }

    public static void removeCurrentUsername() {
        usernameThreadLocal.remove();
    }

    public static void removeCurrentUserType() {
        userTypeThreadLocal.remove();
    }

    public static void clearAll() {
        idThreadLocal.remove();
        usernameThreadLocal.remove();
        userTypeThreadLocal.remove();
    }
}