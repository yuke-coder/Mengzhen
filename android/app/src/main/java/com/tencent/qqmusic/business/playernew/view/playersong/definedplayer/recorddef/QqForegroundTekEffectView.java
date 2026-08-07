package com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef;

import TekEngineLib.Interface.ITekEffectInterface;
import TekEngineLib.Interface.ITekEffectStateListener;
import TekEngineLib.Interface.TekEffectImplement;
import TekEngineLib.Interface.UsualParamInfo;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.shader.a;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Thin application host for QQ Music 20.6.5.8's
 * PlayerDefinedRecordFgEffectViewDelegate and TEK renderer.
 */
public final class QqForegroundTekEffectView extends FrameLayout {
    private static final String TAG = "PlayerDefinedRecordFgEffectViewDelegate";
    private static final String ASSET_ROOT = "qq-player-style";

    private ITekEffectInterface effect;
    private View effectView;
    private a shader;
    private String effectName = "none";

    public QqForegroundTekEffectView(Context context) {
        super(context);
        setVisibility(GONE);
    }

    public void bind(String value) {
        String next = TextUtils.isEmpty(value) ? "none" : value;
        if (TextUtils.equals(next, effectName) && effect != null) {
            setVisibility(VISIBLE);
            startEffect();
            return;
        }
        if (TextUtils.equals(next, "none")) {
            effectName = next;
            setVisibility(GONE);
            releaseEffect();
            return;
        }

        releaseEffect();
        effectName = next;
        File resourcePath;
        try {
            resourcePath = copyEffectResource(next + "_tek");
        } catch (IOException exception) {
            Log.e(TAG, "[createEffect] copy resource failed", exception);
            setVisibility(GONE);
            return;
        }
        createEffect(resourcePath.getAbsolutePath());
    }

    private void createEffect(String resourcePath) {
        setVisibility(VISIBLE);
        TekEffectImplement tekEffectImplement = new TekEffectImplement();
        tekEffectImplement.setContext(getContext());
        tekEffectImplement.setStateListener(new ITekEffectStateListener() {
            @Override
            public void onStateChange(int state, int errorCode, Object data) {
                Log.i(TAG, "[createEffect] onStateChange=" + state + ", errorCode=" + errorCode);
                if (errorCode < 0) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            setVisibility(GONE);
                        }
                    });
                }
            }
        });

        a sourceShader = new a();
        float width = getWidth();
        float height = getHeight();
        sourceShader.setCurrentTime(System.currentTimeMillis() / 1000.0f);
        sourceShader.setViewSize(width, height);
        sourceShader.setAspectRadio(height > 0.0f ? width / height : 1.0f);
        shader = sourceShader;
        tekEffectImplement.setShader(sourceShader);
        effect = tekEffectImplement;

        View sourceView = tekEffectImplement.createView();
        effectView = sourceView;
        tekEffectImplement.setCryptKey("c!sUm_qQ");
        tekEffectImplement.updateEffect(resourcePath);
        HashMap<String, Object> blendMode = new HashMap<>();
        blendMode.put("BlendMode", 3);
        tekEffectImplement.setParam(
                "UsualParam",
                new UsualParamInfo("TargetLayerAll", blendMode)
        );

        if (sourceView != null && sourceView.getParent() == null) {
            LayoutParams layoutParams = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
            );
            layoutParams.gravity = android.view.Gravity.BOTTOM;
            if (sourceView instanceof TextureView) {
                ((TextureView) sourceView).setOpaque(false);
            }
            addView(sourceView, layoutParams);
        }
        startEffect();
    }

    private void startEffect() {
        if (!isAttachedToWindow() || effect == null) {
            return;
        }
        effect.start();
        if (shader != null) {
            shader.start();
        }
    }

    private void pauseEffect(boolean releaseTexture) {
        if (effect != null) {
            effect.pause();
        }
        if (shader != null) {
            shader.stop(releaseTexture);
        }
    }

    private void releaseEffect() {
        pauseEffect(true);
        if (effectView != null && effectView.getParent() == this) {
            removeView(effectView);
        }
        effectView = null;
        if (effect != null) {
            effect.destory();
        }
        effect = null;
        shader = null;
    }

    private File copyEffectResource(String directoryName) throws IOException {
        File destination = new File(
                getContext().getNoBackupFilesDir(),
                ASSET_ROOT + File.separator + directoryName
        );
        copyAssetDirectory(ASSET_ROOT + "/" + directoryName, destination);
        return destination;
    }

    private void copyAssetDirectory(String assetPath, File destination) throws IOException {
        String[] children = getContext().getAssets().list(assetPath);
        if (children != null && children.length > 0) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw new IOException("Cannot create " + destination);
            }
            for (String child : children) {
                copyAssetDirectory(assetPath + "/" + child, new File(destination, child));
            }
            return;
        }
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        try (InputStream input = getContext().getAssets().open(assetPath);
             FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (shader != null && width > 0 && height > 0) {
            shader.setCurrentTime(System.currentTimeMillis() / 1000.0f);
            shader.setViewSize(width, height);
            shader.setAspectRadio(width / (float) height);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!TextUtils.equals(effectName, "none")) {
            startEffect();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        pauseEffect(false);
        super.onDetachedFromWindow();
    }
}
