package onion.fire;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class RestartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restart);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int procid = getIntent().getIntExtra("procid", -1);
                android.os.Process.killProcess(procid);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                                startActivity(new Intent(RestartActivity.this, BrowserActivity.class) /*.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)*/);
                                //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                //overridePendingTransition(0, 0);
                            }
                        });
                    }
                }, 500);
            }
        }, 500);

    }

}
