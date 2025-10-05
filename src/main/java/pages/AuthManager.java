/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pages;

import javax.swing.SwingUtilities;

/**
 *
 * @author VICTUS
 */
public class AuthManager {
    public static String Token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGdtYWlsLmNvbSIsImlhdCI6MTc1OTY0NzUxMiwiZXhwIjoxNzU5NjQ3ODEyfQ.RP2acfLZ48ERHtK4eiffqoj5rfno64GzIBc1fs_J8fk";
    public static String Refresh = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyYkBnbWFpbC5jb20iLCJpYXQiOjE3NTk1MDY5NDgsImV4cCI6MTc2MDcxNjU0OH0.jt96ahpsJjy5rX3Eklp_gXL3KExlDFEcyp4fsfhw_7g";
    // client-side flag indicating whether the currently authenticated user is a
    // vendor
    public static boolean IsVendor = false;

    private static final String STATE_FILE = System.getProperty("user.home") + "/.shopsphere.properties";

    static {
        // load persisted state if available
        loadState();
    }

    public static synchronized void loadState() {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(STATE_FILE)) {
            java.util.Properties p = new java.util.Properties();
            p.load(fis);
            String v = p.getProperty("isVendor", "false");
            IsVendor = Boolean.parseBoolean(v);
        } catch (Exception ignored) {
            // missing file or read error -> default false
        }
    }

    public static synchronized void saveState() {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(STATE_FILE)) {
            java.util.Properties p = new java.util.Properties();
            p.setProperty("isVendor", Boolean.toString(IsVendor));
            p.store(fos, "ShopSphere client state");
        } catch (Exception ignored) {
            // ignore write errors for now
        }
    }

    /**
     * Fetch profile from server endpoints using current tokens and update IsVendor.
     * This is synchronous and should be called after login when Token is set.
     */
    public static synchronized void refreshProfile() {
        String[] endpoints = { "http://localhost:8080/auth/me", "http://localhost:8080/users/me" };
        for (String ep : endpoints) {
            try {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Accept", "application/json");
                if (Token != null && !Token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + Token);
                    if (Refresh != null && !Refresh.isEmpty())
                        headers.put("Refresh-Token", Refresh);
                }
                String body = HttpUtil.getString(ep, headers);
                org.json.JSONObject resp = new org.json.JSONObject(body);
                String role = resp.optString("role", resp.optString("userType", ""));
                if (role != null && !role.isEmpty()) {
                    IsVendor = role.equalsIgnoreCase("vendor");
                    saveState();
                    return;
                }
            } catch (Exception ignored) {
                // try next endpoint
            }
        }
    }

    /**
     * Async wrapper for refreshProfile that runs off the EDT and invokes onDone on
     * the EDT.
     */
    public static void refreshProfileAsync(Runnable onDone) {
        new Thread(() -> {
            try {
                refreshProfile();
            } catch (Throwable ignored) {
            }
            if (onDone != null)
                SwingUtilities.invokeLater(onDone);
        }, "AuthManager-refresh").start();
    }
}
