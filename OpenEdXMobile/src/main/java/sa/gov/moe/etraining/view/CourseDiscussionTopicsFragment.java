package sa.gov.moe.etraining.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.TextViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.inject.Inject;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import sa.gov.moe.etraining.R;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import retrofit2.Call;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;
import sa.gov.moe.etraining.discussion.CourseTopics;
import sa.gov.moe.etraining.discussion.DiscussionService;
import sa.gov.moe.etraining.discussion.DiscussionTopic;
import sa.gov.moe.etraining.discussion.DiscussionTopicDepth;
import sa.gov.moe.etraining.event.CourseDashboardRefreshEvent;
import sa.gov.moe.etraining.event.NetworkConnectivityChangeEvent;
import sa.gov.moe.etraining.http.callback.ErrorHandlingCallback;
import sa.gov.moe.etraining.http.notifications.FullScreenErrorNotification;
import sa.gov.moe.etraining.interfaces.RefreshListener;
import sa.gov.moe.etraining.logger.Logger;
import sa.gov.moe.etraining.model.api.EnrolledCoursesResponse;
import sa.gov.moe.etraining.util.SoftKeyboardUtil;
import sa.gov.moe.etraining.view.adapters.DiscussionTopicsAdapter;
import sa.gov.moe.etraining.view.common.TaskProgressCallback;

public class CourseDiscussionTopicsFragment extends OfflineSupportBaseFragment
        implements RefreshListener {
    private static final Logger logger = new Logger(CourseDiscussionTopicsFragment.class.getName());

    @InjectView(R.id.discussion_topics_searchview)
    private SearchView discussionTopicsSearchView;

    @InjectView(R.id.discussion_topics_listview)
    private ListView discussionTopicsListView;

    @InjectExtra(Router.EXTRA_COURSE_DATA)
    private EnrolledCoursesResponse courseData;

    @Inject
    private DiscussionService discussionService;

    @Inject
    private DiscussionTopicsAdapter discussionTopicsAdapter;

    @Inject
    private Router router;

    @InjectView(R.id.loading_indicator)
    ProgressBar progressSpinner;

    private Call<CourseTopics> getTopicListCall;

    private FullScreenErrorNotification errorNotification;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discussion_topics, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        errorNotification = new FullScreenErrorNotification((View) discussionTopicsListView.getParent());

        final LayoutInflater inflater = LayoutInflater.from(getActivity());

        // Add "All posts" item
        {
            final TextView header = (TextView) inflater.inflate(R.layout.row_discussion_topic, discussionTopicsListView, false);
            header.setText(R.string.discussion_posts_filter_all_posts);

            final DiscussionTopic discussionTopic = new DiscussionTopic();
            discussionTopic.setIdentifier(DiscussionTopic.ALL_TOPICS_ID);
            discussionTopic.setName(getString(R.string.discussion_posts_filter_all_posts));
            discussionTopicsListView.addHeaderView(header, new DiscussionTopicDepth(discussionTopic, 0, true), true);
        }

        // Add "Posts I'm following" item
        {
            final TextView header = (TextView) inflater.inflate(R.layout.row_discussion_topic, discussionTopicsListView, false);
            header.setText(R.string.forum_post_i_am_following);
            Context context = getActivity();
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(header,
                    new IconDrawable(context, FontAwesomeIcons.fa_star)
                            .colorRes(context, R.color.edx_brand_gray_base)
                            .sizeRes(context, R.dimen.edx_base),
                    null, null, null);
            final DiscussionTopic discussionTopic = new DiscussionTopic();
            discussionTopic.setIdentifier(DiscussionTopic.FOLLOWING_TOPICS_ID);
            discussionTopic.setName(getString(R.string.forum_post_i_am_following));
            discussionTopicsListView.addHeaderView(header, new DiscussionTopicDepth(discussionTopic, 0, true), true);
        }

        discussionTopicsListView.setAdapter(discussionTopicsAdapter);

        discussionTopicsSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query == null || query.trim().isEmpty())
                    return false;
                router.showCourseDiscussionPostsForSearchQuery(getActivity(), query, courseData);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        discussionTopicsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                router.showCourseDiscussionPostsForDiscussionTopic(
                        getActivity(),
                        ((DiscussionTopicDepth) parent.getItemAtPosition(position)).getDiscussionTopic(),
                        courseData);
            }
        });

        getTopicList();
    }

    private void getTopicList() {
        if (getTopicListCall != null) {
            getTopicListCall.cancel();
        }
        final TaskProgressCallback.ProgressViewController progressViewController =
                new TaskProgressCallback.ProgressViewController(progressSpinner);
        getTopicListCall = discussionService.getCourseTopics(courseData.getCourse().getId());
        getTopicListCall.enqueue(new ErrorHandlingCallback<CourseTopics>(
                getActivity(), progressViewController, errorNotification, null, this) {
            @Override
            protected void onResponse(@NonNull final CourseTopics courseTopics) {
                logger.debug("GetTopicListTask success=" + courseTopics);
                ArrayList<DiscussionTopic> allTopics = new ArrayList<>();
                allTopics.addAll(courseTopics.getNonCoursewareTopics());
                allTopics.addAll(courseTopics.getCoursewareTopics());

                List<DiscussionTopicDepth> allTopicsWithDepth = DiscussionTopicDepth.createFromDiscussionTopics(allTopics);
                discussionTopicsAdapter.setItems(allTopicsWithDepth);
                discussionTopicsAdapter.notifyDataSetChanged();
            }

            @Override
            protected void onFinish() {
                if (!EventBus.getDefault().isRegistered(CourseDiscussionTopicsFragment.this)) {
                    EventBus.getDefault().registerSticky(CourseDiscussionTopicsFragment.this);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        SoftKeyboardUtil.clearViewFocus(discussionTopicsSearchView);
    }

    @SuppressWarnings("unused")
    public void onEvent(CourseDashboardRefreshEvent event) {
        errorNotification.hideError();
        getTopicList();
    }

    @Override
    public void onRefresh() {
        EventBus.getDefault().post(new CourseDashboardRefreshEvent());
    }

    @Override
    protected boolean isShowingFullScreenError() {
        return errorNotification != null && errorNotification.isShowing();
    }

    @SuppressWarnings("unused")
    public void onEvent(NetworkConnectivityChangeEvent event) {
        onNetworkConnectivityChangeEvent(event);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
    }
}
