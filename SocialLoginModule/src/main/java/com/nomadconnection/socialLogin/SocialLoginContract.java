package com.nomadconnection.socialLogin;

import android.view.ViewGroup;
import com.facebook.CallbackManager;
import com.nomadconnection.socialLogin.base.BasePresenter;
import com.nomadconnection.socialLogin.base.BaseView;
import com.nomadconnection.socialLogin.data.Social;
import com.nomadconnection.socialLogin.data.SocialLoginUser;

public interface SocialLoginContract {

    interface View extends BaseView<Presenter> {
        void responseSocialLogin(SocialLoginUser loginUser);
    }

    interface Presenter extends BasePresenter {
        void addSocialLoginButton(Social social, ViewGroup wrapperView);
        void addFacebookLoginButton(Social social, ViewGroup wrapperView, CallbackManager callbackManager);
    }
}
