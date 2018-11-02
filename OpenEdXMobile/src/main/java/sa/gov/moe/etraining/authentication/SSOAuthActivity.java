package sa.gov.moe.etraining.authentication;

import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.inject.Inject;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import roboguice.RoboGuice;
import roboguice.inject.RoboInjector;
import sa.gov.moe.etraining.R;
import sa.gov.moe.etraining.base.BaseFragmentActivity;
import sa.gov.moe.etraining.databinding.ActivityLoginBinding;
import sa.gov.moe.etraining.exception.LoginErrorMessage;
import sa.gov.moe.etraining.exception.LoginException;
import sa.gov.moe.etraining.http.HttpStatus;
import sa.gov.moe.etraining.http.HttpStatusException;
import sa.gov.moe.etraining.http.constants.ApiConstants;
import sa.gov.moe.etraining.model.api.ProfileModel;
import sa.gov.moe.etraining.module.analytics.AnalyticsRegistry;
import sa.gov.moe.etraining.module.prefs.LoginPrefs;
import sa.gov.moe.etraining.module.prefs.PrefManager;
import sa.gov.moe.etraining.social.ISocial;
import sa.gov.moe.etraining.social.SocialFactory;
import sa.gov.moe.etraining.social.SocialLoginDelegate;
import sa.gov.moe.etraining.social.SocialProvider;
import sa.gov.moe.etraining.social.google.GoogleOauth2;
import sa.gov.moe.etraining.social.google.GoogleProvider;
import sa.gov.moe.etraining.task.Task;
import sa.gov.moe.etraining.util.AppStoreUtils;
import sa.gov.moe.etraining.util.Config;
import sa.gov.moe.etraining.util.StandardCharsets;
import sa.gov.moe.etraining.util.images.ErrorUtils;
import sa.gov.moe.etraining.util.observer.BasicObservable;
import sa.gov.moe.etraining.view.LoginActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import static sa.gov.moe.etraining.http.util.CallUtil.executeStrict;


