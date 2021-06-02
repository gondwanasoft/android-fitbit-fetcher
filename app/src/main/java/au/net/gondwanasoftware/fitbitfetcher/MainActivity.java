// Based on https://stackoverflow.com/a/11107895/2086300
// This provides no security or privacy.
// This will not run in the background. If you need that, turn it into a service.
// This will not handle high transfer rates. If you need that, use WebSockets and/or transmit data in binary (application/octet-stream).
// Prolonged communications (hours or days) probably won't be possible because the companion code within the Fitbit app
// won't be allowed to run or communicate indefinitely.

package au.net.gondwanasoftware.fitbitfetcher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import au.net.gondwanasoftware.fitbitfetcher.databinding.ActivityMainBinding;
import android.view.Menu;
import android.view.MenuItem;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static final String TAG = "Fitbit Fetcher";
    private static final int CREATE_FILE = 1;
    private ServerSocket server;
    private Uri uri;
    //private FileDescriptor fileDescriptor;
    FirstFragment statusFragment;
    private int count = 0;

    Runnable conn = new Runnable() {
        public void run() {
            //FileOutputStream dataFile = null;
            FileOutputStream tempDataFile = null;
            try {
                tempDataFile = openFileOutput("userData.txt", MODE_APPEND);
            } catch (FileNotFoundException e) {
                Log.e(TAG,"Can't open tempDataFile: ", e);
            }
            FileDescriptor fileDescriptor = null;

            try {
                server = new ServerSocket(3000);
                String str;
                int headerColonPos;
                boolean gotUserAgent, gotRequestedWith, gotHost;
                int contentLength;
                Charset charset = StandardCharsets.UTF_8;       //Charset.forName("UTF-8");

                //Log.i(TAG,"Got server");
                while (true) {
                    Log.i(TAG,"before accept");
                    setStatus("Waiting");
                    Socket socket = server.accept();
                    Log.i(TAG,"after accept");
                    setStatus("Received request");

                    // Read request line:
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    str = in.readLine();
                    if (!str.equals("POST / HTTP/1.1")) {
                        throw new Exception("Unrecognised request line");   // should continue listening
                    }

                    // Read headers; check a few to feign an inadequate attempt at security:
                    gotUserAgent = gotRequestedWith = gotHost = false;
                    contentLength = -1;
                    while (!(str = in.readLine()).isEmpty()) {
                        //Log.i(TAG, "req hdr: \"" + str + "\"");
                        headerColonPos = str.indexOf(':');
                        if (headerColonPos < 0) continue;   // invalid header; consider throwing
                        if (str.startsWith("User-Agent: Fitbit/")) gotUserAgent = true;
                        else if (str.equals("X-Requested-With: com.fitbit.FitbitMobile")) gotRequestedWith = true;
                        else if (str.equals("Host: 127.0.0.1:3000")) gotHost = true;
                        else if (str.startsWith("Content-Length:")) {
                            contentLength = Integer.parseInt(str.substring(headerColonPos+1).trim());
                        }
                    }
                    //Log.i(TAG, "read empty string");
                    if (!gotHost || !gotRequestedWith || !gotUserAgent || contentLength<1)
                        throw new Exception("invalid header(s)");

                    // Read data payload:
                    char buf[] = new char[contentLength];
                    int charsRead = in.read(buf, 0, contentLength);
                    Log.i(TAG,"contentLength="+contentLength+" charsRead="+charsRead);
                    Log.i(TAG,"req data: "+ new String(buf));

                    // Write data to temp file:
                    setStatus("Writing to temp file");

                    /*CharBuffer testCharBuffer = CharBuffer.wrap(buf);
                    ByteBuffer testByteBuffer = charset.encode(testCharBuffer);
                    byte[] testArray = testByteBuffer.array();
                    Log.i(TAG,"length="+testByteBuffer.limit());*/

                    ByteBuffer tempByteBuffer = charset.encode(CharBuffer.wrap(buf));
                    Log.i(TAG,"before temp write");
                    tempDataFile.write(tempByteBuffer.array(), 0, tempByteBuffer.limit());
                    Log.i(TAG,"after temp write");
                    //tempDataFile.close();

                    /*if (dataFile==null && uri!=null) {
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "ra");
                        if (pfd != null) {
                            fileDescriptor = pfd.getFileDescriptor();
                        }
                        dataFile = new FileOutputStream(fileDescriptor);
                    }
                    Charset charset = StandardCharsets.UTF_8;       //Charset.forName("UTF-8");
                    ByteBuffer byteBuffer = charset.encode(CharBuffer.wrap(buf));
                    Log.i(TAG,"before write");
                    if (dataFile!=null) dataFile.write(byteBuffer.array());     // frequent "write failed: EBADF (Bad file descriptor)": check dataFile state; open and close every write or on error.
                    //writeOnUiThread(byteBuffer.array());
                    Log.i(TAG,"after write");
                    //dataFile.close();*/

                    // Send response:
                    setStatus("Sending response");
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/plain\r\n");
                    out.println("Saved");   // could change response depending on whether saved ok
                    out.flush();
                    out.close();

                    //Log.i(TAG, "closing input");
                    in.close();
                    //Log.i(TAG, "closed input");

                    //Log.i(TAG, "closing socket");
                    socket.close();

                    //statusFragment.setCount(++count);
                    setCount(++count);
                }
            } catch (IOException e) {
                setStatus(e.getMessage());
                Log.e(TAG, "IOException: ", e);
                //e.printStackTrace();
            } catch (Exception e) {
                setStatus(e.getMessage());
                Log.e(TAG, "Exception: ", e);
                //e.printStackTrace();
            }

            Log.i(TAG, "finishing run()");
            if (tempDataFile!=null) {
                try {
                    //dataFile.close();
                    Log.i(TAG, "Closing tempDataFile");
                    //setStatus("Closing temp file");
                    tempDataFile.close();
                } catch (IOException e) {
                    setStatus("Error closing temp file");
                    Log.e(TAG,"Closing tempDataFile: ", e);
                }
            }
            server = null;
        }
    };

    /*private void writeOnUiThread(byte[] data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG,"writing " + data.length + " bytes");
                    dataFile.write(data);
                    Log.i(TAG, "Bytes written");
                } catch (IOException e) {
                    Log.e(TAG, "IOException on dataFile.write():", e);
                }
            }
        });
    }*/

    private void setCount(int count) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusFragment.setCount(count);
            }
        });
    }

    private void setStatus(String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusFragment.setStatus(status);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO 9 should save state in onDestroy and restore it here

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        /*binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        /* // Get a file into which tempDataFile can be copied:
        File dir = getFilesDir();
        createFile(Uri.parse(dir.toString())); */

        //Log.i(TAG, "Starting server");
        //new Thread(conn).start();
        //Log.i(TAG, "Started server");

        //Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.statusFragment);
        //fragment.setCount(99);
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*if (dataFile != null)
            try {
                Log.i(TAG, "closing dataFile");
                dataFile.flush();   // gets called before all instances of writeOnUnThread have been called; display # requests saved? stop sending first?
                dataFile.close();
                Log.i(TAG, "closed dataFile");
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        if (server != null) {
            try {
                server.close();     // This also ends conn.start() via exception(?)
            } catch (IOException e) {
                Log.e(TAG, "closing server: ", e);
            }
            server = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if (fileDescriptor != null) {
            Log.i(TAG, "opening dataFile");
            //dataFile = new FileOutputStream(fileDescriptor);
            Log.i(TAG, "opened dataFile");*/

        if (server == null) {
            Log.i(TAG, "Starting server");
            new Thread(conn).start();
            Log.i(TAG, "Started server");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void createFile(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "fitbit.txt");

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, CREATE_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                uri = data.getData();
                copyFile(uri);
                /*try {
                    FileOutputStream dataFile = new FileOutputStream(new File(String.valueOf(uri)), true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }*/
            }
        }
    }

    private void copyFile(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "rwt");
            if (pfd != null ) {
                FileDescriptor fileDescriptor = pfd.getFileDescriptor();
                FileChannel userDataFile = new FileOutputStream(pfd.getFileDescriptor()).getChannel();

                FileChannel tempDataFile = openFileInput("userData.txt").getChannel();
                userDataFile.transferFrom(tempDataFile, 0, tempDataFile.size());

                userDataFile.close();
                tempDataFile.close();

                if (!deleteFile("userData.txt"))
                    throw new Exception("Can't delete temp file");

                setStatus("Done!");
            }
        } catch (IOException e) {
            Log.e(TAG,"Can't copy file: ", e);
            setStatus("Error copying file");
        } catch (Exception e) {
            Log.e(TAG,"Exception: ", e);
            setStatus(e.getMessage());
        }
    }

    public void onStatusFragmentCreated(FirstFragment fragment) {
        statusFragment = fragment;
    }

    public void onStopBtnClick(View view) {
        if (server != null) {
            try {
                server.close();     // This also ends conn.start() via exception(?)
            } catch (IOException e) {
                Log.e(TAG, "closing server: ", e);
            }
            server = null;
            // TODO 9 prevent automatic restart when resuming from CREATE_FILE?
        }

        // Get a file into which tempDataFile can be copied:
        File dir = getFilesDir();
        createFile(Uri.parse(dir.toString()));
    }
}