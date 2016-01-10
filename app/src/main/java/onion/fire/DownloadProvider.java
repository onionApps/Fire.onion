package onion.fire;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class DownloadProvider extends ContentProvider {

    String TAG = "DownloadProvider";

    void log(String s) {
        Log.i(TAG, s);
    }

    static DownloadProvider instance;

    public static DownloadProvider getInstance() {
        return instance;
    }

    public DownloadProvider() {
        log("ctor");
        instance = this;
    }

    @Override
    public boolean onCreate() {
        log("onCreate");
        return true;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        log("insert: " + uri);
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        log("update: " + uri);
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        log("delete: " + uri);
        return 0;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        log("query: " + uri);
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        log("getType: " + uri);
        return null;
    }

    SecureRandom secureRandom = new SecureRandom();
    Map<Uri, File> uriToFileMap = new HashMap<>();
    Map<File, Uri> fileToUriMap = new HashMap<>();

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        log("openFile: " + uri + ", " + mode);
        File file = uriToFileMap.get(uri);
        if (file == null) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    public Uri getUri(File file) {
        if (!fileToUriMap.containsKey(file)) {
            char[] aa = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+=".toCharArray();
            byte[] bb = new byte[32];
            secureRandom.nextBytes(bb);
            char[] cc = new char[bb.length];
            for (int i = 0; i < bb.length; i++) {
                cc[i] = aa[(bb[i] & 0xFF) % aa.length];
            }
            Uri uri = Uri.parse("content://onion.fire.downloadprovider/" + new String(cc) + "/" + file.getName());
            log("getUri: " + uri);
            fileToUriMap.put(file, uri);
            uriToFileMap.put(uri, file);
        }
        return fileToUriMap.get(file);
    }

}
