package com.tencent.qqmusic.sword;

/**
 * Host compatibility seam for QQ Music's Sword hot-fix instrumentation.
 * SwordSwitches is copied with every switch unset, matching the no-patch path.
 */
public final class SwordProxy {
    private static final SwordProxyResult NO_SWORD_PROXY_RESULT = new SwordProxyResult();

    static {
        NO_SWORD_PROXY_RESULT.isSupported = false;
    }

    private SwordProxy() {
    }

    public static SwordProxyResult proxyOneArg(Object arg, Object target, int methodId) {
        return NO_SWORD_PROXY_RESULT;
    }

    public static SwordProxyResult proxyMoreArgs(Object[] args, Object target, int methodId) {
        return NO_SWORD_PROXY_RESULT;
    }
}
