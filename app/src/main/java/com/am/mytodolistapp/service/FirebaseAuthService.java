// FirebaseAuthService.java
// 위치: app/src/main/java/com/am/mytodolistapp/service/FirebaseAuthService.java
package com.am.mytodolistapp.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseAuthService {

    private static final String TAG = "FirebaseAuth";
    private static final String COLLECTION_USERS = "users";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;

    public FirebaseAuthService(Context context) {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Google Sign-In 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("YOUR_WEB_CLIENT_ID") // Firebase 콘솔에서 가져온 Web Client ID
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    // ========== 인증 상태 확인 ==========

    public boolean isUserSignedIn() {
        return auth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String getCurrentUserName() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getDisplayName() : "사용자";
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : "";
    }

    // ========== 이메일/비밀번호 인증 ==========

    public void signUpWithEmail(String email, String password, String name, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // 사용자 정보를 Firestore에 저장
                            createUserProfile(user.getUid(), name, email, callback);
                        }
                    } else {
                        Log.e(TAG, "회원가입 실패", task.getException());
                        callback.onError(task.getException() != null ?
                                task.getException().getMessage() : "회원가입에 실패했습니다.");
                    }
                });
    }

    public void signInWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "이메일 로그인 성공");
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "이메일 로그인 실패", task.getException());
                        callback.onError(task.getException() != null ?
                                task.getException().getMessage() : "로그인에 실패했습니다.");
                    }
                });
    }

    // ========== Google 인증 ==========

    public Intent getGoogleSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }

    public void handleGoogleSignInResult(Intent data, AuthCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult();
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken(), callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Google 로그인 결과 처리 실패", e);
            callback.onError("Google 로그인에 실패했습니다.");
        }
    }

    private void firebaseAuthWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // 새 사용자인지 확인하고 프로필 생성
                            checkAndCreateUserProfile(user, callback);
                        }
                    } else {
                        Log.e(TAG, "Firebase Google 인증 실패", task.getException());
                        callback.onError("Google 로그인에 실패했습니다.");
                    }
                });
    }

    // ========== 사용자 프로필 관리 ==========

    private void createUserProfile(String userId, String name, String email, AuthCallback callback) {
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("userId", userId);
        userProfile.put("name", name);
        userProfile.put("email", email);
        userProfile.put("createdAt", System.currentTimeMillis());
        userProfile.put("lastLoginAt", System.currentTimeMillis());

        db.collection(COLLECTION_USERS)
                .document(userId)
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "사용자 프로필 생성 완료");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "사용자 프로필 생성 실패", e);
                    callback.onError("사용자 정보 저장에 실패했습니다.");
                });
    }

    private void checkAndCreateUserProfile(FirebaseUser user, AuthCallback callback) {
        db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // 새 사용자이므로 프로필 생성
                        createUserProfile(user.getUid(),
                                user.getDisplayName() != null ? user.getDisplayName() : "사용자",
                                user.getEmail() != null ? user.getEmail() : "",
                                callback);
                    } else {
                        // 기존 사용자 - 마지막 로그인 시간 업데이트
                        updateLastLoginTime(user.getUid());
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "사용자 프로필 확인 실패", e);
                    callback.onError("사용자 정보 확인에 실패했습니다.");
                });
    }

    private void updateLastLoginTime(String userId) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .update("lastLoginAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "마지막 로그인 시간 업데이트"))
                .addOnFailureListener(e -> Log.e(TAG, "마지막 로그인 시간 업데이트 실패", e));
    }

    // ========== 로그아웃 ==========

    public void signOut(SignOutCallback callback) {
        // Firebase 로그아웃
        auth.signOut();

        // Google 로그아웃
        googleSignInClient.signOut()
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "로그아웃 완료");
                    callback.onSignOutComplete();
                });
    }

    // ========== 비밀번호 재설정 ==========

    public void sendPasswordResetEmail(String email, PasswordResetCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "비밀번호 재설정 이메일 전송 완료");
                        callback.onEmailSent();
                    } else {
                        Log.e(TAG, "비밀번호 재설정 이메일 전송 실패", task.getException());
                        callback.onError(task.getException() != null ?
                                task.getException().getMessage() : "이메일 전송에 실패했습니다.");
                    }
                });
    }

    // ========== 사용자 검색 (초대용) ==========

    public void findUserByEmail(String email, UserSearchCallback callback) {
        db.collection(COLLECTION_USERS)
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // 사용자 찾음
                        String userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        String userName = queryDocumentSnapshots.getDocuments().get(0).getString("name");
                        callback.onUserFound(userId, userName, email);
                    } else {
                        // 사용자 없음
                        callback.onUserNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "사용자 검색 실패", e);
                    callback.onError("사용자 검색에 실패했습니다.");
                });
    }

    // ========== 콜백 인터페이스들 ==========

    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface SignOutCallback {
        void onSignOutComplete();
    }

    public interface PasswordResetCallback {
        void onEmailSent();
        void onError(String error);
    }

    public interface UserSearchCallback {
        void onUserFound(String userId, String userName, String email);
        void onUserNotFound();
        void onError(String error);
    }
}
