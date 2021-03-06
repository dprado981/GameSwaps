package com.codepath.gameswap.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codepath.gameswap.BlocksAdapter;
import com.codepath.gameswap.R;
import com.codepath.gameswap.models.Block;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ManageBlocksFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = ManageBlocksFragment.class.getSimpleName();

    private Context context;
    private FragmentActivity activity;
    private FragmentManager fragmentManager;

    private RecyclerView rvBlocked;
    private Button btnClear;

    private LinearLayoutManager layoutManager;
    private List<Block> allBlocks;
    private BlocksAdapter adapter;

    public ManageBlocksFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manage_blocks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = getContext();
        activity = (FragmentActivity) context;
        if (activity != null) {
            fragmentManager = activity.getSupportFragmentManager();
        }

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        TextView tvTitle = toolbar.findViewById(R.id.tvTitle);
        rvBlocked = view.findViewById(R.id.rvBlocked);
        btnClear = view.findViewById(R.id.btnClear);

        tvTitle.setText(R.string.manage_blocked_users);
        btnClear.setOnClickListener(this);

        layoutManager = new LinearLayoutManager(context);
        allBlocks = new ArrayList<>();
        adapter = new BlocksAdapter(context, allBlocks);
        rvBlocked.setAdapter(adapter);
        rvBlocked.setLayoutManager(layoutManager);

        queryBlocked();
    }

    private void queryBlocked() {
        ParseRelation<Block> relation = ParseUser.getCurrentUser().getRelation("blocks");
        ParseQuery<Block> query = relation.getQuery();
        query.include("user");
        query.include("blockedBy");
        query.findInBackground(new FindCallback<Block>() {
            @Override
            public void done(List<Block> blocks, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error getting reports");
                    return;
                }
                adapter.addAll(blocks);
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view == btnClear) {
            new AlertDialog.Builder(context)
                    .setTitle("Unblock Accounts")
                    .setMessage("This action cannot be undone. Are you sure you want to unblock all blocked accounts?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.deleteAll();
                        }
                    })
                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(R.drawable.ic_caution)
                    .show();
        }
    }
}