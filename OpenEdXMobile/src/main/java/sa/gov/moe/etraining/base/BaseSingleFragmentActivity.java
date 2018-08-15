package sa.gov.moe.etraining.base;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import sa.gov.moe.etraining.R;

import roboguice.inject.InjectView;
import sa.gov.moe.etraining.view.AuthPanelUtils;
import sa.gov.moe.etraining.view.common.MessageType;
import sa.gov.moe.etraining.view.common.TaskProcessCallback;

public abstract class BaseSingleFragmentActivity extends BaseFragmentActivity implements TaskProcessCallback {

    public static final String FIRST_FRAG_TAG = "first_frag";

    @InjectView(R.id.loading_indicator)
    @Nullable
    ProgressBar progressSpinner;

    @InjectView(R.id.center_message_box)
    @Nullable
    TextView centerMessageBox;

    @InjectView(R.id.toolbar_placeholder)
    @NonNull
    View toolbarPlaceholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment_base);
        addToolbar();
        super.setToolbarAsActionBar();
    }

    /**
     * It will add the custom toolbar in the activity's layout.
     * <p>
     * Toolbar addition will be done by finding a placeholder view with id
     * {@link R.id#toolbar_placeholder R.id.toolbar_placeholder} and then replacing it with an
     * inflated custom toolbar layout. Custom toolbar layout will be obtained from
     * {@link BaseSingleFragmentActivity#getToolbarLayoutId()} function.
     * </p>
     */
    private void addToolbar() {
        final ViewGroup parent = (ViewGroup) toolbarPlaceholder.getParent();
        final int index = parent.indexOfChild(toolbarPlaceholder);
        parent.removeView(toolbarPlaceholder);
        final View toolbar = getLayoutInflater().inflate(getToolbarLayoutId(), parent, false);
        parent.addView(toolbar, index);
    }

    @LayoutRes
    protected int getToolbarLayoutId() {
        return R.layout.toolbar;
    }

    @Override
    protected void onResume() {
        super.onResume();
        AuthPanelUtils.configureAuthPanel(findViewById(R.id.auth_panel), environment);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (savedInstanceState == null) {
            try {
                this.loadFirstFragment();
            } catch (Exception e) {
                logger.error(e);
            }
        }

    }

    private void loadFirstFragment() throws Exception {
        Fragment singleFragment = getFirstFragment();

        //this activity will only ever hold this lone fragment, so we
        // can afford to retain the instance during activity recreation
        singleFragment.setRetainInstance(true);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.my_groups_list_container, singleFragment, FIRST_FRAG_TAG);
        fragmentTransaction.disallowAddToBackStack();
        fragmentTransaction.commit();
    }

    public abstract Fragment getFirstFragment();

    protected void showLoadingProgress() {
        if (progressSpinner != null) {
            progressSpinner.setVisibility(View.VISIBLE);
        }
    }

    protected void hideLoadingProgress() {
        if (progressSpinner != null) {
            progressSpinner.setVisibility(View.GONE);
        }
    }

    /**
     * implements TaskProcessCallback
     */
    public void startProcess() {
        showLoadingProgress();
    }

    /**
     * implements TaskProcessCallback
     */
    public void finishProcess() {
        hideLoadingProgress();
    }

    public void onMessage(@NonNull MessageType messageType, @NonNull String message) {
        //TODO - -we need to define different UI message view for different message type?
        switch (messageType) {
            case FLYIN_ERROR:
                this.showErrorMessage("", message);
                break;
            case FLYIN_WARNING:
            case FLYIN_INFO:
                this.showInfoMessage(message);
                break;
            case ERROR:
            case WARNING:
            case INFO:
                this.showMessageInSitu(message);
                break;
            case EMPTY:
                this.hideMessageInSitu();
                break;
            case DIALOG:
                this.showAlertDialog(null, message);
        }
    }

    protected void showMessageInSitu(String message) {
        if (centerMessageBox != null) {
            centerMessageBox.setVisibility(View.VISIBLE);
            centerMessageBox.setText(message);
        }
    }

    protected void hideMessageInSitu() {
        if (centerMessageBox != null) {
            centerMessageBox.setVisibility(View.GONE);
        }
    }
}
