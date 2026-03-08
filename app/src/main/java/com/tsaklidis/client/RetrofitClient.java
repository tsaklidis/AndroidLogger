package com.tsaklidis.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    private static OkHttpClient okHttpClient;
    private static Methods methods;
    private static final String BASE_URL = "https://logs.tsaklidis.gr/";

    private static synchronized OkHttpClient getOkHttpClient(Context context) {
        if (okHttpClient == null) {
            final Context appContext = context.getApplicationContext();
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            SharedPreferences prefs = appContext.getSharedPreferences("LoggerPrefs", Context.MODE_PRIVATE);
                            String token = prefs.getString("auth_token", "").trim();
                            
                            Request original = chain.request();
                            Request.Builder builder = original.newBuilder();
                            
                            // Log the request URL
                            Log.d("RetrofitClient", "Request URL: " + original.url());
                            
                            // Use same session-like headers
                            builder.header("Connection", "keep-alive");
                            builder.header("Accept", "application/json");
                            
                            if (!token.isEmpty()) {
                                builder.header("Authorization", "Token " + token);
                            }
                            
                            return chain.proceed(builder.build());
                        }
                    })
                    .build();
        }
        return okHttpClient;
    }

    public static synchronized Retrofit getRetrofitInstance(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static synchronized Methods getMethods(Context context) {
        if (methods == null) {
            methods = getRetrofitInstance(context).create(Methods.class);
        }
        return methods;
    }
}
