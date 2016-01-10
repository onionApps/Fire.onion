package onion.fire;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;

public class KillActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kill);
    }

    @Override
    protected void onResume() {

        super.onResume();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int procid = getIntent().getIntExtra("procid", -1);
                android.os.Process.killProcess(procid);
                finish();
                overridePendingTransition(0, 0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, 500);

    }

}
