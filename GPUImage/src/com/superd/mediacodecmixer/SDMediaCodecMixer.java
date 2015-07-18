package com.superd.mediacodecmixer;

import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.SurfaceHolder;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.superd.gpuimage.GPUImageOutput;
import com.superd.gpuimage.GPUImagePicture;
import com.superd.gpuimage.GPUImageRawDataOutput;
import com.superd.gpuimage.GPUImageView;
import com.superd.gpuimage.android.AndroidDispatchQueue;
import com.superd.gpuimage.android.AndroidResourceManager;
import com.superd.gpuimage.android.AndroidUtils;

public class SDMediaCodecMixer implements SurfaceHolder.Callback{
	
	public static final String PICTURE_IMAGE_ASSETS_FILENAME_KEY = "PICTURE_IMAGE_ASSETS_FILENAME_KEY";
	
	public SDMediaCodecMixer init(Context context) {
		AndroidResourceManager.getAndroidResourceManager().setContext(context);
		return this;
	}
	
	public SDMediaCodecMixer initWithPreviewContainerView(LinearLayout previewContainerView, Context context) {
		mPreviewContainerView = previewContainerView;
		mDispatchQueue = AndroidDispatchQueue.dispatchQueueCreate();
		AndroidResourceManager.getAndroidResourceManager().setContext(context);
		return this;
	}
	
	public boolean setParameters(Map<String, String> params) {
		mAssetFileName = params.get(PICTURE_IMAGE_ASSETS_FILENAME_KEY);
		if (mAssetFileName.equals("")) {
			return false;
		}
		return true;
	}
	
	public void setCompletionBlock(Runnable runnable) {
		mCompletionRunnable = runnable;
	}
	
	public void startProcess() {
		GPUImageOutput.runOnMainQueueWithoutDeadlocking(new Runnable(){
			@Override
			public void run() {
				LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
				mGPUImageView = new GPUImageView(mPreviewContainerView.getContext());
				mGPUImageView.setSurfaceHolderCallBack(SDMediaCodecMixer.this);
				mPreviewContainerView.addView(mGPUImageView);
			}
		});		
	}
	
	public void canncelProcess() {
		
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mDispatchQueue.dispatchAsync(new Runnable(){
			@Override
			public void run() {
				mGPUImagePicture = new GPUImagePicture().initWithAssets(mAssetFileName);
				mGPUImagePicture.addTarget(mGPUImageView);
				
				final GPUImageRawDataOutput rawDataOutput = new GPUImageRawDataOutput().initWithImageSize(mGPUImagePicture.outputImageSize(), false);
				mGPUImagePicture.addTarget(rawDataOutput);
				rawDataOutput.setmNewFrameAvailableBlock(new Runnable(){
					@Override
					public void run() {
						rawDataOutput.lockFramebufferForReading();
						
						Bitmap bitmap = rawDataOutput.copyFrameToBitmap();
						
						AndroidUtils.saveBitmap("/mnt/sdcard/eminem.png", bitmap);
						
						rawDataOutput.unlockFramebufferAfterReading();
					}
				});
				
//				while(true) {
//					mGPUImageView.drawFrame();
					
					mGPUImagePicture.processImage();
					
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
//				}
			}
		});
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		
	}
	
	private GPUImageView mGPUImageView = null;
	private GPUImagePicture mGPUImagePicture = null;
	private LinearLayout mPreviewContainerView = null;
	private Runnable mCompletionRunnable = null;
	private AndroidDispatchQueue mDispatchQueue = null;
	private String mAssetFileName = null;
}
