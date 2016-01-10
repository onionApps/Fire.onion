package onion.fire;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

public class ProxySocket {

    private static String TAG = "ProxySocket";

    private long len = -1;
    private InputStream is;
    private String mime = "application/octet-stream";

    private HashMap<String, String> headers = new HashMap<>();

    public ProxySocket(String url, String proxyhost, int proxyport) throws Exception {
        init(url, proxyhost, proxyport, 5);
    }

    private void init(String url, String proxyhost, int proxyport, int redirs) throws Exception {

        URL u = new URL(url);

        Socket sock = new Socket();
        sock.connect(new InetSocketAddress(proxyhost, proxyport));


        // connect to proxy
        {
            OutputStream os = sock.getOutputStream();

            os.write(4); // socks 4a
            os.write(1); // stream

            int p = u.getPort();
            Log.i(TAG, "proto " + u.getProtocol());
            if (p < 0 && u.getProtocol().equals("http")) p = 80;
            if (p < 0 && u.getProtocol().equals("https")) p = 443;
            Log.i(TAG, "port " + p);
            os.write((p >> 8) & 0xff);
            os.write((p >> 0) & 0xff);

            os.write(0);
            os.write(0);
            os.write(0);
            os.write(1);

            os.write(0);

            os.write(u.getHost().getBytes());
            os.write(0);

            os.flush();
        }


        // get proxy response from proxy
        {
            InputStream is = sock.getInputStream();

            byte[] h = new byte[8];
            is.read(h);
        }


        // https
        if (u.getProtocol().equals("https")) {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sock = factory.createSocket(sock, null, sock.getPort(), false);
        }


        // send http request
        {
            BufferedWriter sw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            sw.write("GET " + u.getPath() + " HTTP/1.0\r\n");
            sw.write("Host: " + u.getHost() + "\r\n");
            //sw.write("User-Agent: Mozilla/5.0 (Windows NT 6.1; rv:31.0) Gecko/20100101 Firefox/31.0\r\n");
            sw.write("User-Agent: " + Prefs.getUserAgent(BrowserActivity.getInstance()) + "\r\n");
            sw.write("\r\n");
            sw.flush();
        }


        // read http response headers
        HashMap<String, String> headers = new HashMap<>();
        {
            InputStream is = sock.getInputStream();
            while (true) {
                StringBuilder sb = new StringBuilder();
                while (true) {
                    int c = is.read();
                    if (c < 0) return;
                    if (c == '\n') break;
                    sb.append((char) c);
                }
                String l = sb.toString().trim();
                if (l.equals("")) break;

                Log.i(TAG, "header " + l);

                String[] hh = l.split("\\:\\ ", 2);
                if (hh.length != 2) continue;
                headers.put(hh[0], hh[1]);
            }
        }
        this.headers = headers;


        // handle redirections
        if (redirs > 0 && headers.containsKey("Location")) {
            try {
                close();
            } catch (IOException ex) {
            }
            init(headers.get("Location"), proxyhost, proxyport, redirs - 1);
            return;
        }


        // interpret headers
        {
            String contenttype = headers.get("Content-Type");
            String charset = null;
            String charsettag = "; charset=";
            if (contenttype != null && contenttype.contains(charsettag)) {
                charset = contenttype.substring(contenttype.indexOf(charsettag) + charsettag.length());
                contenttype = contenttype.substring(0, contenttype.indexOf(charsettag));
            }
            Log.i(TAG, "contenttype " + contenttype);
            Log.i(TAG, "charset " + charset);
            try {
                len = Long.parseLong(headers.get("Content-Length"));
            } catch (Exception ex) {
            }
            try {
                String m = headers.get("Content-Type");
                if (m != null && m.length() > 0)
                    mime = m;
            } catch (Exception ex) {
            }
        }

        is = sock.getInputStream();
    }

    public void close() throws IOException {
        if (is != null) {
            is.close();
            is = null;
        }
    }

    public InputStream getInputStream() {
        return is;
    }

    public long getContentLength() {
        return len;
    }

    public String getMimeType() {
        return mime;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

}
