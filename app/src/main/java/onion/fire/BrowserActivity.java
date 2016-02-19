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

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.gecko.EventDispatcher;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoEvent;
import org.mozilla.gecko.GeckoView;
import org.mozilla.gecko.GeckoViewChrome;
import org.mozilla.gecko.GeckoViewContent;
import org.mozilla.gecko.PrefsHelper;
import org.mozilla.gecko.Tab;
import org.mozilla.gecko.Tabs;
import org.mozilla.gecko.ThumbnailHelper;
import org.mozilla.gecko.util.Clipboard;
import org.mozilla.gecko.util.EventCallback;
import org.mozilla.gecko.util.GeckoEventListener;
import org.mozilla.gecko.util.HardwareUtils;
import org.mozilla.gecko.util.NativeEventListener;
import org.mozilla.gecko.util.NativeJSObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class BrowserActivity extends ActionBarActivity {

    static Tor tor;
    static private BrowserActivity instance;
    String TAG = "BrowserActivity";
    EditText editUri;
    BrowserView geckoView;
    BookmarkDatabase bookmarkDatabase;
    //String startpage = homepage();
    TextView logView;
    View progressBar;
    ArrayList<String> actions = new ArrayList<>();
    String selectionID;
    long downloadWaitTime = 0;

    String startpage;
    Object textSelection;
    boolean skipcleanup = false;
    Cursor bookmarkCursor;
    Timer timer;

    public static BrowserActivity getInstance() {
        return instance;
    }

    String homepage() {
        return Settings.getPrefs(this).getString("homepage", "");
    }

    void log(String s) {
        Log.i(TAG, s);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        skipcleanup = false;

        instance = this;

        Prefs.init(this);

        Prefs.setPrefs(this);

        super.onCreate(savedInstanceState);

        final boolean init = getIntent() == null || getIntent().getBooleanExtra("init", true);

        if (init) {
            tor = new Tor(this);
        }

        if (init) {
            cleanup();
        }

        setContentView(R.layout.activity_browser);


        Clipboard.init(this);
        HardwareUtils.init(this);

        bookmarkDatabase = new BookmarkDatabase(this);

        progressBar = findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.GONE);

        editUri = (EditText) findViewById(R.id.editUri);

        geckoView = (BrowserView) findViewById(R.id.geckoView);

        findViewById(R.id.loading).setVisibility(View.VISIBLE);

        final View menuButton = findViewById(R.id.menuButton);
        menuButton.setClickable(true);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });
        menuButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).playSoundEffect(2);
                    showMenu();
                }
                return false;
            }
        });
        menuButton.setSoundEffectsEnabled(false);

        Prefs.setPrefs(this);

        final Context c = this;


        geckoView.setChromeDelegate(new GeckoViewChrome() {


            @Override
            public void onPrompt(GeckoView geckoView, GeckoView.Browser browser, String message, String defaultValue, final GeckoView.PromptResult promptResult) {

                SharedPreferences p = Settings.getPrefs(BrowserActivity.this);
                if (!p.getBoolean("javascript", true)) {
                    promptResult.cancel();
                    return;
                }

                try {

                    if (message == null) message = "";
                    if (defaultValue == null) defaultValue = "";

                    View view = getLayoutInflater().inflate(R.layout.prompt, null);
                    TextView messageView = (TextView) view.findViewById(R.id.message);
                    final TextView valueView = (TextView) view.findViewById(R.id.value);
                    messageView.setText(message);
                    if (message.isEmpty()) messageView.setVisibility(View.GONE);
                    valueView.setText(defaultValue);

                    Dialog diag = new AlertDialog.Builder(c)
                            .setTitle(new URL(getCurrentUrl()).getHost())
                            .setView(view)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.i("prompt", "ok");
                                    promptResult.confirmWithValue(valueView.getText().toString());
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.i("prompt", "cancel");
                                    promptResult.cancel();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Log.i("prompt", "cancel");
                                    promptResult.cancel();
                                }
                            })
                            .create();
                    diag.setCanceledOnTouchOutside(true);
                    diag.show();
                } catch (Exception ex) {
                    promptResult.cancel();
                }

            }

            @Override
            public void onAlert(final GeckoView geckoView, final GeckoView.Browser browser, final String s, final GeckoView.PromptResult promptResult) {

                SharedPreferences p = Settings.getPrefs(BrowserActivity.this);
                if (!p.getBoolean("javascript", true)) {
                    promptResult.cancel();
                    return;
                }

                try {
                    Dialog diag = new AlertDialog.Builder(c)
                            .setTitle(new URL(getCurrentUrl()).getHost())
                            .setMessage(s)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.i("alert", "confirm");
                                    promptResult.confirm();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Log.i("alert", "cancel");
                                    promptResult.cancel();
                                }
                            })
                            .create();
                    diag.setCanceledOnTouchOutside(true);
                    diag.show();
                } catch (Exception ex) {
                    promptResult.cancel();
                }
            }

            @Override
            public void onConfirm(final GeckoView geckoView, final GeckoView.Browser browser, final String s, final GeckoView.PromptResult promptResult) {

                SharedPreferences p = Settings.getPrefs(BrowserActivity.this);
                if (!p.getBoolean("javascript", true)) {
                    promptResult.cancel();
                    return;
                }

                try {
                    Dialog diag = new AlertDialog.Builder(c)
                            .setTitle(new URL(getCurrentUrl()).getHost())
                            .setMessage(s)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.i("alert", "yes");
                                    promptResult.confirm();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.i("alert", "no");
                                    promptResult.cancel();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Log.i("alert", "cancel");
                                    promptResult.cancel();
                                }
                            })
                            .create();
                    diag.setCanceledOnTouchOutside(true);
                    diag.show();
                } catch (Exception ex) {
                    promptResult.cancel();
                }
            }


        });
        geckoView.setContentDelegate(new GeckoViewContent() {

            @Override
            public void onPageShow(GeckoView geckoView, GeckoView.Browser browser) {
                super.onPageShow(geckoView, browser);
                updateUi();
            }

            @Override
            public void onPageStart(GeckoView geckoView, GeckoView.Browser browser, String s) {
                super.onPageStart(geckoView, browser, s);
                updateUi();
            }

            @Override
            public void onPageStop(GeckoView geckoView, GeckoView.Browser browser, boolean b) {
                super.onPageStop(geckoView, browser, b);
                updateUi();
            }

            @Override
            public void onReceivedTitle(GeckoView geckoView, GeckoView.Browser browser, String s) {
                super.onReceivedTitle(geckoView, browser, s);
                updateUi();
            }

        });


        geckoView.setFocusable(true);

        Prefs.setPrefs(this);

        if (init) {
            Tabs.getInstance().addPrivateTab();
            Tabs.getInstance().loadUrl("about:blank");
        }

        Prefs.setPrefs(this);

        geckoView.requestFocus();

        editUri.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT ||
                        (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String s = editUri.getText().toString();

                    s = Address.filter(s, Settings.getPrefs(BrowserActivity.this).getString("searchengine", ""));

                    loadUrl(s);

                    hideKeyboard();
                    resetKeyboard();

                    geckoView.requestFocus();

                    return true;
                }

                return false;
            }
        });


        final View bar = findViewById(R.id.bar);

        editUri.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    clearSpans(editUri);
                    bar.setBackgroundColor(0xff0b0b0b);
                } else {
                    setUriSpans(editUri);
                    bar.setBackgroundResource(R.drawable.bg_bar);
                }
            }
        });


        logView = (TextView) findViewById(R.id.statusText);

        startpage = homepage();

        if (init) {
            handleIntent(getIntent());
            if (!startpage.equals(homepage())) {
                loadUrl(startpage);
            }
        } else {
            logView = null;
        }

        tor.setOnStatusListener(new Tor.OnStatusListener() {
            @Override
            public void onMessage(final String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (logView == null) return;

                        int i = s.indexOf("%:");
                        if (i >= 0) {
                            logView.setText(s.substring(i + 2).trim());
                            ((TextView) findViewById(R.id.percentage)).setText(s.substring(0, i + 1));
                            return;
                        }

                        logView.setText(s);

                        findViewById(R.id.load_pleasewait).setVisibility(s.startsWith("Error:") || s.contains("Network is unreachable") ? View.INVISIBLE : View.VISIBLE);
                        findViewById(R.id.progressBar).setVisibility(s.startsWith("Error:") ? View.INVISIBLE : View.VISIBLE);
                        ((TextView) findViewById(R.id.load_connecting)).setText(s.startsWith("Error:") ? "Error" : "Connecting");
                    }
                });
            }

            @Override
            public void onFinish() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initProxy();

                        if (init) {
                            Log.i(TAG, "loading start page");
                            loadUrl(startpage);
                        }

                        hidePage(R.id.loading);

                        logView = null;
                    }
                });
            }
        });

        if (!init) {
            initProxy();
        }

        if (init) {
            tor.start();
        }


        EventDispatcher.getInstance().registerGeckoThreadListener(new NativeEventListener() {
                                                                      @Override
                                                                      public void handleMessage(String s, NativeJSObject nativeJSObject, EventCallback eventCallback) {

                                                                          try {
                                                                              Log.i(TAG, "Message " + s + " " + nativeJSObject.toString());
                                                                              for (String ss : nativeJSObject.toString().split("\n")) {
                                                                                  Log.i(TAG, "Message " + s + " " + ss);
                                                                              }

                                                                              for (String ss : nativeJSObject.optString("cookie", "").split("\n")) {
                                                                                  Log.i(TAG, "Cookie " + s + " " + ss);
                                                                              }

                                                                              String cookie = nativeJSObject.optString("cookie", "");
                                                                              String text = nativeJSObject.optString("text", "");

                                                                              String url = cookie.substring(cookie.indexOf(text) + text.length()).split(" ", 2)[0];
                                                                              url = url.substring(0, url.length() - 3);

                                                                              Log.i(TAG, "Download " + url);

                                                                              if (url.length() < 2000) {
                                                                                  askDownload(url, text);
                                                                              }

                                                                          } catch (Throwable ex) {
                                                                              ex.printStackTrace();
                                                                          }
                                                                      }
                                                                  }
                , "Notification:Show"
                //"TextSelection:Update", "Toast:Show", "Notification:Show", "Notification:Hide"
        );

        findViewById(R.id.copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copy();
            }
        });

        findViewById(R.id.paste).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paste();
            }
        });

        updateTools();


        findViewById(R.id.tab_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newTab();
            }
        });


        findViewById(R.id.menuview).setVisibility(View.GONE);


        hideKeyboard();
        resetKeyboard();


        try {
            Constructor ctor = Class.forName("org.mozilla.gecko.TextSelection").getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            textSelection = ctor.newInstance(
                    findViewById(R.id.anchor_handle),
                    findViewById(R.id.caret_handle),
                    findViewById(R.id.focus_handle)
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        EventDispatcher.getInstance().unregisterGeckoThreadListener((GeckoEventListener) textSelection
                , "TextSelection:ShowHandles"
                , "TextSelection:HideHandles"
                , "TextSelection:Update");

        EventDispatcher.getInstance().registerGeckoThreadListener(new GeckoEventListener() {
                                                                      @Override
                                                                      public void handleMessage(final String s, final JSONObject nativeJSObject) {
                                                                          try {
                                                                              Method destroy = textSelection.getClass().getDeclaredMethod("handleMessage", String.class, JSONObject.class);
                                                                              destroy.setAccessible(true);
                                                                              destroy.invoke(textSelection, s, nativeJSObject);
                                                                          } catch (Exception ex) {
                                                                              throw new RuntimeException(ex);
                                                                          }
                                                                          try {
                                                                              Log.i(TAG, "Message " + s + " " + nativeJSObject.toString());
                                                                              for (String ss : nativeJSObject.toString().split("\n")) {
                                                                                  Log.i(TAG, "Message " + s + " " + ss);
                                                                              }
                                                                              if (s.equals("TextSelection:ShowHandles")) {
                                                                                  actions.clear();
                                                                                  selectionID = nativeJSObject.getString("selectionID");
                                                                              }
                                                                              if (s.equals("TextSelection:Update")) {
                                                                                  actions.clear();
                                                                                  JSONArray aa = nativeJSObject.getJSONArray("actions");
                                                                                  for (int i = 0; i < aa.length(); i++) {
                                                                                      actions.add(aa.getJSONObject(i).getString("id"));
                                                                                  }
                                                                              }
                                                                              if (s.equals("TextSelection:HideHandles")) {
                                                                                  actions.clear();
                                                                              }
                                                                          } catch (Exception ex) {
                                                                              ex.printStackTrace();
                                                                          }
                                                                          runOnUiThread(new Runnable() {
                                                                              @Override
                                                                              public void run() {
                                                                                  updateTools();
                                                                              }
                                                                          });
                                                                      }
                                                                  }
                , "TextSelection:ShowHandles"
                , "TextSelection:HideHandles"
                , "TextSelection:Update"
        );


        findViewById(R.id.loadRestart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartApp();
            }
        });


        findViewById(R.id.settings_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsFragment.getInstance().reset();
            }
        });

        findViewById(R.id.load_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPage(R.id.settings_page);
            }
        });


        if (!init) {
            findViewById(R.id.loading).setVisibility(View.GONE);
            initPage(findViewById(R.id.settings_page));
            findViewById(R.id.settings_page).setVisibility(View.VISIBLE);
        }


        updateUi();
    }

    void initProxy() {
        Log.i(TAG, "proxy config");
        PrefsHelper.setPref("network.proxy.type", 1); //manual proxy settings
        PrefsHelper.setPref("network.proxy.socks_remote_dns", true);
        PrefsHelper.setPref("network.proxy.socks", tor.getProxyHost()); //manual proxy settings
        PrefsHelper.setPref("network.proxy.socks_port", tor.getProxyPort()); //manual proxy settings
        PrefsHelper.setPref("network.proxy.socks_version", 5); //manual proxy settings
    }

    void restartApp() {
        startActivity(new Intent(BrowserActivity.this, RestartActivity.class).putExtra("procid", android.os.Process.myPid()));
    }

    void cleanup() {
        deleteFiles(new File(getFilesDir(), "mozilla"));
        //deleteFiles(new File(getFilesDir(), "downloads"));
    }

    @Override
    protected void onDestroy() {
        if (skipcleanup) {
            Log.i(TAG, "onDestroy skipped");
            super.onDestroy();
            return;
        }
        Log.i(TAG, "onDestroy");
        geckoView.destroy();
        tor.destroy();
        cleanup();
        super.onDestroy();
        Log.i(TAG, "onDestroy finished");
    }

    void loadUrl(String s) {
        Tabs.getInstance().getSelectedTab().updateURL(s);
        geckoView.getCurrentBrowser().loadUrl(s);
        Tabs.getInstance().getSelectedTab().updateURL(s);
    }

    String getCurrentUrl() {
        Tab tab = Tabs.getInstance().getSelectedTab();
        return tab != null ? tab.getURL() : "";
    }

    String getCurrentTitle() {
        Tab tab = Tabs.getInstance().getSelectedTab();
        return tab != null ? tab.getDisplayTitle() : "";
    }

    boolean getCurrentLoading() {
        Tab tab = Tabs.getInstance().getSelectedTab();
        return tab != null ? tab.getLoadProgress() != 100 : false;
    }

    void updateUi() {
        setUriStr(getCurrentUrl());
        progressBar.setVisibility(getCurrentLoading() ? View.VISIBLE : View.GONE);
        updateTabPageItems();
    }

    void initMenu(View v) {
        if (v.getId() != View.NO_ID && v.isClickable()) {
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = v.getId();
                    hideMenuOnAction();
                    onOptionsItemSelected(id);
                }
            });
            v.setAlpha(v.isEnabled() ? 1.0f : 0.5f);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                initMenu(g.getChildAt(i));
            }
        }
    }

    void hideMenuOnAction() {
        hideMenu(150);
    }

    void hideMenu() {
        hideMenu(0);
    }

    void hideMenu(int delay) {
        final View v = findViewById(R.id.menuview);
        if ("0".equals(v.getTag())) return;
        Animation loadhide = AnimationUtils.loadAnimation(BrowserActivity.this, R.anim.popup_hide);
        loadhide.setStartOffset(delay);
        loadhide.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        findViewById(R.id.menumenu).startAnimation(loadhide);
        resetKeyboard();
        v.setTag("0");
        findViewById(R.id.menuButton).setBackground(null);
    }

    void showMenu() {
        final View v = findViewById(R.id.menuview);
        if ("1".equals(v.getTag())) return;
        hideKeyboard();
        v.findViewById(R.id.action_back).setEnabled(geckoView.getCurrentBrowser().canGoBack());
        v.findViewById(R.id.action_forward).setEnabled(geckoView.getCurrentBrowser().canGoForward());
        v.findViewById(R.id.action_reload).setVisibility(!getCurrentLoading() ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.action_stop).setVisibility(getCurrentLoading() ? View.VISIBLE : View.GONE);
        final long bm = bookmarkDatabase.getBookmarkId(getCurrentUrl());
        v.findViewById(R.id.action_bookmark_add).setVisibility(bm < 0 ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.action_bookmark_del).setVisibility(bm >= 0 ? View.VISIBLE : View.GONE);
        initMenu(v);
        v.findViewById(R.id.action_bookmark_del).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bm >= 0) {
                    showBookmarkDialog(bm);
                    hideMenu();
                }
            }
        });
        v.setVisibility(View.VISIBLE);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
            }
        });
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //((AudioManager) getSystemService(Context.AUDIO_SERVICE)).playSoundEffect(2);
                    hideMenu();
                }
                return true;
            }
        });
        findViewById(R.id.menumenu).startAnimation(AnimationUtils.loadAnimation(BrowserActivity.this, R.anim.popup_show));
        v.setTag("1");
        findViewById(R.id.menuButton).setBackgroundColor(0x88222222);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    void handleIntent(Intent intent) {
        if (intent != null && intent.getDataString() != null && !intent.getDataString().trim().isEmpty()) {
            startpage = intent.getDataString();
            if (geckoView != null && geckoView.getCurrentBrowser() != null)
                geckoView.getCurrentBrowser().loadUrl(startpage);
        } else {
            startpage = homepage();
        }
    }

    void updateTools() {
        findViewById(R.id.copy).setVisibility(actions.contains("copy_action") ? View.VISIBLE : View.GONE);
        findViewById(R.id.paste).setVisibility(actions.contains("paste_action") ? View.VISIBLE : View.GONE);
    }

    void askDownload(final String url, final String name) {
        long downloadTime = System.currentTimeMillis();
        if (downloadTime < downloadWaitTime) {
            Log.i(TAG, "Download blocked. Try again later.");
            return;
        }
        downloadWaitTime = downloadTime + 5000;
        SharedPreferences p = Settings.getPrefs(this);
        if (!p.getBoolean("downloads", true)) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(BrowserActivity.this)
                        .setTitle("Download file?")
                        .setMessage(url)
                        .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                runDownload(url, name);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setCancelable(true)
                        .create().show();
            }
        });
    }

    void runDownload(final String url, final String name) {
        runDownload(url);
    }

    void runDownload(final String url) {
        DownloadManager.getInstance(this).startDownload(tor, url);
        showDownloads();
    }

    void deleteFiles(File f) {
        Log.i("DELETE", f.toString());
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteFiles(c);
                if (c.delete())
                    Log.i(TAG, "success");
                else
                    Log.i(TAG, "failure");
            }
        }
    }

    void goBack() {
        if (findViewById(R.id.tab_page).getVisibility() == View.VISIBLE) {
            closeTabPage();
            return;
        }
        if (findViewById(R.id.bookmark_page).getVisibility() == View.VISIBLE) {
            hideBookmarks();
            return;
        }
        if (findViewById(R.id.download_page).getVisibility() == View.VISIBLE) {
            hideDownloads();
            return;
        }
        if (!Tabs.getInstance().getSelectedTab().canDoBack() && Tabs.getInstance().getDisplayCount() > 1) {
            Tabs.getInstance().closeTab(Tabs.getInstance().getSelectedTab());
            Log.i(TAG, "goBack close");
            updateUi();
            return;
        }
        Log.i(TAG, "goBack current");
        geckoView.getCurrentBrowser().goBack();
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        return true;
    }

    public void rate() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
            for (ApplicationInfo packageInfo : getPackageManager().getInstalledApplications(0)) {
                if (packageInfo.packageName.equals("com.android.vending"))
                    intent.setPackage("com.android.vending");
            }
            startActivity(intent);
        } catch (Throwable t) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    public void share() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + getPackageName());
        intent.setType("text/plain");
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onOptionsItemSelected(item.getItemId());
    }

    boolean onOptionsItemSelected(int id) {
        if (id == R.id.action_rate) {
            rate();
            return true;
        }
        if (id == R.id.action_bookmark_add) {
            View v = getLayoutInflater().inflate(R.layout.addbookmark, null);
            final EditText title = ((EditText) v.findViewById(R.id.title));
            final EditText url = ((EditText) v.findViewById(R.id.url));
            title.setText(getCurrentTitle());
            url.setText(getCurrentUrl());
            new AlertDialog.Builder(this)
                    .setView(v)
                    .setTitle("Bookmark")
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String u = url.getText().toString();
                            String t = title.getText().toString();
                            if (t == null || t.isEmpty()) t = u;
                            if (u != null && !u.isEmpty()) {
                                bookmarkDatabase.saveBookmark(u, t);
                                Toast.makeText(BrowserActivity.this, "Bookmark added", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(BrowserActivity.this, "URL empty", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create().show();
            return true;
        }
        if (id == R.id.action_bookmark_list) {
            showBookmarks();
            return true;
        }
        if (id == R.id.action_download_list) {
            showDownloads();
            return true;
        }
        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        if (id == R.id.action_home) {
            loadUrl(homepage());
            return true;
        }
        if (id == R.id.action_reload) {
            geckoView.getCurrentBrowser().reload();
            return true;
        }
        if (id == R.id.action_forward) {
            geckoView.getCurrentBrowser().goForward();
            return true;
        }
        if (id == R.id.action_back) {
            goBack();
            return true;
        }
        if (id == R.id.action_stop) {
            geckoView.getCurrentBrowser().stop();
            return true;
        }
        if (id == R.id.action_new_identity) {
            tor.killTorProcess();
            GeckoAppShell.killAnyZombies();
            cleanup();
            restartApp();
            return true;
        }
        if (id == R.id.action_exit) {
            tor.killTorProcess();
            GeckoAppShell.killAnyZombies();
            cleanup();
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                finishAndRemoveTask();
            }
            finish();
            startActivity(new Intent(this, KillActivity.class).putExtra("procid", android.os.Process.myPid()));
            overridePendingTransition(0, 0);
            return true;
        }
        if (id == R.id.action_tabs) {
            showTabPage();
            return true;
        }
        if (id == R.id.action_settings) {
            showPage(R.id.settings_page);
            return true;
        }
        return false;
    }

    void hideBookmarks() {
        hidePage(R.id.bookmark_page);
    }

    void initBookmarks() {
        if (bookmarkCursor != null) {
            bookmarkCursor.close();
            bookmarkCursor = null;
        }
        final Cursor cursor = bookmarkDatabase.getBookmarkCursor();
        bookmarkCursor = cursor;
        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return cursor.getCount();
            }

            @Override
            public Object getItem(int position) {
                cursor.moveToPosition(position);
                return cursor.getString(1);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                }
                cursor.moveToPosition(position);
                final String u = cursor.getString(1);
                final String t = cursor.getString(2);
                TextView text1 = ((TextView) convertView.findViewById(android.R.id.text1));
                TextView text2 = ((TextView) convertView.findViewById(android.R.id.text2));
                text1.setSingleLine();
                text2.setSingleLine();
                text1.setText(t);
                text2.setText(u);
                return convertView;
            }

        };
        final ListView lv = (ListView) findViewById(R.id.bookmark_list_view);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cursor.moveToPosition(position);
                loadUrl(cursor.getString(1));
                hideBookmarks();
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                cursor.moveToPosition(position);
                final long i = cursor.getLong(0);
                showBookmarkDialog(i);
                return true;
            }
        });
    }

    void showBookmarks() {
        initBookmarks();
        showPage(R.id.bookmark_page);
    }

    void showBookmarkDialog(final long i) {
        final BookmarkDatabase.Bookmark bookmark = bookmarkDatabase.getBookmark(i);
        if (bookmark == null) {
            return;
        }
        new AlertDialog.Builder(BrowserActivity.this)
                .setTitle("Bookmark")
                .setMessage(bookmark.title + "\n\n" + bookmark.url)
                .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bookmarkDatabase.deleteBookmark(i);
                        Toast.makeText(BrowserActivity.this, "Bookmark deleted", Toast.LENGTH_SHORT).show();
                        if (findViewById(R.id.bookmark_page).getVisibility() == View.VISIBLE) {
                            initBookmarks();
                        }
                    }
                })
                .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadUrl(bookmark.url);
                        hideBookmarks();
                    }
                })
                .setNegativeButton("New Tab", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Tabs.getInstance().addPrivateTab();
                        loadUrl(bookmark.url);
                        hideBookmarks();
                    }
                })
                .create().show();
    }

    void showDownloads() {
        showPage(R.id.download_page);
    }

    void hideDownloads() {
        hidePage(R.id.download_page);
    }

    void hideKeyboard() {
        Log.i("TAG", "hideKeyboard");
        geckoView.allowInput = false;
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (focus != null) {
            IBinder token = getCurrentFocus().getWindowToken();
            if (token != null) {
                inputMethodManager.hideSoftInputFromWindow(token, 0);
            }
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        editUri.setFocusable(false);
        editUri.setFocusableInTouchMode(false);
        geckoView.setFocusable(false);
        geckoView.setFocusableInTouchMode(false);
        findViewById(R.id.menuButton).requestFocus();
    }

    void resetKeyboard() {
        Log.i("TAG", "resetKeyboard");
        geckoView.allowInput = true;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        editUri.setFocusable(true);
        editUri.setFocusableInTouchMode(true);
        geckoView.setFocusable(true);
        geckoView.setFocusableInTouchMode(true);
        geckoView.requestFocus();
    }

    void clearAnims(View v) {
        v.clearAnimation();
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                clearAnims(g.getChildAt(i));
            }
        }
    }

    void initPage(final View v) {
        log("initPage");
        hideKeyboard();
        clearAnims(v);
        v.setClickable(true);
        v.setVisibility(View.VISIBLE);
        v.setFocusable(true);
        v.requestFocus();
        v.setTag("1");
        v.setClickable(true);
        View closeButton = v.findViewById(R.id.page_close);
        if (closeButton != null) {
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View vv) {
                    hidePage(v);
                }
            });
            closeButton.setClickable(true);
        }
        v.setAlpha(1);
    }

    void showPage(final View v) {
        if ("1".equals(v.getTag())) return;
        log("showPage");
        initPage(v);
        v.setAlpha(0);
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                v.setAlpha(1);
                Animation anim = AnimationUtils.loadAnimation(BrowserActivity.this, R.anim.page_show);
                anim.setStartOffset(150);
                v.startAnimation(anim);
            }
        }, 20);
    }

    void showPage(int id) {
        showPage(findViewById(id));
    }

    void hidePage(final View v) {
        if ("0".equals(v.getTag())) return;
        log("hidePage");
        resetKeyboard();
        Animation anim = AnimationUtils.loadAnimation(BrowserActivity.this, R.anim.page_hide);
        anim.setStartOffset(100);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        v.startAnimation(anim);
        v.setClickable(false);
        v.setFocusable(false);
        geckoView.requestFocus();
        v.setTag("0");
        if (v.getId() == R.id.settings_page) {
            Prefs.applyPrefs(this);
        }
        v.setClickable(false);
        View closeButton = v.findViewById(R.id.page_close);
        if (closeButton != null) {
            closeButton.setClickable(false);
        }
    }

    void hidePage(int id) {
        hidePage(findViewById(id));
    }

    void closeTabPage() {
        hidePage(R.id.tab_page);
    }

    void initTabPageItem(final Tab tab, View v) {
        ((TextView) v.findViewById(R.id.title)).setText(tab.getDisplayTitle());
        ((TextView) v.findViewById(R.id.address)).setText(tab.getURL());
        ThumbnailHelper.getInstance().getAndProcessThumbnailFor(tab);
        ((ImageView) v.findViewById(R.id.image)).setImageDrawable(tab.getThumbnail());
        if (Tabs.getInstance().getSelectedTab() == tab) {
            v.setBackgroundColor(0x33ffffff);
        } else {
            v.setBackgroundColor(0x08ffffff);
        }
    }

    void updateTabPageItems() {
        log("updateTabPageItems");
        Tabs.getInstance().refreshThumbnails();
        final LinearLayout list = (LinearLayout) findViewById(R.id.tab_list);
        if (list.getVisibility() != View.VISIBLE) return;
        for (int i = 0; i < list.getChildCount(); i++) {
            View view = list.getChildAt(i);
            Object tag = view.getTag();
            if (tag == null) continue;
            Tab tab = (Tab) tag;
            initTabPageItem(tab, view);
        }
    }

    void addTabPageItem(final Tab tab) {
        final LinearLayout list = (LinearLayout) findViewById(R.id.tab_list);
        final View v = getLayoutInflater().inflate(R.layout.tab_item, list, false);
        v.setTag(tab);
        initTabPageItem(tab, v);
        v.findViewById(R.id.tab_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View __v) {
                list.removeView(v);
                if (Tabs.getInstance().getDisplayCount() > 1) {
                    Tabs.getInstance().closeTab(tab);
                    updateTabPageItems();
                } else {
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Tab t = Tabs.getInstance().addPrivateTab();
                            Tabs.getInstance().loadUrl("about:blank");
                            addTabPageItem(t);
                            Tabs.getInstance().closeTab(tab);
                        }
                    }, 150);
                }
            }
        });
        v.findViewById(R.id.item).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tabs.getInstance().selectTab(tab.getId());
                updateUi();
                closeTabPage();
            }
        });
        list.addView(v);
    }

    void updateTabPage() {
        log("updateTabPage");
        Tabs tabs = Tabs.getInstance();
        final LinearLayout list = (LinearLayout) findViewById(R.id.tab_list);
        list.setLayoutTransition(null);
        list.removeAllViews();
        Tabs.getInstance().refreshThumbnails();
        for (final Tab tab : tabs.getTabsInOrder()) {
            addTabPageItem(tab);
        }
        LayoutTransition transition = new LayoutTransition();
        transition.setStartDelay(LayoutTransition.APPEARING, 0);
        list.setLayoutTransition(transition);
    }

    void showTabPage() {
        updateTabPage();
        showPage(R.id.tab_page);
    }

    void copy() {
        geckoView.requestFocus();
        geckoView.requestFocusFromTouch();
        try {
            GeckoAppShell.sendEventToGecko(GeckoEvent.createBroadcastEvent("TextSelection:Action", "copy_action"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void newTab() {
        Tab tab = Tabs.getInstance().addPrivateTab();
        loadUrl("about:blank");
        addTabPageItem(tab);
        findViewById(R.id.tab_page).postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUi();
                closeTabPage();
                editUri.setText("");
                editUri.selectAll();
                editUri.requestFocus();
            }
        }, 100);
    }

    void newTab(String addr) {
        Tab current = Tabs.getInstance().getSelectedTab();
        Tabs.getInstance().loadUrl(addr, Tabs.LOADURL_NEW_TAB | Tabs.LOADURL_PRIVATE | Tabs.LOADURL_BACKGROUND);
        Tabs.getInstance().selectTab(current.getId());
        //Toast.makeText(this, "New tab opened.", Toast.LENGTH_SHORT).show();
        animNewTab();
    }

    void animNewTab() {

        //Toast.makeText(this, "New tab opened.", Toast.LENGTH_SHORT).show();

        //findViewById(R.id.layout_root).startAnimation(AnimationUtils.loadAnimation(BrowserActivity.this, R.anim.newtab));
        //findViewById(R.id.newtab_bottom).startAnimation(AnimationUtils.loadAnimation(BrowserActivity.this, R.anim.newtab_bottom));


        //findViewById(R.id.root_frame).startAnimation(AnimationUtils.loadAnimation(BrowserActivity.this, R.anim.newtab_root));

        {
            final View v = findViewById(R.id.root_frame);
            v.setVisibility(View.VISIBLE);
            Animation anim = AnimationUtils.loadAnimation(BrowserActivity.this, R.anim.newtab_root);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //v.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    //animation.setStartOffset(500);
                }
            });
            v.startAnimation(anim);
        }

        {
            final View v = findViewById(R.id.newtab_bottom);
            v.setVisibility(View.VISIBLE);
            Animation anim = AnimationUtils.loadAnimation(BrowserActivity.this, R.anim.newtab_bottom);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    v.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            v.startAnimation(anim);
        }

        {
            final View v = findViewById(R.id.newtab_over);
            v.setVisibility(View.VISIBLE);
            Animation anim = AnimationUtils.loadAnimation(BrowserActivity.this, R.anim.newtab_over);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    v.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            v.startAnimation(anim);
        }

    }

    void paste() {
        geckoView.requestFocus();
        geckoView.requestFocusFromTouch();
        try {
            GeckoAppShell.sendEventToGecko(GeckoEvent.createBroadcastEvent("TextSelection:Action", "paste_action"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
        super.onCreateThumbnail(outBitmap, canvas);
        canvas.drawColor(0xff222222);
        return true;
    }

    public String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int i;
        try {
            i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            return null;
        }
        return byteArrayOutputStream.toString();
    }

    @Override
    public void onResume() {
        super.onResume();
        Prefs.applyPrefs(this);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        step();
                    }
                });
            }
        }, 1000, 1000);
    }

    @Override
    protected void onPause() {
        timer.cancel();
        timer.purge();
        timer = null;
        super.onPause();
    }

    void step() {
        if (findViewById(R.id.tab_page).getVisibility() == View.VISIBLE) {
            updateTabPageItems();
        }
    }

    void clearSpans(EditText editUri) {
        ForegroundColorSpan[] spans = editUri.getText().getSpans(0, editUri.getText().length(), ForegroundColorSpan.class);
        for (int i = 0; i < spans.length; i++)
            editUri.getText().removeSpan(spans[i]);
    }

    void setUriSpans(EditText editUri) {
        clearSpans(editUri);

        String uri = editUri.getText().toString();

        editUri.getText().setSpan(new ForegroundColorSpan(0xffdddddd), 0, uri.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int i1 = uri.indexOf("//");
        if (i1 >= 0) {
            editUri.getText().setSpan(new ForegroundColorSpan(0xff888888), 0, i1 + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int i2 = uri.indexOf('/', i1 + 2);
        if (i2 >= 0) {
            editUri.getText().setSpan(new ForegroundColorSpan(0xff888888), i2, uri.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    void setUriStr(String uri) {
        if (editUri.hasFocus()) return;
        editUri.setText(uri);
        setUriSpans(editUri);
    }
}
