/* Based on https://stackoverflow.com/a/11107895/2086300
   This provides no security or privacy.
   This may not work with non-UTF-8 characters.
   This does not run in the background. If you need that, turn it into a service.
   This will not handle high transfer rates. If you need that, use WebSockets and/or transmit data in binary (application/octet-stream).
   Prolonged communications (hours or days) probably won't be possible because the companion code within the Fitbit app
   won't be allowed to run or communicate indefinitely.
   If you need to access your data in some other application in real time, add an API to this, or add the ability for this to push data to your
   application using WebSockets.
 */

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

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import au.net.gondwanasoftware.fitbitfetcher.databinding.ActivityMainBinding;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
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
import java.util.HashMap;
import java.util.Map;

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
    Charset charset = StandardCharsets.UTF_8;       //Charset.forName("UTF-8");
    Map<Integer, String> statusResponses = new HashMap<Integer, String>();

    Runnable conn = new Runnable() {
        public void run() {
            //FileOutputStream dataFile = null;
            //FileOutputStream tempDataFile = null;
            /*try {
                tempDataFile = openFileOutput("userData.txt", MODE_APPEND);
            } catch (FileNotFoundException e) {
                Log.e(TAG,"Can't open tempDataFile: ", e);
            }*/
            //FileDescriptor fileDescriptor = null;

            try {
                server = new ServerSocket(3000);
                String str, fileName;
                int headerColonPos;
                boolean gotUserAgent, gotRequestedWith, gotHost, binary, json;
                int contentLength;
                Integer statusCode;

                //Log.i(TAG,"Got server");
                while (true) {
                    Log.i(TAG,"before accept");
                    setStatus("Ready");
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
                    gotUserAgent = gotRequestedWith = gotHost = binary = json = false;
                    contentLength = -1;
                    fileName = null;
                    while (!(str = in.readLine()).isEmpty()) {
                        Log.i(TAG, "request header: \"" + str + "\"");
                        headerColonPos = str.indexOf(':');
                        if (headerColonPos < 0) continue;   // invalid header; consider throwing
                        if (str.startsWith("User-Agent: Fitbit/")) gotUserAgent = true;
                        else if (str.equals("X-Requested-With: com.fitbit.FitbitMobile")) gotRequestedWith = true;
                        else if (str.equals("Content-Type: application/octet-stream")) binary = true;
                        else if (str.equals("Content-Type: application/json")) json = true;
                        else if (str.equals("Host: 127.0.0.1:3000")) gotHost = true;
                        else if (str.startsWith("Content-Length:")) {
                            contentLength = Integer.parseInt(str.substring(headerColonPos+1).trim());
                        }
                        else if (str.startsWith("FileName:")) {
                            fileName = str.substring(headerColonPos+1).trim();
                        }
                    }
                    //Log.i(TAG, "read empty string");

                    if (!gotHost || !gotRequestedWith || !gotUserAgent || contentLength<0)
                        throw new Exception("invalid header(s)");

                    if (json) {
                        statusCode = processControl(in, contentLength);
                    } else {
                        statusCode = processData(fileName, binary, in, contentLength);
                    }

                    /*if (fileName==null)
                        throw new Exception("fileName header missing");

                    Log.i(TAG, "Opening tempDataFile " + fileName);
                    tempDataFile = openFileOutput(fileName, 0);

                    // Process data payload:
                    statusCode = binary? processContentBinary(in,contentLength,tempDataFile) : processContentText(in,contentLength,tempDataFile);

                    Log.i(TAG, "Closing tempDataFile");
                    tempDataFile.close();
                    tempDataFile = null;*/

                    /*char buf[] = new char[contentLength];
                    int charsRead = in.read(buf, 0, contentLength);
                    Log.i(TAG,"contentLength="+contentLength+" charsRead="+charsRead);
                    if (!binary) Log.i(TAG,"req data: "+ new String(buf));

                    // Write data to temp file:
                    setStatus("Writing to temp file");
                    ByteBuffer tempByteBuffer = charset.encode(CharBuffer.wrap(buf));
                    Log.i(TAG,"before temp write");
                    tempDataFile.write(tempByteBuffer.array(), 0, tempByteBuffer.limit());
                    Log.i(TAG,"after temp write");*/

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
                    Log.i(TAG, "Sending response");
                    String statusResponse = "HTTP/1.1 " + statusCode + ' ' + statusResponses.get(statusCode);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(statusResponse);
                    out.println("Content-Type: text/plain\r\n");
                    out.println(fileName);
                    out.flush();
                    out.close();

                    //Log.i(TAG, "closing input");
                    in.close();
                    //Log.i(TAG, "closed input");

                    //Log.i(TAG, "closing socket");
                    socket.close();

                    //setCount(++count);
                }
            } catch (FileNotFoundException e) {
                setStatus("Can't open temp file");
                Log.e(TAG,"Can't open tempDataFile: ", e);
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
            /*if (tempDataFile != null) {
                try {
                    //dataFile.close();
                    Log.i(TAG, "Closing tempDataFile");
                    //setStatus("Closing temp file");
                    tempDataFile.close();
                } catch (IOException e) {
                    setStatus("Error closing temp file");
                    Log.e(TAG,"Closing tempDataFile: ", e);
                }
            }*/
            server = null;
        }
    };

    Integer processControl(BufferedReader in, int contentLength) throws IOException, JSONException {
        char buf[] = new char[contentLength];
        int charsRead = in.read(buf, 0, contentLength);
        String strg = new String(buf);
        JSONObject controlMessage = new JSONObject(strg);
        String status = controlMessage.getString("status");
        if (status.equals("done")) {
            setStatus("All files received!");
            enableGetData(true);
            return 200;
        }
        return 501;
    }

    Integer processData(String fileName, boolean binary, BufferedReader in, int contentLength) throws Exception {
        if (fileName==null)
            throw new Exception("fileName header missing");

        if (fileName.equals("1")) {     // we're starting a new session
            enableGetData(false);
            deleteFiles();
        }

        setFileName(fileName);

        Log.i(TAG, "Opening tempDataFile " + fileName);
        FileOutputStream tempDataFile = openFileOutput(fileName, 0);

        // Process data payload:
        Integer statusCode = 500;
        try {
            statusCode = binary ? processContentBinary(in, contentLength, tempDataFile) : processContentText(in, contentLength, tempDataFile);
        } catch(Exception e) {
            Log.i(TAG, "Exception: closing tempDataFile");
            tempDataFile.close();
            throw e;
        }

        Log.i(TAG, "Closing tempDataFile");
        tempDataFile.close();

        return statusCode;
    }

    Runnable combineFiles = new Runnable() {
        public void run() {
            String fileNamePrefix = "";       // should get this from companion, or keep track of every file received
            String fileNameExtension = "";    // should get this from companion, or keep track of every file received
            int part = 1;
            boolean fileExists;
            String dir = getFilesDir().getAbsolutePath();

            /*// Create some test files:
            try {
                String content = "File 1\r\n";
                FileOutputStream tempDataFile = openFileOutput("accel-1.dat", 0);
                tempDataFile.write(content.getBytes());
                tempDataFile.close();
                content = "File 2\r\n";
                tempDataFile = openFileOutput("accel-2.dat", 0);
                tempDataFile.write(content.getBytes());
                tempDataFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "rwt")) {
                if (pfd != null ) {
                    FileDescriptor fileDescriptor = pfd.getFileDescriptor();
                    FileChannel userDataFile = new FileOutputStream(pfd.getFileDescriptor()).getChannel();

                    do {
                        String fileName = fileNamePrefix + part + fileNameExtension;
                        try {
                            FileChannel tempDataFile = openFileInput(fileName).getChannel();
                            fileExists = true;
                            Log.i(TAG, "Appending " + fileName);
                            //userDataFile.transferFrom(tempDataFile, 0, tempDataFile.size());
                            tempDataFile.transferTo(0, tempDataFile.size(), userDataFile);
                            tempDataFile.close();
                            part++;
                        } catch (FileNotFoundException e) {
                            fileExists = false;
                        }
                    } while (fileExists);

                    userDataFile.close();
                    pfd.close();

                    deleteFiles();      // Delete all temp files

                    Snackbar.make(statusFragment.getView(), "Your file is ready!", Snackbar.LENGTH_LONG).show();
                    setStatus("Your file is ready!");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    void deleteFiles() {
        // Delete all temp files:
        Log.i(TAG, "Deleting temp files");
        File filesDir = getFilesDir();
        File[] files = filesDir.listFiles();
        for (File file: files) {
            file.delete();
        }
    }

    Integer processContentText(BufferedReader in, int contentLength, FileOutputStream tempDataFile) throws IOException {
        char buf[] = new char[contentLength];
        int charsRead = in.read(buf, 0, contentLength);
        Log.i(TAG,"contentLength="+contentLength+" charsRead="+charsRead);
        Log.i(TAG,"req data: "+ new String(buf));

        // Write data to temp file:
        setStatus("Writing to temp file");
        ByteBuffer tempByteBuffer = charset.encode(CharBuffer.wrap(buf));
        Log.i(TAG,"before temp write");
        tempDataFile.write(tempByteBuffer.array(), 0, tempByteBuffer.limit());
        Log.i(TAG,"after temp write");
        return 200;
    }

    Integer processContentBinary(BufferedReader in, int contentLength, FileOutputStream tempDataFile) throws Exception {
        // TODO 9 finish?
        Log.e(TAG,"Not implemented");
        return 501;
    }

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

    /*private void setCount(int count) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusFragment.setCount(count);
            }
        });
    }*/

    private void setFileName(String fileName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusFragment.setFileName(fileName);
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

    private void enableGetData(boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusFragment.enableGetData(enable);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO 9 should save state in onDestroy and restore it here

        statusResponses.put(200, "OK");
        statusResponses.put(400, "Bad Request");
        statusResponses.put(501, "Not implemented");
        statusResponses.put(507, "Insufficient Storage");

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
                enableGetData(false);
                uri = data.getData();
                //copyFile(uri);
                setStatus("Combining files; wait...");
                new Thread(combineFiles).start();
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

    public void onGetDataBtnClick(View view) {
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
