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

import android.util.Patterns;
import android.webkit.URLUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Address {

    static Pattern pattern = Pattern.compile(
            "(?i)" +
                    "(" +
                    "(?:http|https|file):\\/\\/" +
                    "|(?:inline|data|about|javascript):" +
                    "|(?:.*:.*@)" +
                    ")" +
                    "(.*)");

    public static String filter(String url, String search) {
        url = url.trim();
        boolean sp = url.indexOf(' ') != -1;

        if (!sp && url.indexOf(':') != -1)
            return url;

        Matcher m = pattern.matcher(url);
        if (m.matches()) {
            String s = m.group(1);
            String l = s.toLowerCase();
            if (!l.equals(s)) {
                url = l + m.group(2);
            }
            if (sp && Patterns.WEB_URL.matcher(url).matches()) {
                url = url.replace(" ", "%20");
            }
            return url;
        }
        if (!sp) {
            if (Patterns.WEB_URL.matcher(url).matches()) {
                return URLUtil.guessUrl(url);
            }
        }

        return URLUtil.composeSearchUrl(url, search, "%s");
    }

}