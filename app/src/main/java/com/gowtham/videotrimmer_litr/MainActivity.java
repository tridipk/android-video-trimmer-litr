package com.gowtham.videotrimmer_litr;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cocosw.bottomsheet.BottomSheet;
import com.example.videotrimmer_litr.R;
import com.videotrimmer.library.utils.CompressOption;
import com.videotrimmer.library.utils.LogMessage;
import com.videotrimmer.library.utils.TrimType;
import com.videotrimmer.library.utils.TrimVideo;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_TAKE_VIDEO = 552;

    private VideoView videoView;

    private MediaController mediaController;

    private EditText edtFixedGap, edtMinGap, edtMinFrom, edtMAxTo;

    private int trimType;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoView = findViewById(R.id.video_view);
        edtFixedGap = findViewById(R.id.edt_fixed_gap);
        edtMinGap = findViewById(R.id.edt_min_gap);
        edtMinFrom = findViewById(R.id.edt_min_from);
        edtMAxTo = findViewById(R.id.edt_max_to);
        mediaController = new MediaController(this);



        findViewById(R.id.btn_default_trim).setOnClickListener(this);
        findViewById(R.id.btn_fixed_gap).setOnClickListener(this);
        findViewById(R.id.btn_min_gap).setOnClickListener(this);
        findViewById(R.id.btn_min_max_gap).setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == TrimVideo.VIDEO_TRIMMER_REQ_CODE && data != null) {
                Uri uri = Uri.parse(TrimVideo.getTrimmedVideoPath(data));
                Log.d(TAG, "Trimmed path:: " + uri);
                videoView.setMediaController(mediaController);
                videoView.setVideoURI(uri);
                videoView.requestFocus();
                videoView.start();

                videoView.setOnPreparedListener(mediaPlayer -> {
                    mediaController.setAnchorView(videoView);
                });

                String filepath = String.valueOf(uri);
                File file = new File(filepath);
                long length = file.length();
                Log.d(TAG, "Video size:: " + (length / 1024));
            } else if (requestCode == REQUEST_TAKE_VIDEO && resultCode == RESULT_OK) {
            /*    //check video duration if needed
                if (TrimmerUtils.getVideoDuration(this,data.getData())<=30){
                    Toast.makeText(this,"Video should be larger than 30 sec",Toast.LENGTH_SHORT).show();
                    return;
                }*/
                if (data.getData() != null) {
                    LogMessage.v("Video path:: " + data.getData());
                    openTrimActivity(String.valueOf(data.getData()));
                } else {
                    Toast.makeText(this, "video uri is null", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openTrimActivity(String data) {
        if (trimType == 0) {
            LogMessage.v("Valasdd "+data);
            Intent intent = TrimVideo.activity(data)
                    .setHideSeekBar(true)
                    .setCompressOption(new CompressOption(3)) //pass empty constructor for default compress option
                    .setDestination("/storage/emulated/0/DCIM/TESTFOLDER")
                    .build(this);
            startActivityForResult(intent, TrimVideo.VIDEO_TRIMMER_REQ_CODE);
        } else if (trimType == 1) {
            Intent intent = TrimVideo.activity(data)
                    .setTrimType(TrimType.FIXED_DURATION)
                    .setFixedDuration(getEdtValueLong(edtFixedGap))
                    .build(this);
            startActivityForResult(intent, TrimVideo.VIDEO_TRIMMER_REQ_CODE);
        } else if (trimType == 2) {
            Intent intent = TrimVideo.activity(data)
                    .setTrimType(TrimType.MIN_DURATION)
                    .setMinDuration(getEdtValueLong(edtMinGap))
                    .build(this);
            startActivityForResult(intent, TrimVideo.VIDEO_TRIMMER_REQ_CODE);
        } else {
            Intent intent = TrimVideo.activity(data)
                    .setTrimType(TrimType.MIN_MAX_DURATION)
                    .setMinToMax(getEdtValueLong(edtMinFrom), getEdtValueLong(edtMAxTo))
                    .build(this);
            startActivityForResult(intent, TrimVideo.VIDEO_TRIMMER_REQ_CODE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_default_trim:
                onDefaultTrimClicked();
                break;
            case R.id.btn_fixed_gap:
                onFixedTrimClicked();
                break;
            case R.id.btn_min_gap:
                onMinGapTrimClicked();
                break;
            case R.id.btn_min_max_gap:
                onMinToMaxTrimClicked();
                break;
        }
    }

    private void onDefaultTrimClicked() {
        trimType = 0;
        if (checkCamStoragePer())
            showVideoOptions();
    }

    private void onFixedTrimClicked() {
        trimType = 1;
        if (isEdtTxtEmpty(edtFixedGap))
            Toast.makeText(this, "Enter fixed gap duration", Toast.LENGTH_SHORT).show();
        else if (checkCamStoragePer())
            showVideoOptions();
    }

    private void onMinGapTrimClicked() {
        trimType = 2;
        if (isEdtTxtEmpty(edtMinGap))
            Toast.makeText(this, "Enter min gap duration", Toast.LENGTH_SHORT).show();
        else if (checkCamStoragePer())
            showVideoOptions();
    }


    private void onMinToMaxTrimClicked() {
        trimType = 3;
        if (isEdtTxtEmpty(edtMinFrom))
            Toast.makeText(this, "Enter min gap duration", Toast.LENGTH_SHORT).show();
        else if (isEdtTxtEmpty(edtMAxTo))
            Toast.makeText(this, "Enter max gap duration", Toast.LENGTH_SHORT).show();
        else if (checkCamStoragePer())
            showVideoOptions();
    }

    public void showVideoOptions() {
        try {
            BottomSheet.Builder builder = getBottomSheet();
            builder.sheet(R.menu.menu_video);
            builder.listener(item -> {
                if (R.id.action_take == item.getItemId())
                    captureVideo();
                else
                    openVideo();
                return false;
            });
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BottomSheet.Builder getBottomSheet() {
        return new BottomSheet.Builder(this).title(R.string.txt_option);
    }

    public void captureVideo() {
        try {
            Intent intent = new Intent("android.media.action.VIDEO_CAPTURE");
            intent.putExtra("android.intent.extra.durationLimit", 30);
            startActivityForResult(intent, REQUEST_TAKE_VIDEO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openVideo() {
        try {
            Intent intent = new Intent();
            intent.setType("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_VIDEO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (isPermissionOk(grantResults))
            showVideoOptions();
    }

    private boolean isEdtTxtEmpty(EditText editText) {
        return editText.getText().toString().trim().isEmpty();
    }

    private long getEdtValueLong(EditText editText) {
        return Long.parseLong(editText.getText().toString().trim());
    }

    private boolean checkCamStoragePer() {
        return checkPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA);
    }

    private boolean checkPermission(String... permissions) {
        boolean allPermitted = false;
        for (String permission : permissions) {
            allPermitted = (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED);
            if (!allPermitted)
                break;
        }
        if (allPermitted)
            return true;
        ActivityCompat.requestPermissions(this, permissions,
                220);
        return false;
    }

    private boolean isPermissionOk(int... results) {
        boolean isAllGranted = true;
        for (int result : results) {
            if (PackageManager.PERMISSION_GRANTED != result) {
                isAllGranted = false;
                break;
            }
        }
        return isAllGranted;
    }
}