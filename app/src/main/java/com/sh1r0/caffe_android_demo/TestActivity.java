package com.sh1r0.caffe_android_demo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.sh1r0.caffe_android_lib.CaffeMobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static android.content.ContentValues.TAG;

public class TestActivity extends Activity implements CNNListener {

    private ImageView imageView;
    private TextView pasteTime;
    private TextView classify;
    private TextView infomation;
    private Button start;
    private Button camera;

    private CaffeMobile caffeMobile;
    private static String[] IMAGENET_CLASSES;

    String paste;

    //获取文件路径
    File sdcard = Environment.getExternalStorageDirectory();
    String modelDir = sdcard.getAbsolutePath() + "/caffe_mobile/bvlc_reference_caffenet";
    String modelProto = modelDir + "/deploy.prototxt";
    String modelBinary = modelDir + "/bvlc_reference_caffenet.caffemodel";
    String imagePath = sdcard.getAbsolutePath()+"/caffe_mobile/re/test";


    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_test);

        imageView = (ImageView) findViewById(R.id.imageView);
        pasteTime = (TextView) findViewById(R.id.imageText);
        classify = (TextView) findViewById(R.id.Classifi);
        infomation = (TextView) findViewById(R.id.TextView);
        start = (Button) findViewById(R.id.button);
        camera = (Button) findViewById(R.id.camera);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    loadModel();

//                    for(int i=0;i<5;i++){
                        testImage();
//                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }




            }
        });

        infomation.setText("hello world");

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(TestActivity.this,Camera2.class));
            }
        });
    }

    public void loadModel() throws InterruptedException {
        Log.e(TAG, "onCreate: Start loading model");


        long start_time = SystemClock.uptimeMillis();
        caffeMobile = new CaffeMobile();
        caffeMobile.setNumThreads(4);
        caffeMobile.loadModel(modelProto, modelBinary);

        float[] meanValues = {104, 117, 123};
        caffeMobile.setMean(meanValues);

        AssetManager am = this.getAssets();
        try {
            InputStream is = am.open("synset_words.txt");
            Scanner sc = new Scanner(is);
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            IMAGENET_CLASSES = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        infomation.setText("End of the load model,"+String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - start_time));
        Log.e(TAG, "onCreate: End of the load model");


    }


    int array[] = {300,301,302,303,304,305,306,307,308,309,310,
            400,401,402,403,404,405,406,407,408,409,410,
            500,501,502,503,504,505,506,507,508,509,510,
            600,601,602,603,604,605,606,607,608,609,610,
            700,701,702,703,704,705,706,707,708,709,710
    };


    public void testImage() throws FileNotFoundException {
        int temp = (int) (Math.random()*54);
        int imagenum = array[temp];
        //        String imagePath = modelDir+"/test/";
        File file = new File(imagePath,imagenum+".jpg");
        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.fromFile(file)));
        Log.e(TAG, "testImage: "+imagePath);
        imageView.setImageBitmap(bitmap);
        String imgPath = file.getPath();
        CNNTask cnnTask = new CNNTask(TestActivity.this);
        Log.e(TAG, "testImage: ");
        cnnTask.execute(imgPath);


    }

    @Override
    public void onTaskCompleted(int result) {
        classify.setText("The category of the last picture: "+IMAGENET_CLASSES[result]);
        try {
            testImage();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private class CNNTask extends AsyncTask<String, Void, Integer> {
        private CNNListener listener;
        private long startTime;

        public CNNTask(CNNListener listener) {
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(String... strings) {
            Log.e(TAG, "doInBackground: ");
            startTime = SystemClock.uptimeMillis();
            return caffeMobile.predictImage(strings[0])[0];
        }

        @Override
        protected void onPostExecute(Integer integer) {
            Log.e("", String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
            pasteTime.setText(String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
            listener.onTaskCompleted(integer);
            super.onPostExecute(integer);
        }
    }




}

























