package com.am.mytodolistapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.am.mytodolistapp.MainActivity;
import com.am.mytodolistapp.R;
import com.am.mytodolistapp.service.FirebaseAuthService;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private Button buttonGoogleLogin;
    private TextView textSignUp;
    private TextView textForgotPassword;

    private FirebaseAuthService authService;

    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = new FirebaseAuthService(this);

        // 이미 로그인된 경우 메인 화면으로
        if (authService.isUserSignedIn()) {
            startMainActivity();
            return;
        }

        initViews();
        setupGoogleSignInLauncher();
        setupClickListeners();
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.edit_text_email);
        editTextPassword = findViewById(R.id.edit_text_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonGoogleLogin = findViewById(R.id.button_google_login);
        textSignUp = findViewById(R.id.text_sign_up);
        textForgotPassword = findViewById(R.id.text_forgot_password);
    }

    private void setupGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        authService.handleGoogleSignInResult(result.getData(), new FirebaseAuthService.AuthCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(LoginActivity.this, "Google 로그인 성공!", Toast.LENGTH_SHORT).show();
                                startMainActivity();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(LoginActivity.this, "Google 로그인 실패: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
        );
    }

    private void setupClickListeners() {
        // 이메일 로그인
        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            buttonLogin.setEnabled(false);
            authService.signInWithEmail(email, password, new FirebaseAuthService.AuthCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                    startMainActivity();
                }

                @Override
                public void onError(String error) {
                    buttonLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "로그인 실패: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });

        // Google 로그인
        buttonGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = authService.getGoogleSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        // 회원가입 화면으로
        textSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        });

        // 비밀번호 찾기
        textForgotPassword.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력한 후 비밀번호 찾기를 눌러주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            authService.sendPasswordResetEmail(email, new FirebaseAuthService.PasswordResetCallback() {
                @Override
                public void onEmailSent() {
                    Toast.makeText(LoginActivity.this, "비밀번호 재설정 이메일을 전송했습니다.", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(LoginActivity.this, "이메일 전송 실패: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}