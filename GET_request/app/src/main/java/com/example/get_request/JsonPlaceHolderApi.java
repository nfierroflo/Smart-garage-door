package com.example.get_request;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface JsonPlaceHolderApi {

    @GET("1.json?api_key=7HKPDYRI1LSKJW57&results=1")
    Call<Post> getPosts();
}
