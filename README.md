# Buddy Android SDK

These release notes are for the Buddy Platform Android SDK.

Please refer to [buddyplatform.com/docs](http://buddyplatform.com/docs) for more details on the Android SDK.

## Introduction

Buddy enables developers to build engaging, cloud-connected apps without having to write, test, manage or scale server-side code and infrastructure. We noticed that most mobile app developers end up writing the same code over and over again: user management, photo management, geolocation checkins, metadata, and more.  

Buddy's easy-to-use, scenario-focused APIs let you spend more time building your app, and less time worrying about backend infrastructure.  

This SDK is a thin wrapper over the Buddy REST interfaces, but takes care of the hard parts for you:

* Building and formatting requests
* Managing authentication
* Parsing responses
* Loading and saving credentials

The remainder of the Buddy API is easily accessible via standard REST API calls.

## Features

The Buddy Platform offers turnkey support for many common features, including:

* *User Accounts* - Create, delete, and authenticate users
* *Photos* - Add, search, and share photos with other users
* *Geolocation* - Check in, search for places, and list previous checkins
* *Push Notifications* - Easily send push notifications to iOS, Android, and Microsoft devices
* *Messaging* - Send messages to individuals and groups
* *User Lists* - Set up relationships between users
* *Game Scores, Metadata, and Boards* - Develop fully-featured, persistent games for your users
* *And more* - Check out the rest of our API at [buddy.com](http://buddy.com)

## Getting Started

To get started with the Buddy Platform SDK, please reference the _Getting Started_ series of documents at [buddyplatform.com/docs](http://buddyplatform.com/docs). You will need an App ID and Key before you can use the SDK. The _Getting Started_ documents will walk you through obtaining everything you need and show you where to find the SDK for your platform.

App IDs and Keys can be obtained at the Buddy Developer Dashboard at [buddyplatform.com](http://buddyplatform.com/login).

Full documentation for Buddy's services are available at [buddyplatform.com/docs](http://buddyplatform.com/docs).

## Installing the SDK

### Install from Maven/Gradle

In your build.gradle file under 'src', add a line for the Buddy Android SDK dependency

    dependencies {
        Compile fileTree(dir: 'libs', include: ['*.jar'])
        Compile 'com.buddy:androidsdk:+'
    }

This will install the "latest" release of the Buddy Android SDK.**
**Note:** If you wish to limit yourself to a narrower set of releases, you can do so like this (e.g. the latest release in the 0.1 series):

    Compile 'com.buddy:androidsdk:0.1.+'

Then in your source files, you can import from com.buddy.sdk to access the Buddy Classes
(e.g. import com.buddy.sdk.BuddyClient)

### Install Locally

#### Prerequisites

To build the SDK you need to:

1.  Install the [Android SDK Tools](http://developer.android.com/sdk/index.html)
2.  Set the ANDROID_HOME environment variable to the Android SDK install directory
3.  Install Android SDK build tools version 19.1.0, and SDK Platform for API 10 (Android 2.3.3) 

#### Build and Install

1.  Clone this repository to your local machine
2.  From the root of this repository, run `./gradlew build` (Mac/Linux) or `gradlew.bat build` (Windows) to build the SDK
3.  Look in the **library/build/libs** folder to find the JARs.
4.  Add The buddy-sdk-_version_.jar file as a dependency for your Android application.

## Using the Android SDK

Collect your App ID and App Key from the [Buddy Dashboard](http://buddyplatform.com).

To initialize the SDK:

    import com.buddy.sdk;
    // ...
    // Create the SDK client
    BuddyClient client = new BuddyClient(context, myAppId, myAppKey);
    
There are some helper functions for creating users, logging in users, and logging out users:  

    // Login a user
    client.loginUser('username', 'password', null, null, null, null, null, new BuddyCallback<User>(User.class) {
        @Override
        public void completed(BuddyResult<User> result) {
            if (result.getIsSuccess()) {
                TextView tv = (TextView)findViewById(R.id.textView1);
                tv.setText("Hello " + result.getResult().username);
            }
        }
    });
	
#### Standard REST requests
	  
The majority of the calls map directly to REST.  For all the calls you can either create a wrapper java class such as those found in `com.buddy.sdk.models`, or you can simply pass a type of `JsonObject` to retur a standard Gson JsonObject.

In this example we will create a checkin. Take a look at the [Create Checkin REST documentation](http://buddyplatform.com/docs/Create%20Checkin/HTTP), then:

    // Create a checkin
    Location location = getTheDeviceLocation();
    Map<String,Object> parameters = new HashMap<String,Object>();
    parameters.put("comment", "My first checkin");
    parameters.put("description", "This is where I was doing that thing.");
    parameters.put("location", String.format("%f,%f", location.getLatitude(), location.getLongitude());
    client.<JsonObject>post("/checkins", parameters, new BuddyCallback<JsonObject>(JsonObject.class) {
        @Override
        public void completed(BuddyResult<JsonObject> result) {
            if (result.getIsSuccess()) {
                JsonObject obj = result.getResult();
                // get the ID of the created checkin.
                String id = obj.getMember("id").getAsString();
            }
        }
    });
	
#### Creating Response Objects

Creating strongly typed response objects is simple.  If the REST operation that you intend to call returns a response that's not available in `com.buddy.sdk.models`, you can easily create one by creating a Java object with fields that match the JSON response fields for the operation.

1.  Go to the Buddy Console and try your operation
2.  When the operation completes, note the fields and their types in the response
3.  Create a Java class that derives from `com.buddy.sdk.models.ModelBase` with the appropriate properties.

For example, if the response to **POST /checkins** looks like:

     {
       "status": 201,
       "result": {
         "comment": "h1",
         "userID": "bv.HrcbbDkMPgfn",
         "id": "cb.gBgbvKFkdhnp",
         "location": {
           "lat": 46.2,
            "lng": -120.1
          },
         "created": "2014-07-09T07:07:21.463Z",
         "lastModified": "2014-07-09T07:07:21.463Z"
     },
     "request_id": "53bcea29b32fad0c405372b6",
     "success": true
    }

The corresponding Java object for the unique field under `result` is:

    public class Checkin extends ModelBase {
        public String comment;
    }
    
**Note:** we do not need to specify the default common properties `id`, `userID`, `location`, `created`, or `lastModified`.

We can then call:

     client.<Checkin>get("/checkins/" + myCheckinId, null, new BuddyCallback<Checkin>(Checkin.class){...});
	 
#### Managing Files

The Buddy Android SDK handles all necessary file management for you. The key class is `com.buddy.sdk.BuddyFile`, which is a wrapper around an Android `File` or `InputStream`, along with a MIME content type.

To upload a picture:

    BuddyFile file = new BuddyFile(new File("/some/image/foo.jpg"), "image/jpg");
    Map<String,Object> parameters = new HashMap<String,Object>();
    parameters.put("caption", "My first image");
    parameters.put("data", file);
    client.<Picture>post("/pictures", parameters, new BuddyCallback<Picture>(Picture.class){
        @Override
        public void completed(BuddyResult<Picture> result) {
            if (result.getIsSuccess()) {...}
        }
    });
    	
Likewise, to download a picture, specify BuddyFile as the operation type:

    client.<BuddyFile>get("/pictures/" + myPictureId + "/file", null, new BuddyCallback<BuddyFile>(BuddyFile.class){...});


## Contributing Back: Pull Requests

We'd love to have your help making the Buddy SDK as good as it can be!

To submit a change to the Buddy SDK please do the following:

1. Create your own fork of the Buddy SDK
2. Make the change to your fork
3. Before creating your pull request, please sync your repository to the current state of the parent repository: ```git pull origin master```
4. Commit your changes, then [submit a pull request](https://help.github.com/articles/using-pull-requests) for just that commit

## License

#### Copyright (C) 2014 Buddy Platform, Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.

