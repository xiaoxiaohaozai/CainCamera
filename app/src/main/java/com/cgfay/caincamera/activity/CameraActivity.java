package com.cgfay.caincamera.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.cgfay.caincamera.R;
import com.cgfay.caincamera.core.CameraDrawer;
import com.cgfay.caincamera.utils.CameraUtils;
import com.cgfay.caincamera.utils.PermissionUtils;
import com.cgfay.caincamera.view.AspectFrameLayout;
import com.cgfay.caincamera.view.CameraSurfaceView;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener,
        CameraSurfaceView.OnClickListener, CameraSurfaceView.OnTouchScroller {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA = 0x01;
    private static final int REQUEST_STORAGE_READ = 0x02;
    private static final int REQUEST_STORAGE_WRITE = 0x03;
    private static final int REQUEST_RECORD = 0x04;
    private static final int REQUEST_LOCATION = 0x05;

    // 权限使能标志
    private boolean mCameraEnable = false;
    private boolean mStorageReadEnable = false;
    private boolean mStorageWriteEnable = false;
    private boolean mRecordEnable = false;
    private boolean mLocationEnable = false;

    // 状态标志
    private boolean mOnPreviewing = false;
    private boolean mOnRecording = false;

    private AspectFrameLayout mAspectLayout;
    private CameraSurfaceView mCameraSurfaceView;
    private Button mBtnBack;
    private Button mBtnSetting;
    private Button mBtnMore;
    private Button mBtnViewPhoto;
    private Button mBtnTake;
    private Button mBtnSwitch;

    private float[] mAspectRatio = {
            CameraUtils.Ratio_3_4,
            CameraUtils.Ratio_1_1,
            CameraUtils.Ratio_9_16
    };
    private int mRatioIndex = 0;
    private float mCurrentRatio = mAspectRatio[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        if (PermissionUtils.permissionChecking(this, Manifest.permission.CAMERA)) {
            mCameraEnable = true;
            initView();
        } else {
            requestCameraPermission();
        }
    }

    private void initView() {
        mAspectLayout = (AspectFrameLayout) findViewById(R.id.layout_aspect);
        mAspectLayout.setAspectRatio(mCurrentRatio);
        mCameraSurfaceView = new CameraSurfaceView(this);
        mCameraSurfaceView.addScroller(this);
        mCameraSurfaceView.addClickListener(this);
        mAspectLayout.addView(mCameraSurfaceView);
        mAspectLayout.requestLayout();
        mBtnBack = (Button) findViewById(R.id.btn_back);
        mBtnBack.setOnClickListener(this);
        mBtnSetting = (Button)findViewById(R.id.btn_setting);
        mBtnSetting.setOnClickListener(this);
        mBtnMore = (Button) findViewById(R.id.btn_more);
        mBtnMore.setOnClickListener(this);
        mBtnViewPhoto = (Button) findViewById(R.id.btn_view_photo);
        mBtnViewPhoto.setOnClickListener(this);
        mBtnTake = (Button) findViewById(R.id.btn_take);
        mBtnTake.setOnClickListener(this);
        mBtnSwitch = (Button) findViewById(R.id.btn_switch);
        mBtnSwitch.setOnClickListener(this);

        CameraUtils.calculateCameraPreviewOrientation(CameraActivity.this);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{ Manifest.permission.CAMERA }, REQUEST_CAMERA);
    }

    private void requestStorageReadPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_READ);
    }

    private void requestStorageWritePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_WRITE);
    }

    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission_group.MICROPHONE}, REQUEST_RECORD);
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                },
                REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            // 相机权限
            case REQUEST_CAMERA:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraEnable = true;
                    initView();
                    CameraUtils.startPreview();
                    mOnPreviewing = true;
                }
                break;

            // 读取存储权限
            case REQUEST_STORAGE_READ:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStorageReadEnable = true;
                }
                break;

            // 写入存储权限(只有在拍照时才请求)
            case REQUEST_STORAGE_WRITE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStorageWriteEnable = true;
                    if (mOnPreviewing) {
                        CameraDrawer.INSTANCE.takePicture();
                    }
                }
                break;

            // 录音权限
            case REQUEST_RECORD:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mRecordEnable = true;
                }
                break;

            // 位置权限
            case REQUEST_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationEnable = true;
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraEnable = PermissionUtils.permissionChecking(this, Manifest.permission.CAMERA);
        if (mCameraEnable) {
            CameraUtils.startPreview();
            mOnPreviewing = true;
        } else {
            requestCameraPermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraEnable) {
            CameraUtils.stopPreview();
            mOnPreviewing = false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_view_photo:
                break;

            case R.id.btn_take:
                takePicture();
                break;

            case R.id.btn_switch:
                switchCamera();
                break;

            case R.id.btn_back:
                onBackPressed();
                break;

            case R.id.btn_setting:
                showSettingPopView();
                break;

            case R.id.btn_more:
                openMoreSetting();
                break;
        }
    }

    @Override
    public void swipeBack() {
        Log.d(TAG, "swipeBack");
    }

    @Override
    public void swipeFrontal() {
        Log.d(TAG, "swipeFrontal");
    }

    @Override
    public void swipeUpper(boolean startInLeft) {
        Log.d(TAG, "swipeUpper, startInLeft ? " + startInLeft);
    }

    @Override
    public void swipeDown(boolean startInLeft) {
        Log.d(TAG, "swipeDown, startInLeft ? " + startInLeft);
    }

    @Override
    public void onClick(float x, float y) {
        surfaceViewClick(x, y);
    }

    @Override
    public void doubleClick(float x, float y) {
        changePreviewRatio();
    }

    /**
     * 切换预览宽高比
     */
    private void changePreviewRatio() {
        mRatioIndex++;
        mRatioIndex %= mAspectRatio.length;
        mCurrentRatio = mAspectRatio[mRatioIndex];
        if (mAspectLayout != null) {
            CameraUtils.setCurrentRatio(mCurrentRatio);
            mAspectLayout.setAspectRatio(mCurrentRatio);
        }
    }

    /**
     * 点击SurfaceView
     * @param x x轴坐标
     * @param y y轴坐标
     */
    private void surfaceViewClick(float x, float y) {

    }

    /**
     * 拍照
     */
    private void takePicture() {
        if (!mOnPreviewing) {
            return;
        }
        if (mStorageWriteEnable
                || PermissionUtils.permissionChecking(this, Manifest.permission_group.STORAGE)) {
            CameraDrawer.INSTANCE.takePicture();
        } else {
            requestStorageWritePermission();
        }
    }

    /**
     * 切换相机
     */
    private void switchCamera() {
        if (!mCameraEnable) {
            requestCameraPermission();
            return;
        }
        if (mCameraSurfaceView != null) {
            CameraUtils.switchCamera(1 - CameraUtils.getCameraID());
        }
    }

    private void showSettingPopView() {

    }

    private void openMoreSetting() {

    }
}