package au.net.gondwanasoftware.fitbitfetcher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import au.net.gondwanasoftware.fitbitfetcher.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private TextView countTextView, statusTextView;
    private Button getDataBtn;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        countTextView = (TextView) view.findViewById(R.id.textview_count);
        statusTextView = (TextView) view.findViewById(R.id.status);
        getDataBtn = (Button) view.findViewById(R.id.getDataBtn);
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.onStatusFragmentCreated(this);

        /*binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });*/
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /*public void setCount(int count) {
        countTextView.setText(Integer.toString(count));
    }*/

    public void setFileName(String fileName) {
        countTextView.setText(fileName);
    }
    public void setStatus(String status) {
        statusTextView.setText(status);
    }

    public void enableGetData(boolean enable) {
        getDataBtn.setEnabled(enable);
    }
}