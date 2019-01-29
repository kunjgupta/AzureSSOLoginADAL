package sso.azure.kunj.com.azuressologin;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.IDispatcher;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.Telemetry;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sso.azure.kunj.com.azuressologin.api.MicrosoftGraphApiClient;
import sso.azure.kunj.com.azuressologin.api.MicrosoftGraphApiInterface;
import sso.azure.kunj.com.azuressologin.pojo.UserDetails;

public class MainActivity extends AppCompatActivity {

    private AuthenticationContext authenticationContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the authentication context.
        authenticationContext = new AuthenticationContext(MainActivity.this, Constants.AUTHORITY, true);

        // Acquire tokens using necessary UI.
        authenticationContext.acquireToken(MainActivity.this, Constants.GRAPH_RESOURCE, Constants.CLIENT_ID, Constants.REDIRECT_URI,
                PromptBehavior.Auto, "", new AuthenticationCallback<AuthenticationResult>() {
                    @Override
                    public void onSuccess(AuthenticationResult result) {
                        String idToken = result.getIdToken();
                        String accessToken = result.getAccessToken();

                        // Print tokens.
                        Log.e(Constants.TAG, "Token Type: " + result.getAccessTokenType());
                        Log.e(Constants.TAG, "Auth Status Name: " + result.getStatus().name());
                        Log.e(Constants.TAG, "ID Token: " + idToken);
                        Log.e(Constants.TAG, "Access Token: " + accessToken);
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString(Constants.PREFS_ACCESS_TOKEN, accessToken).apply();
                    }

                    @Override
                    public void onError(Exception exc) {
                        // TODO: Handle error
                        Log.e(Constants.TAG, "onError: exc: " + exc);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Error logging in");
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().clear().apply();
                                finish();
                            }
                        });
                        builder.show();
                    }
                });

        (findViewById(R.id.get_graph_api_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MicrosoftGraphApiInterface graphApiInterface = MicrosoftGraphApiClient.getClient().create(MicrosoftGraphApiInterface.class);

                Call<UserDetails> call = graphApiInterface.me(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(Constants.PREFS_ACCESS_TOKEN, ""));
                call.enqueue(new Callback<UserDetails>() {
                    @Override
                    public void onResponse(Call<UserDetails> call, Response<UserDetails> response) {
                        Log.d(Constants.TAG,"Response code: "+response.code());
                        Log.d(Constants.TAG,"Response body: "+response.body());

                        UserDetails details = response.body();
                        ((TextView) findViewById(R.id.results_textview)).setText(details.toString());
                    }

                    @Override
                    public void onFailure(Call<UserDetails> call, Throwable t) {
                        call.cancel();
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result to the authentication context.
        if (authenticationContext != null) {
            authenticationContext.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setUpADALForCallingBroker() {
        // Set the calling app will talk to broker
        // Note: Starting from version 1.1.14, calling app has to explicitly call
        // AuthenticationSettings.Instance.setUserBroker(true) to call broker.
        // AuthenticationSettings.Instance.setSkipBroker(boolean) is already deprecated.
        AuthenticationSettings.INSTANCE.setUseBroker(true);

        // Provide secret key for token encryption.
        try {
            // For API version lower than 18, you have to provide the secret key. The secret key
            // needs to be 256 bits. You can use the following way to generate the secret key. And
            // use AuthenticationSettings.Instance.setSecretKey(secretKeyBytes) to supply us the key.
            // For API version 18 and above, we use android keystore to generate keypair, and persist
            // the keypair in AndroidKeyStore. Current investigation shows 1)Keystore may be locked with
            // a lock screen, if calling app has a lot of background activity, keystore cannot be
            // accessed when locked, we'll be unable to decrypt the cache items 2) AndroidKeystore could
            // be reset when gesture to unlock the device is changed.
            // We do recommend the calling app the supply us the key with the above two limitations.
            if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
                // use same key for tests
                SecretKeyFactory keyFactory = SecretKeyFactory
                        .getInstance("Pass your secret Key here");
                SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                        "abcdedfdfd".getBytes("UTF-8"), 100, 256));
                SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
                AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | UnsupportedEncodingException ex) {
            showMessage("Fail to generate secret key:" + ex.getMessage());
        }

        ApplicationInfo appInfo = getApplicationContext().getApplicationInfo();
        Log.v(Constants.TAG, "App info:" + appInfo.uid + " package:" + appInfo.packageName);

        // If you're directly talking to ADFS server, you should set validateAuthority=false.
        SampleTelemetry telemetryDispatcher = new SampleTelemetry();
        Telemetry.getInstance().registerDispatcher(telemetryDispatcher, true);
    }

    private void showMessage(final String msg) {
        Log.v(Constants.TAG, msg);
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    class SampleTelemetry implements IDispatcher {
        private static final String TAG = "SampleTelemetry";

        @Override
        public void dispatchEvent(final Map<String, String> events) {
            final Iterator iterator = events.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry pair = (Map.Entry) iterator.next();
                Log.e(Constants.TAG, pair.getKey() + ":" + pair.getValue());
            }
        }
    }

    private Handler getHandler() {
        return new Handler(MainActivity.this.getMainLooper());
    }

}