package com.buddy.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import com.buddy.sdk.models.TimedMetric;
import com.buddy.sdk.models.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BuddyClient {

	private String app_id;
	private String app_key;

    private BuddyServiceClient serviceClient;
    private BuddyClientOptions options;
    private Context context;
	private Location lastLocation;
    private UserAuthenticationRequiredCallback userAuthCallback;

	public BuddyClient(Context context, String appId, String appKey){
		this(context, appId, appKey, null);
	}

    public BuddyClient(Context context, String appId, String appKey, BuddyClientOptions options) {

        this.app_id = appId;
        this.app_key = appKey;
        this.context = context;

        BuddyClientSettings settings = getSettings();
        if (options == null) {
            this.options = new BuddyClientOptions();
        }
        else {
            this.options = options;
        }

        if (options.serviceRoot != null && settings.serviceRoot == null) {
            settings.serviceRoot = options.serviceRoot;
        }
        getServiceClient();
    }


    public void setUserAuthenticationRequiredCallback(UserAuthenticationRequiredCallback callback) {
        this.userAuthCallback = callback;
    }

    public void setLastLocation(Location loc) {
        lastLocation = loc;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    private void setDefaultParameters(Map<String,Object> parameters) {

        if (lastLocation != null && !parameters.containsKey("location")) {
            parameters.put("location", String.format("%s,%s", lastLocation.getLatitude(), lastLocation.getLongitude()));
        }
    }

    void runOnUiThread(Runnable r) {

        if (context != null) {
            // Get a handler that can be used to post to the main thread
            Handler mainHandler = new Handler(Looper.getMainLooper());

            mainHandler.post(r);
        }
        else
        {
            r.run();
        }
    }

    void getAccessToken(boolean autoRegister, final AccessTokenCallback callback) {

        String token = getSettings().getAccessToken();
		if (token != null) {
            callback.completed(null, token);
        }
        else if(autoRegister) {
            registerDevice(new BuddyCallback<AccessTokenResult>(AccessTokenResult.class) {
                @Override
                public void completed(BuddyResult<AccessTokenResult> result) {


                    if (result.getIsSuccess()) {
                        AccessTokenResult atr = result.getResult();

                        BuddyClientSettings settings = getSettings();

                        settings.deviceToken = atr.accessToken;
                        settings.deviceTokenExpires = atr.accessTokenExpires;
                        settings.appVersion = options.appVersion;
                        if (atr.serviceRoot != null) {
                            settings.serviceRoot = atr.serviceRoot;
                        }
                        saveSettings();
                        callback.completed(null, atr.accessToken);
                    } else {
                        callback.completed(result.convert(Boolean.FALSE), null);
                    }
                }
            });
        }
        else {
            callback.completed(null, null);
        }
    }


    //
    // REST Stuff
    //

    public String getServiceRoot() {
        return
                getSettings().getServiceRoot();
    }

    BuddyServiceClient getServiceClient() {
		
		if (serviceClient == null) {
			serviceClient = new BuddyServiceClient(this);
            serviceClient.setSynchronousMode(options.synchronousMode);
		}
		return serviceClient;
	}


    public <T> Future<BuddyResult<T>> get(String path, Map<String,Object> parameters, Class<T> clazz) {

        return getServiceClient().<T>makeRequest(BuddyServiceClient.GET, path, parameters, null, clazz);
    }

    public <T> Future<BuddyResult<T>> get(String path, Map<String,Object> parameters, final BuddyCallback<T> callback) {

        return getServiceClient().<T>makeRequest(BuddyServiceClient.GET, path, parameters, callback, null);
    }


    public <T> Future<BuddyResult<T>> post(String path, Map<String,Object> parameters, Class<T> clazz) {

        return getServiceClient().<T>makeRequest(BuddyServiceClient.POST, path, parameters, null, clazz);
    }

    public <T> Future<BuddyResult<T>> post(String path, Map<String,Object> parameters, final BuddyCallback<T> callback) {

        return getServiceClient().<T>makeRequest(BuddyServiceClient.POST, path, parameters, callback, null);
    }


    public <T> Future<BuddyResult<T>> patch(String path, Map<String,Object> parameters, Class<T> clazz) {

        return getServiceClient().<T>makeRequest(BuddyServiceClient.PATCH, path, parameters, null, clazz);
    }

    public <T> Future<BuddyResult<T>> patch(String path, Map<String,Object> parameters, final BuddyCallback<T> callback) {

        return getServiceClient().<T>makeRequest(BuddyServiceClient.PATCH, path, parameters, callback, null);
    }


    public <T> Future<BuddyResult<T>> delete(String path, Map<String,Object> parameters, Class<T> clazz) {

        return getServiceClient().<T>makeRequest(BuddyServiceClient.DELETE, path, parameters, null, clazz);
    }

    public <T> Future<BuddyResult<T>> delete(String path, Map<String,Object> parameters, final BuddyCallback<T> callback) {

        return getServiceClient().<T>makeRequest(BuddyServiceClient.DELETE, path, parameters, callback, null);
    }


    public <T> Future<BuddyResult<T>> put(String path, Map<String,Object> parameters,  Class<T> clazz) {

        return getServiceClient().<T>makeRequest(BuddyServiceClient.PUT, path, parameters, null, clazz);
    }

    public <T> Future<BuddyResult<T>> put(String path, Map<String,Object> parameters, final BuddyCallback<T> callback) {

        return getServiceClient().<T>makeRequest(BuddyServiceClient.PUT, path, parameters, callback, null);
    }

    private String getDeviceId() {

        if (context != null) {
            return Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        }
        return null;
    }

    private class AccessTokenResult {
        public String accessToken;
        public Date accessTokenExpires;
        public String serviceRoot;
    }

    public static final String NoRegisterDevice = "__noregdevice";

    private void registerDevice(final BuddyCallback<AccessTokenResult> callback) {

        Map<String, Object> parameters = new HashMap<String, Object>();

        parameters.put("platform", "Android");
        parameters.put("model",android.os.Build.MODEL);
        parameters.put("osVersion", android.os.Build.VERSION.RELEASE);

        if (options.deviceTag != null) {
            parameters.put("tag", options.deviceTag);
        }

        BuddyClientSettings settings = getSettings();

        if (settings.pushToken != null) {
            parameters.put("pushToken", settings.pushToken);
        }

        if (context != null) {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = null;
            try {
                info = manager.getPackageInfo(context.getPackageName(), 0);
                parameters.put("appVersion", String.format("%s (%d)", info.versionName, info.versionCode));
            } catch (PackageManager.NameNotFoundException e) {

            }
        }

        parameters.put("uniqueId", getDeviceId());
        parameters.put("appid", app_id);
        parameters.put("appkey", app_key);

        if (options.appVersion != null) {
            parameters.put("appversion", options.appVersion);
        }
        parameters.put(NoRegisterDevice, true);
        this.<AccessTokenResult>post("/devices", parameters, callback);
    }


    //
    // User Stuff
    //

    public Future<BuddyResult<User>> getCurrentUser(final BuddyCallback<User> callback) {

        return this.get("/users/me", null, new BuddyCallback<User>(User.class) {
            @Override
            public void completed(BuddyResult<User> result) {
                if (callback != null) {
                    callback.completed(result);
                }
            }
        });
    }

    private BuddyCallback<User> getUserCallback(final BuddyCallback<User> callback) {
        return new BuddyCallback<User>(User.class) {

            @Override
            public void completed(BuddyResult<User> result) {

                if (result.getIsSuccess()) {

                    JsonObject json;
                    json = result.getResult().getJsonObject();

                    if (json != null && json.has("accessToken")) {

                        BuddyClientSettings settings = getSettings();

                        settings.userToken = json.get("accessToken").getAsString();
                        settings.userTokenExpires = BuddyDateDeserializer.deserialize(json.get("accessTokenExpires").getAsString());
                        settings.userid = result.getResult().id;
                        saveSettings();
                    }
                }
                if (callback != null) {
                    callback.completed(result);
                }
            }
        };
    }

    public Future<BuddyResult<User>> createUser(String username, String password, String firstName, String lastName, String email, Date dateOfBirth, String gender, String tag, final BuddyCallback<User> callback) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("username", username);
        parameters.put("password", password);
        parameters.put("firstname", firstName);
        parameters.put("lastname", lastName);
        parameters.put("email", email);

        if (dateOfBirth != null) {
            parameters.put("dateOfBirth", dateOfBirth);
        }

        if (gender != null) {
            parameters.put("gender", gender);
        }

        parameters.put("tag", tag);

        return this.<User>post("/users", parameters, getUserCallback(callback));

    }

    public Future<BuddyResult<User>> loginUser(String username, String password, final BuddyCallback<User> callback) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("username", username);
        parameters.put("password", password);

        return this.<User>post("/users/login", parameters, getUserCallback(callback));

    }

    public Future<BuddyResult<Boolean>> logoutUser(final BuddyCallback<Boolean> callback) {
        Map<String, Object> parameters = new HashMap<String, Object>();


        final BuddyFuture<BuddyResult<Boolean>> promise = new BuddyFuture<BuddyResult<Boolean>>();


        BuddyFuture<BuddyResult<AccessTokenResult>> handle = (BuddyFuture<BuddyResult<AccessTokenResult>>)this.post("/users/me/logout", parameters, new BuddyCallback<AccessTokenResult>(AccessTokenResult.class) {

            @Override
            public void completed(BuddyResult<AccessTokenResult> result) {

                if (result.getIsSuccess()) {

                    BuddyClientSettings settings = getSettings();

                    AccessTokenResult r = result.getResult();

                    boolean hadUser = settings.userid != null && settings.userToken != null;

                    settings.deviceTokenExpires = r.accessTokenExpires;
                    settings.deviceToken = r.accessToken;
                    settings.userToken = null;
                    saveSettings();

                    if (hadUser && userAuthCallback != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                userAuthCallback.authenticate();
                            }
                        });
                    }
                }
                if (callback != null) {
                    callback.completed(result.<Boolean>convert(Boolean.TRUE));
                }
            }
        });


        handle.continueWith(new BuddyFutureCallback<BuddyResult<AccessTokenResult>>() {
            @Override
            public void completed(BuddyFuture<BuddyResult<AccessTokenResult>> future) {
                try {
                    promise.setValue(future.get().convert(future.get().getIsSuccess()));
                    return;
                } catch (InterruptedException e) {

                } catch (ExecutionException e) {

                }
                promise.setValue(null);
            }
        });

        return promise;
    }

    //
    // Metrics stuff
    //

    public Future<BuddyResult<TimedMetric>> recordMetricEvent(String eventName, Map<String,Object> values, final int timeoutInSeconds, final BuddyCallback<TimedMetric> callback) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        if (values != null) {
            parameters.put("values", values);
        }
        if (timeoutInSeconds > 0) {
            parameters.put("timeoutInSeconds", timeoutInSeconds);
        }

        try {
            eventName = URLEncoder.encode(eventName, "utf-8");
        } catch (UnsupportedEncodingException e) {

        }

        return this.<TimedMetric>post("/metrics/events/" + eventName,parameters, new BuddyCallback<TimedMetric>(TimedMetric.class) {
            @Override
            public void completed(BuddyResult<TimedMetric> result) {
                if (result.getIsSuccess() && timeoutInSeconds > 0) {
                    TimedMetric tm = result.getResult();
                    tm.setBuddyClient(BuddyClient.this);
                }
                if (callback != null) {
                    callback.completed(result);
                }
            }
           }
        );
    }

    //
    // Push Notification Stuff
    //
    public Future<BuddyResult<Boolean>> setPushToken(String pushToken, final BuddyCallback<Boolean> callback) {

        Map<String, Object> parameters = new HashMap<String, Object>();
        if (pushToken != null) {
            parameters.put("pushToken", pushToken);
        }

        BuddyClientSettings settings = getSettings();
        settings.pushToken = pushToken;
        saveSettings();

        final BuddyFuture<BuddyResult<Boolean>> promise = new BuddyFuture<BuddyResult<Boolean>>();


        BuddyFuture<BuddyResult<Object>> handle = (BuddyFuture<BuddyResult<Object>>) this.patch("/devices/current", parameters, new BuddyCallback<Object>(Object.class) {
            @Override
            public void completed(BuddyResult<Object> result) {
                if (callback != null) {
                    callback.completed(result.<Boolean>convert(result.getIsSuccess()));
                }
            }
        });

        handle.continueWith(new BuddyFutureCallback<BuddyResult<Object>>() {
            @Override
            public void completed(BuddyFuture<BuddyResult<Object>> future) {
                try {
                    promise.setValue(future.get().convert(Boolean.TRUE));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }


        });

        return promise;
    }


    public void recordNotificationReceived(Intent message) {

        String id = message.getStringExtra("_bId");
        if (id != null && id.length() > 0) {
            this.post(String.format("/notifications/received/%s", id), null, (Class) null);
        }
    }


    private static String DefaultRoot = "https://api.buddyplatform.com";

    private class BuddyClientSettings {



        public String serviceRoot;
        public String deviceToken;
        public Date   deviceTokenExpires;
        public String userToken;
        public Date   userTokenExpires;
        public String userid;
        public String pushToken;
        public String appVersion;

        public String getAccessToken() {
            Date now = new Date();
            if (userToken != null && userTokenExpires.after(now)) {
                return userToken;
            }
            else if (deviceToken != null && deviceTokenExpires.after(now)) {
                return deviceToken;
            }
            return null;
        }

        public String getServiceRoot() {

            if (serviceRoot == null) {
                return DefaultRoot;
            }
            return serviceRoot;
        }
    }

    private BuddyClientSettings settings;

    // preferences
    private SharedPreferences getPreferences()
    {
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(String.format("com.buddy-%s-%s", app_id, options == null ? null : options.settingsPrefix), Context.MODE_PRIVATE);

            return preferences;
        }
        return null;
    }

    private void saveSettings() {
        SharedPreferences preferences = getPreferences();

        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            String json = new Gson().toJson(settings);
            editor.putString(String.format("%s", app_id), json);
            editor.commit();
        }
    }

    private BuddyClientSettings getSettings() {
        if (settings == null) {
            SharedPreferences preferences = getPreferences();

            if (preferences != null) {
                String json = preferences.getString(String.format("%s", this.app_id), null);
                if (json != null) {
                    settings = new Gson().fromJson(json, BuddyClientSettings.class);
                }
            }

            if (settings == null) {
                settings = new BuddyClientSettings();
            }
        }
        return settings;
    }

    void handleError(BuddyResult result) {
        String error = result.getError();

        if (error != null) {

            BuddyClientSettings settings = getSettings();

            if (error.equals("AuthAppCredentialsInvalid") || error.equals("AuthAccessTokenInvalid")) {
                // Bad token, clear all settings so they'll be required.
                //
                settings.deviceToken = settings.userToken = null;
                saveSettings();
            }
            else if (error.equals("AuthUserAccessTokenRequired") && userAuthCallback != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        userAuthCallback.authenticate();
                    }
                });
            }


        }
    }
}
