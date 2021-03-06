package com.kitchenservice.fileUpload;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Upload extends AppCompatActivity {

    TextView imgPath;
    private static final int PICK_IMAGE_REQUEST = 9544;
    ImageView image;
    Uri selectedImage;
    String part_image;

    private static final int IMAGE_MEAN = 256;
    private static final float IMAGE_STD = 256.0f;

    // Permissions for accessing the storage
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload);
        imgPath = findViewById(R.id.item_img);
        image = findViewById(R.id.img);
    }

    // Method for starting the activity for selecting image from phone storage
    public void pick(View view) {
        verifyStoragePermissions(Upload.this);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Open Gallery"), PICK_IMAGE_REQUEST);
    }

    // Method to get the absolute path of the selected image from its URI
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                selectedImage = data.getData();                                                         // Get the image file URI
                String[] imageProjection = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedImage, imageProjection, null, null, null);
                if(cursor != null) {
                    cursor.moveToFirst();
                    part_image = selectedImage.getPath();
                    imgPath.setText(part_image);                                                        // Get the image file absolute path
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    image.setImageBitmap(bitmap);                                                       // Set the ImageView with the bitmap of the image
                }
            }
        }
    }

    // Upload the image to the remote database
    public void uploadImage(View view) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount()); //Create a new buffer
        bitmap.copyPixelsToBuffer(buffer);
        //File imageFile = new File(part_image);                                                          // Create a file using the absolute path of the image
        RequestBody reqBody = RequestBody.create(MediaType.parse("multipart/form-file"), buffer.array());
        MultipartBody.Part partImage = MultipartBody.Part.createFormData("image1", UUID.randomUUID().toString(), reqBody);
        API api = RetrofitClient.getInstance().getAPI();
        Call<ResponseBody> upload = api.uploadImage(partImage);
        upload.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()) {
                    try {
                        Toast.makeText(Upload.this, "Kitchen Photo Accepted" + response.body().string(), Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(Upload.this, Results.class);
                        i.putExtra("key", response.body().string());
                        startActivity(i);                    // Start the activity to get all images

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(Upload.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Upload the image to the remote database
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void uploadImageTfLite(View view) throws IOException {
        long startTime = SystemClock.uptimeMillis();
        //File imageFile = new File(part_image);                                                          // Create a file using the absolute path of the image
        Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
        try {
            // Initialize interpreter with GPU delegate
            Interpreter.Options options = new Interpreter.Options();
            CompatibilityList compatList = new CompatibilityList();

            if(compatList.isDelegateSupportedOnThisDevice()){
                // if the device has a supported GPU, add the GPU delegate
                GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
                GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
                options.addDelegate(gpuDelegate);
                Log.d("Result", "kitchenClassifer: GPU Enabled for Tensorflow.");
            } else {
                // if the GPU is not supported, run on 4 threads
                options.setNumThreads(4);
            }

            //ConvertedModel model = ConvertedModel.newInstance(this);
            Interpreter interpreter = new Interpreter(FileUtil.loadMappedFile(this, "converted_model.tflite"), options);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 512, 512, 3}, DataType.FLOAT32);
            Bitmap bitmap = Bitmap.createScaledBitmap(imageBitmap, 512, 512, true);
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);
            inputFeature0.loadBuffer(byteBuffer);

            float[][] result = new float[1][3];
            interpreter.run(byteBuffer, result);
            Toast.makeText(Upload.this, "Kitchen Photo Accepted" + getResult(result), Toast.LENGTH_LONG).show();

            long endTime = SystemClock.uptimeMillis();
            String runTime = String.valueOf(endTime - startTime);

            Log.d("Result", "kitchenClassifer: " + runTime + "ms");

            Thread.sleep(3000);

            Intent i = new Intent(Upload.this, Results.class);
            i.putExtra("key", getResult(result));
            startActivity(i);
        } catch (Exception e) {
            // TODO Handle the exception
            Toast.makeText(Upload.this, "Kitchen Not Accepted" + e.getMessage(), Toast.LENGTH_LONG).show();

        }

    }

    @SuppressLint("DefaultLocale")
    private String getResult(float[][] labelProbArray) {

        List<String> labelList = new ArrayList<>();
        labelList.add("One-Wall Layout");
        labelList.add("U-Shaped Layout");
        labelList.add("L-Shaped Layout");

        for (int i = 0; i < labelList.size(); ++i) {
            // confidence : ??????, detection percentage
            float confidence = (labelProbArray[0][i] * 100) / 127.0f; //  & 0xff ????????????, ?????????????????? 100 ???????????? ???????????? ??? ????????????

            Log.d("Result", "kitchenClassifer: Confidence:" + labelProbArray[0][i]);

            // 0.1(10%) ???????????? ??????, ?????? ??????
            if (Math.abs(labelProbArray[0][i]) > 0.08) {
                return labelList.get(i);
            }
        }
        return "No Labels";
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 512 * 512 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[512 * 512];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < 512; ++i) {
            for (int j = 0; j < 512; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                byteBuffer.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                byteBuffer.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
        return byteBuffer;
    }


    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}