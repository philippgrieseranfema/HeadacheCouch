package com.happyheadache.menu;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.happyheadache.R;

import static com.happyheadache.Constants.EMPTY_STRING;
import static com.happyheadache.Constants.PREFERENCE_LOGIN;

public class LoginActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;
    private boolean mIsLogin;

    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mConfirmPasswordEditText;
    private Button mMemberButton;
    private Button mLoginJoinButton;
    private Button mForgotPasswordButton;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Show toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_login);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Enable up navigation (back arrow)
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        final LoginActivity loginActivity = this;

        // Set up buttons and edit texts
        mEmailEditText = (EditText) findViewById(R.id.edittext_login_email);
        mPasswordEditText = (EditText) findViewById(R.id.edittext_login_password);
        mConfirmPasswordEditText = (EditText) findViewById(R.id.edittext_login_confirmpassword);
        mMemberButton = (Button) findViewById(R.id.button_login_member);
        mLoginJoinButton = (Button) findViewById(R.id.button_login_loginjoin);
        mForgotPasswordButton = (Button) findViewById(R.id.button_login_forgotpassword);

        // Are we showing Login or Signup view?
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mIsLogin = mSharedPreferences.getBoolean(PREFERENCE_LOGIN, true);
        updateViews();

        // Initiate firebase authenticaition
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null && user.getEmail() != null && !user.getEmail().equals(EMPTY_STRING)) {
                    // User is signed in --> Go back to Home screen
                    NavUtils.navigateUpFromSameTask(loginActivity);
                }
            }
        };

        // Setup firebase analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void updateViews() {
        if (mIsLogin) {
            mMemberButton.setText(R.string.login_notyetmember);
            mLoginJoinButton.setText(R.string.login_login);
            mConfirmPasswordEditText.setVisibility(View.GONE);
            mForgotPasswordButton.setVisibility(View.VISIBLE);
        } else {
            mMemberButton.setText(R.string.login_alreadymember);
            mLoginJoinButton.setText(R.string.login_signup);
            mConfirmPasswordEditText.setVisibility(View.VISIBLE);
            mForgotPasswordButton.setVisibility(View.GONE);
        }

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(mIsLogin ? R.string.login_login : R.string.login_signup);
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void loginSignUp(View view) {
        String email = mEmailEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String confirmPassword = mConfirmPasswordEditText.getText().toString();

        FirebaseUser user = mAuth.getCurrentUser();

        if (email.equals(EMPTY_STRING) || email.length() == 0) {
            Toast.makeText(LoginActivity.this, R.string.login_pleaseenteremail, Toast.LENGTH_SHORT).show();
        } else if (password.equals(EMPTY_STRING) || password.length() == 0) {
            Toast.makeText(LoginActivity.this, R.string.login_pleaseenterpassword, Toast.LENGTH_SHORT).show();
        } else if (mIsLogin) {
            // Login with given email and password
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, R.string.login_loginsuccessful, Toast.LENGTH_SHORT).show();
                    } else if (task.getException() != null) {
                        Toast.makeText(LoginActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, null);
            // TODO: Transfer things created with anonymous account to this account?
        } else if (user != null && (user.getEmail() == null || user.getEmail().equals(EMPTY_STRING))) {
            // Link anonymous account with email account
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);
            user.linkWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, R.string.login_signupsuccessful, Toast.LENGTH_SHORT).show();
                    } else if (task.getException() != null) {
                        Toast.makeText(LoginActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, null);
        } else if (password.equals(confirmPassword)) {
            // Signup with email and password - This shouldn't really be called!
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, R.string.login_signupsuccessful, Toast.LENGTH_SHORT).show();
                    } else if (task.getException() != null) {
                        Toast.makeText(LoginActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, null);
        } else {
            // Show error message
            Toast.makeText(LoginActivity.this, R.string.login_passwordsdontmatch, Toast.LENGTH_SHORT).show();
        }
    }

    public void openLoginSignUp(View view) {
        // Switch from Login to Signup or from Signup to Login
        mIsLogin = !mIsLogin;
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.putBoolean(PREFERENCE_LOGIN, mIsLogin);
        e.apply();
        updateViews();
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void forgotPassword(View view) {
        String email = mEmailEditText.getText().toString();

        if (email.equals(EMPTY_STRING) || email.length() == 0) {
            Toast.makeText(LoginActivity.this, R.string.login_pleaseenteremail, Toast.LENGTH_SHORT).show();
        } else {
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, R.string.login_passwordresetsent, Toast.LENGTH_SHORT).show();
                    } else if (task.getException() != null) {
                        Toast.makeText(LoginActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
