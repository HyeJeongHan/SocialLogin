package com.nomadconnection.socialLogin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.*;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.kakao.auth.AccessTokenCallback;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;
import com.nomadconnection.socialLogin.data.Social;
import com.nomadconnection.socialLogin.data.SocialLoginUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SocialLoginModel {

    private static SocialLoginModel instance = null;

    public static final int RC_SIGN_IN = 123;
    public static final int REQUEST_AUTHORIZATION = 1234;

    private static String NAVER_CLIENT_ID;
    private static String NAVER_CLIENT_SECRET;
    private static String NAVER_CLIENT_NAME;
    private static String GOOGLE_CLIENT_ID;

    private OAuthLogin naverLoginInstance;//naver
    private OAuthLoginButton oauthLoginNaverButton;

    private CallbackManager callbackManager;//facebook
    private LoginButton oauthLoginFacebookButton;

    private Session kakaoSession;
    private com.kakao.usermgmt.LoginButton kakaoLoginButton;//kakao

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private String mAccessToken;
    private SignInButton googleButton;

    private Context context;
    private Activity activity;

    private onClickButtonListener listener;

    public interface onClickButtonListener {
        void setUserInfo(SocialLoginUser userInfo);
    }

    public static SocialLoginModel getInstance() {
        if (instance == null)
            instance = new SocialLoginModel();
        return instance;
    }

    public void setModel(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    //region kakao
    private void initKakaoLogin() {
        Log.e("", "initKakaoLogin");
        getHashKey(context);
        kakaoSession = Session.getCurrentSession();
        kakaoSession.addCallback(new ISessionCallback() {
            @Override
            public void onSessionOpened() {
                requestMe();
            }

            @Override
            public void onSessionOpenFailed(KakaoException exception) {
                Log.e("", "onSessionOpenFailed");
            }
        });

    }

    private void requestMe() {
        List<String> keys = new ArrayList<>();
        keys.add("properties.nickname");
        keys.add("properties.profile_image");
        keys.add("kakao_account.email");

        UserManagement.getInstance().me(keys, new MeV2ResponseCallback() {
            @Override
            public void onSuccess(MeV2Response response) {
                handleScopeError(response);
            }

            @Override
            public void onFailure(ErrorResult errorResult) {
                String message = "failed to get user info. msg=" + errorResult;
//                CommonUtil.showToastShort(message);
                SocialLoginUser socialLoginUser = new SocialLoginUser();
                socialLoginUser.setError(errorResult.getErrorMessage());
                listener.setUserInfo(socialLoginUser);
            }

            @Override
            public void onSessionClosed(ErrorResult errorResult) {

            }

        });
    }

    private void handleScopeError(final MeV2Response response) {
        List<String> neededScopes = new ArrayList<>();
        if (response.getKakaoAccount().needsScopeAccountEmail()) {
            neededScopes.add("account_email");
        }

        Session.getCurrentSession().updateScopes(activity, neededScopes, new AccessTokenCallback() {
            @Override
            public void onAccessTokenReceived(com.kakao.auth.authorization.accesstoken.AccessToken accessToken) {

                SocialLoginUser socialLoginUser = new SocialLoginUser();
                socialLoginUser.setSocial(Social.KAKAO);
                socialLoginUser.setName(response.getNickname());
                socialLoginUser.setEmail(response.getKakaoAccount().getEmail());
                socialLoginUser.setImageUrlStr(response.getProfileImagePath());
                socialLoginUser.setAccessToken(accessToken.getAccessToken());

                listener.setUserInfo(socialLoginUser);

                Log.e("user id : ", "" + response.getId());
            }

            @Override
            public void onAccessTokenFailure(ErrorResult errorResult) {
                SocialLoginUser socialLoginUser = new SocialLoginUser();
                socialLoginUser.setError(errorResult.getErrorMessage());
                listener.setUserInfo(socialLoginUser);
            }
        });
    }

    public void kakaoSessionCallBackNull() {
        kakaoSession.addCallback(null);
    }

    public void closeKakaoSession() {
        kakaoSession.close();
    }
    //endregion

    //region google
    private void initGoogleLogin() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(GOOGLE_CLIENT_ID)
                .requestServerAuthCode(GOOGLE_CLIENT_ID)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
        mAuth = FirebaseAuth.getInstance();

    }

    private void setSignInGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void getGoogleAccessToken(final GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Logger.d("signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            Logger.d("signInWithCredential:failure " + task.getException());

                            SocialLoginUser socialLoginUser = new SocialLoginUser();
                            socialLoginUser.setError(task.getException().getMessage());
                            listener.setUserInfo(socialLoginUser);
                        }

                    }
                });


        new AsyncTask<Void, Void, SocialLoginUser>() {
            @Override
            protected SocialLoginUser doInBackground(Void... voids) {
                SocialLoginUser socialLoginUser = null;

                try {

                    mAccessToken = GoogleAuthUtil.getToken(activity, acct.getEmail(),
                            "oauth2:profile email");
//                    https://www.googleapis.com/auth/plus.login https://www.googleapis.com/auth/plus.profile.emails.read

                    socialLoginUser = new SocialLoginUser();
                    socialLoginUser.setSocial(Social.GOOGLE);
                    socialLoginUser.setName(acct.getDisplayName());
                    socialLoginUser.setEmail(acct.getEmail());
                    socialLoginUser.setImageUrlStr(acct.getPhotoUrl().toString());
                    socialLoginUser.setAccessToken(mAccessToken);

                } catch (UserRecoverableAuthException e) {
                    activity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);

                } catch (GoogleAuthException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return socialLoginUser;
            }

            @Override
            protected void onPostExecute(SocialLoginUser s) {
                Logger.e("google token   : " + mAccessToken);

                listener.setUserInfo(s);

            }
        }.execute();

    }

    public void googleSignOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(activity, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // ...
                    }
                });
    }
    //endregion

    //region naver
    private void initNaverLogin() {
        naverLoginInstance = OAuthLogin.getInstance();
        naverLoginInstance.init(context, NAVER_CLIENT_ID, NAVER_CLIENT_SECRET, NAVER_CLIENT_NAME);

        oauthLoginNaverButton.setOAuthLoginHandler(new OAuthLoginHandler() {
            @Override
            public void run(boolean success) {
                if (success) {
                    // 로그인 성공"

                    String accessToken = naverLoginInstance.getAccessToken(context);
                    String refreshToken = naverLoginInstance.getRefreshToken(context);
                    long expiresAt = naverLoginInstance.getExpiresAt(context);
                    String tokenType = naverLoginInstance.getTokenType(context);
                    Log.e("accessToken >>>>   ", "" + accessToken);
                    Log.e("refreshToken >>>>   ", "" + refreshToken);
                    Log.e("expiresAt >>>>   ", "" + String.valueOf(expiresAt));
                    Log.e("tokenType >>>>   ", "" + tokenType);
                    Log.e("state >>>>   ", "" + naverLoginInstance.getState(context).toString());

                    SocialLoginUser socialLoginUser = new SocialLoginUser();
                    socialLoginUser.setSocial(Social.NAVER);
                    socialLoginUser.setAccessToken(accessToken);
                    listener.setUserInfo(socialLoginUser);

                } else {//로그인 실패
                    String errorCode = naverLoginInstance.getLastErrorCode(context).getCode();
                    String errorDesc = naverLoginInstance.getLastErrorDesc(context);

                    Log.e("errorCode", "errorCode >>> " + errorCode + "         errorDesc >>> " + errorDesc);

                    SocialLoginUser socialLoginUser = new SocialLoginUser();
                    socialLoginUser.setError(errorDesc);
                    listener.setUserInfo(socialLoginUser);

//                    CommonUtil.showToastShort("errorCode >>> " + errorCode + "\n errorDesc >>> " + errorDesc);
                }
            }
        });

        //로그아웃
        //naverLoginInstance.logout(context);

        //정보 가져오기 ex)email
        //new RequestNaverApiTask().execute();

//        email.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new RequestNaverApiTask().execute();
//            }
//        });
//
//        delete.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new RequestNaverLogoutApiTask().execute();
//            }
//        });

    }

    private class RequestNaverApiTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {//작업이 실행되기 전에 먼저 실행.

        }

        @Override
        protected String doInBackground(Void... params) {//네트워크에 연결하는 과정이 있으므로 다른 스레드에서 실행되어야 한다.
            String url = "https://openapi.naver.com/v1/nid/me";
            String at = naverLoginInstance.getAccessToken(context);
            return naverLoginInstance.requestApi(context, at, url);//url, 토큰을 넘겨서 값을 받아온다.json 타입으로 받아진다.
        }

        protected void onPostExecute(String content) {//doInBackground 에서 리턴된 값이 여기로 들어온다.
            try {
                JSONObject jsonObject = new JSONObject(content);
                JSONObject response = jsonObject.getJSONObject("response");
                String email = response.getString("email");
                Log.e("email >> ", "" + email);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class RequestNaverLogoutApiTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {//작업이 실행되기 전에 먼저 실행.

        }

        @Override
        protected Boolean doInBackground(Void... params) {//네트워크에 연결하는 과정이 있으므로 다른 스레드에서 실행되어야 한다.
            return naverLoginInstance.logoutAndDeleteToken(context);
        }

        @Override
        protected void onPostExecute(Boolean content) {//doInBackground 에서 리턴된 값이 여기로 들어온다.
            try {
                Log.e("logout >> ", "" + content);

                if (!content) {
                    // 서버에서 토큰 삭제에 실패했어도 클라이언트에 있는 토큰은 삭제되어 로그아웃된 상태입니다.
                    // 클라이언트에 토큰 정보가 없기 때문에 추가로 처리할 수 있는 작업은 없습니다.
                    Log.e("errorCode:", "" + naverLoginInstance.getLastErrorCode(context));
                    Log.e("errorDesc:", "" + naverLoginInstance.getLastErrorDesc(context));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //endregion

    //region facebook
    private void initFacebookLogin() {
        oauthLoginFacebookButton.setReadPermissions(Arrays.asList("public_profile", "email"));
        oauthLoginFacebookButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            // 로그인 성공 시 호출 됩니다. Access Token 발급 성공.
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.e("Callback :: ", "onSuccess");
                Log.e("onSuccess >>>  ", "" + loginResult.getAccessToken());
                requestMe(loginResult.getAccessToken());
            }

            // 로그인 창을 닫을 경우, 호출됩니다.
            @Override
            public void onCancel() {
                Log.e("Callback :: ", "onCancel");
            }


            // 로그인 실패 시에 호출됩니다.
            @Override
            public void onError(FacebookException error) {
                Log.e("Callback :: ", "onError : " + error.getMessage());
                SocialLoginUser socialLoginUser = new SocialLoginUser();
                socialLoginUser.setError(error.getMessage());
                listener.setUserInfo(socialLoginUser);
            }

            // 사용자 정보 요청
            public void requestMe(final AccessToken token) {
                GraphRequest graphRequest = GraphRequest.newMeRequest(token,
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.e("result", object.toString());

                                try {
                                    SocialLoginUser socialLoginUser = new SocialLoginUser();
                                    socialLoginUser.setSocial(Social.FACEBOOK);
                                    socialLoginUser.setName(object.get("name").toString());
                                    socialLoginUser.setEmail(object.get("email").toString());
                                    socialLoginUser.setAccessToken(token.toString());
//                                    socialLoginUser.setImageUrlStr(getFacebookProfilePicture(object.get("id").toString()));
                                    listener.setUserInfo(socialLoginUser);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender,birthday");
                graphRequest.setParameters(parameters);
                graphRequest.executeAsync();
            }
        });

        //로그아웃
        //LoginManager.getInstance().logOut();
    }
    //endregion

    @Nullable
    public static String getHashKey(Context context) {

        final String TAG = "KeyHash";
        String keyHash = null;
        try {
            PackageInfo info =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);


            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                keyHash = new String(Base64.encode(md.digest(), 0));
                Log.e(TAG, keyHash);
            }
        } catch (Exception e) {
            Log.e("name not found", e.toString());
        }

        if (keyHash != null) {
            return keyHash;
        } else {
            return null;
        }

    }

    public static String getFacebookProfilePicture(String userID) {
        Bitmap bitmap = null;
        try {
            URL imageURL = new URL("https://graph.facebook.com/" + userID + "/picture?type=large");
            bitmap = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return changeBitMapToString(bitmap);
    }

    public static String changeBitMapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String temp = Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    public void setListener(onClickButtonListener listener) {
        this.listener = listener;
    }

    public void setFacebookButton(LoginButton loginButton, CallbackManager callbackManager) {
        oauthLoginFacebookButton = loginButton;
        this.callbackManager = callbackManager;

        initFacebookLogin();
    }

    public void setNaverButton(OAuthLoginButton loginButton) {
        oauthLoginNaverButton = loginButton;

        initNaverLogin();
    }

    public void setKakaoButton(com.kakao.usermgmt.LoginButton loginButton) {
        kakaoLoginButton = loginButton;

        initKakaoLogin();
    }

    public void setGoogleButton(SignInButton loginButton) {
        googleButton = loginButton;

        initGoogleLogin();

        googleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSignInGoogle();
            }
        });
    }

    public static void setNaverClientInfo(String id, String secret, String name) {
        NAVER_CLIENT_ID = id;
        NAVER_CLIENT_SECRET = secret;
        NAVER_CLIENT_NAME = name;
    }

    public com.kakao.usermgmt.LoginButton getKakaoLoginButton() {
        if (kakaoLoginButton == null) {
            kakaoLoginButton = new com.kakao.usermgmt.LoginButton(context);
        }
        return kakaoLoginButton;
    }

    public Session kakaoSession() {
        if (kakaoSession == null) {
            kakaoSession = Session.getCurrentSession();
        }
        return kakaoSession;
    }

    public OAuthLoginButton getNaverLoginButton() {
        if (oauthLoginNaverButton == null) {
            oauthLoginNaverButton = new OAuthLoginButton(context);
        }
        return oauthLoginNaverButton;
    }

    public SignInButton getGoogleButton() {
        if (googleButton == null) {
            googleButton = new SignInButton(context);
        }
        return googleButton;
    }

    public LoginButton getFacebookButton() {
        if (oauthLoginFacebookButton == null) {
            oauthLoginFacebookButton = new LoginButton(context);
        }
        return oauthLoginFacebookButton;
    }

    public static void setGoogleClientInfo(String id) {
        GOOGLE_CLIENT_ID = id;
    }

}
