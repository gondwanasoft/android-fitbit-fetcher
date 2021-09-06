# android-fitbit-fetcher
android-fitbit-fetcher accepts accelerometer data sent from Fitbit companion to 127.0.0.1:3000, and saves it to an accessible file in the Android file system.

This repo is (hopefully) an Android Studio Java project that builds to an Android native app. To avoid the need for a security certificate, it must be run on the same phone/tablet as that on which the Fitbit app runs.

This works in conjunction with [fitbit-accel-fetcher](https://github.com/gondwanasoft/fitbit-accel-fetcher), but should work with any text data sent as a request body to 127.0.0.1:3000.

Architecture
-
Accelerometer data is collected in batches and stored on the watch. Multiple binary files are used to keep file sizes relatively small, which allows faster data transfer and facilitates retries if necessary.

The companion app (which runs within the Fitbit app on your phone) is responsible for converting binary files received from the watch back to text, and then forwarding them to the Android server (*ie*, this repository). The companion does not attempt to store files because the Fitbit OS API doesn't support that. The companion also does not attempt to retain any state information (*eg*, persisted variables) since it can be closed between receiving successive files.

The Android server (*ie*, this repository) receives files from the companion and stores them in private local storage. When all files have been received, it can merge them into one large file that represents the whole of the recorded session, and allows the user to save the combined file in public storage so they can access it.

Communication Protocol
-

The watch initiates the process by sending a data file to the companion using the file-transfer API. For simplicity, filenames are simple integers starting with 1.

When the companion receives a data file from the watch, it converts it from binary to text and sends it to the server using the fetch API.

The communication link between the watch and companion is assumed to be the bottleneck. It uses Bluetooth, and typically runs at about 1 kB/sec. Communications between the companion and the server should be much faster since the data is only transferred between apps on the same device (phone). This is why binary data is only used for the watch-to-companion leg; text data is used for the companion-to-server leg because it's simpler and allows the server to ignore the structure of the data.

Enhancements and Customisations
-

 See the start of [MainActivity.java](https://github.com/gondwanasoft/android-fitbit-fetcher/blob/master/app/src/main/java/au/net/gondwanasoftware/fitbitfetcher/MainActivity.java).

Limitations
-
This project is a minimal modification of a default Android Studio project. As such, it contains a lot of unnecessary code, resources, *etc*.

The code is poorly structured.

There is negligible attempt to provide security or flexibility.

Binary (executable) builds are not provided, because google would not be able to assess it without a suitable Fitbit watch, security is inadequate, and the source code will need to be adapted to meet individual requirements.

This project is not in active development or maintenance.

No support is provided.