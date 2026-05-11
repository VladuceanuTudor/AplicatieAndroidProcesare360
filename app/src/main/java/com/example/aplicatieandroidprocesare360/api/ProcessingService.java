package com.example.aplicatieandroidprocesare360.api;

import com.example.aplicatieandroidprocesare360.api.model.JobCreateResponse;
import com.example.aplicatieandroidprocesare360.api.model.LoginRequest;
import com.example.aplicatieandroidprocesare360.api.model.LoginResponse;
import com.example.aplicatieandroidprocesare360.api.model.ProcessingJob;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ProcessingService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @Multipart
    @POST("jobs")
    Call<JobCreateResponse> createJob(@Part MultipartBody.Part video);

    @GET("jobs/{job_id}/status")
    Call<ProcessingJob> getStatus(@Path("job_id") String jobId);
}
