package com.superd.gpuimage;

import java.util.concurrent.Semaphore;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.superd.gpuimage.android.AndroidResourceManager;
import com.superd.gpuimage.android.AndroidSize;

public class GPUImagePicture extends GPUImageOutput {
	
	public GPUImagePicture() {
		
	}
	
	protected void finalize() {
		mOutputFramebuffer.enableReferenceCounting();
		mOutputFramebuffer = null;
		
		mImageUpdateSemaphore.release();
		mImageUpdateSemaphore = null;
	}
	
	public GPUImagePicture initWithAssets(String fileName) {
		Bitmap bitmap = AndroidResourceManager.getAndroidResourceManager().readBitmapFromAssets(fileName);
		return initWithBitmap(bitmap);
	}
	
	public GPUImagePicture initWithBitmap(final Bitmap bitmap) {
		mHasProcessedImage = false;
		mImageUpdateSemaphore = new Semaphore(0);
		mImageUpdateSemaphore.release();
		
		int widthOfImage = bitmap.getWidth();
		int heightOfImage = bitmap.getHeight();
		
		mPixelSizeOfImage = new AndroidSize(widthOfImage, heightOfImage);
		
		GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				GPUImageContext.useImageProcessingContext();
				
				mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(mPixelSizeOfImage, true);
				mOutputFramebuffer.disableReferenceCounting();
				
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOutputFramebuffer.getmTexture());
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
				
				//restore the default texture
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			}
		});
		
		return this;
	}
	
	public void processImage() {
		processImageWithCompletionHandler(null);
	}
	
	public AndroidSize outputImageSize() {
		return mPixelSizeOfImage;
	}
	
	public boolean processImageWithCompletionHandler(final Runnable runnable) {
		mHasProcessedImage = true;

		try {
			mImageUpdateSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		GPUImageOutput.runAsynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				for (int i = 0; i < mTargets.size(); ++i) {
					GPUImageInput currentTarget = mTargets.get(i);
					int textureIndexOfTarget = mTargetTextureIndices.get(i).intValue();
					
					currentTarget.setCurrentlyReceivingMonochromeInput(false);
					currentTarget.setInputSize(mPixelSizeOfImage, textureIndexOfTarget);
					currentTarget.setInputFramebuffer(mOutputFramebuffer, textureIndexOfTarget);
					currentTarget.newFrameReadyAtTime(Integer.MAX_VALUE, textureIndexOfTarget);
				}
				
				mImageUpdateSemaphore.release();
				
				if (runnable != null) {
					runnable.run();
				}
			}
		});
		
		return true;
	}
	
	private AndroidSize mPixelSizeOfImage = null;
	private boolean mHasProcessedImage = false;
	private Semaphore mImageUpdateSemaphore = null;
}
