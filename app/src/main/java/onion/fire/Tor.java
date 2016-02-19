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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Tor {

    private final static String TAG = "Tor";

    private final static String torname = "ftor";
    private final static String tordirname = "tordata";

    private final static String sockstag = "Opening Socks listener on";

    private Activity context;
    private Process tor;
    private String proxyhost;
    private int proxyport;
    private OnStatusListener listener;

    public Tor(Activity context) {
        this.context = context;
    }

    public int getProxyPort() {
        return proxyport;
    }

    public String getProxyHost() {
        return proxyhost;
    }

    public void killTorProcess()
    {
        Native.killTor();
    }

    public void ls(File f) {
        Log.i(TAG, f.toString());
        if(f.isDirectory()) {
            for (File s : f.listFiles()) {
                ls(s);
            }
        }
    }

    public void destroy() {
        tor.destroy();
    }

    private void extractFile(int id, String name) {
        try {
            InputStream i = context.getResources().openRawResource(id);
            OutputStream o = context.openFileOutput(name, Context.MODE_PRIVATE);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = i.read(buffer)) > 0) {
                o.write(buffer, 0, read);
            }
            i.close();
            o.close();
        } catch(Exception ex) {
            ex.printStackTrace();
            throw new Error(ex);
        }
    }

    public void setOnStatusListener(OnStatusListener l) {
        listener = l;
    }

    private void loadlog(String str) {
        OnStatusListener l = listener;
        if(l != null) {
            l.onMessage(str);
        }
    }

    public void start() {

        new Thread() {
            public void run() {
                try {

                    ls(context.getFilesDir());

                    //deleteFiles(getFilesDir());

                    // copy tor binary
                    Log.i(TAG, "extracting tor");
                    loadlog("Loading");
                    //torname = "tor." + Math.random();

                    killTorProcess();

                    //Runtime.getRuntime().exec("kill `ps -C " + getFileStreamPath(torname) + " -o pid | tail -n 1`").waitFor();

                    Thread.sleep(100);

                    extractFile(R.raw.license_tor, "licensetor");
                    extractFile(R.raw.license_geckoview, "licensegeckoview");
                    extractFile(R.raw.tor, torname);
                    extractFile(R.raw.geoip, "geoip");
                    extractFile(R.raw.geoip6, "geoip6");

                    context.getFileStreamPath(torname).deleteOnExit();

                    context.getFileStreamPath(torname).setExecutable(true);

                    // make tor data directory
                    File tordir = new File(context.getFilesDir(), tordirname);
                    tordir.mkdirs();

                    // create config file
                    loadlog("Configuring");
                    //PrintWriter torcfg = new PrintWriter(context.openFileOutput("torcfg", Context.MODE_PRIVATE));
                    StringWriter torcfgsw = new StringWriter();
                    PrintWriter torcfg = new PrintWriter(torcfgsw);
                    torcfg.println("DataDirectory " + tordir.getAbsolutePath());
                    torcfg.println("SOCKSPort auto");

                    torcfg.println("GeoIPFile " + context.getFileStreamPath("geoip"));
                    torcfg.println("GeoIPv6File " + context.getFileStreamPath("geoip6"));

                    SharedPreferences prefs = Settings.getPrefs(context);
                    {
                        String value = prefs.getString("entrynodes", "").trim();
                        if (!"".equals(value)){
                            torcfg.println("EntryNodes " + value);
                        }
                    }
                    {
                        String value = prefs.getString("exitnodes", "").trim();
                        if (!"".equals(value)){
                            torcfg.println("ExitNodes " + value);
                        }
                    }
                    {
                        String value = prefs.getString("excludenodes", "").trim();
                        if (!"".equals(value)){
                            torcfg.println("ExcludeNodes " + value);
                        }
                    }
                    {
                        if(prefs.getBoolean("strictnodes", false)) {
                            torcfg.println("StrictNodes 1");
                        }
                    }
                    {
                        torcfg.println(prefs.getString("torcustom", ""));
                    }

                    torcfg.println();
                    torcfg.close();

                    Log.i(TAG, torcfgsw.toString());

                    PrintWriter torcfgx = new PrintWriter(context.openFileOutput("torcfg", Context.MODE_PRIVATE));
                    torcfgx.write(torcfgsw.toString());
                    torcfgx.close();



                    // kill last tor
                    /*Log.i(TAG, "killing");
                    Runtime.getRuntime().exec(new String[] {
                            "killall", getFilesDir() + ".*" }
                    ).waitFor();*/

                    // start tor
                    Log.i(TAG, "starting tor");
                    loadlog("Starting");
                    Log.i(TAG, "cmd " + context.getFileStreamPath(torname).getAbsolutePath() + " " + "-f" + " " + context.getFileStreamPath("torcfg").getAbsolutePath());
                    tor = Runtime.getRuntime().exec(
                            new String[]{
                                    context.getFileStreamPath(torname).getAbsolutePath(),
                                    "-f", context.getFileStreamPath("torcfg").getAbsolutePath()
                            });

                    //killTorProcess();
                    //Runtime.getRuntime().exec("kill `ps -C " + getFileStreamPath(torname) + " -o pid | tail -n 1`").waitFor();

                    BufferedReader torreader = new BufferedReader(new InputStreamReader(tor.getInputStream()));
                    while (true) {
                        final String line = torreader.readLine();
                        if (line == null) break;
                        Log.i(TAG, "tor: " + line);

                        {
                            String status = line;

                            if(status.contains("[err]")) {
                                status = status.substring(status.indexOf("]") + 1);
                                if(status.contains("--")) {
                                    status = status.substring(0, status.indexOf("--"));
                                }
                                status = status.trim();
                                status = "Error: " + status;
                                loadlog(status);
                            }
                            else
                            if(status.contains("Network is unreachable")) {
                                status = "Network is unreachable";
                                loadlog(status);
                            }
                            else
                            {

                                int i = status.indexOf(']');
                                if (i >= 0) status = status.substring(i + 1);
                                status = status.trim();

                                String prefix = "Bootstrapped";
                                if (status.contains("%") && status.length() > prefix.length() && status.startsWith(prefix)) {
                                    status = status.substring(prefix.length());
                                    status = status.trim();
                                    loadlog(status);
                                }

                            }
                        }

                        if (line.contains(sockstag) && proxyhost == null) {
                            Log.i(TAG, "tor listening");
                            String proxy = line.substring(line.indexOf(sockstag) + sockstag.length()).trim();
                            proxyhost = proxy.split(":")[0];
                            proxyport = Integer.parseInt(proxy.split(":")[1]);
                            Log.i(TAG, "tor at " + proxyhost + " " + proxyport);
                        }

                        if (line.contains("Socks listener listening on port") && proxyport <= 0) {
                            String[] ss = line.trim().split("[ \\.]");
                            proxyport = Integer.parseInt(ss[ss.length - 1]);
                        }

                        if (line.contains("Bootstrapped 100%: Done") && proxyhost != null) {
                            //Thread.currentThread().sleep(1000);
                            context.runOnUiThread(new Runnable() {
                                public void run() {


                                    OnStatusListener l = listener;
                                    if(l != null) {
                                        l.onFinish();
                                    }

                                }
                            });
                        }
                    }
                    Log.i(TAG, "tor has exited");

                } catch (Exception e) {
                    e.printStackTrace();
                    throw new Error(e);
                }
            }
        }.start();

    }

    public interface OnStatusListener {
        void onMessage(String message);
        void onFinish();
    }

}
