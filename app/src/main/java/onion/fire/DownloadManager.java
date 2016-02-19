/*
 * Fire.onion
 *
 * http://play.google.com/store/apps/details?id=onion.fire
 * http://onionapps.github.io/Fire.onion/
 * http://github.com/onionApps/Fire.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.fire;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadManager {

    static String TAG = "DownloadManager";

    private DownloadManager(Context context) {
        this.context = context;
        loadDownloads();
    }

    private Context context;
    private static DownloadManager instance;

    public static DownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadManager(context);
        }
        return instance;
    }

    private File getExternalDir() {
        File f = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        Log.i(TAG, "getExternalDir " + f);
        return f;
    }

    private File getInternalDir() {
        File f = new File(context.getFilesDir(), "Download");
        Log.i(TAG, "getInternalDir " + f);
        return f;
    }

    private File getDir() {

        if(!Settings.getPrefs(context).getBoolean("externalstorage", true)) {
            return getInternalDir();
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT || !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return getInternalDir();
        }

        return getExternalDir();
    }

    private ArrayList<Download> downloads = new ArrayList<>();

    public List<Download> getDownloads() {
        return downloads;
    }

    private File[] listFiles(File dir) {
        if (dir == null) {
            return new File[0];
        }
        File[] ff = dir.listFiles();
        if (ff == null) {
            return new File[0];
        }
        return ff;
    }

    private void loadDownloads() {
        downloads.clear();
        for (File f : listFiles(getExternalDir())) {
            downloads.add(new Download(f));
        }
        for (File f : listFiles(getInternalDir())) {
            downloads.add(new Download(f));
        }
        Collections.sort(downloads, new Comparator<Download>() {
            @Override
            public int compare(Download lhs, Download rhs) {
                return Long.valueOf(lhs.getFile().lastModified()).compareTo(rhs.getFile().lastModified());
            }
        });
    }

    public void startDownload(Tor tor, String url) {
        downloads.add(new Download(tor, url));
        DownloadView.update();
    }

    public void removeDownload(Download download) {
        download.getFile().delete();
        downloads.remove(download);
    }

    public class Download {
        String error = null;
        File file;
        ProxySocket proxySocket;
        long progress = 0;
        long size = -1;
        long date = 0;

        public String getError() {
            return error;
        }

        public boolean isExternal() {
            Log.i(TAG, "isExternal: " + file);
            File extdir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            Log.i(TAG, "isExternal: " + extdir);
            if (extdir == null) return false;
            return file.toString().startsWith(extdir.toString());
        }

        public File getFile() {
            return file;
        }

        public long getProgress() {
            return progress;
        }

        public long getSize() {
            return size;
        }

        private void init(File file) {
            this.file = file;
            long s = file.length();
            progress = s;
            size = s;
            date = file.lastModified();
        }

        private void fname(String name) {
            file = new File(getDir(), name);
            for (int i = 0; file.exists(); i++) {
                String n = name;
                String e = "";
                int ci = name.lastIndexOf('.');
                if (ci > 0) {
                    n = name.substring(0, ci);
                    e = name.substring(ci);
                }
                file = new File(getDir(), n + "-" + i + e);
            }
        }

        public Download(File file) {
            init(file);
        }

        public Download(final Tor tor, final String url) {
            fname("file");
            date = System.currentTimeMillis();
            new Thread() {
                @Override
                public void run() {
                    getDir().mkdirs();
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    try {
                        proxySocket = new ProxySocket(url, tor.getProxyHost(), tor.getProxyPort());

                        String name = "file";

                        {
                            String n = Uri.parse(url).getLastPathSegment();
                            if (n != null && n.length() > 2) {
                                name = n;
                            }
                        }

                        Map<String, String> headers = proxySocket.getHeaders();
                        if (headers.containsKey("Content-Disposition")) {
                            String value = headers.get("Content-Disposition");
                            int i = value.indexOf("filename=");
                            if (i > 0) {
                                name = value.substring(i + "filename=".length());
                                name = name.replace("\"", "");
                                name = name.replace("'", "");
                                name = name.replace(" ", "");
                                name = name.replace("=", "");
                            }
                        }

                        if (name.startsWith(".")) {
                            name = name.substring(1);
                        }

                        if (name.isEmpty()) {
                            name = "file";
                        }

                        String mime = proxySocket.getMimeType();
                        if (mime != null && !mime.isEmpty() && !name.contains(".")) {
                            int si = mime.indexOf(';');
                            if (si > 0) {
                                mime = mime.substring(0, si);
                            }
                            Log.i(TAG, "mime " + mime);
                            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
                            Log.i(TAG, "ext " + ext);
                            if (ext != null) {
                                name += "." + ext;
                            }
                        }

                        fname(name);

                        size = proxySocket.getContentLength();
                        progress = 0;
                        inputStream = proxySocket.getInputStream();
                        outputStream = new FileOutputStream(file);
                        byte[] buffer = new byte[1024 * 16];
                        while (true) {
                            int n = inputStream.read(buffer);
                            if (n <= 0) break;
                            outputStream.write(buffer, 0, n);
                            progress += (long) n;
                            DownloadView.update();
                        }
                    } catch (FileNotFoundException ex) {
                        error = "Can't write file";
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        //error = ex.getMessage();
                        error = ex.getClass().getName();
                        Matcher m = Pattern.compile("[A-Z][a-z]+").matcher(error);
                        error = "";
                        while (m.find()) {
                            error += m.group() + " ";
                        }
                        error = error.replace(" Exception ", " ");
                        error = error.trim();
                    }
                    if (proxySocket != null) {
                        try {
                            proxySocket.getInputStream().close();
                        } catch (Exception e) {
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e) {
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Exception e) {
                        }
                    }
                    if (proxySocket != null) {
                        try {
                            proxySocket.close();
                        } catch (Exception e) {
                        }
                    }
                    proxySocket = null;
                    init(file);
                    DownloadView.update();
                }
            }.start();
        }
    }

}
