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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.EventDispatcher;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoEvent;
import org.mozilla.gecko.GeckoView;
import org.mozilla.gecko.Tab;
import org.mozilla.gecko.Tabs;
import org.mozilla.gecko.util.EventCallback;
import org.mozilla.gecko.util.GeckoEventListener;
import org.mozilla.gecko.util.NativeEventListener;
import org.mozilla.gecko.util.NativeJSObject;

import java.lang.reflect.Field;
import java.net.URL;

public class BrowserView extends GeckoView {

    private static BrowserView instance;

    public static BrowserView getInstance() {
        return instance;
    }

    private String getCurrentUrl() {
        Tab tab = Tabs.getInstance().getSelectedTab();
        return tab != null ? tab.getURL() : "";
    }

    private String getHost() {
        try {
            return new URL(getCurrentUrl()).getHost();
        } catch (Exception ex) {
            return "";
        }
    }

    String TAG = "BrowserView";

    void log(String s) {
        Log.i(TAG, s);
    }

    public BrowserView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        instance = this;

        gestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            {
                setIsLongpressEnabled(true);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                contextmenu_longpress = true;
                contextmenu_step();
            }
        });

        EventDispatcher.getInstance().registerGeckoThreadListener(new NativeEventListener() {
            public void handleMessage(final String s, final NativeJSObject nativeJSObject, final EventCallback eventCallback) {
                try {
                    if (s.equals("Intent:GetHandlers")) {
                        contextmenu_address = nativeJSObject.getString("url");
                        contextmenu_step();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, "Intent:GetHandlers");

        setInputConnectionHandler(null);

        try {

            Field baseListenerField = GeckoView.class.getDeclaredField("mGeckoEventListener");
            baseListenerField.setAccessible(true);
            final GeckoEventListener baseListener = (GeckoEventListener) baseListenerField.get(this);

            EventDispatcher.getInstance().unregisterGeckoThreadListener(baseListener, "Prompt:Show", "Prompt:ShowTop");
            EventDispatcher.getInstance().registerGeckoThreadListener(new GeckoEventListener() {
                public void handleMessage(final String event, final JSONObject message) {
                    Log.i(TAG, "handleMessage: " + event);
                    Log.i(TAG, "handleMessage: " + message.toString());

                    final JSONArray listitems = message.optJSONArray("listitems");

                    if (listitems != null) {

                        new Thread() {
                            @Override
                            public void run() {

                                post(new Runnable() {
                                    @Override
                                    public void run() {


                                        final String[] items = new String[listitems.length()];
                                        final int[] ids = new int[listitems.length()];
                                        final boolean[] selected = new boolean[listitems.length()];

                                        for (int i = 0; i < listitems.length(); i++) {
                                            JSONObject o = listitems.optJSONObject(i);
                                            if (o != null) {
                                                int id = o.optInt("id", i);
                                                ids[i] = id;
                                                String label = o.optString("label");
                                                if (label != null) {
                                                    items[i] = label;
                                                }
                                                selected[i] = o.optBoolean("selected", false);
                                            }
                                        }

                                        //final PromptResult promptResult = new PromptResult(message);

                                        AlertDialog.Builder b = new AlertDialog.Builder(getContext())
                                                .setTitle(getHost());

                                        if ("multiple".equals(message.optString("choiceMode"))) {
                                            b.setMultiChoiceItems(items, selected, new DialogInterface.OnMultiChoiceClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                                    selected[which] = isChecked;
                                                }
                                            });
                                            b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    JSONObject result = new JSONObject();
                                                    try {
                                                        result.put("button", 0);
                                                        JSONArray list = new JSONArray();
                                                        for (int i = 0; i < items.length; i++)
                                                            if (selected[i])
                                                                list.put(ids[i]);
                                                        result.put("list", list);
                                                    } catch (JSONException ex) {
                                                        ex.printStackTrace();
                                                    }
                                                    Log.i(TAG, "OK: " + result);
                                                    EventDispatcher.sendResponse(message, result);
                                                }
                                            });
                                            b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    JSONObject result = new JSONObject();
                                                    try {
                                                        result.put("button", -1);
                                                        result.put("list", new JSONArray());
                                                    } catch (JSONException ex) {
                                                        ex.printStackTrace();
                                                    }
                                                    Log.i(TAG, "Cancel: " + result);
                                                    EventDispatcher.sendResponse(message, result);
                                                }
                                            });
                                        } else {
                                            b.setItems(items, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, final int which) {
                                                    JSONObject result = new JSONObject();
                                                    try {
                                                        result.put("button", ids[which]);
                                                        JSONArray selected = new JSONArray();
                                                        selected.put(ids[which]);
                                                        result.put("list", selected);
                                                    } catch (JSONException ex) {
                                                        ex.printStackTrace();
                                                    }
                                                    Log.i(TAG, "onClick: " + result);
                                                    EventDispatcher.sendResponse(message, result);
                                                }
                                            });
                                        }

                                        b.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                JSONObject result = new JSONObject();
                                                try {
                                                    result.put("button", -1);
                                                    result.put("list", new JSONArray());
                                                } catch (JSONException ex) {
                                                    ex.printStackTrace();
                                                }
                                                Log.i(TAG, "onCancel: " + result);
                                                EventDispatcher.sendResponse(message, result);
                                            }
                                        });

                                        b.show();

                                    }
                                });

                            }
                        }.start();

                        return;

                    }

                    baseListener.handleMessage(event, message);
                }
            }, "Prompt:Show", "Prompt:ShowTop");

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    GestureDetectorCompat gestureDetector;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        //Log.i(TAG, "touch");
        gestureDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            contextmenu_mousedown = true;
            contextmenu_longpress = false;
            contextmenu_address = null;
            contextmenu_stepped = false;
            //requestFocus();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            contextmenu_mousedown = false;
            contextmenu_longpress = false;
            contextmenu_address = null;
            contextmenu_stepped = false;
        }

        return true;
    }

    boolean contextmenu_mousedown = false;
    boolean contextmenu_longpress = false;
    String contextmenu_address = null;
    boolean contextmenu_stepped = false;

    void contextmenu_step() {
        if (contextmenu_longpress == false) {
            return;
        }
        if (contextmenu_address == null) {
            return;
        }
        if (contextmenu_stepped == true) {
            return;
        }
        contextmenu_stepped = true;
        final String addr = contextmenu_address;
        post(new Runnable() {
            @Override
            public void run() {

                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

                new AlertDialog.Builder(getContext())
                        .setTitle(addr)
                        .setItems(new String[]{
                                "Open link",
                                "Open in new tab",
                                "Copy address",
                                "Download",
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        Tabs.getInstance().loadUrl(addr);
                                        break;
                                    case 1:
                                        BrowserActivity.getInstance().newTab(addr);
                                        break;
                                    case 2:
                                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                        clipboard.setText(addr);
                                        Toast.makeText(getContext(), "Copied to clipboard.", Toast.LENGTH_SHORT).show();
                                        break;
                                    case 3:
                                        BrowserActivity.getInstance().runDownload(addr);
                                        break;
                                }
                            }
                        })
                        .show();
            }
        });


    }


    public boolean allowInput = true;


    {
        setInputConnectionHandler(null);
    }

    @Override
    public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
        log("onKeyPreIme " + i + " " + keyEvent);
        return super.onKeyPreIme(i, keyEvent);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent event) {
        log("onKeyDown " + i + " " + event);
        //return super.onKeyDown(i, event);
        GeckoAppShell.sendEventToGeckoSync(GeckoEvent.createKeyEvent(event, event.getAction(), 0));
        return true;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent event) {
        log("onKeyUp " + i + " " + event);
        //return super.onKeyUp(i, event);
        GeckoAppShell.sendEventToGeckoSync(GeckoEvent.createKeyEvent(event, event.getAction(), 0));
        return true;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return null;
    }

}
