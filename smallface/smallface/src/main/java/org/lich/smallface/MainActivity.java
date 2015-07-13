package org.lich.smallface;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity implements View.OnClickListener{

    private static final int PICK_CODE = 0x110;
    private ImageView mPhoto;
    private Button mGetImage;
    private Button mDetect;
    private TextView mTip;
    private View mWaitting;

    private String mCurrentPhotoStr;

    private Bitmap mPhotoImg;
    private Paint mPaint;

    private Button mChangeImg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initEvents();

        mPaint = new Paint();
    }

    private void initEvents() {
        mGetImage.setOnClickListener(this);
        mDetect.setOnClickListener(this);
        mChangeImg.setOnClickListener(this);
    }

    private void initViews(){
        mPhoto = (ImageView) findViewById(R.id.id_photo);
        mGetImage = (Button) findViewById(R.id.id_getImage);
        mDetect = (Button) findViewById(R.id.id_detect);
        mTip = (TextView) findViewById(R.id.id_tip);
        mWaitting = findViewById(R.id.id_waitting);

        mChangeImg = (Button) findViewById(R.id.id_change_right);


   }

    private static final int MSG_SUCESS = 0x111;
    private static final int MSG_ERR = 0x112;


    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case MSG_SUCESS:
                    mWaitting.setVisibility(View.GONE);
                    JSONObject obj = (JSONObject) msg.obj;

                    prepareRsBitmap(obj);

                    mPhoto.setImageBitmap(mPhotoImg);

                    break;
                case MSG_ERR:
                    mWaitting.setVisibility(View.GONE);
                    String errorMsg = msg.obj.toString();
                    if(TextUtils.isEmpty(errorMsg)){
                        mTip.setText("Error.");
                    }else{
                        mTip.setText(errorMsg);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void prepareRsBitmap(JSONObject obj){

        Bitmap bitmap = Bitmap.createBitmap(mPhotoImg.getWidth(),mPhotoImg.getHeight(),mPhotoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImg,0,0,null);

        try {
            JSONArray faces = obj.getJSONArray("face");
            int faceCount = faces.length();
            mTip.setText("找到" + faceCount + "张脸");

            for (int i = 0; i < faceCount; i++) {
                //拿到单独的face对象
                JSONObject face = faces.getJSONObject(i);
                JSONObject posObj = face.getJSONObject("position");

                float x = (float) posObj.getJSONObject("center").getDouble("x");
                float y = (float) posObj.getJSONObject("center").getDouble("y");

                float w = (float) posObj.getDouble("width");
                float h = (float) posObj.getDouble("height");


                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getHeight();

                w = w / 100 * bitmap.getWidth() ;
                h = h / 100 * bitmap.getHeight();

                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(5);
                //画box
                canvas.drawLine(x - w/2,y - h/2 ,x - w/2 , y + h/2,mPaint);
                canvas.drawLine(x - w/2,y - h/2 ,x + w/2 , y - h/2,mPaint);
                canvas.drawLine(x + w/2,y - h/2 ,x + w/2 , y + h/2,mPaint);
                canvas.drawLine(x - w/2,y + h/2 ,x + w/2 , y + h/2,mPaint);

                //get age and gender

                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                Bitmap ageBitmap = buildAgeBitmap(age,"Male".equals(gender));

                int ageWidth = ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();

                if(ageBitmap.getWidth() < mPhoto.getWidth() && ageBitmap.getHeight() < mPhoto.getHeight()){
                    float ratio = Math.max(ageBitmap.getWidth()*1.0f/mPhoto.getWidth(),ageBitmap.getHeight()*1.0f/mPhoto.getHeight())*2;
                    ageBitmap = Bitmap.createScaledBitmap(ageBitmap,(int)(ageWidth*ratio),(int)(ageHeight*ratio),false);
                }


                canvas.drawBitmap(ageBitmap,x - ageBitmap.getWidth()/2,y-h/2-ageBitmap.getHeight(),null);
                mPhotoImg = bitmap;
            }
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }

    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView tv = (TextView) mWaitting.findViewById(R.id.id_age_and_gender);

        tv.setText(age +"");

        if(isMale){
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.men),null,null,null);
        }else{
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female),null,null,null);
        }

        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();

        return bitmap;
    }

    @Override
    public void onClick(View v) {
        if(v == mGetImage){
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent,PICK_CODE);
        }
        if(v == mDetect){
            if(mPhotoImg!=null) {
                mWaitting.setVisibility(View.VISIBLE);
                FaceppDetect.detect(mPhotoImg, new FaceppDetect.CallBack() {

                    @Override
                    public void success(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what = MSG_SUCESS;
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void error(FaceppParseException exception) {
                        Message msg = Message.obtain();
                        msg.what = MSG_ERR;
                        msg.obj = exception;
                        mHandler.sendMessage(msg);
                    }
                });
            }
            else{
                Toast.makeText(MainActivity.this,"请先选择一张照片",Toast.LENGTH_SHORT).show();
            }
        }
        if(v == mChangeImg){
            if(mPhotoImg!=null){
               Matrix matrix = new Matrix();
                matrix.reset();
                matrix.setRotate(90);
                mPhotoImg = Bitmap.createBitmap(mPhotoImg,0,0,mPhotoImg.getWidth(),mPhotoImg.getHeight(),matrix,true);
                mPhoto.setImageBitmap(mPhotoImg);
            }else{
                Toast.makeText(MainActivity.this,"请先选择一张照片",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_CODE) if (data != null) {
            Uri uri = data.getData();
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();

            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            mCurrentPhotoStr = cursor.getString(idx);
            cursor.close();


            resizePhoto();

//            mPhotoImg = BitmapResizeUtils.compressImage(BitmapFactory.decodeFile(mCurrentPhotoStr),500);
            mPhoto.setImageBitmap(mPhotoImg);
            mTip.setText("点击发现 →");

        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mCurrentPhotoStr,options);

        double ratio = Math.max(options.outWidth*1.d/ 1024f,options.outHeight* 1.0d /1024f);

        options.inSampleSize = (int) Math.ceil(ratio);

        options.inJustDecodeBounds = false;

        mPhotoImg = BitmapFactory.decodeFile(mCurrentPhotoStr,options);
    }
}
