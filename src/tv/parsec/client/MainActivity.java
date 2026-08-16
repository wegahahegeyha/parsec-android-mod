package tv.parsec.client;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import group.matoya.lib.Matoya;

/* loaded from: classes.dex */
public class MainActivity extends Activity {
    Matoya mty;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        FrameLayout root = new FrameLayout(this);
        this.mty = new Matoya(this);
        root.addView(this.mty, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.mty.attachOverlays(root);
        setContentView(root);
    }

    @Override // android.app.Activity
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mty != null) {
            this.mty.updatePanelPosition();
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        this.mty.destroy();
        this.mty = null;
    }
}