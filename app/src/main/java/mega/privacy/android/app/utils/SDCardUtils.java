package mega.privacy.android.app.utils;

import android.content.Context;

import java.io.File;

public class SDCardUtils {

    public static String getSDCardRoot(String path) {
        int i = 0, x = 0;
        char[] chars = path.toCharArray();
        for (; x < chars.length; x++) {
            char c = chars[x];
            if (c == '/') {
                i++;
            }
            if (i == 3) {
                break;
            }
        }
        return path.substring(0, x);
    }

    public static boolean isLocalFolderOnSDCard(Context context, String localPath) {
        File[] fs = context.getExternalFilesDirs(null);
        if(fs.length > 1 && fs[1] != null) {
            String sdRoot = getSDCardRoot(fs[1].getAbsolutePath());
            return localPath.startsWith(sdRoot);
        }
        return false;
    }
}