public class SSOAuthActivity extends BaseFragmentActivity
        implements SocialLoginDelegate.MobileLoginCallback {

    @Inject
    private LoginPrefs loginPrefs;
    private static final String TAG = "TAG";
    public static String SSOAuthUrl;
    public static String accessTokenRequestUrl;
    public static String accessTokenRequestData;

    private final String USER_AGENT = "Mozilla/5.0";

    private final OkHttpClient client = new OkHttpClient();
    private SocialLoginDelegate socialLoginDelegate;

    private ActivityLoginBinding activityLoginBinding;

    private ISocial google, facebook;

    @NonNull
    private final LoginService loginService = this.loginService;

    @NonNull
    private final BasicObservable<LogInEvent> logInEvents = new BasicObservable<>();

    @NonNull
    private final AnalyticsRegistry analyticsRegistry = this.analyticsRegistry;
    private WebView htmlWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.sso_login_activity);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        socialLoginDelegate = new SocialLoginDelegate(this, savedInstanceState, this, environment.getConfig(), environment.getLoginPrefs());
        activityLoginBinding = DataBindingUtil.setContentView(this, R.layout.sso_login_activity);

        final RoboInjector injector = RoboGuice.getInjector(this);
        final Config config = injector.getInstance(Config.class);
        SSOAuthUrl = config.getSSOHostURL();
        accessTokenRequestUrl = config.getAccessTokenRequestUrl();
        accessTokenRequestData = config.getAccessTokenRequestData();

        htmlWebView = (WebView)findViewById(R.id.login_web_view);
        htmlWebView.setWebViewClient(new CustomWebViewClient());
        htmlWebView.clearCache(true);
        htmlWebView.clearHistory();
        htmlWebView.loadUrl("about:blank");
        WebSettings webSetting = htmlWebView.getSettings();
        webSetting.setJavaScriptEnabled(true);
        webSetting.setDisplayZoomControls(true);
        htmlWebView.loadUrl(SSOAuthUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.course_dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.login_web_view) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override public void onBackPressed() {
        WebView htmlWebView = (WebView)findViewById(R.id.login_web_view);
        if(htmlWebView.canGoBack()) {
            htmlWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void sendPost (String accessCode) throws Exception {

        System.out.println("POST >>> >>> >>>");
        URL url = new URL(accessTokenRequestUrl);

        System.out.println("URL: " + url.toString());
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.connect();

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        writer.write( accessTokenRequestData + accessCode);
        writer.flush();
        writer.close();
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "HTTPS AUTH TOKEN response code is " + conn.getResponseCode());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String decodedString;
        StringBuilder stringBuilder = new StringBuilder();
        while ((decodedString = in.readLine()) != null) {
            stringBuilder.append(decodedString);
        }

        String accessTokenRequestResponse = stringBuilder.toString();
        System.out.println("RESPONSE: " + accessTokenRequestResponse);
        os.close();
        in.close();
        conn.disconnect();
        destroyWebView();
        if (accessTokenRequestResponse.contains("access_token")) {
            HashMap responseMap = jsonToMap(accessTokenRequestResponse);

            String access_token = (String) responseMap.get("access_token");
            AuthResponse responseObject = new AuthResponse();

            responseObject.access_token = (String) responseMap.get("access_token");
            responseObject.refresh_token = (String) responseMap.get("refresh_token");
            responseObject.expires_in = Long.valueOf((String) responseMap.get("expires_in"));
            responseObject.scope = (String) responseMap.get("scope");
            responseObject.token_type = (String) responseMap.get("token_type");

//            Long id = 1000L;
//            String username = "a";
//            String email = "a@a.com";
//            String name = "aaaa";
//            ProfileModel profileModel = new ProfileModel();
//            profileModel.username = username;
//            profileModel.email = email;
//            profileModel.name = name;
//            profileModel.id = id;
//            responseObject.profile = profileModel;

            socialLoginDelegate.onOauthLoginSuccess(access_token, responseObject, PrefManager.Value.BACKEND_MOE);

//            environment.getRouter().showMainDashboard(this);
        }

    }

    public void destroyWebView() {

//        mWebContainer.removeAllViews();

        htmlWebView.clearHistory();
        htmlWebView.clearCache(true);
        htmlWebView.loadUrl("about:blank");
        htmlWebView.onPause();
        htmlWebView.removeAllViews();
        htmlWebView.destroyDrawingCache();
        htmlWebView.pauseTimers();
        htmlWebView.destroy();
        htmlWebView = null;
    }

    public static HashMap jsonToMap(String t) throws JSONException {

        HashMap<String, String> map = new HashMap<>();
        JSONObject jObject = new JSONObject(t);
        Iterator<?> keys = jObject.keys();

        while( keys.hasNext() ){
            String key = (String)keys.next();
            String value = jObject.getString(key);
            map.put(key, value);
        }
        return map;
    }


    private class CustomWebViewClient extends WebViewClient{
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d("WebView", "your current url when webpage loading... " + url);
            if (url.contains("code=")) {
                String authCode = url.split("code=", 2)[1];
                Log.d("WebView", "IT's ALIVE!!!" + authCode);
                try {
                    sendPost(authCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d("WebView", "your current url when webpage loading.. finish " + url);
            super.onPageFinished(view, url);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            // TODO Auto-generated method stub
            super.onLoadResource(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (environment.getLoginPrefs().getUsername() != null) {
            finish();
            environment.getRouter().showMainDashboard(this);
        }
    }


    /**
     * Starts fetching profile of the user after login by Facebook or Google.
     *
     * @param accessToken
     * @param backend
     */
    public void onSocialLoginSuccess(String accessToken, String backend, Task task) {
        tryToSetUIInteraction(false);
        task.setProgressDialog(findViewById(R.id.progress_indicator));
    }

    public void onUserLoginSuccess(ProfileModel profile) {
        setResult(RESULT_OK);
        finish();
        environment.getRouter().showMainDashboard(this);
        if (!environment.getConfig().isRegistrationEnabled()) {
            environment.getRouter().showMainDashboard(this);
        }
    }

    public void onUserLoginFailure(Exception ex, String accessToken, String backend) {
        tryToSetUIInteraction(true);

        if (ex != null && ex instanceof LoginException) {
            LoginErrorMessage errorMessage = (((LoginException) ex).getLoginErrorMessage());
            showAlertDialog(
                    errorMessage.getMessageLine1(),
                    (errorMessage.getMessageLine2() != null) ?
                            errorMessage.getMessageLine2() : getString(R.string.login_failed));
        } else if (ex != null && ex instanceof HttpStatusException &&
                ((HttpStatusException) ex).getStatusCode() == HttpStatus.UPGRADE_REQUIRED) {
            SSOAuthActivity.this.showAlertDialog(null,
                    getString(R.string.app_version_unsupported_login_msg),
                    getString(R.string.label_update),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppStoreUtils.openAppInAppStore(SSOAuthActivity.this);
                        }
                    }, getString(android.R.string.cancel), null);
        } else {
            showAlertDialog(getString(R.string.login_error), ErrorUtils.getErrorMessage(ex, SSOAuthActivity.this));
            logger.error(ex);
        }
    }

    @Override
    protected void onDestroy() {
//        htmlWebView.destroy();
//        htmlWebView = null;
        super.onDestroy();
    }

}