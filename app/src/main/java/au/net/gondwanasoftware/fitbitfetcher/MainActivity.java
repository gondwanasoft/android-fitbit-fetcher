// Based on https://stackoverflow.com/a/11107895/2086300

package au.net.gondwanasoftware.fitbitfetcher;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import au.net.gondwanasoftware.fitbitfetcher.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static String TAG = "Fitbit Fetcher";
    private ServerSocket server;

    Runnable conn = new Runnable() {
        public void run() {
            try {
                server = new ServerSocket(3000);

                Log.i(TAG,"Got server");
                while (true) {
                    Log.i(TAG,"before accept");
                    Socket socket = server.accept();
                    Log.i(TAG,"after accept");
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String str;
                    while ((str = in.readLine()) != null){
                        Log.i(TAG, "got client fetch request: " + str);
                    }

                    //in.close();
                    // TODO close somewhere!

                    // Send response:
                    // TODO why do we only get one message until companion closes? Send response? Response not received?
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("response from server");
                    out.close();

                    //socket.close();
                    // TODO close somewhere!
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //Log.i(TAG, "Starting server");
        new Thread(conn).start();
        //Log.i(TAG, "Started server");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (server != null) {
            try {
                server.close();     // TODO reopen in onResume?
            } catch (IOException e) {
                e.printStackTrace();
            }
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
}