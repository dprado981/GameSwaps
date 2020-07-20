package com.codepath.gameswap.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codepath.gameswap.ConversationsAdapter;
import com.codepath.gameswap.EndlessRecyclerViewScrollListener;
import com.codepath.gameswap.R;
import com.codepath.gameswap.models.Conversation;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ChatsFragment extends Fragment {

    public static final String TAG = ChatsFragment.class.getSimpleName();

    private Context context;

    private RecyclerView rvConversations;
    private SwipeRefreshLayout swipeContainer;
    private EndlessRecyclerViewScrollListener scrollListener;

    private List<Conversation> conversations;
    private LinearLayoutManager layoutManager;
    private ConversationsAdapter adapter;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getContext();

        rvConversations = view.findViewById(R.id.rvConversations);
        swipeContainer = view.findViewById(R.id.swipeContainer);

        conversations = new ArrayList<>();
        layoutManager = new LinearLayoutManager(context);
        adapter = new ConversationsAdapter(context, conversations);
        rvConversations.setAdapter(adapter);
        rvConversations.setLayoutManager(layoutManager);

        queryConversations(false);

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                queryConversations(true);
            }
        };
        // Adds the scroll listener to RecyclerView
        rvConversations.addOnScrollListener(scrollListener);

        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                queryConversations(false);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    private void queryConversations(final boolean loadNext) {
        // Specify which class to query
        ParseQuery<Conversation> userOneQuery = ParseQuery.getQuery(Conversation.class);
        ParseQuery<Conversation> userTwoQuery = ParseQuery.getQuery(Conversation.class);

        // Find all Conversations that include the current user
        userOneQuery.whereEqualTo(Conversation.KEY_USER_ONE, ParseUser.getCurrentUser());
        userTwoQuery.whereEqualTo(Conversation.KEY_USER_TWO, ParseUser.getCurrentUser());

        // Combine queries into a compound query
        List<ParseQuery<Conversation>> queries = new ArrayList<>();
        queries.add(userOneQuery);
        queries.add(userTwoQuery);
        ParseQuery<Conversation> query = ParseQuery.or(queries);

        // Include Users and sort by most recent
        query.include(Conversation.KEY_USER_ONE);
        query.include(Conversation.KEY_USER_TWO);
        query.include(Conversation.KEY_LAST_MESSAGE);
        query.setLimit(20);
        query.addDescendingOrder(Conversation.KEY_UPDATED_AT);
        if (loadNext) {
            Date olderThanDate = conversations.get(conversations.size()-1).getCreatedAt();
            Log.i(TAG, "Loading conversations older than " + olderThanDate);
            query.whereLessThan(Conversation.KEY_UPDATED_AT, olderThanDate);
        }

        query.findInBackground(new FindCallback<Conversation>() {
            @Override
            public void done(List<Conversation> conversations, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting conversations", e);
                    return;
                }
                if (!loadNext) {
                    adapter.clear();
                    scrollListener.resetState();
                    swipeContainer.setRefreshing(false);
                }
                adapter.addAll(conversations);
                adapter.notifyDataSetChanged();
            }
        });
    }

}