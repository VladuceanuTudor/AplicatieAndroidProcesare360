package com.example.aplicatieandroidprocesare360;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicatieandroidprocesare360.api.ApiClient;
import com.example.aplicatieandroidprocesare360.api.ProcessingService;
import com.example.aplicatieandroidprocesare360.api.model.LoginRequest;
import com.example.aplicatieandroidprocesare360.api.model.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getSharedPreferences("panorama_prefs", MODE_PRIVATE);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);

        TextView tvServerUrl = findViewById(R.id.tv_server_url);
        TextView tvChangeUrl = findViewById(R.id.tv_change_url);

        tvServerUrl.setText(prefs.getString("api_url", ""));
        etUsername.setText(prefs.getString("api_username", ""));

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvChangeUrl.setOnClickListener(v -> {
            startActivity(new Intent(this, SetupUrlActivity.class));
            finish();
        });
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.error_credentials_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String apiUrl = prefs.getString("api_url", "");
        if (apiUrl.isEmpty()) {
            startActivity(new Intent(this, SetupUrlActivity.class));
            finish();
            return;
        }

        setLoading(true);
        ProcessingService service = ApiClient.getProcessingService(apiUrl);
        service.login(new LoginRequest(username, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> r) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            if (r.isSuccessful() && r.body() != null) {
                                prefs.edit().putString("api_username", username).apply();
                                ApiClient.saveToken(LoginActivity.this, r.body().getAccessToken());
                                startActivity(new Intent(LoginActivity.this, MainActivity.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        R.string.settings_login_failed, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this,
                                    R.string.error_network, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
