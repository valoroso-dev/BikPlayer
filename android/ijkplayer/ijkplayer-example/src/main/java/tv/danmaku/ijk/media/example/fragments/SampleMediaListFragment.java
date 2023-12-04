/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tv.danmaku.ijk.media.example.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import tv.danmaku.ijk.media.example.R;
import tv.danmaku.ijk.media.example.activities.VideoActivity;

public class SampleMediaListFragment extends Fragment {
    private EditText mPathView;
    private ListView mFileListView;
    private SampleMediaAdapter mAdapter;

    public static SampleMediaListFragment newInstance() {
        SampleMediaListFragment f = new SampleMediaListFragment();
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_sample_file_list, container, false);
        mPathView = viewGroup.findViewById(R.id.path_view);
        mFileListView = (ListView) viewGroup.findViewById(R.id.file_list_view);
        return viewGroup;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();

        mPathView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
//                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
//                        Toast.makeText(getContext(), "ok", Toast.LENGTH_SHORT).show();
//                        return true;
//                    }
//                }
                String name = "";//todo
                String url = mPathView.getText().toString();
                VideoActivity.intentTo(activity, url, name);
                return false;
            }
        });

        mAdapter = new SampleMediaAdapter(activity);
        mFileListView.setAdapter(mAdapter);
        mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, final long id) {
                SampleMediaItem item = mAdapter.getItem(position);
                String name = item.mName;
                String url = item.mUrl;
                String licenseUrl = item.mDrmLicenseUrl;
                String licenseToken = item.mDrmLicenseToken;
                boolean adaptive = item.mAdaptive;
                Map<String, Object> map = new HashMap<>();
                if (licenseUrl != null) {
                    map.put("licenseUrl", licenseUrl);
                }
                if (licenseToken != null) {
                    map.put("licenseToken", licenseToken);
                }
                map.put("adaptive", adaptive);
                VideoActivity.intentTo(activity, url, name, map);
            }
        });

        String manifest_string =
                "{\n" +
                "    \"version\": \"1.0.0\",\n" +
                "    \"adaptationSet\": [\n" +
                "        {\n" +
                "            \"duration\": 1000,\n" +
                "            \"id\": 1,\n" +
                "            \"representation\": [\n" +
                "                {\n" +
                "                    \"id\": 1,\n" +
                "                    \"codec\": \"avc1.64001e,mp4a.40.5\",\n" +
                "                    \"url\": \"http://las-tech.org.cn/kwai/las-test_ld500d.flv\",\n" +
                "                    \"backupUrl\": [],\n" +
                "                    \"host\": \"las-tech.org.cn\",\n" +
                "                    \"maxBitrate\": 700,\n" +
                "                    \"width\": 640,\n" +
                "                    \"height\": 360,\n" +
                "                    \"frameRate\": 25,\n" +
                "                    \"qualityType\": \"SMOOTH\",\n" +
                "                    \"qualityTypeName\": \"流畅\",\n" +
                "                    \"hidden\": false,\n" +
                "                    \"disabledFromAdaptive\": false,\n" +
                "                    \"defaultSelected\": false\n" +
                "                },\n" +
                "                {\n" +
                "                    \"id\": 2,\n" +
                "                    \"codec\": \"avc1.64001f,mp4a.40.5\",\n" +
                "                    \"url\": \"http://las-tech.org.cn/kwai/las-test_sd1000d.flv\",\n" +
                "                    \"backupUrl\": [],\n" +
                "                    \"host\": \"las-tech.org.cn\",\n" +
                "                    \"maxBitrate\": 1300,\n" +
                "                    \"width\": 960,\n" +
                "                    \"height\": 540,\n" +
                "                    \"frameRate\": 25,\n" +
                "                    \"qualityType\": \"STANDARD\",\n" +
                "                    \"qualityTypeName\": \"标清\",\n" +
                "                    \"hidden\": false,\n" +
                "                    \"disabledFromAdaptive\": false,\n" +
                "                    \"defaultSelected\": true\n" +
                "                },\n" +
                "                {\n" +
                "                    \"id\": 3,\n" +
                "                    \"codec\": \"avc1.64001f,mp4a.40.5\",\n" +
                "                    \"url\": \"http://las-tech.org.cn/kwai/las-test.flv\",\n" +
                "                    \"backupUrl\": [],\n" +
                "                    \"host\": \"las-tech.org.cn\",\n" +
                "                    \"maxBitrate\": 2300,\n" +
                "                    \"width\": 1280,\n" +
                "                    \"height\": 720,\n" +
                "                    \"frameRate\": 30,\n" +
                "                    \"qualityType\": \"HIGH\",\n" +
                "                    \"qualityTypeName\": \"高清\",\n" +
                "                    \"hidden\": false,\n" +
                "                    \"disabledFromAdaptive\": false,\n" +
                "                    \"defaultSelected\": false\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        //mAdapter.addItem(manifest_string, "las test");
        mAdapter.addItem("http://cph-msl.akamaized.net/hls/live/2000341/test/master.m3u8", "[HLS Live]MSL HLS test stream master", true);
        mAdapter.addItem("http://cph-msl.akamaized.net/hls/live/2000341/test/level_0.m3u8", "[HLS Live]MSL HLS test stream 256x106 @ 0.5Mbps");
        mAdapter.addItem("http://cph-msl.akamaized.net/hls/live/2000341/test/level_1.m3u8", "[HLS Live]MSL HLS test stream master 640x266 @ 1.5Mbps");
        mAdapter.addItem("http://cph-msl.akamaized.net/hls/live/2000341/test/level_2.m3u8", "[HLS Live]MSL HLS test stream master 1280x534 @ 2.5Mbps");
        mAdapter.addItem("http://cph-msl.akamaized.net/hls/live/2000341/test/level_3.m3u8", "[HLS Live]MSL HLS test stream master 1920x800 @ 4.5Mbps");
        mAdapter.addItem("http://cph-msl.akamaized.net/hls/live/2000341/test/level_4.m3u8", "[HLS Live]MSL HLS test stream master 1920x800 @ 7.5Mbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8", "bipbop basic master playlist", true);
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear1/prog_index.m3u8", "bipbop basic 400x300 @ 232 kbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear2/prog_index.m3u8", "bipbop basic 640x480 @ 650 kbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear3/prog_index.m3u8", "bipbop basic 640x480 @ 1 Mbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear4/prog_index.m3u8", "bipbop basic 960x720 @ 2 Mbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear0/prog_index.m3u8", "bipbop basic 22.050Hz stereo @ 40 kbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8", "bipbop advanced master playlist", true);
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/gear1/prog_index.m3u8", "bipbop advanced 416x234 @ 265 kbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/gear2/prog_index.m3u8", "bipbop advanced 640x360 @ 580 kbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/gear3/prog_index.m3u8", "bipbop advanced 960x540 @ 910 kbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/gear4/prog_index.m3u8", "bipbop advanced 1289x720 @ 1 Mbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/gear5/prog_index.m3u8", "bipbop advanced 1920x1080 @ 2 Mbps");
        mAdapter.addItem("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/gear0/prog_index.m3u8", "bipbop advanced 22.050Hz stereo @ 40 kbps");

        mAdapter.addItem("https://storage.googleapis.com/wvmedia/clear/h264/tears/tears.mpd", "[Clear DASH]HD (MP4, H264)");
        mAdapter.addItem("https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd", "[Widevine DASH]HD (MP4, H264, cenc)", "https://proxy.uat.widevine.com/proxy?provider=widevine_test", null);
        mAdapter.addItem("https://storage.googleapis.com/wvmedia/cbcs/h264/tears/tears_aes_cbcs.mpd", "[Widevine DASH]HD (MP4, H264, cbcs)", "https://proxy.uat.widevine.com/proxy?provider=widevine_test", null);
    }

    final class SampleMediaItem {
        String mUrl;
        String mName;
        String mDrmLicenseUrl;
        String mDrmLicenseToken;
        boolean mAdaptive;

        public SampleMediaItem(String url, String name) {
            mUrl = url;
            mName = name;
        }

        public SampleMediaItem(String url, String name, String licenseUrl, String licenseToken) {
            mUrl = url;
            mName = name;
            mDrmLicenseUrl = licenseUrl;
            mDrmLicenseToken = licenseToken;
        }
    }

    final class SampleMediaAdapter extends ArrayAdapter<SampleMediaItem> {
        public SampleMediaAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2);
        }

        public void addItem(String url, String name) {
            add(new SampleMediaItem(url, name));
        }

        public void addItem(String url, String name, boolean adaptive) {
            SampleMediaItem sampleMediaItem = new SampleMediaItem(url, name);
            sampleMediaItem.mAdaptive = adaptive;
            add(sampleMediaItem);
        }

        public void addItem(String url, String name, String licenseUrl, String licenseToken) {
            add(new SampleMediaItem(url, name, licenseUrl, licenseToken));
        }

        public void addItem(String url, String name, String licenseUrl, String licenseToken, boolean adaptive) {
            SampleMediaItem sampleMediaItem = new SampleMediaItem(url, name, licenseUrl, licenseToken);
            sampleMediaItem.mAdaptive = adaptive;
            add(sampleMediaItem);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            ViewHolder viewHolder = (ViewHolder) view.getTag();
            if (viewHolder == null) {
                viewHolder = new ViewHolder();
                viewHolder.mNameTextView = (TextView) view.findViewById(android.R.id.text1);
                viewHolder.mUrlTextView = (TextView) view.findViewById(android.R.id.text2);
            }

            SampleMediaItem item = getItem(position);
            viewHolder.mNameTextView.setText(item.mName);
            viewHolder.mUrlTextView.setText(item.mUrl);

            return view;
        }

        final class ViewHolder {
            public TextView mNameTextView;
            public TextView mUrlTextView;
        }
    }
}
