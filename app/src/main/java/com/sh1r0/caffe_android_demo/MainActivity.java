package com.sh1r0.caffe_android_demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sh1r0.caffe_android_lib.CaffeMobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class MainActivity extends Activity implements CNNListener{
    private static final String TAG = "MainActivity";

    public static final int TAKE_PHOTO = 1;
    public static final int CHOOSE_PHOTO = 2;

    private ImageView picture;
    private Uri imageUri;
    private Button takePhoto;
    private Button chooseFromAlbum;
    private ProgressDialog dialog;
    private TextView tvLabel;

    private Button testBtn;

    private CaffeMobile caffeMobile;
    private String imgPath;
    private static String[] IMAGENET_CLASSES;
    private TextView timeText;

    Button button;



    //获取文件路径
    File sdcard = Environment.getExternalStorageDirectory();
    String modelDir = sdcard.getAbsolutePath() + "/caffe_mobile/bvlc_reference_caffenet";
    String modelProto = modelDir + "/deploy.prototxt";
    String modelBinary = modelDir + "/bvlc_reference_caffenet.caffemodel";
    String imagePath = sdcard.getAbsolutePath()+"/caffe_mobile/re/test/";





    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePhoto = (Button) findViewById(R.id.btnCamera);
        picture = (ImageView) findViewById(R.id.ivCaptured);
        tvLabel = (TextView) findViewById(R.id.tvLabel);
        timeText = (TextView) findViewById(R.id.timeText);

//        loadModel();

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //通过Context.getExternalCacheDir()方法可以获取到 SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
                File outputImage = new File(getExternalCacheDir(),"output_image.jpg");
                if(outputImage.exists()){
                    outputImage.delete();
                }
                imgPath = outputImage.getPath();
                //区分版本,运行版本低于Android7.0
                /*if (Build.VERSION.SDK_INT < 24) {
                    imageUri = Uri.fromFile(outputImage);
                    Log.d(TAG, "onClick: Build.VERSION.SDK_INT < 24");
                } else {
                    imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.cameraalbumtest.fileprovider", outputImage);
                    Log.d(TAG, "onClick: Build.VERSION.SDK_INT >= 24");
                }*/

                imageUri = Uri.fromFile(outputImage);

                //启动相机程序
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent,TAKE_PHOTO);

            }
        });

        chooseFromAlbum = (Button) findViewById(R.id.btnSelect);
        chooseFromAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.GET_CONTENT");
                intent.setType("image/*");
                startActivityForResult(intent,CHOOSE_PHOTO);

            }
        });

        testBtn = (Button) findViewById(R.id.Classtest);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    showTest(R.drawable.test_image);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e(TAG, "onClick: "+e.toString());
                }


            }
        });

        button = (Button) findViewById(R.id.next);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,TestActivity.class));
            }
        });

        takePhoto.setEnabled(false);
        chooseFromAlbum.setEnabled(false);
        testBtn.setEnabled(false);

        final Button load = (Button) findViewById(R.id.load);
        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadModel();
                takePhoto.setEnabled(true);
                chooseFromAlbum.setEnabled(true);
                testBtn.setEnabled(true);
                load.setEnabled(false);
            }
        });


    }

    public void showTest(int num) throws FileNotFoundException {
        int array[] = {300,301,302,303,304,305,306,307,308,309,310,
                       400,401,402,403,404,405,406,407,408,409,410,
                       500,501,502,503,504,505,506,507,508,509,510,
                       600,601,602,603,604,605,606,607,608,609,610,
                       700,701,702,703,704,705,706,707,708,709,710
        };
        int temp = (int) (Math.random()*29);
        int imagenum = array[temp];
//        String imagePath = modelDir+"/test/";
        File file = new File(imagePath+imagenum+".jpg");
        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.fromFile(file)));
        picture.setImageBitmap(bitmap);
        imgPath = file.getPath();
        test(bitmap);


    }


    public void loadModel(){
        Log.e(TAG, "onCreate: Start loading model");

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
        Log.e(TAG, "onCreate: End of the load model");




    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case TAKE_PHOTO:
                if(resultCode == RESULT_OK){
                    try{
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        test(bitmap);
                        picture.setImageBitmap(bitmap);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if(resultCode == RESULT_OK){
                    if(Build.VERSION.SDK_INT >= 19){
                        handleImageOnKitKat(data);
                    }else{
                        handleImageBeforeKitKat(data);
                    }

                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.fromFile(new File(imgPath))));
                        test(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            default:
                break;
        }



    }

    public void test(Bitmap bitmap){
        //进度对话框
        dialog = ProgressDialog.show(MainActivity.this, "Predicting...", "Wait for one sec...", true);

        CNNTask cnnTask = new CNNTask(MainActivity.this);
        cnnTask.execute(imgPath);


    }

    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        Log.d("TAG", "handleImageOnKitKat: uri is " + uri);
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // 如果是document类型的Uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1]; // 解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);

            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);

            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);

        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // 如果是file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();

        }
        this.imgPath = imagePath;
        displayImage(imagePath); // 根据图片路径显示图片
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        this.imgPath = imagePath;
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        // 通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            picture.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTaskCompleted(int result) {

        tvLabel.setText(IMAGENET_CLASSES[result]);

        if (dialog != null) {
            dialog.dismiss();
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
            startTime = SystemClock.uptimeMillis();
            return caffeMobile.predictImage(strings[0])[0];
        }

        @Override
        protected void onPostExecute(Integer integer) {
            Log.e("", String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
            timeText.setText(String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
            listener.onTaskCompleted(integer);
            super.onPostExecute(integer);
        }
    }
}

