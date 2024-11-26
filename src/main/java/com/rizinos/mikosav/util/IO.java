package com.rizinos.mikosav.util;

import org.bukkit.Bukkit;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class IO {
    private static String getcall(String rawUrl) throws Exception {
        StringBuilder builder = new StringBuilder();
        URL url = new URL(rawUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) builder.append(line).append("\n");
        reader.close();

        String result;
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
            result = builder.toString().equals("") ? "" : builder.substring(0, builder.length() - 1);
        else {
            result = "ERRINV";
            Bukkit.getLogger().warning("[" + rawUrl + "]: ERROR " + connection.getResponseCode());
        }
        connection.disconnect();
        return result;
    }

    private static String postcall(String rawUrl, String json) throws Exception {
        StringBuilder builder = new StringBuilder();
        URL url = new URL(rawUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(json);
        outputStream.flush();
        outputStream.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) builder.append(line).append("\n");
        reader.close();

        String result;
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
            result = builder.toString().equals("") ? "" : builder.substring(0, builder.length() - 1);
        else {
            result = "ERRINV";
            Bukkit.getLogger().warning("[" + rawUrl + "]: ERROR " + connection.getResponseCode());
        }
        connection.disconnect();
        return result;
    }

    public static String get(String rawUrl) {
        try {
            return getcall(rawUrl);
        } catch (Exception e) {
            Bukkit.getLogger().warning(e.toString());
            return "ERRINV";
        }
    }

    public static String post(String rawUrl, String json) {
        try {
            return postcall(rawUrl, json);
        } catch (Exception e) {
            Bukkit.getLogger().warning(e.toString());
            return "ERRINV";
        }
    }

    public static boolean exists(String url) {
        if (url.equals("")) return false;
        File file = new File(url);
        return file.exists();
    }

    public static boolean write(String url, String data) {
        File file = new File(url);
        if (!exists(url)) try {
            if (!file.createNewFile()) return false;
        } catch (IOException e) {
            return false;
        }
        try {
            FileWriter myWriter = new FileWriter(url);
            myWriter.write(data);
            myWriter.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean write(String url, String[] data) {
        return write(url, String.join("\n", data));
    }

    public static boolean delete(String url) {
        if (!exists(url)) return true;
        File file = new File(url);
        return file.delete();
    }

    private static String[] getContents(String url) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(url));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) builder.append(line).append("\n");
        reader.close();
        if (builder.toString().equals("")) return new String[0];
        return builder.substring(0, builder.length() - 1).split("\n");
    }

    public static String[] read(String url) {
        try {
            return getContents(url);
        } catch (Exception e) {
            return new String[0];
        }
    }

    public static boolean mkdir(String url) {
        if (exists(url)) return true;
        File file = new File(url);
        return file.mkdir();
    }
}
