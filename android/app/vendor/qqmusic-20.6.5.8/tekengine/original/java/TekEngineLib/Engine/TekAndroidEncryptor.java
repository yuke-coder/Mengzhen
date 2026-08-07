package TekEngineLib.Engine;

import TekEngineLib.State.TekLog;
import android.util.Base64;
import androidx.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import okhttp3.HttpUrl;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekAndroidEncryptor {
    private static String ALGORITHM = "DES";
    private static String CHARSET_UTF_8 = "UTF-8";
    private static String DES_MODE = "DES/CBC/PKCS7Padding";
    private static String LOGTAG = "TEK TekAndroidEncryptor";
    private static DecryptionLyricInterface mDecryptionLyricInterface;

    public interface DecryptionLyricInterface {
        @Nullable
        String doDecryptionLyric(@Nullable String str);
    }

    public static String desDecrypt(String str, String str2) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Input is empty");
        }
        if (str2 == null || str2.length() != 8) {
            throw new IllegalArgumentException("Key length is not 8 bytes");
        }
        try {
            byte[] bArrDecode = Base64.decode(str.getBytes(CHARSET_UTF_8), 0);
            SecretKey secretKeyGenerateSecret = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(new DESKeySpec(str2.getBytes(CHARSET_UTF_8)));
            IvParameterSpec ivParameterSpec = new IvParameterSpec(str2.getBytes(CHARSET_UTF_8));
            Cipher cipher = Cipher.getInstance(DES_MODE);
            cipher.init(2, secretKeyGenerateSecret, ivParameterSpec);
            String str3 = new String(cipher.doFinal(bArrDecode));
            TekLog.write(LOGTAG, "desDecrypt: " + str3);
            return str3;
        } catch (Throwable th) {
            TekLog.write(LOGTAG, "desDecrypt error", th.getMessage());
            return HttpUrl.FRAGMENT_ENCODE_SET;
        }
    }

    public static String desEncrypt(String str, String str2) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Input is empty");
        }
        if (str2 == null || str2.length() != 8) {
            throw new IllegalArgumentException("Key length is not 8 bytes");
        }
        try {
            SecretKey secretKeyGenerateSecret = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(new DESKeySpec(str2.getBytes(CHARSET_UTF_8)));
            IvParameterSpec ivParameterSpec = new IvParameterSpec(str2.getBytes(CHARSET_UTF_8));
            Cipher cipher = Cipher.getInstance(DES_MODE);
            cipher.init(1, secretKeyGenerateSecret, ivParameterSpec);
            String strEncodeToString = Base64.encodeToString(cipher.doFinal(str.getBytes(CHARSET_UTF_8)), 0);
            TekLog.write(LOGTAG, "desEncrypt: " + strEncodeToString);
            return strEncodeToString;
        } catch (Throwable th) {
            TekLog.write(LOGTAG, "desEncrypt error", th.getMessage());
            return HttpUrl.FRAGMENT_ENCODE_SET;
        }
    }

    public static String doDecryptionLyric(String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Input is empty");
        }
        try {
            DecryptionLyricInterface decryptionLyricInterface = mDecryptionLyricInterface;
            if (decryptionLyricInterface == null) {
                return str;
            }
            String strDoDecryptionLyric = decryptionLyricInterface.doDecryptionLyric(str);
            TekLog.write(LOGTAG, "doDecryptionLyric: " + strDoDecryptionLyric);
            return strDoDecryptionLyric;
        } catch (Throwable th) {
            TekLog.write(LOGTAG, "desDecrypt error", th.getMessage());
            return HttpUrl.FRAGMENT_ENCODE_SET;
        }
    }

    public static void setDecryptionLyricInterface(DecryptionLyricInterface decryptionLyricInterface) {
        mDecryptionLyricInterface = decryptionLyricInterface;
    }
}
