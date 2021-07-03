# android-fitbit-fetcher
Accepts acceleromete data sent from Fitbit companion to 127.0.0.1:3000, and saves it to an accessible file in the Android file system.

This repo is (hopfully) an Android Studio Java project that builds to an Android native app. To avoid the need for a non-self-signed security certificate, it must be run on the same phone/tablet as that on which the Fitbit app runs.

This works in conjunction with [fitbit-accel-fetcher](https://github.com/gondwanasoft/fitbit-accel-fetcher), but should work with any data sent as a request body to 127.0.0.1:3000. It assumes that filenames are simple integers starting with 1. Please inspect the source code to see other assumptions and details of the communication protocol.

Customisation suggestions are included at the start of [MainActivity.java](https://github.com/gondwanasoft/android-fitbit-fetcher/blob/master/app/src/main/java/au/net/gondwanasoftware/fitbitfetcher/MainActivity.java).
