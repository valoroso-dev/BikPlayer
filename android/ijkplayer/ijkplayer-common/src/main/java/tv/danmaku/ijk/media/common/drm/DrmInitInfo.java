package tv.danmaku.ijk.media.common.drm;

import java.util.UUID;

public final class DrmInitInfo {
    public String sampleMimeType;
    public String schemeType;
    public UUID uuid;
    public byte[] psshData;
    public int index;

    public DrmInitInfo(String sampleMimeType, String schemeType, UUID uuid, byte[] psshData, int index) {
        this.sampleMimeType = sampleMimeType;
        this.schemeType = schemeType;
        this.uuid = uuid;
        this.psshData = psshData;
        this.index = index;
    }
}
