package com.nomadconnection.socialLogin.data;

public class SocialLoginUser {

    private Social social;
    private String name;
    private String email;
    private String phone;
    private String imageUrlStr;
    private Error error;

    public Social getSocial() {
        return social;
    }

    public void setSocial(Social social) {
        this.social = social;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImageUrlStr() {
        return imageUrlStr;
    }

    public void setImageUrlStr(String imageUrlStr) {
        this.imageUrlStr = imageUrlStr;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }
}
