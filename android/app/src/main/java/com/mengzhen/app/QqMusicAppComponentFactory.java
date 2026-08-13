package com.mengzhen.app;

import android.app.Service;
import android.content.Intent;

import androidx.core.app.CoreComponentFactory;

/** Instantiates the QQ player service through the Android-compatibility subclass. */
public final class QqMusicAppComponentFactory extends CoreComponentFactory {
    private static final String QQ_PLAYER_SERVICE =
            "com.tencent.qqmusicplayerprocess.servicenew.QQPlayerServiceNew";

    @Override
    public Service instantiateService(
            ClassLoader classLoader,
            String className,
            Intent intent
    ) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (QQ_PLAYER_SERVICE.equals(className)) {
            return new QqMusicServiceCompat();
        }
        return super.instantiateService(classLoader, className, intent);
    }
}
