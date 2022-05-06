package com.kitchenservice.fileUpload;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetByName extends AppCompatActivity {

    ListView list;
    ImageView image;
    ProgressBar progressBar;
    List<Image> images;
    List<String> fileNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get_by_name);
        list = findViewById(R.id.image_name);
        image = findViewById(R.id.img2);
        progressBar = findViewById(R.id.progressBar2);
        getList();                                                                                      // Get all the image file name
        // Set a listener event when the user clicks on a list item
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = parent.getItemAtPosition(position).toString();
                image.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                getImage(name);
            }
        });
    }

    // Get all the image file names
    public void getList() {
        API api = RetrofitClient.getInstance().getAPI();
        Call<List<Image>> getList = api.getImages();
        getList.enqueue(new Callback<List<Image>>() {
            @Override
            public void onResponse(Call<List<Image>> call, Response<List<Image>> response) {
                if (response.isSuccessful()) {
                    images = response.body();
                    for (Image img : images) {
                        fileNames.add(img.getName());
                    }
                    ArrayAdapter arrayAdapter = new ArrayAdapter<String>(GetByName.this, R.layout.list_item, fileNames);
                    list.setAdapter(arrayAdapter);
                    Toast.makeText(GetByName.this, "Fetched List", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Image>> call, Throwable t) {
                Toast.makeText(GetByName.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Get the image upon selecting the specific file name from the ListView
    public void getImage(String name) {
        final Bitmap[] bmp = new Bitmap[1];
        API api = RetrofitClient.getInstance().getAPI();
        Call<ResponseBody> getImage = api.getImageByName(name);
        getImage.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                byte[] bytes = new byte[0];
                                try {
                                    bytes = response.body().bytes();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                bmp[0] = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            }
                        });
                        thread.start();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    image.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    image.setImageBitmap(bmp[0]);
                    Toast.makeText(GetByName.this, "Image Loaded", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(GetByName.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
