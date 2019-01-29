package sso.azure.kunj.com.azuressologin.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import sso.azure.kunj.com.azuressologin.pojo.UserDetails;

public interface MicrosoftGraphApiInterface {
    @GET("/v1.0/me")
    Call<UserDetails> me(@Header("Authorization") String authorization);
}
