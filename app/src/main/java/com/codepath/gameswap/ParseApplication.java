package com.codepath.gameswap;

import android.app.Application;
import android.util.Log;

import com.codepath.gameswap.models.Conversation;
import com.codepath.gameswap.models.Message;
import com.codepath.gameswap.models.Post;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.livequery.ParseLiveQueryClient;
import com.parse.livequery.SubscriptionHandling;

public class ParseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Register Parse models
        ParseObject.registerSubclass(Post.class);
        ParseObject.registerSubclass(Conversation.class);
        ParseObject.registerSubclass(Message.class);

        // set applicationId, and server server based on the values in the Heroku settings.
        // clientKey is not needed unless explicitly configured
        // any network interceptors must be added with the Configuration Builder given this syntax
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("diego-gameswap") // should correspond to APP_ID env variable
                .clientKey("ec7GNKG3Ad2Fx7kyh2EbT2d4x")  // set explicitly unless clientKey is explicitly configured on Parse server
                .server("https://diego-gameswap.herokuapp.com/parse/")
                .build());


        /* TODO: GET THE LIVEQUERY WORKING: basically, make a new parse server then upload it to heroku
        * Then you can get the settings to allow live queries
        */
    }
}