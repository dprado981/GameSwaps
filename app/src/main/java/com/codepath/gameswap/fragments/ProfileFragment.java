package com.codepath.gameswap.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.codepath.gameswap.LoginActivity;
import com.codepath.gameswap.ProfilePagerAdapter;
import com.codepath.gameswap.R;
import com.codepath.gameswap.models.Block;
import com.codepath.gameswap.models.Conversation;
import com.codepath.gameswap.models.Post;
import com.codepath.gameswap.models.Report;
import com.google.android.material.tabs.TabLayout;
import com.parse.FindCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = ProfileFragment.class.getSimpleName();

    private Context context;
    private FragmentActivity activity;
    private FragmentManager fragmentManager;
    private int lastPosition;

    private ParseUser user;
    private ParseUser currentUser;
    private Conversation targetConversation;

    private ImageView ivProfile;
    private TextView tvName;
    private TextView tvBio;
    private Button btnMessage;

   public ProfileFragment() {

   }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = getContext();
        activity = (FragmentActivity) context;
        if (activity != null) {
            fragmentManager = activity.getSupportFragmentManager();
        }
        currentUser = ParseUser.getCurrentUser();

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        TextView tvTitle = toolbar.findViewById(R.id.tvTitle);
        ivProfile = view.findViewById(R.id.ivProfile);
        tvName = view.findViewById(R.id.tvName);
        tvBio = view.findViewById(R.id.tvBio);
        btnMessage = view.findViewById(R.id.btnMessage);
        ViewPager viewPager = view.findViewById(R.id.viewPager);
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);

        toolbar.setTitle("");
        tvTitle.setText(getString(R.string.profile));
        setHasOptionsMenu(true);
        toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow));
        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        if (appCompatActivity != null) {
            appCompatActivity.setSupportActionBar(toolbar);
        }

        Bundle bundle = getArguments();
        if (bundle == null) {
            user = ParseUser.getCurrentUser();
        } else {
            user = bundle.getParcelable(Post.KEY_USER);
        }

        if (user == null) {
            Log.e(TAG, "user not found");
            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = user.getUsername();
        if (username != null && !username.isEmpty()) {
            if (username.equals(ParseUser.getCurrentUser().getUsername())) {
                btnMessage.setVisibility(View.GONE);
            } else {
                btnMessage.setVisibility(View.VISIBLE);
            }
            tvTitle.setText(username);
        }

        String firstName = user.getString("firstName");
        if (firstName != null && !firstName.isEmpty())  {
            String displayName = firstName;
            String lastName = user.getString("lastName");
            if (lastName != null && !lastName.isEmpty()) {
                displayName += " " + lastName.charAt(0) + ".";
            }
            tvName.setText(displayName);
        }

        String bio = user.getString("bio");
        if (bio != null && !bio.isEmpty()) {
            tvBio.setText(bio);
        } else if (user.getUsername().equals(currentUser.getUsername())) {
            ClickableSpan goToEdit = new ClickableSpan() {
                @Override
                public void onClick(@NotNull View textView) {
                    goToEditProfile();
                }
                @Override
                public void updateDrawState(@NotNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            String registerText = "Click here to set a bio!";
            SpannableString ss = new SpannableString(registerText);
            ss.setSpan(goToEdit, 0, registerText.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent)),
                    0, registerText.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            tvBio.setText(ss);
            tvBio.setMovementMethod(LinkMovementMethod.getInstance());
            tvBio.setHighlightColor(Color.TRANSPARENT);
        }

        ParseFile image = (ParseFile) user.get("image");
        if (image != null) {
            Glide.with(context)
                    .load(image.getUrl())
                    .placeholder(R.drawable.ic_profile)
                    .into(ivProfile);
        }

        btnMessage.setOnClickListener(this);

        // Create an adapter that knows which fragment should be shown on each page
        ProfilePagerAdapter pagerAdapter = new ProfilePagerAdapter(context, getChildFragmentManager(),
                new ProfilePostsFragment(false, user), new ProfilePostsFragment(true, user));

        // Set the adapter onto the view pager
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onPause() {
        super.onPause();
        //lastPosition = layoutManager.findFirstVisibleItemPosition();
        lastPosition = 0;
    }

    @Override
    public void onClick(View view) {
       if (view == btnMessage) {
            goToConversation();
        }
    }

    private void logOut() {
        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with logout:", e);
                    return;
                }
                Intent intent = new Intent(context, LoginActivity.class);
                startActivity(intent);
                Activity activity = getActivity();
                if (activity != null) {
                    activity.finishAffinity();
                }
            }
        });
    }


    private void goToConversation() {
        // Specify which class to query
        ParseQuery<Conversation> userOneQuery = ParseQuery.getQuery(Conversation.class);
        ParseQuery<Conversation> userTwoQuery = ParseQuery.getQuery(Conversation.class);

        // Find the Conversation that include the current user and the other user
        userOneQuery.whereEqualTo(Conversation.KEY_USER_ONE, ParseUser.getCurrentUser());
        userOneQuery.whereEqualTo(Conversation.KEY_USER_TWO, user);

        userTwoQuery.whereEqualTo(Conversation.KEY_USER_TWO, ParseUser.getCurrentUser());
        userTwoQuery.whereEqualTo(Conversation.KEY_USER_ONE, user);

        // Combine queries into a compound query
        List<ParseQuery<Conversation>> queries = new ArrayList<>();
        queries.add(userOneQuery);
        queries.add(userTwoQuery);
        ParseQuery<Conversation> query = ParseQuery.or(queries);

        // Include Users and sort by most recent
        query.include(Conversation.KEY_USER_ONE);
        query.include(Conversation.KEY_USER_TWO);
        query.include(Conversation.KEY_LAST_MESSAGE);
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
                    goToConversationFragment(targetConversation);
                } else {
                    targetConversation = new Conversation();
                    targetConversation.setUserOne(ParseUser.getCurrentUser());
                    targetConversation.setUserTwo(user);
                    targetConversation.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            goToConversationFragment(targetConversation);
                        }
                    });
                }

            }
        });
    }

    private void goToConversationFragment(Conversation targetConversation) {
        // Go to conversation fragment
        Fragment fragment = new ConversationFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Conversation.TAG, targetConversation);
        fragment.setArguments(bundle);
        fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).addToBackStack(null).commit();
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_profile_options, menu);
        if (user.getUsername().equals(ParseUser.getCurrentUser().getUsername())) {
            menu.findItem(R.id.actionReport).setVisible(false);
            menu.findItem(R.id.actionBlock).setVisible(false);
        } else {
            menu.findItem(R.id.actionLogOut).setVisible(false);
            menu.findItem(R.id.actionEdit).setVisible(false);
            menu.findItem(R.id.actionSettings).setVisible(false);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.actionReport) {
            reportUser(user);
            return true;
        } else if (id == R.id.actionBlock) {
            blockUser(user);
            return true;
        } else if (id == R.id.actionLogOut) {
            logOut();
            return true;
        } else if (id == R.id.actionEdit) {
            goToEditProfile();
            return true;
        } else if (id == R.id.actionReportedPosts) {
            goToReportedPosts();
            return true;
        } else if (id == R.id.actionReportedUsers) {
            goToReportedUsers();
            return true;
        } else if (id == R.id.actionBlockedAccounts) {
            goToBlockedAccounts();
            return true;
        } else {
            return false;
        }
    }

    private void reportUser(final ParseUser reportedUser) {
        ParseRelation<Report> relation = currentUser.getRelation("reports");
        ParseQuery<Report> query = relation.getQuery();
        query.include(Report.KEY_USER);
        query.include(Report.KEY_REPORTED_BY);
        query.findInBackground(new FindCallback<Report>() {
            @Override
            public void done(List<Report> reports, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error retrieving reports", e);
                    Toast.makeText(context, "Error while reporting", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (Report report: reports) {
                    if (report.getUser().getUsername().equals(reportedUser.getUsername())) {
                        Toast.makeText(context, "Already reported!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                final Report report = new Report();
                report.setUser(reportedUser);
                report.setReportedBy(currentUser);
                report.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Toast.makeText(context, "Error while reporting", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(context, "Report sent!", Toast.LENGTH_SHORT).show();
                        currentUser.getRelation("reports").add(report);
                        currentUser.saveInBackground();
                    }
                });
            }
        });
    }

    private void blockUser(final ParseUser blocked) {
        ParseRelation<Block> relation = currentUser.getRelation("blocks");
        ParseQuery<Block> query = relation.getQuery();
        query.include(Block.KEY_USER);
        query.include(Block.KEY_BLOCKED_BY);
        query.findInBackground(new FindCallback<Block>() {
            @Override
            public void done(List<Block> blocks, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error retrieving blocks", e);
                    Toast.makeText(context, "Error while blocking", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (Block block: blocks) {
                    if (block.getBlockedBy().getUsername().equals(blocked.getUsername())) {
                        Toast.makeText(context, blocked.getUsername() + " is already blocked!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                final Block block = new Block();
                block.setUser(user);
                block.setBlockedBy(ParseUser.getCurrentUser());
                block.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Toast.makeText(context, "Error while blocking", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(context, blocked.getUsername() + " is now blocked!", Toast.LENGTH_SHORT).show();
                        currentUser.getRelation("blocks").add(block);
                        currentUser.saveInBackground();
                    }
                });
            }
        });
    }

    private void goToEditProfile() {
        Fragment fragment = new EditProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Post.KEY_USER, currentUser);
        fragment.setArguments(bundle);
        fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).addToBackStack(null).commit();
    }


    private void goToReportedPosts() {
        Fragment fragment = new ManagePostReportsFragment();
        fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).addToBackStack(null).commit();
    }

    private void goToReportedUsers() {
        Fragment fragment = new ManageUserReportsFragment();
        fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).addToBackStack(null).commit();
    }

    private void goToBlockedAccounts() {
        Fragment fragment = new ManageBlocksFragment();
        fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).addToBackStack(null).commit();
    }

}