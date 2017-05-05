package ai.deepen.android.deepenlibsample;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.deepen.android.deepenlib.DeepenImage;

public class MainActivity extends AppCompatActivity {

    private static int REQUEST_IMAGE_CAPTURE = 173;

    private static final String MODE_CUSTOM_CLASSIFIER = "Custom Classifier";
    private static final String MODE_GENERAL_CLASSIFIER = "General Classifier";
    private static final String MODE_GENDER_CLASSIFIER = "Gender Classifier";

    private static final List<String> sModes = Arrays.asList(
            new String[]{MODE_CUSTOM_CLASSIFIER, MODE_GENERAL_CLASSIFIER, MODE_GENDER_CLASSIFIER});

    private FloatingActionButton mFloatingActionButton;
    private ListView mGeneralClassesListView;
    private LinearLayout mGenderLinearLayout;
    private LinearLayout mCustomObjectLinearLayout;
    private ProgressBar mProgressBar;
    private AsyncTask<Void, Void, List<ClassifierArrayAdapter.ClassifierResult>> mRefreshAsyncTask;
    private ImageView mImageView;
    private ArrayAdapter mGeneralClassesListViewAdapter;
    private TextView mGenderTextView;
    private TextView mCustomObjectTextView;
    private ImageView mGenderImageView;
    private ImageView mCustomObjectImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabAction();
            }
        });

        mGenderTextView = (TextView) findViewById(R.id.text_view_gender);
        mCustomObjectTextView = (TextView) findViewById(R.id.text_view_custom_object);
        mGenderImageView = (ImageView) findViewById(R.id.image_view_gender);
        mCustomObjectImageView = (ImageView) findViewById(R.id.image_view_custom_object);

        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mProgressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(
                R.color.colorPrimaryDark),
                PorterDuff.Mode.MULTIPLY);
        mImageView = (ImageView) findViewById(R.id.image_view);
        Picasso.with(this).load("file://" + getImagePath(Prefs.getMode(this))).into(mImageView);
        mCustomObjectLinearLayout = (LinearLayout) findViewById(R.id.linear_layout_custom_object);
        mGenderLinearLayout = (LinearLayout) findViewById(R.id.linear_layout_gender);
        mGeneralClassesListView = (ListView) findViewById(R.id.list_view_general_classes);
        mGeneralClassesListViewAdapter = new ClassifierArrayAdapter(this);
        mGeneralClassesListView.setAdapter(mGeneralClassesListViewAdapter);

        if (!PermissionUtil.hasPermission(this)) {
            PermissionUtil.requestPermission(this);
        }
        refreshResults();
    }

    private String getImagePath(int mMode) {
        return getExternalCacheDir().getAbsolutePath() + "/" + mMode + ".png";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_mode) {
            showModeDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void fabAction() {
        if (!PermissionUtil.hasPermission(this)) {
            Toast.makeText(this, "Need camera and storage permissions to continue",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            String filePath = getImagePath(Prefs.getMode(MainActivity.this));
            File imageFile = new File(filePath);
            Uri photoURI = FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID + ".provider", imageFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            takePictureIntent.putExtra("filePath", filePath);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            String filePath = getImagePath(Prefs.getMode(this));
            if (filePath != null) {
                Picasso.with(this).load("file://" + filePath).into(mImageView);
                refreshResults();
            }
        }
    }

    void refreshResults() {
        if (mRefreshAsyncTask != null) {
            mRefreshAsyncTask.cancel(true);
        }
        mRefreshAsyncTask = new RefreshAsyncTask();
        mRefreshAsyncTask.execute();
    }

    void showModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("");
        builder.setSingleChoiceItems(sModes.toArray(new String[0]),
                Prefs.getMode(MainActivity.this),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        Prefs.setMode(MainActivity.this, item);
                        Toast.makeText(getApplicationContext(),
                        "Selected: "+ sModes.get(item), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();// dismiss the alertbox after chose option
                        refreshResults();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void updateGeneralClassifierView(List<ClassifierArrayAdapter.ClassifierResult> results) {
        mGeneralClassesListViewAdapter.clear();
        mGeneralClassesListViewAdapter.addAll(results);
    }

    private void updateCustomClassifierView(List<ClassifierArrayAdapter.ClassifierResult> results) {
        mCustomObjectTextView.setText("Object probability: " + results.get(0).mScore + "");
        if (results.get(0).mScore > 0.5) {
            mCustomObjectImageView.setImageResource(R.drawable.checkbox_marked_circle);
            mCustomObjectImageView.setColorFilter(ContextCompat.getColor(this,
                    android.R.color.holo_green_dark));
        } else {
            mCustomObjectImageView.setImageResource(R.drawable.close_circle);
            mCustomObjectImageView.setColorFilter(ContextCompat.getColor(this,
                    android.R.color.holo_red_dark));
        }
    }

    private void updateGenderClassifierView(List<ClassifierArrayAdapter.ClassifierResult> results) {
        mGenderTextView.setText(results.get(0).mScore + "");
        float maleProbability = 0;
        for (ClassifierArrayAdapter.ClassifierResult classifierResult: results) {
            if (classifierResult.mClass.equals("male")) {
                maleProbability = classifierResult.mScore;
                break;
            }
        }
        if (maleProbability < 0.5) {
            mGenderImageView.setImageResource(R.drawable.human_female);
            mGenderTextView.setText("Female Probability: " + results.get(0).mScore);
        } else {
            mGenderImageView.setImageResource(R.drawable.human_male);
            mGenderTextView.setText("Male Probability: " + results.get(0).mScore);
        }
    }


    public static class ClassifierArrayAdapter extends ArrayAdapter<ClassifierArrayAdapter.ClassifierResult> {
        private Context mContext;
        public ClassifierArrayAdapter(Context context){
            super(context, R.layout.list_view_item_classifier_result);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(
                        R.layout.list_view_item_classifier_result,
                        parent, false);
            }
            final ClassifierResult classifierResult = getItem(position);
            ((TextView) convertView.findViewById(R.id.list_item_classifier_result_class)).setText(classifierResult.mClass);
            ((TextView) convertView.findViewById(R.id.list_item_classifier_result_score)).setText("" + classifierResult.mScore);

            return convertView;
        }

        public static class ClassifierResult {
            public final String mClass;
            public final float mScore;

            public ClassifierResult(String class1, float score) {
                mClass = class1;
                mScore = score;
            }
        }
    }

    private class RefreshAsyncTask extends AsyncTask<Void, Void, List<ClassifierArrayAdapter.ClassifierResult>> {
        private void progressBarIsRunning(boolean isRunning) {
            mProgressBar.setVisibility(isRunning ? View.VISIBLE : View.GONE);
            int mode = Prefs.getMode(MainActivity.this);
            mGeneralClassesListView.setVisibility(
                    !isRunning && sModes.get(mode).equals(MODE_GENERAL_CLASSIFIER) ?
                            View.VISIBLE : View.GONE);
            mGenderLinearLayout.setVisibility(
                    !isRunning && sModes.get(mode).equals(MODE_GENDER_CLASSIFIER) ?
                            View.VISIBLE : View.GONE);
            mCustomObjectLinearLayout.setVisibility(
                    !isRunning && sModes.get(mode).equals(MODE_CUSTOM_CLASSIFIER) ?
                            View.VISIBLE : View.GONE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBarIsRunning(true);
        }

        @Override
        protected List<ClassifierArrayAdapter.ClassifierResult> doInBackground(Void... params) {
            long startTime = System.currentTimeMillis();
            String imageFilePath = getImagePath(Prefs.getMode(MainActivity.this));
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath, options);

            if (bitmap == null) {
                return null;
            } else {
                Map<String, Float> classifierResultsMap = new HashMap<>();
                int mode = Prefs.getMode(MainActivity.this);
                if (sModes.get(mode).equals(MODE_GENERAL_CLASSIFIER)) {
                    classifierResultsMap.putAll(DeepenImage.identifyImage(MainActivity.this, bitmap));
                } else if(sModes.get(mode).equals(MODE_GENDER_CLASSIFIER)) {
                    classifierResultsMap.putAll(DeepenImage.identifyGender(MainActivity.this, bitmap));
                } else if(sModes.get(mode).equals(MODE_CUSTOM_CLASSIFIER)) {
                    classifierResultsMap.put("custom_object", DeepenImage.getCustomObjectProbability(
                            MainActivity.this, bitmap));
                }

                Log.i("Deepenlib", "Classifier inference done. Time elapsed:" + (System.currentTimeMillis() - startTime));

                List<ClassifierArrayAdapter.ClassifierResult> classifierResultsList = new ArrayList<>();
                for (String key: classifierResultsMap.keySet()) {
                    classifierResultsList.add(new ClassifierArrayAdapter.ClassifierResult(key,
                            classifierResultsMap.get(key)));
                }

                Collections.sort(classifierResultsList, new Comparator<ClassifierArrayAdapter.ClassifierResult>() {
                    @Override
                    public int compare(ClassifierArrayAdapter.ClassifierResult o1, ClassifierArrayAdapter.ClassifierResult o2) {
                        return o1.mScore <= o2.mScore ? 1 : -1;
                    }
                });

                return classifierResultsList;
            }
        }

        @Override
        protected void onPostExecute(List<ClassifierArrayAdapter.ClassifierResult> results) {
            super.onPostExecute(results);

            if (results == null) {
            } else if (results.isEmpty()) {
                Toast.makeText(MainActivity.this, "Could not get results", Toast.LENGTH_LONG).show();
            } else {
                int mode = Prefs.getMode(MainActivity.this);
                if (sModes.get(mode).equals(MODE_GENERAL_CLASSIFIER)) {
                    updateGeneralClassifierView(results);
                } else if(sModes.get(mode).equals(MODE_GENDER_CLASSIFIER)) {
                    updateGenderClassifierView(results);
                } else if(sModes.get(mode).equals(MODE_CUSTOM_CLASSIFIER)) {
                    updateCustomClassifierView(results);
                }
            }
            progressBarIsRunning(false);
        }
    }
}
