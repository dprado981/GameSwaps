package com.codepath.gameswap.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.codepath.gameswap.ImagePagerAdapter;
import com.codepath.gameswap.R;
import com.codepath.gameswap.models.Conversation;
import com.codepath.gameswap.models.Post;
import com.codepath.gameswap.models.PostReport;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public abstract class DetailFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = DetailFragment.class.getSimpleName();

    protected Context context;
    private FragmentActivity activity;
    private FragmentManager fragmentManager;

    protected Post post;
    private ParseUser user;
    private Conversation targetConversation;
    private List<ParseFile> images;
    private ImagePagerAdapter<ParseFile> adapter;

    private ImageView ivProfile;
    private TextView tvUsername;
    private TextView tvTitle;
    private ViewPager viewPager;
    private TextView tvNotesContent;
    private RatingBar rbCondition;
    private RatingBar rbDifficulty;
    private TextView tvAgeRatingValue;
    private Button btnMessage;

    public DetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_detail_game, container, false);
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
        toolbar.setTitle("");
        ivProfile = view.findViewById(R.id.ivProfile);
        tvUsername = view.findViewById(R.id.tvUsername);
        btnMessage = view.findViewById(R.id.btnMessage);
        tvTitle = view.findViewById(R.id.tvTitle);
        viewPager = view.findViewById(R.id.viewPager);
        tvNotesContent = view.findViewById(R.id.tvNotesContent);
        rbCondition = view.findViewById(R.id.rbCondition);
        rbDifficulty = view.findViewById(R.id.rbDifficulty);
        tvAgeRatingValue = view.findViewById(R.id.tvAgeRatingValue);
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        // Set up options menu
        setHasOptionsMenu(true);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
        }

        images = new ArrayList<>();
        adapter = new ImagePagerAdapter<>(context, images);
        viewPager.setAdapter(adapter);
        ViewTreeObserver viewTreeObserver = viewPager.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    viewPager.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    ViewGroup.LayoutParams params = viewPager.getLayoutParams();
                    params.height = viewPager.getWidth();
                    viewPager.setLayoutParams(params);
                }
            });
        }

        Bundle bundle = getArguments();
        if (bundle == null) {
            Log.e(TAG, "Error getting bundle for detail");
            if (fragmentManager != null) {
                fragmentManager.popBackStack();
            }
            return;
        }
        post = bundle.getParcelable(Post.TAG);
        if (post == null) {
            Log.e(TAG, "Error getting post for detail");
            if (fragmentManager != null) {
                fragmentManager.popBackStack();
            }
            return;
        }
        user = post.getUser();
        try {
            tvUsername.setText(user.fetchIfNeeded().getUsername());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (user.getUsername().equals(ParseUser.getCurrentUser().getUsername())) {
            btnMessage.setVisibility(View.INVISIBLE);
        }
        tvTitle.setText(post.getTitle());
        rbCondition.setRating((float) post.getCondition() / 10);
        rbDifficulty.setRating((float) post.getDifficulty() / 10);

        String ageRating = post.getAgeRating() + "+";
        tvAgeRatingValue.setText(ageRating);

        String notes = post.getNotes();
        if (notes.isEmpty()) {
            tvNotesContent.setText(R.string.not_specified);
        } else {
            tvNotesContent.setText(notes);
        }

        List<ParseFile> postImages = post.getImages();
        int maxSize = postImages.size();
        if (maxSize == 1) {
            tabLayout.setVisibility(View.GONE);
        }
        adapter.setMaxSize(maxSize);
        images.addAll(post.getImages());
        adapter.addAll(post.getImages());

        ParseFile profileImage = (ParseFile) post.getUser().get("image");
        if (profileImage != null) {
            Glide.with(context)
                    .load(profileImage.getUrl())
                    .placeholder(R.drawable.ic_profile)
                    .into(ivProfile);
        }

        ivProfile.setOnClickListener(this);
        tvUsername.setOnClickListener(this);
        btnMessage.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        if (view == btnMessage) {
            setTargetConversation();
        } else if (view == ivProfile || view == tvUsername) {
            // Ensure that correct menu item is selected
            if (user.getUsername().equals(ParseUser.getCurrentUser().getUsername())) {
                ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.actionProfile);
            }
            // Go to home fragment
            Fragment fragment = new ProfileFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable(Post.KEY_USER, user);
            fragment.setArguments(bundle);
            fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).addToBackStack(null).commit();
        }
    }

    private void setTargetConversation() {
        // Specify which class to query
        ParseQuery<Conversation> userOneQuery = ParseQuery.getQuery(Conversation.class);
        ParseQuery<Conversation> userTwoQuery = ParseQuery.getQuery(Conversation.class);

        // Find the Conversation that include the current user and the other user
        userOneQuery.whereEqualTo(Conversation.KEY_USER_ONE, ParseUser.getCurrentUser());
        userOneQuery.whereEqualTo(Conversation.KEY_USER_TWO, post.getUser());

        userTwoQuery.whereEqualTo(Conversation.KEY_USER_TWO, ParseUser.getCurrentUser());
        userTwoQuery.whereEqualTo(Conversation.KEY_USER_ONE, post.getUser());

        // Combine queries into a compound query
        List<ParseQuery<Conversation>> queries = new ArrayList<>();
        queries.add(userOneQuery);
        queries.add(userTwoQuery);
        ParseQuery<Conversation> query = ParseQuery.or(queries);

        // Include Users and sort by most recent
        query.include(Conversation.KEY_USER_ONE);
        query.include(Conversation.KEY_USER_TWO);
        query.include(Conversation.KEY_LAST_MESSAGE);
        query.include(Conversation.KEY_FROM_POST);
        query.addDescendingOrder(Conversation.KEY_UPDATED_AT);

        query.findInBackground(new FindCallback<Conversation>() {
            @Override
            public void done(List<Conversation> conversations, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting posts", e);
                    return;
                }
                if (!conversations.isEmpty()) {
                    targetConversation = conversations.get(0);
                } else {
                    targetConversation = new Conversation();
                    targetConversation.setUserOne(ParseUser.getCurrentUser());
                    targetConversation.setUserTwo(post.getUser());
                }
                targetConversation.setFromPost(post);
                targetConversation.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        goToConversationFragment(targetConversation);
                    }
                });

            }
        });
    }

    private void goToConversationFragment(Conversation targetConversation) {
        FragmentActivity activity = (FragmentActivity) context;
        // Ensure that correct menu item is selected
        ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.actionChat);
        // Go to home fragment
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment fragment = new ConversationFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Conversation.TAG, targetConversation);
        fragment.setArguments(bundle);
        fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).addToBackStack(null).commit();

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_post_options, menu);
        if (user.getUsername().equals(ParseUser.getCurrentUser().getUsername())) {
            menu.findItem(R.id.actionReport).setVisible(false);
        } else {
            menu.findItem(R.id.actionEdit).setVisible(false);
            menu.findItem(R.id.actionDelete).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.actionReport) {
            reportPost(post);
            return true;
        } else if (id == R.id.actionEdit) {
            goToEditPost();
            return true;
        } else if (id == R.id.actionDelete) {
            deletePost(post);
            return true;
        } else {
            Log.e(TAG, "Not yet implemented");
            return false;
        }
    }

    private void reportPost(final Post post) {
        ParseRelation<PostReport> relation = post.getRelation("reports");
        ParseQuery<PostReport> query = relation.getQuery();
        query.include(PostReport.KEY_POST);
        query.include(PostReport.KEY_REPORTED_BY);
        query.findInBackground(new FindCallback<PostReport>() {
            @Override
            public void done(List<PostReport> reports, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error retrieving reports", e);
                    Toast.makeText(context, "Error while reporting", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (PostReport report: reports) {
                    if (report.getReportedBy().getUsername().equals(ParseUser.getCurrentUser().getUsername())) {
                        Toast.makeText(context, "Already reported!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                final PostReport report = new PostReport();
                report.setReportedBy(ParseUser.getCurrentUser());
                report.setPost(post);
                report.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Toast.makeText(context, "Error while reporting", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(context, "Report sent!", Toast.LENGTH_SHORT).show();
                        post.getRelation("reports").add(report);
                        post.saveInBackground();
                        ParseUser.getCurrentUser().getRelation("postReports").add(report);
                        ParseUser.getCurrentUser().saveInBackground();
                    }
                });
            }
        });
    }


    protected abstract void goToEditPost();

    private void deletePost(final Post post) {
        final Post copy = Post.copy(post);
        post.deleteInBackground(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error deleting post", e);
                    Toast.makeText(context, "Error deleting post", Toast.LENGTH_SHORT).show();
                    return;
                }
                View view = getView();
                if (view != null) {
                    Snackbar.make(view, "Post deleted", Snackbar.LENGTH_SHORT)
                            .setAction("Undo", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    //post.saveInBackground();
                                    //referenceCopy.saveInBackground();
                                    copy.saveInBackground();
                                }
                            }).show();
                }
            }
        });
    }

}