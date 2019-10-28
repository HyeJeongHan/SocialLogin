package com.nomadconnection.socialLogin;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.facebook.CallbackManager;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.SignInButton;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;
import com.nomadconnection.socialLogin.data.Social;
import com.nomadconnection.socialLogin.data.SocialLoginUser;

public class SocialLoginPresenter implements SocialLoginContract.Presenter, SocialLoginModel.onClickButtonListener {

    private Context context;
    private SocialLoginModel loginModel;
    private SocialLoginContract.View view;

    private LoginButton facebookLoginButton;
    private OAuthLoginButton naverLoginButton;
    private com.kakao.usermgmt.LoginButton kakaoLoginButton;
    private SignInButton googleLoginButton;

    public SocialLoginPresenter(Context context, SocialLoginContract.View view, Social[] social, Activity activity) {
        this.context = context;
        this.view = view;
        view.setSocialPresenter(this);
        loginModel = SocialLoginModel.getInstance();
        loginModel.setModel(context, activity);
        loginModel.setListener(this);

        for (Social s : social) {
            switch (s) {
                case FACEBOOK:
                    facebookLoginButton = new LoginButton(context);
                    break;

                case NAVER:
                    naverLoginButton = new OAuthLoginButton(context);
                    break;

                case KAKAO:
                    kakaoLoginButton = new com.kakao.usermgmt.LoginButton(context);
                    break;

                case GOOGLE:
                    googleLoginButton = new SignInButton(context);
                    break;

            }
        }


    }

    @Override
    public void addSocialLoginButton(Social social, ViewGroup wrapperView) {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(wrapperView.getLayoutParams().width, wrapperView.getLayoutParams().height);

        RelativeLayout relativeLayout = new RelativeLayout(context);
        relativeLayout.setLayoutParams(params);
        relativeLayout.setClickable(false);

        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT));
        imageView.setImageDrawable(wrapperView.getBackground());
        imageView.setClickable(false);

        switch (social) {
            case KAKAO:
                kakaoLoginButton.setLayoutParams(params);
                kakaoLoginButton.setClickable(true);

                relativeLayout.addView(kakaoLoginButton, 0);
                relativeLayout.addView(imageView);

                wrapperView.addView(relativeLayout, 0);
                wrapperView.setClickable(false);

                loginModel.setKakaoButton(kakaoLoginButton);
                break;

            case NAVER:
                naverLoginButton.setLayoutParams(params);
                naverLoginButton.setClickable(true);

                relativeLayout.addView(naverLoginButton, 0);
                relativeLayout.addView(imageView);

                wrapperView.addView(relativeLayout, 0);
                wrapperView.setClickable(false);

                loginModel.setNaverButton(naverLoginButton);
                break;

            case GOOGLE:
                googleLoginButton.setLayoutParams(params);
                googleLoginButton.setClickable(true);

                relativeLayout.addView(googleLoginButton, 0);
                relativeLayout.addView(imageView);

                wrapperView.addView(relativeLayout, 0);
                wrapperView.setClickable(false);

                loginModel.setGoogleButton(googleLoginButton);
                break;

        }

    }

    @Override
    public void addFacebookLoginButton(Social social, ViewGroup wrapperView, CallbackManager callbackManager) {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(wrapperView.getLayoutParams().width, wrapperView.getLayoutParams().height);

        RelativeLayout relativeLayout = new RelativeLayout(context);
        relativeLayout.setLayoutParams(params);
        relativeLayout.setClickable(false);

        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT));
        imageView.setImageDrawable(wrapperView.getBackground());
        imageView.setClickable(false);

        facebookLoginButton.setLayoutParams(params);
        facebookLoginButton.setClickable(true);
        facebookLoginButton.setPadding(0, wrapperView.getLayoutParams().height, 0, wrapperView.getLayoutParams().height);

        relativeLayout.addView(facebookLoginButton, 0);
        relativeLayout.addView(imageView);

        wrapperView.addView(relativeLayout, 0);
        wrapperView.setClickable(false);

        loginModel.setFacebookButton(facebookLoginButton, callbackManager);
    }

    @Override
    public void setUserInfo(SocialLoginUser userInfo) {
        view.responseSocialLogin(userInfo);
    }
}
