package cn.melvin.imageblur;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.zhouwei.blurlibrary.EasyBlur;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView mImageView;
    private Button mBtRenderScript;
    private Button mBtFastblur;
    private Button mBtEasyblur;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        initView();
    }

    private void initView() {

        mImageView = findViewById(R.id.imageview);
        mBtRenderScript = findViewById(R.id.bt_RenderScript);
        mBtFastblur = findViewById(R.id.bt_fastblur);
        mBtEasyblur = findViewById(R.id.bt_easyblur);
        mBtRenderScript.setOnClickListener(this);
        mBtFastblur.setOnClickListener(this);
        mBtEasyblur.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_RenderScript:
                final Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.img_test);
                Bitmap desBitmap = rsBlur(MainActivity.this, bitmap, 5,0.125F);
                mImageView.setImageBitmap(bitMapScale(desBitmap, 8));
                break;
            case R.id.bt_fastblur:
                final Bitmap orgBitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.img_test);
                Bitmap finalBitmap = fastBlur(orgBitmap, 0.125F, 5);
                mImageView.setImageBitmap(bitMapScale(finalBitmap, 8));
                break;
            case R.id.bt_easyblur:
                final Bitmap firstBitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.img_test);

                Bitmap lastBitmap = EasyBlur.with(MainActivity.this)
                        .bitmap(firstBitmap)
                        .radius(5)
                        .blur();
                Bitmap newBitmap = bitMapScale(lastBitmap, 8);
                mImageView.setImageBitmap(newBitmap);
                break;
        }
    }

    /**
     * 对图片放大缩小
     * @param bitmap
     * @param scale
     * @return
     */
    public static Bitmap bitMapScale(Bitmap bitmap,float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale,scale); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        return resizeBmp;
    }

    /**
     * 使用RenderScript处理图片高斯模糊
     * @param context
     * @param source
     * @param radius
     * @param scale
     * @return
     */
    private static Bitmap rsBlur(Context context, Bitmap source, int radius,float scale){

        int width = Math.round(source.getWidth() * scale);
        int height = Math.round(source.getHeight() * scale);

        Bitmap inputBmp = Bitmap.createScaledBitmap(source,width,height,false);
        //(1)
        RenderScript renderScript = RenderScript.create(context);
        Log.i("rsBlur","scale size:"+inputBmp.getWidth()+"*"+inputBmp.getHeight());
        // Allocate memory for Renderscript to work with
        // (2)
        final Allocation input = Allocation.createFromBitmap(renderScript,inputBmp);
        final Allocation output = Allocation.createTyped(renderScript,input.getType());
        //(3)
        // Load up an instance of the specific script that we want to use.
        ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        //(4)
        scriptIntrinsicBlur.setInput(input);
        //(5)
        // Set the blur radius
        scriptIntrinsicBlur.setRadius(radius);
        //(6)
        // Start the ScriptIntrinisicBlur
        scriptIntrinsicBlur.forEach(output);
        //(7)
        // Copy the output to the blurred bitmap
        output.copyTo(inputBmp);
        //(8)
        renderScript.destroy();
        return inputBmp;
    }

    /**
     * 用fastBlur处理图片高斯模糊
     * @param sentBitmap
     * @param scale
     * @param radius
     * @return
     */
    private static Bitmap fastBlur(Bitmap sentBitmap, float scale, int radius) {
        int width = Math.round((float)sentBitmap.getWidth() * scale);
        int height = Math.round((float)sentBitmap.getHeight() * scale);
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false);
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        if(radius < 1) {
            return null;
        } else {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int[] pix = new int[w * h];
            Log.e("pix", w + " " + h + " " + pix.length);
            bitmap.getPixels(pix, 0, w, 0, 0, w, h);
            int wm = w - 1;
            int hm = h - 1;
            int wh = w * h;
            int div = radius + radius + 1;
            int[] r = new int[wh];
            int[] g = new int[wh];
            int[] b = new int[wh];
            int[] vmin = new int[Math.max(w, h)];
            int divsum = div + 1 >> 1;
            divsum *= divsum;
            int[] dv = new int[256 * divsum];

            int i;
            for(i = 0; i < 256 * divsum; ++i) {
                dv[i] = i / divsum;
            }

            int yi = 0;
            int yw = 0;
            int[][] stack = new int[div][3];
            int r1 = radius + 1;

            int rsum;
            int gsum;
            int bsum;
            int x;
            int y;
            int p;
            int stackpointer;
            int stackstart;
            int[] sir;
            int rbs;
            int routsum;
            int goutsum;
            int boutsum;
            int rinsum;
            int ginsum;
            int binsum;
            for(y = 0; y < h; ++y) {
                bsum = 0;
                gsum = 0;
                rsum = 0;
                boutsum = 0;
                goutsum = 0;
                routsum = 0;
                binsum = 0;
                ginsum = 0;
                rinsum = 0;

                for(i = -radius; i <= radius; ++i) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))];
                    sir = stack[i + radius];
                    sir[0] = (p & 16711680) >> 16;
                    sir[1] = (p & '\uff00') >> 8;
                    sir[2] = p & 255;
                    rbs = r1 - Math.abs(i);
                    rsum += sir[0] * rbs;
                    gsum += sir[1] * rbs;
                    bsum += sir[2] * rbs;
                    if(i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                }

                stackpointer = radius;

                for(x = 0; x < w; ++x) {
                    r[yi] = dv[rsum];
                    g[yi] = dv[gsum];
                    b[yi] = dv[bsum];
                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;
                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];
                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];
                    if(y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm);
                    }

                    p = pix[yw + vmin[x]];
                    sir[0] = (p & 16711680) >> 16;
                    sir[1] = (p & '\uff00') >> 8;
                    sir[2] = p & 255;
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;
                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[stackpointer % div];
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];
                    ++yi;
                }

                yw += w;
            }

            for(x = 0; x < w; ++x) {
                bsum = 0;
                gsum = 0;
                rsum = 0;
                boutsum = 0;
                goutsum = 0;
                routsum = 0;
                binsum = 0;
                ginsum = 0;
                rinsum = 0;
                int yp = -radius * w;

                for(i = -radius; i <= radius; ++i) {
                    yi = Math.max(0, yp) + x;
                    sir = stack[i + radius];
                    sir[0] = r[yi];
                    sir[1] = g[yi];
                    sir[2] = b[yi];
                    rbs = r1 - Math.abs(i);
                    rsum += r[yi] * rbs;
                    gsum += g[yi] * rbs;
                    bsum += b[yi] * rbs;
                    if(i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }

                    if(i < hm) {
                        yp += w;
                    }
                }

                yi = x;
                stackpointer = radius;

                for(y = 0; y < h; ++y) {
                    pix[yi] = -16777216 & pix[yi] | dv[rsum] << 16 | dv[gsum] << 8 | dv[bsum];
                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;
                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];
                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];
                    if(x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w;
                    }

                    p = x + vmin[y];
                    sir[0] = r[p];
                    sir[1] = g[p];
                    sir[2] = b[p];
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;
                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[stackpointer];
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];
                    yi += w;
                }
            }

            Log.e("pix", w + " " + h + " " + pix.length);
            bitmap.setPixels(pix, 0, w, 0, 0, w, h);
            return bitmap;
        }
    }






}
