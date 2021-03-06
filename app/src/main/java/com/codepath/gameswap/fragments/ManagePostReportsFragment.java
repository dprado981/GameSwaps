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

import com.codepath.gameswap.R;
import com.codepath.gameswap.ReportedPostsAdapter;
import com.codepath.gameswap.models.Post;
import com.codepath.gameswap.models.PostReport;
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
public class ManagePostReportsFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = ManagePostReportsFragment.class.getSimpleName();

    private Context context;
    private FragmentActivity activity;
    private FragmentManager fragmentManager;

    private RecyclerView rvUnderReview;
    private RecyclerView rvCompletedReview;
    private Button btnClear;

    private LinearLayoutManager underReviewLayoutManager;
    private List<Post> underReviewPosts;
    private ReportedPostsAdapter underReviewAdapter;
    private LinearLayoutManager completedReviewLayoutManager;
    private List<Post> completedReviewPosts;
    private ReportedPostsAdapter completedReviewAdapter;

    public ManagePostReportsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manage_reports, container, false);
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
        rvUnderReview = view.findViewById(R.id.rvUnderReview);
        rvCompletedReview = view.findViewById(R.id.rvCompletedReview);
        btnClear = view.findViewById(R.id.btnClear);

        tvTitle.setText(R.string.manage_reported_posts);
        btnClear.setOnClickListener(this);

        underReviewLayoutManager = new LinearLayoutManager(context);
        underReviewPosts = new ArrayList<>();
        underReviewAdapter = new ReportedPostsAdapter(context, underReviewPosts);
        rvUnderReview.setAdapter(underReviewAdapter);
        rvUnderReview.setLayoutManager(underReviewLayoutManager);

        completedReviewLayoutManager = new LinearLayoutManager(context);
        completedReviewPosts = new ArrayList<>();
        completedReviewAdapter = new ReportedPostsAdapter(context, completedReviewPosts);
        rvCompletedReview.setAdapter(completedReviewAdapter);
        rvCompletedReview.setLayoutManager(completedReviewLayoutManager);

        queryReportedPosts();
    }

    private void queryReportedPosts() {
        ParseRelation<PostReport> relation = ParseUser.getCurrentUser().getRelation("postReports");
        ParseQuery<PostReport> query = relation.getQuery();
        query.include("post");
        query.include("reportedBy");
        query.findInBackground(new FindCallback<PostReport>() {
            @Override
            public void done(List<PostReport> postReports, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error getting reports");
                    return;
                }
                List<Post> underReview = new ArrayList<>();
                List<Post> completedReview = new ArrayList<>();
                for (PostReport postReport : postReports) {
                    if (postReport.isUnderReview()) {
                        underReview.add(postReport.getPost());
                    } else {
                        completedReview.add(postReport.getPost());
                    }
                }
                underReviewAdapter.addAll(underReview);
                completedReviewAdapter.addAll(completedReview);
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view == btnClear) {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Completed Reports")
                    .setMessage("This action cannot be undone. Are you sure you want to delete the completed reports?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            completedReviewAdapter.deleteAll();
                        }
                    })
                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(R.drawable.ic_caution)
                    .show();

        }
    }
}