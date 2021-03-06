package sa.gov.moe.etraining.view;

import sa.gov.moe.etraining.base.BaseFragment;
import sa.gov.moe.etraining.event.NetworkConnectivityChangeEvent;
import sa.gov.moe.etraining.http.notifications.FullScreenErrorNotification;
import sa.gov.moe.etraining.http.notifications.SnackbarErrorNotification;

/**
 * Provides support for offline mode handling within a Fragment.
 * <br/>
 * Ensures that no two types of errors appear at the same time in a Fragment e.g. if
 * {@link FullScreenErrorNotification} is already appearing in an activity
 * {@link SnackbarErrorNotification} should never appear until and unless the
 * {@link FullScreenErrorNotification} is hidden.
 */
public abstract class OfflineSupportBaseFragment extends BaseFragment {
    /**
     * Tells if the Fragment is currently showing a {@link FullScreenErrorNotification}.
     *
     * @return <code>true</code> if {@link FullScreenErrorNotification} is visible,
     * <code>false</code> otherwise.
     */
    protected abstract boolean isShowingFullScreenError();

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        OfflineSupportUtils.setUserVisibleHint(getActivity(), isVisibleToUser,
                isShowingFullScreenError());
    }

    public void onNetworkConnectivityChangeEvent(NetworkConnectivityChangeEvent event) {
        OfflineSupportUtils.onNetworkConnectivityChangeEvent(getActivity(), getUserVisibleHint(), isShowingFullScreenError());
    }

    @Override
    protected void onRevisit() {
        OfflineSupportUtils.onRevisit(getActivity());
    }
}
