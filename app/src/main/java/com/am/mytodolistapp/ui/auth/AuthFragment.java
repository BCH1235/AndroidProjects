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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

// 사용자 로그인 및 회원가입 UI와 로직을 처리하는 프래그먼트
// Firebase Authentication을 사용하여 이메일/비밀번호 및 Google 로그인을 지원한다.

public class AuthFragment extends Fragment {
    private static final String TAG = "AuthFragment";
    private static final int RC_SIGN_IN = 9001;

    // UI 컴포넌트들
    private EditText editEmail, editPassword, editDisplayName; // 닉네임 EditText 추가
    private TextInputLayout layoutDisplayName; // 닉네임 레이아웃 추가
    private Button btnLogin, btnGoogleSignIn;

    private ProgressBar progressBar;
    private TextView textSwitchMode, textTitle;

    // Firebase 관련
    private FirebaseAuth firebaseAuth;
    private FirebaseRepository firebaseRepository;
    private GoogleSignInClient googleSignInClient;

    // 상태 관리
    private boolean isLoginMode = true; // true: 로그인 모드, false: 회원가입 모드

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase 인스턴스 초기화
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseRepository = FirebaseRepository.getInstance();

        // Google 로그인 옵션 설정
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


    //XML 레이아웃의 뷰들을 초기화합니다.
    private void initViews(View view) {
        editEmail = view.findViewById(R.id.edit_email);
        editPassword = view.findViewById(R.id.edit_password);
        editDisplayName = view.findViewById(R.id.edit_display_name); // 닉네임 EditText 초기화
        layoutDisplayName = view.findViewById(R.id.layout_display_name); // 닉네임 레이아웃 초기화
        btnLogin = view.findViewById(R.id.btn_login);
        btnGoogleSignIn = view.findViewById(R.id.btn_google_sign_in);
        progressBar = view.findViewById(R.id.progress_bar);
        textSwitchMode = view.findViewById(R.id.text_switch_mode);
        textTitle = view.findViewById(R.id.text_title);
    }


    //버튼 클릭 리스너들을 설정
    private void setupClickListeners() {
        // 이메일 로그인/회원가입 버튼
        btnLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                performEmailLogin();
            } else {
                performEmailRegister();
            }
        });

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle()); // Google 로그인 버튼

        // 로그인/회원가입 모드 전환 텍스트
        textSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });
    }


    // 현재 모드(로그인/회원가입)에 따라 UI를 업데이트
    private void updateUI() {
        if (isLoginMode) {
            textTitle.setText("로그인");
            btnLogin.setText("로그인");
            textSwitchMode.setText("계정이 없으신가요? 회원가입");
            layoutDisplayName.setVisibility(View.GONE); // 로그인 모드에서는 닉네임 필드 숨김
        } else {
            textTitle.setText("회원가입");
            btnLogin.setText("회원가입");
            textSwitchMode.setText("이미 계정이 있으신가요? 로그인");
            layoutDisplayName.setVisibility(View.VISIBLE); // 회원가입 모드에서는 닉네임 필드 보임
        }
    }


    // 이메일과 비밀번호로 로그인을 시도
    private void performEmailLogin() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (!validateInput(email, password, null)) { // 로그인 시에는 닉네임 유효성 검사 안함
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


    // 이메일, 비밀번호, 닉네임으로 회원가입을 시도
    private void performEmailRegister() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String displayName = editDisplayName.getText().toString().trim(); // 닉네임 가져오기

        if (!validateInput(email, password, displayName)) { // 회원가입 시에는 닉네임 유효성 검사
            return;
        }

        showProgress(true);
        Log.d(TAG, "Performing email registration for: " + email);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email registration successful, updating profile...");
                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        // 사용자 프로필(닉네임) 업데이트
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build();

                        if (user != null) {
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        showProgress(false);
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "User profile updated.");
                                            handleSuccessfulLogin(user); // 프로필 업데이트 후 로그인 처리
                                        } else {
                                            Log.e(TAG, "Failed to update profile", profileTask.getException());
                                            Toast.makeText(getContext(), "프로필 업데이트 실패", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        showProgress(false);
                        Log.e(TAG, "Email registration failed", task.getException());
                        Toast.makeText(getContext(), "회원가입 실패: " +
                                        (task.getException() != null ? task.getException().getMessage() : "알 수 없는 오류"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }


    // Google 로그인 과정
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
                firebaseAuthWithGoogle(account.getIdToken()); // Firebase에 인증 정보 전달
            } catch (ApiException e) {
                Log.e(TAG, "Google sign-in failed", e);
                showProgress(false);
                Toast.makeText(getContext(), "Google 로그인 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Google 계정 정보를 사용하여 Firebase에 로그인합니다.
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


   /* 로그인 성공 후 공통 처리 로직.
      사용자 정보를 Firestore에 저장하고, 메인 화면으로 이동 */
    private void handleSuccessfulLogin(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            Log.e(TAG, "FirebaseUser is null after successful login");
            return;
        }

        Log.d(TAG, "Handling successful login for user: " + firebaseUser.getEmail());

        // Firestore에 저장할 User 객체 생성
        User user = new User(firebaseUser.getUid(), firebaseUser.getEmail(), firebaseUser.getDisplayName());

        firebaseRepository.saveUser(user, new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "User saved to Firestore successfully");
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onUserLoggedIn(); // MainActivity에 로그인 상태 알림
                }
                Toast.makeText(getContext(), "로그인 성공! 환영합니다, " +
                                (firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : firebaseUser.getEmail()),
                        Toast.LENGTH_SHORT).show();
                navigateToCollaboration();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to save user to Firestore", e);
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onUserLoggedIn();
                }
                Toast.makeText(getContext(), "로그인은 성공했지만 사용자 정보 저장에 실패했습니다.", Toast.LENGTH_LONG).show();
                navigateToCollaboration();
            }
        });
    }

    // 협업 프래그먼트로 화면을 전환
    private void navigateToCollaboration() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CollaborationFragment())
                    .commit();
            Log.d(TAG, "Navigated to CollaborationFragment");
        }
    }

    // 사용자 입력 값의 유효성 검사
    private boolean validateInput(String email, String password, @Nullable String displayName) {
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

        // 회원가입 시에만 닉네임 검사
        if (!isLoginMode && (displayName == null || displayName.isEmpty())) {
            editDisplayName.setError("닉네임을 입력해주세요");
            editDisplayName.requestFocus();
            return false;
        }

        return true;
    }


    // ProgressBar의 표시 여부
    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnLogin.setEnabled(!show);
        btnGoogleSignIn.setEnabled(!show);
        Log.d(TAG, "Progress visibility: " + (show ? "VISIBLE" : "GONE"));
    }

    @Override
    public void onStart() {
        super.onStart();
        // 프래그먼트 시작 시 이미 로그인된 사용자가 있는지 확인
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: " + currentUser.getEmail());
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