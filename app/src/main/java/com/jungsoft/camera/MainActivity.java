package com.jungsoft.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int REQUEST_PERMISSION = 1001;

    // 카메라 제공자
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;


    Button btnTakePicture, btnRecording;
    PreviewView previewView;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();

        btnTakePicture = findViewById(R.id.btnCapture);
        btnRecording = findViewById(R.id.btnRecord);
        previewView = findViewById(R.id.previewView);

        btnTakePicture.setOnClickListener(this);
        btnRecording.setOnClickListener(this);

        // 카메라 제공자 인스턴스 초기화
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        // 카메라 제공자에 리스너 등록 (카메라 추가)
        cameraProviderFuture.addListener(() -> {
            try {
                // 카메라 장치 가져오기
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // 카메라 사용 함수
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor()); // 실행기
    }


    // Executor 스레드를 직접적으로 다루는 가장 최상위 API
    // Executor 를 사용하면 개발자가 직접 스레드를 만들 필요가 없다
    // Runnable 처럼 Executor 는 Callable 을 제공
    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission
                    (this, Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA
                        },
                        REQUEST_PERMISSION);


                // You can use the API that requires the permission.

            }
        }
    }


    @SuppressLint("RestrictedApi") // RestrictedApi 린트검사 중지
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        // 카메라 이전 바인딩 해제
        cameraProvider.unbindAll();

        // 기본 카메라 선택
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // 카메라 미리보기
        Preview preview = new Preview.Builder().build();

        // 서페이스뷰와 카메라 미리보기 연결
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 이미지 캡쳐
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // 비디오 캡쳐
        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .build();

        // 카메라 바인딩
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture, videoCapture);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnCapture:
                capturePhoto();
                break;

            case R.id.btnRecord:
                if (btnRecording.getText() == "start recording") {
                    btnRecording.setText("stop recording");
                    recordVideo();
                } else {
                    btnRecording.setText("start recording");
                    videoCapture.stopRecording();
                }
                break;
        }
    }

    @SuppressLint("RestrictedApi")
    private void recordVideo() {
        if (videoCapture != null) {
            File newPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File photoDir = new File(newPath, "CameraXMovies/");

            // 사진 폴더가 존재하는지 체크
            if (!photoDir.exists()) {
                photoDir.mkdir();
            }

            Date date = new Date();
            String timeStamp = String.valueOf(date.getTime());
            String vidFilePath = photoDir.getAbsolutePath() + "/" + timeStamp + ".mp4";

            File vidFile = new File(vidFilePath);


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            videoCapture.startRecording(
                    new VideoCapture.OutputFileOptions.Builder(vidFile).build(),
                    getExecutor(),
                    new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            Toast.makeText(MainActivity.this, "동영상 녹화가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                            Toast.makeText(MainActivity.this, "Error " + videoCaptureError, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }
    }

    private void capturePhoto() {

        File newPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File photoDir = new File(newPath,"CameraXPhotos/");

        // 사진 폴더가 존재하는지 체크
        if(!photoDir.exists()) {
            photoDir.mkdir();
        }

        Date date = new Date();
        String timeStamp = String.valueOf(date.getTime());
        String photoFilePath = photoDir.getAbsolutePath() + "/" + timeStamp + ".jpeg";

        File photoFile = new File(photoFilePath);

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(photoFile).build(), // 사진을 저장할 위치
                getExecutor(), // 스레드 생성
                new ImageCapture.OnImageSavedCallback() {
                    // 사진이 정상적으로 저장 됐다면..
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this, "사진이 정상적으로 저장돼었습니다.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Error " + exception, Toast.LENGTH_SHORT).show();
                    }
                }

        );

    }
}