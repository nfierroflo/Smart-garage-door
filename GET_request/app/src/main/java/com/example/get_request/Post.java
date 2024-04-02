package com.example.get_request;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit2.Call;

public class Post {
    private channel channel;
    private List<feed> feeds;

    public com.example.get_request.channel getChannel() {
        return channel;
    }

    public List<feed> getFeeds() {
        return feeds;
    }
}
