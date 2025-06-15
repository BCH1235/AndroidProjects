package com.am.mytodolistapp.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class AuthFragment extends Fragment {

    private EditText editEmail;
    private EditText editPassword;
    private Button buttonLogin;
    private TextView textToggleMode;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseRepository firebaseRepository;
    private boolean isLoginMode = true;

    private EditText editDisplayName; // 닉네임 EditText 추가
    private TextInputLayout layoutDisplayName; // 닉네임 레이아웃 추가

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseRepository = FirebaseRepository.getInstance();
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
    }

    private void initViews(View view) {
        editEmail = view.findViewById(R.id.edit_email);
        editPassword = view.findViewById(R.id.edit_password);
        buttonLogin = view.findViewById(R.id.button_login);
        textToggleMode = view.findViewById(R.id.text_toggle_mode);
        progressBar = view.findViewById(R.id.progress_bar);
        editDisplayName = view.findViewById(R.id.edit_display_name); // 추가
        layoutDisplayName = view.findViewById(R.id.layout_display_name); // 추가
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                loginUser();
            } else {
                registerUser();
            }
        });

        textToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            buttonLogin.setText("로그인");
            textToggleMode.setText("계정이 없으신가요? 회원가입하기");
            layoutDisplayName.setVisibility(View.GONE); // 로그인 모드에서는 닉네임 필드 숨김
        } else {
            buttonLogin.setText("회원가입");
            textToggleMode.setText("이미 계정이 있으신가요? 로그인하기");
            layoutDisplayName.setVisibility(View.VISIBLE); // 회원가입 모드에서는 닉네임 필드 표시
        }
    }

    private void loginUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        showProgress(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(getContext(), "로그인 성공!", Toast.LENGTH_SHORT).show();
                            onLoginSuccess();
                        }
                    } else {

                        Exception exception = task.getException();
                        String errorMessage;


                        if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                            // 존재하지 않는 계정일 경우
                            errorMessage = "존재하지 않는 계정입니다.";
                        } else if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                            // 비밀번호가 틀렸거나, 이메일 형식이 잘못된 경우
                            errorMessage = "이메일 또는 비밀번호가 올바르지 않습니다.";
                        } else {
                            // 그 외 다른 네트워크 오류 등
                            errorMessage = "로그인 중 오류가 발생했습니다. 다시 시도해주세요.";
                        }
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();

                    }
                });
    }

    private void registerUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String displayName = editDisplayName.getText().toString().trim(); // 닉네임 가져오기

        if (!validateInput(email, password)) {
            return;
        }
        if (displayName.isEmpty()) {
            Toast.makeText(getContext(), "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getContext(), "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build();
                            firebaseUser.updateProfile(profileUpdates);

                            // 2. Firestore에 사용자 정보(닉네임 포함) 저장
                            User user = new User(firebaseUser.getUid(), firebaseUser.getEmail(), displayName); // 수정

                            firebaseRepository.saveUser(user, new FirebaseRepository.OnCompleteListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    showProgress(false); // 성공 시 프로그레스바 숨김
                                    Toast.makeText(getContext(), "회원가입 성공!", Toast.LENGTH_SHORT).show();
                                    onLoginSuccess();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    showProgress(false); // 실패 시 프로그레스바 숨김
                                    Toast.makeText(getContext(), "사용자 정보 저장 실패: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } else {
                        showProgress(false); // 실패 시 프로그레스바 숨김
                        Toast.makeText(getContext(), "회원가입 실패: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // 로그인/회원가입 성공 시 호출되는 메서드
    private void onLoginSuccess() {
        // MainActivity의 메뉴 상태 업데이트
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).onUserLoggedIn();
        }

        // CollaborationFragment로 이동
        navigateToCollaboration();
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            Toast.makeText(getContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "올바른 이메일 형식을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.isEmpty()) {
            Toast.makeText(getContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showProgress(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            buttonLogin.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            buttonLogin.setEnabled(true);
        }
    }

    private void navigateToCollaboration() {
        CollaborationFragment collaborationFragment = new CollaborationFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, collaborationFragment)
                .commit();
    }
}