package com.am.mytodolistapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.am.mytodolistapp.MainActivity;
import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.FirebaseRepository;
import com.am.mytodolistapp.data.firebase.User;
import com.am.mytodolistapp.ui.collaboration.CollaborationFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthFragment extends Fragment {
    private static final String TAG = "AuthFragment";
    private static final int RC_SIGN_IN = 9001;

    // UI 컴포넌트들
    private EditText editEmail, editPassword;
    private Button btnLogin, btnRegister, btnGoogleSignIn;
    private ProgressBar progressBar;
    private TextView textSwitchMode, textTitle;

    // Firebase 관련
    private FirebaseAuth firebaseAuth;
    private FirebaseRepository firebaseRepository;
    private GoogleSignInClient googleSignInClient;

    // 상태 관리
    private boolean isLoginMode = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase 초기화
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseRepository = FirebaseRepository.getInstance();

        // Google Sign-In 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        Log.d(TAG, "AuthFragment created");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();
        updateUI();

        Log.d(TAG, "AuthFragment view created");
    }

    private void initViews(View view) {
        editEmail = view.findViewById(R.id.edit_email);
        editPassword = view.findViewById(R.id.edit_password);
        btnLogin = view.findViewById(R.id.btn_login);
        btnRegister = view.findViewById(R.id.btn_register);
        btnGoogleSignIn = view.findViewById(R.id.btn_google_sign_in);
        progressBar = view.findViewById(R.id.progress_bar);
        textSwitchMode = view.findViewById(R.id.text_switch_mode);
        textTitle = view.findViewById(R.id.text_title);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                performEmailLogin();
            } else {
                performEmailRegister();
            }
        });

        btnRegister.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        textSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            textTitle.setText("로그인");
            btnLogin.setText("로그인");
            btnRegister.setText("회원가입으로 전환");
            textSwitchMode.setText("계정이 없으신가요? 회원가입");
        } else {
            textTitle.setText("회원가입");
            btnLogin.setText("회원가입");
            btnRegister.setText("로그인으로 전환");
            textSwitchMode.setText("이미 계정이 있으신가요? 로그인");
        }
    }

    private void performEmailLogin() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        showProgress(true);
        Log.d(TAG, "Performing email login for: " + email);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email login successful");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        handleSuccessfulLogin(user);
                    } else {
                        Log.e(TAG, "Email login failed", task.getException());
                        Toast.makeText(getContext(), "로그인 실패: " +
                                        (task.getException() != null ? task.getException().getMessage() : "알 수 없는 오류"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performEmailRegister() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        showProgress(true);
        Log.d(TAG, "Performing email registration for: " + email);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email registration successful");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        handleSuccessfulLogin(user);
                    } else {
                        Log.e(TAG, "Email registration failed", task.getException());
                        Toast.makeText(getContext(), "회원가입 실패: " +
                                        (task.getException() != null ? task.getException().getMessage() : "알 수 없는 오류"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        showProgress(true);
        Log.d(TAG, "Starting Google sign-in");

        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google sign-in successful: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Google sign-in failed", e);
                showProgress(false);
                Toast.makeText(getContext(), "Google 로그인 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase auth with Google successful");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        handleSuccessfulLogin(user);
                    } else {
                        Log.e(TAG, "Firebase auth with Google failed", task.getException());
                        Toast.makeText(getContext(), "Google 로그인 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 🆕 로그인 성공 처리 (동기화 시작 포함)
    private void handleSuccessfulLogin(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            Log.e(TAG, "FirebaseUser is null after successful login");
            return;
        }

        Log.d(TAG, "Handling successful login for user: " + firebaseUser.getEmail());

        // 사용자 정보를 Firestore에 저장
        User user = new User();
        user.setUid(firebaseUser.getUid());
        user.setEmail(firebaseUser.getEmail());
        user.setDisplayName(firebaseUser.getDisplayName());
        user.setCreatedAt(System.currentTimeMillis());

        firebaseRepository.saveUser(user, new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "User saved to Firestore successfully");

                // 🆕 MainActivity에 로그인 성공 알림 (동기화 시작)
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onUserLoggedIn();
                }

                // UI 업데이트
                Toast.makeText(getContext(), "로그인 성공! 환영합니다, " +
                                (firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : firebaseUser.getEmail()),
                        Toast.LENGTH_SHORT).show();

                // 협업 화면으로 이동
                navigateToCollaboration();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to save user to Firestore", e);

                // Firestore 저장 실패해도 로그인은 성공한 상태이므로 계속 진행
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onUserLoggedIn();
                }

                Toast.makeText(getContext(), "로그인은 성공했지만 사용자 정보 저장에 실패했습니다.", Toast.LENGTH_LONG).show();
                navigateToCollaboration();
            }
        });
    }

    private void navigateToCollaboration() {
        if (getActivity() != null) {
            // Fragment 교체
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CollaborationFragment())
                    .commit();

            Log.d(TAG, "Navigated to CollaborationFragment");
        }
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            editEmail.setError("이메일을 입력해주세요");
            editEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("올바른 이메일 형식을 입력해주세요");
            editEmail.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            editPassword.setError("비밀번호를 입력해주세요");
            editPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            editPassword.setError("비밀번호는 6자 이상이어야 합니다");
            editPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        // 버튼들 비활성화/활성화
        btnLogin.setEnabled(!show);
        btnRegister.setEnabled(!show);
        btnGoogleSignIn.setEnabled(!show);

        Log.d(TAG, "Progress visibility: " + (show ? "VISIBLE" : "GONE"));
    }

    @Override
    public void onStart() {
        super.onStart();

        // 이미 로그인된 사용자가 있는지 확인
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: " + currentUser.getEmail());

            // 🆕 이미 로그인된 경우에도 동기화 확인
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onUserLoggedIn();
            }

            navigateToCollaboration();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "AuthFragment view destroyed");
    }
}