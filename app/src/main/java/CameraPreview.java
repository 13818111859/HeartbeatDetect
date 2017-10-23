/**
 * Created by zyyin on 10/18/17.
 */
package peitumedia.com.heartbeatdetect;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;

import static android.content.ContentValues.TAG;
import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private PreviewCallback mPreviewCallback = new PreviewCallback();

    float factor = 0.96f;
    float thr = 0.05f;
    int sign_last = 1;
    int sign = 1;
    float sign_count = 0f;
    int frame_count = 0;
    int point_last = 0;
    float sum_avg = 0.0f;

    final int  CURVE_LENGTH = 12;
    float[] curve = new float[CURVE_LENGTH];

    private  boolean mFlash = false;
    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void startPreview() {
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(mPreviewCallback);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(1280, 720);
            parameters.setFlashMode(FLASH_MODE_TORCH);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    public void onAutoFocus(boolean success, Camera camera) {
                        if(!mFlash)
                            return;
                        Camera.Parameters parameters = mCamera.getParameters();
                        parameters.setFlashMode(FLASH_MODE_TORCH);
                        mCamera.setParameters(parameters);
                    }
                });
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }
    public void stopPreview() {
        try {
            mCamera.autoFocus(null);
            mCamera.setPreviewCallback(null);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(FLASH_MODE_OFF);

            mCamera.setParameters(parameters);
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }
    }
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.

            startPreview();

    }
    //Implement the previewCallback
    private final class PreviewCallback
            implements android.hardware.Camera.PreviewCallback {
        public void onPreviewFrame(byte [] data, Camera camera) {

            Camera.Size size = camera.getParameters().getPreviewSize();
            int format = camera.getParameters().getPreviewFormat();
            int bitsPerPixel = ImageFormat.getBitsPerPixel(format);
            float fps = camera.getParameters().getPreviewFrameRate();
            float sum = 0.0f;
            float heartbeat = 0f;
            float diff = 0f;

            for(int i = 0; i < size.height; i++) {
                for(int j = 0; j < size.width; j++) {
                    sum += data[i*size.width + j];
                }
            }
            sum /= size.height*size.width;
            frame_count++;
            if (frame_count < CURVE_LENGTH) curve[frame_count] = sum;
            else
            {
                for (int k = 1;k < CURVE_LENGTH;k++)
                {
                    curve[k - 1] = curve[k];
                }
                curve[CURVE_LENGTH - 1] = sum;
                for (int k = 0;k < CURVE_LENGTH/2;k++)
                {
                    diff += (curve[k] - curve[ CURVE_LENGTH -1 - k]);
                }
                diff /= (CURVE_LENGTH / 2);

            }

            if (diff > thr) sign = 1;
            if (diff < -thr) sign = -1;

            if (sign != sign_last)
            {
                sign_last = sign;
                sign_count++;
            }

            heartbeat = sign_count * fps *60.0f / frame_count/2.0f;
            float fCount = (float)frame_count;
            String str = String.format("heartbeat:%.2f  Time:%.2f , fps: %.2f",  heartbeat, fCount/fps, fps);
            ((HeartBeatActivity)getContext()).updateButton(str);


        }
    }


    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.

        stopPreview();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings

            startPreview();
    }

    public void setFlash() {

        Camera.Parameters parameters = mCamera.getParameters();

        if (mFlash == false) {
            parameters.setFlashMode(FLASH_MODE_TORCH);
        } else  {

            parameters.setFlashMode(FLASH_MODE_OFF);

        }
        mCamera.setParameters(parameters);
        mFlash = !mFlash;

    }
}
