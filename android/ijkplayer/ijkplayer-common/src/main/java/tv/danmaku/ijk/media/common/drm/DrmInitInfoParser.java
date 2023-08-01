package tv.danmaku.ijk.media.common.drm;

import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static tv.danmaku.ijk.media.common.drm.DrmConstant.AUDIO_INDEX;
import static tv.danmaku.ijk.media.common.drm.DrmConstant.VIDEO_INDEX;

public class DrmInitInfoParser {
    public static List<DrmInitInfo> parse(String drmInfoSet) {
        List<DrmInitInfo> drmInitInfoList = new ArrayList<>();

        String[] drmInfoArray = drmInfoSet.split(";");
        for (String drmItem : drmInfoArray) {
            if (TextUtils.isEmpty(drmItem)) {
                continue;
            }

            String[] drmItemDetail = drmItem.split(",");
            String sampleMimeType = getSampleMimeType(drmItemDetail[0]);
            String schemeType = drmItemDetail[1];
            String uuidString = drmItemDetail[2].substring(drmItemDetail[2].lastIndexOf(":") + 1);
            String psshString = drmItemDetail[3];
            int index = getIndex(drmItemDetail[0]);

            if (isStringInvalid(sampleMimeType)) {
                continue;
            }
            if (!isDrmSupported(schemeType)) {
                continue;
            }
            if (index != AUDIO_INDEX && index != VIDEO_INDEX) {
                continue;
            }

            UUID uuid = isStringInvalid(uuidString) ? null : UUID.fromString(uuidString);
            byte[] psshData = isStringInvalid(psshString) ? null : Base64.decode(psshString, Base64.DEFAULT);

            drmInitInfoList.add(new DrmInitInfo(sampleMimeType, schemeType, uuid, psshData, index));
        }

        return drmInitInfoList;
    }

    private static boolean isDrmSupported(String schemeType) {
        boolean isDrmSupported;
        if (TextUtils.equals(schemeType, "cenc")) {
            isDrmSupported = true;
        } else if (TextUtils.equals(schemeType, "cbcs")) {
            isDrmSupported = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1);
        } else {
            isDrmSupported = false;
        }
        return isDrmSupported;
    }

    private static boolean isStringInvalid(String string) {
        return TextUtils.isEmpty(string) || TextUtils.equals(string, "unknown");
    }

    private static String getSampleMimeType(String type) {
        if (TextUtils.equals(type, "audio")) {
            return "video/mp4";
        } else if (TextUtils.equals(type, "video")) {
            return "video/mp4";
        } else {
            return "unknown";
        }
    }

    private static int getIndex(String type) {
        if (TextUtils.equals(type, "audio")) {
            return AUDIO_INDEX;
        } else if (TextUtils.equals(type, "video")) {
            return VIDEO_INDEX;
        } else {
            return -1;
        }
    }
}
