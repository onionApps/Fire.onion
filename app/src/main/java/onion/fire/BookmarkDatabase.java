/*
 * Fire.onion
 *
 * https://play.google.com/store/apps/details?id=onion.fire
 * http://onionapps.github.io/Fire.onion/
 * http://github.com/onionApps/Fire.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.fire;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BookmarkDatabase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;

    Context context;

    public BookmarkDatabase(Context context) {
        super(context, "dbb", null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE bookmarks (" +
                "_id INTEGER PRIMARY KEY," +
                "url TEXT NOT NULL," +
                "title TEXT" +
                ");");
        addDefault(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("DROP TABLE IF EXISTS bookmarks");
        //onCreate(db);
        addDefault(db);
    }

    private void addDefault(SQLiteDatabase db) {
        String[][] defaultbookmarks = new String[][]{
                {"http://3g2upl4pq6kufc4m.onion/", "DuckDuckGo (.onion)"},
                {"https://blockchainbdgpzk.onion/", "Blockchain (.onion)"},
                {"https://facebookcorewwwi.onion/", "Facebook (.onion)"},
        };
        for (int i = defaultbookmarks.length - 1; i >= 0; i--) {
            saveBookmark(db, defaultbookmarks[i][0], defaultbookmarks[i][1]);
        }
    }

    private void saveBookmark(SQLiteDatabase db, String url, String title) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("url", url);
        db.insertOrThrow("bookmarks", null, values);
    }

    public void saveBookmark(String url, String title) {
        saveBookmark(getWritableDatabase(), url, title);
    }

    public void deleteBookmark(long id) {
        getWritableDatabase().delete("bookmarks", "_id=?", new String[]{"" + id});
    }

    private long getBookmarkId0(String url) {
        Cursor cursor = getReadableDatabase().query("bookmarks", null, "url=?", new String[]{url}, null, null, null);
        long id = -1;
        if (cursor.moveToNext()) {
            id = cursor.getLong(cursor.getColumnIndex("_id"));
        }
        cursor.close();
        return id;
    }

    public long getBookmarkId(String url) {
        long i;

        i = getBookmarkId0(url);
        if (i >= 0) {
            return i;
        }

        if (url.length() > 1 && url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);

            i = getBookmarkId0(url);
            if (i >= 0) {
                return i;
            }
        }

        return -1;
    }

    public Bookmark getBookmark(long id) {
        Bookmark bookmark = null;
        Cursor cursor = getReadableDatabase().query("bookmarks", null, "_id=?", new String[]{"" + id}, null, null, null);
        if (cursor.moveToNext()) {
            bookmark = new Bookmark();
            bookmark.url = cursor.getString(cursor.getColumnIndex("url"));
            bookmark.title = cursor.getString(cursor.getColumnIndex("title"));
        }
        cursor.close();
        return bookmark;
    }

    public Cursor getBookmarkCursor() {
        return getReadableDatabase().query("bookmarks", null, null, null, null, null, "_id desc");
    }

    public class Bookmark {
        public String url;
        public String title;
    }

}
