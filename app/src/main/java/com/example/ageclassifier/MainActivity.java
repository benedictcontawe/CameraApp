package com.example.ageclassifier;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.ageclassifier.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static Intent newIntent(Context context) { Log.d(TAG,"newIntent()");
        return new Intent(context.getApplicationContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }
    private static final int PERMISSION_STATE = 0;
    private static final int CAMERA_REQUEST = 1;
    private Button imgCamera;
    private ImageView imgResult;
    private Button btnPredict;
    private TextView txtPrediction;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) { Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgCamera = (Button) findViewById(R.id.captureButton);
        imgResult = (ImageView) findViewById(R.id.resultImage);
        txtPrediction = (TextView) findViewById(R.id.textViewPrediction);
        btnPredict = (Button) findViewById(R.id.scanButton);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scanButton:
                predict();
                break;
            case R.id.captureButton:
                launchCamera();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() { Log.d(TAG,"onResume()");
        super.onResume();
        btnPredict.setOnClickListener(this::onClick);
        imgCamera.setOnClickListener(this::onClick);
        checkPermissions();
    }

    @Override
    protected void onPause() { Log.d(TAG,"onPause()");
        super.onPause();
        btnPredict.setOnClickListener(null);
        imgCamera.setOnClickListener(null);
    }

    private void launchCamera() { Log.d(TAG,"launchCamera()");
        startActivityForResult(new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST);
    }



    private void predict() { Log.d(TAG,"predict()");
        bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        try { Log.d(TAG,"try");
            Model model = Model.newInstance(getApplicationContext());
            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.UINT8);
            TensorImage tensorImage = new TensorImage(DataType.UINT8);
            tensorImage.load(bitmap);
            ByteBuffer byteBuffer = tensorImage.getBuffer();

            inputFeature0.loadBuffer(byteBuffer);
            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            // Releases model resources if no longer used.
            model.close();
            txtPrediction.setText(getMax(outputFeature0.getFloatArray()));//txtPrediction.setText(outputFeature0.getFloatArray()[0] + "\n" + outputFeature0.getFloatArray()[1] + "\n" + outputFeature0.getFloatArray()[2]);
            getMax(outputFeature0.getFloatArray());
            Log.d("Result",Arrays.toString(outputFeature0.getFloatArray()));
        } catch (IOException e) {
            Log.e(TAG,"IOException " + e.getMessage());
        }
    }

    private String getMax(float [] outputs) { Log.d(TAG,"getMax( " + Arrays.toString(outputs) + ")");
        if (outputs.length != 0 & outputs[0] > outputs[1] & outputs[0] > outputs[2] & outputs[0] > outputs[3]) {
            return "BARBODES";
        } else if (outputs.length != 0 & outputs[1] > outputs[0] & outputs[1] > outputs[2] & outputs[1] > outputs[3]) {
            return "CAT FISH";
        } else if (outputs.length != 0 & outputs[2] > outputs[0] & outputs[2] > outputs[1] & outputs[2] > outputs[3]) {
            return "GALUNGGONG";
        }else if (outputs.length != 0 & outputs[3] > outputs[0] & outputs[3] > outputs[1]  & outputs[3] > outputs[2]) {
            return "NAPOLEON WRASSE";
        } else {
            return "";
        }
    }

    private void checkPermissions() {
        String[] manifestPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            manifestPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else {
            manifestPermissions = new String[] {
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
        for (String permission : manifestPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"Permission Granted " + permission);
            }
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG,"Permission Denied " + permission);
                requestPermissions();
            }
        }
    }

    private void requestPermissions() { Log.d(TAG, "requestPermissions()");
        String[] manifestPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            manifestPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else {
            manifestPermissions = new String[] {
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        ActivityCompat.requestPermissions(
                this,
                manifestPermissions,
                PERMISSION_STATE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "PermissionsResult requestCode " + requestCode);
        Log.d(TAG, "PermissionsResult permissions " + Arrays.toString(permissions));
        Log.d(TAG, "PermissionsResult grantResults " + Arrays.toString(grantResults));
        if (requestCode == PERMISSION_STATE) {
            for (int grantResult : grantResults) {
                switch (grantResult) {
                    case PackageManager.PERMISSION_GRANTED:
                        Log.d(TAG, "PermissionsResult grantResult Allowed " + grantResult);
                        break;
                    case PackageManager.PERMISSION_DENIED:
                        Log.d(TAG, "PermissionsResult grantResult Denied " + grantResult);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult requestCode " + requestCode + " resultCode" + resultCode + "data " + data);
         if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null) {
            bitmap = (Bitmap) data.getExtras().get("data");
            imgResult.setImageBitmap(bitmap);
        }
    }
}