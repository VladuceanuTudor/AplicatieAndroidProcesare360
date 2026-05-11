package com.example.aplicatieandroidprocesare360.api.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("access_token") private String accessToken;

    public String getAccessToken() { return accessToken; }
}
