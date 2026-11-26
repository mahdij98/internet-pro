package ir.internetpro.app;

import android.app.Application;
import ir.tapsell.sdk.Tapsell;

/** Initializes Tapsell with the legacy key supplied through the build environment. */
public final class InternetProApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Tapsell.initialize(this, BuildConfig.TAPSELL_APP_KEY);
    }
}
