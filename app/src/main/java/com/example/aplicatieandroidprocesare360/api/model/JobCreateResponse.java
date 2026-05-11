package com.example.aplicatieandroidprocesare360.api.model;

import com.google.gson.annotations.SerializedName;

public class JobCreateResponse {
    @SerializedName("job_id") private String jobId;
    @SerializedName("status") private String status;

    public String getJobId()  { return jobId; }
    public String getStatus() { return status; }
}
