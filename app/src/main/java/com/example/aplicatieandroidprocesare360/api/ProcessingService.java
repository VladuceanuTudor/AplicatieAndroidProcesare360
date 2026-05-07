package com.example.aplicatieandroidprocesare360.api;

import com.example.aplicatieandroidprocesare360.api.model.ProcessingJob;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ProcessingService {

    @Multipart
    @POST("upload")
    Call<ProcessingJob> uploadFile(
            @Part MultipartBody.Part file,
            @Part("quality") RequestBody quality,
            @Part("depth_estimation") RequestBody depthEstimation,
            @Part("mesh_generation") RequestBody meshGeneration,
            @Part("color_correction") RequestBody colorCorrection,
            @Part("hdr") RequestBody hdr
    );

    @GET("status/{job_id}")
    Call<ProcessingJob> getStatus(@Path("job_id") String jobId);

    @GET("result/{job_id}")
    Call<ProcessingJob> getResult(@Path("job_id") String jobId);
}
