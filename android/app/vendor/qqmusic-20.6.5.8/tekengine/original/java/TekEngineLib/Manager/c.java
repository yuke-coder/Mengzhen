package TekEngineLib.Manager;

import TekEngineLib.State.TekLog;
import TekEngineLib.State.TekProxyLog;
import TekEngineLib.State.TekRunningState;
import java.util.LinkedList;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class c extends Thread {
    private static String LOGTAG = "TEK TekThread";
    private static LinkedList<c> ThreadsCache = new LinkedList<>();
    private volatile a _onReleaseListener;
    private volatile boolean _isRuning = true;
    public final Object _lock = new Object();
    public volatile b _runningState = b.IDLEING;
    volatile LinkedList<Runnable> _runQueue = new LinkedList<>();

    public interface a {
        void a();
    }

    public enum b {
        IDLEING,
        RENDERING,
        PAUSING
    }

    public c() {
        TekLog.write(LOGTAG, "TekThread construct.");
        ThreadsCache.add(this);
    }

    private final Runnable getRunbale() {
        synchronized (this._lock) {
            if (this._runQueue.isEmpty()) {
                return null;
            }
            return this._runQueue.removeFirst();
        }
    }

    private boolean isRunning() {
        boolean z;
        TekLog.write(LOGTAG, "isRunning 00:false");
        synchronized (this._lock) {
            z = this._isRuning;
            this._lock.notifyAll();
        }
        TekLog.write(LOGTAG, "isRunning 11:" + z);
        return z;
    }

    private void processRunable() {
        String str = LOGTAG;
        String str2 = "processRunable.";
        while (true) {
            TekLog.write(str, str2);
            Runnable runbale = getRunbale();
            if (runbale == null) {
                return;
            }
            TekLog.write(LOGTAG, "processRunable 0.");
            runbale.run();
            str = LOGTAG;
            str2 = "processRunable 1.";
        }
    }

    public void addRunable(Runnable runnable) {
        TekProxyLog.i(LOGTAG, "addRunable.");
        synchronized (this._lock) {
            TekProxyLog.i(LOGTAG, "addRunable 0.");
            this._runQueue.addLast(runnable);
            this._lock.notifyAll();
            TekProxyLog.i(LOGTAG, "addRunable 1.");
        }
    }

    public void addRunableWithOutNotify(Runnable runnable) {
        TekLog.write(LOGTAG, "addRunableWithOutNotify.");
        synchronized (this._lock) {
            TekLog.write(LOGTAG, "addRunableWithOutNotify 0.");
            this._runQueue.addLast(runnable);
            TekLog.write(LOGTAG, "addRunableWithOutNotify 1.");
        }
    }

    public void doDraw() {
    }

    public void doRelease() {
        a aVar = this._onReleaseListener;
        if (aVar != null) {
            aVar.a();
            this._onReleaseListener = null;
        }
    }

    public void forceRun(Runnable runnable) {
        TekProxyLog.i(LOGTAG, "forceRun.");
        synchronized (this._lock) {
            TekProxyLog.i(LOGTAG, "forceRun 0.");
            this._lock.notifyAll();
            this._runQueue.addLast(runnable);
            this._lock.notifyAll();
            TekProxyLog.i(LOGTAG, "forceRun 1.");
        }
    }

    public TekRunningState getRunningState() {
        if (!this._isRuning) {
            TekLog.write(LOGTAG, "getRunningState !_isRuning.");
            return TekRunningState.IDLEING;
        }
        if (this._runningState == b.RENDERING) {
            TekLog.write(LOGTAG, "getRunningState RENDERING.");
            return TekRunningState.RENDERING;
        }
        TekLog.write(LOGTAG, "getRunningState IDLEING.");
        return TekRunningState.IDLEING;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        String str;
        String str2;
        while (isRunning()) {
            try {
                processRunable();
                TekProxyLog.f(LOGTAG, "TekThread run:" + this._runningState);
                if (this._runningState == b.RENDERING) {
                    doDraw();
                } else {
                    if (this._runningState == b.PAUSING) {
                        TekProxyLog.f(LOGTAG, "TekThread run:PAUSING 0");
                        waitForever();
                        str = LOGTAG;
                        str2 = "TekThread run:PAUSING 1";
                    } else {
                        TekProxyLog.f(LOGTAG, "TekThread run: waiting 0");
                        waitForever();
                        str = LOGTAG;
                        str2 = "TekThread run: waiting 1";
                    }
                    TekProxyLog.f(str, str2);
                }
            } catch (Throwable th) {
                TekLog.write(LOGTAG, "TekThread run: finally");
                doRelease();
                if (ThreadsCache.contains(this)) {
                    ThreadsCache.remove(this);
                }
                throw th;
            }
        }
        TekLog.write(LOGTAG, "TekThread run: finally");
        doRelease();
        if (ThreadsCache.contains(this)) {
            ThreadsCache.remove(this);
        }
    }

    public void setOnReleaseListener(a aVar) {
        this._onReleaseListener = aVar;
    }

    public void stopRun() {
        TekProxyLog.i(LOGTAG, "stopRun.");
        synchronized (this._lock) {
            TekProxyLog.i(LOGTAG, "stopRun 0.");
            this._runQueue.clear();
            this._isRuning = false;
            this._lock.notifyAll();
            TekProxyLog.i(LOGTAG, "stopRun 1.");
        }
    }

    public void waitForever() {
        TekLog.write(LOGTAG, "waitForever.");
        if (!this._isRuning) {
            TekLog.write(LOGTAG, "waitForever !_isRuning.");
            return;
        }
        synchronized (this._lock) {
            try {
                if (!this._runQueue.isEmpty()) {
                    TekLog.write(LOGTAG, "waitForever !_runQueue.isEmpty().");
                    return;
                }
                TekLog.write(LOGTAG, "waitForever 0.");
                this._lock.wait();
                TekLog.write(LOGTAG, "waitForever 1.");
            } catch (InterruptedException e) {
                TekLog.write(LOGTAG, "waitForever 2.");
                e.printStackTrace();
            }
        }
    }

    public void waitfor(long j) {
        TekLog.write(LOGTAG, "waitfor.");
        if (!this._isRuning) {
            TekLog.write(LOGTAG, "waitfor !_isRuning.");
            return;
        }
        synchronized (this._lock) {
            try {
                TekLog.write(LOGTAG, "waitfor 0.");
                this._lock.wait(j);
                TekLog.write(LOGTAG, "waitfor 1.");
            } catch (InterruptedException e) {
                TekLog.write(LOGTAG, "waitfor 2.");
                e.printStackTrace();
            }
        }
    }
}
