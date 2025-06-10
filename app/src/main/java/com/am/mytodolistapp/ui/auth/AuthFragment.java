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

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.FirebaseRepository;
import com.am.mytodolistapp.data.firebase.User;
import com.am.mytodolistapp.ui.collaboration.CollaborationFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthFragment extends Fragment {

    private EditText editEmail;
    private EditText editPassword;
    private Button buttonLogin;
    private Button buttonRegister;
    private TextView textToggleMode;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseRepository firebaseRepository;
    private boolean isLoginMode = true;

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
        buttonRegister = view.findViewById(R.id.button_register);
        textToggleMode = view.findViewById(R.id.text_toggle_mode);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                loginUser();
            } else {
                registerUser();
            }
        });

        buttonRegister.setOnClickListener(v -> {
            if (isLoginMode) {
                registerUser();
            } else {
                loginUser();
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
            buttonRegister.setText("회원가입");
            textToggleMode.setText("계정이 없으신가요? 회원가입하기");
        } else {
            buttonLogin.setText("회원가입");
            buttonRegister.setText("로그인");
            textToggleMode.setText("이미 계정이 있으신가요? 로그인하기");
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
                            navigateToCollaboration();
                        }
                    } else {
                        Toast.makeText(getContext(), "로그인 실패: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
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
                            // Firestore에 사용자 정보 저장
                            User user = new User(firebaseUser.getUid(), firebaseUser.getEmail(),
                                    firebaseUser.getDisplayName());

                            firebaseRepository.saveUser(user, new FirebaseRepository.OnCompleteListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    Toast.makeText(getContext(), "회원가입 성공!", Toast.LENGTH_SHORT).show();
                                    navigateToCollaboration();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(getContext(), "사용자 정보 저장 실패: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(getContext(), "회원가입 실패: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
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
            buttonRegister.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            buttonLogin.setEnabled(true);
            buttonRegister.setEnabled(true);
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