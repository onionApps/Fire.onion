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

public class Native {

    static {
        System.loadLibrary("app");
    }

    native public static void killTor();

}
