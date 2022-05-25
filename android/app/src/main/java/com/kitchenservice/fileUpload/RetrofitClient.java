package com.kitchenservice.fileUpload;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private  static final String BASE_URL = "http://ec2-35-165-5-106.us-west-2.compute.amazonaws.com:5000";                          // Base URL
    private static RetrofitClient mInstance;
    private Retrofit retrofit;

    private RetrofitClient() {
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (mInstance == null) {
            mInstance = new RetrofitClient();
        }
        return mInstance;
    }

    public API getAPI() {
        return retrofit.create(API.class);
    }

    public API getImageAPI() {
        return new Retrofit.Builder()
                .baseUrl("https://api.jsonbin.io/v3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(API.class);
    }

    public API getSearchAPI() {
        return new Retrofit.Builder()
                .baseUrl("https://api.jsonbin.io/v3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(API.class);
    }
}

