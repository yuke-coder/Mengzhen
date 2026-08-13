package org.extra.tools;

import android.app.Fragment;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class a extends Fragment {
    private final Set<LifecycleListener> b = Collections.newSetFromMap(new WeakHashMap());
    private final Object d = new Object();

    public void a(LifecycleListener lifecycleListener) {
        synchronized (this.d) {
            this.b.add(lifecycleListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        synchronized (this.d) {
            for (LifecycleListener lifecycleListener : this.b) {
                if (lifecycleListener != null) {
                    lifecycleListener.onResume();
                }
            }
        }
    }
}
