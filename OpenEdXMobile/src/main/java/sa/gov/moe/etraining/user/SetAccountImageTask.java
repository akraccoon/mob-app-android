package sa.gov.moe.etraining.user;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.inject.Inject;

import java.io.File;

import de.greenrobot.event.EventBus;
import sa.gov.moe.etraining.event.ProfilePhotoUpdatedEvent;
import sa.gov.moe.etraining.task.Task;
import sa.gov.moe.etraining.third_party.crop.CropUtil;

public class SetAccountImageTask extends
        Task<Void> {

    @Inject
    private UserAPI userAPI;

    @NonNull
    private final String username;

    @NonNull
    private Uri uri;

    @NonNull
    private final Rect cropRect;

    public SetAccountImageTask(@NonNull Context context, @NonNull String username, @NonNull Uri uri, @NonNull Rect cropRect) {
        super(context);
        this.username = username;
        this.uri = uri;
        this.cropRect = cropRect;
    }


    public Void call() throws Exception {
        final File cropped = new File(context.getExternalCacheDir(), "cropped-image" + System.currentTimeMillis() + ".jpg");
        CropUtil.crop(getContext(), uri, cropRect, 500, 500, cropped);
        userAPI.setProfileImage(username, cropped).execute();
        uri = Uri.fromFile(cropped);
        return null;
    }

    @Override
    protected void onSuccess(Void response) throws Exception {
        EventBus.getDefault().post(new ProfilePhotoUpdatedEvent(username, uri));
    }
}
