package ai.deepen.android.deepenlibsample;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

public class PermissionUtil {

    public static final int PERMISSIONS_REQUEST = 451;

    public static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    public static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED
                    && context.checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public static void requestPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.shouldShowRequestPermissionRationale(PERMISSION_CAMERA)
                    || activity.shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(activity, "Need camera and storage permissions to continue",
                        Toast.LENGTH_LONG).show();
            }
            activity.requestPermissions(new String[]{PERMISSION_CAMERA,
                    PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }
}
