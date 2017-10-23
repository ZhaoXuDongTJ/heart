package com.blue.heart;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import cn.bmob.v3.Bmob;


public class RegisterActivity extends AppCompatActivity {

    ImageView headImView;
    ImageView ScanNode;
    EditText ID;
    Button Register;

    public static int SUCCEED_PHONE=0;
    public static int SUCCEED_ID=0;
//pic
    public static final int TAKE_PHOTO = 1;
    File outputImage;
    private Uri imageUri;
//scanNode
    public static final int TAKE_SCANNODE = 2;
//
    IFThread thread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        init();
        headImView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outputImage = new File(getExternalCacheDir(),"output_image.jpg");
                try {
                    if(outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(Build.VERSION.SDK_INT>=24){
                    imageUri = FileProvider.getUriForFile(RegisterActivity.this,"com.example.cameraalbumtest.fileprovider",outputImage);
                }else {
                    imageUri = Uri.fromFile(outputImage);
                }
                // 启动相机
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent,TAKE_PHOTO);
            }
        });
        ScanNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, CaptureActivity.class);
                startActivityForResult(intent, TAKE_SCANNODE);
            }
        });
        Register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode){
            case TAKE_PHOTO:
                Bitmap bitmap=null;
                try {
                    bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                    headImView.setImageBitmap(bitmap);
                    SUCCEED_PHONE=1;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case TAKE_SCANNODE:
                Bundle bundle = data.getExtras();
                String result = bundle.getString(CodeUtils.RESULT_STRING);
                ID.setText(result);
                SUCCEED_ID =1;
                break;
        }
    }

    class IFThread extends Thread{
        @Override
        public void run() {
            while (SUCCEED_PHONE==1&&SUCCEED_ID==1){
                Register.setEnabled(true);
            }
        }
    }

    public void init(){
        Bmob.initialize(this, "e41d17b489440cf5664f2f5761a84cb9");
        headImView = (ImageView)findViewById(R.id.headImView);
        ScanNode = (ImageView)findViewById(R.id.ScanNode);
        ID = (EditText)findViewById(R.id.ID);
        Register = (Button)findViewById(R.id.Register);
        ZXingLibrary.initDisplayOpinion(this);
        thread = new IFThread();
        thread.start();
    }
}
