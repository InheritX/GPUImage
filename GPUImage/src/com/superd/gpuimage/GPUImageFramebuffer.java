package com.superd.gpuimage;

import java.nio.IntBuffer;

import android.opengl.GLES20;

import com.superd.gpuimage.android.AndroidConstants;
import com.superd.gpuimage.android.AndroidSize;

public class GPUImageFramebuffer {
	
	public GPUImageFramebuffer() {
		
	}
	
	protected void finalize() {
		destroyFramebuffer();
	}
	
	public GPUImageFramebuffer initWithSize(AndroidSize framebufferSize) {
		GPUTextureOptions defaultTextureOptions = new GPUTextureOptions();
	    defaultTextureOptions.minFilter = GLES20.GL_LINEAR;
	    defaultTextureOptions.magFilter = GLES20.GL_LINEAR;
	    defaultTextureOptions.wrapS = GLES20.GL_CLAMP_TO_EDGE;
	    defaultTextureOptions.wrapT = GLES20.GL_CLAMP_TO_EDGE;
	    defaultTextureOptions.internalFormat = GLES20.GL_RGBA;
	    defaultTextureOptions.format = AndroidConstants.GL_BGRA;	//This is important
	    defaultTextureOptions.type = GLES20.GL_UNSIGNED_BYTE;
	    return initWithSize(framebufferSize, defaultTextureOptions, false);
	}
	
	public GPUImageFramebuffer initWithSize(AndroidSize framebufferSize, GPUTextureOptions fboTextureOptions, boolean onlyGenerateTexture) {
	    mTextureOptions = fboTextureOptions;
	    mSize = framebufferSize;
	    mFramebufferReferenceCount = 0;
	    mReferenceCountingDisabled = false;
	    mMissingFramebuffer = onlyGenerateTexture;
	    
	    if (mMissingFramebuffer) {
	    	GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable(){
				@Override
				public void run() {
					GPUImageContext.useImageProcessingContext();
					generateTexture();
					mFramebuffer = 0;
				}
	    	});
	    }else {
	    	generateFramebuffer();
	    }
		return this;
	}
	
	public GPUImageFramebuffer initWithSize(AndroidSize framebufferSize, int inputTexture) {
		GPUTextureOptions defaultTextureOptions = new GPUTextureOptions();
	    defaultTextureOptions.minFilter = GLES20.GL_LINEAR;
	    defaultTextureOptions.magFilter = GLES20.GL_LINEAR;
	    defaultTextureOptions.wrapS = GLES20.GL_CLAMP_TO_EDGE;
	    defaultTextureOptions.wrapT = GLES20.GL_CLAMP_TO_EDGE;
	    defaultTextureOptions.internalFormat = GLES20.GL_RGBA;
	    defaultTextureOptions.format = AndroidConstants.GL_BGRA;	//This is important
	    defaultTextureOptions.type = GLES20.GL_UNSIGNED_BYTE;
	    
	    mTextureOptions = defaultTextureOptions;
	    mSize = framebufferSize;
	    mFramebufferReferenceCount = 0;
	    mReferenceCountingDisabled = true;
	    mTexture = inputTexture;
	    
		return this;
	}
	
	public void activateFramebuffer() {
	    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
	    GLES20.glViewport(0, 0, (int)mSize.width, (int)mSize.height);
	}
	
	private void generateFramebuffer() {
		GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				GPUImageContext.useImageProcessingContext();
				
				IntBuffer outputFramebuffer = IntBuffer.allocate(1);
				GLES20.glGenFramebuffers(1, outputFramebuffer);
				mFramebuffer = outputFramebuffer.get(0);
				
		        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
		        
		        generateTexture();
		        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
		        
		        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, mTextureOptions.internalFormat, (int)mSize.width, (int)mSize.height, 0, mTextureOptions.format, mTextureOptions.type, null);
		        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTexture, 0);
		        
//	            GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
	            
	            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			}
		});
	}
	
	private void destroyFramebuffer() {
		GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				GPUImageContext.useImageProcessingContext();
				
				if (mFramebuffer != 0) {
					GLES20.glDeleteFramebuffers(1, IntBuffer.wrap(new int[]{mFramebuffer}));
					mFramebuffer = 0;
				}
				
				GLES20.glDeleteTextures(1, IntBuffer.wrap(new int[]{mTexture}));
			}
		});
	}
	
	private void generateTexture() {
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		
		IntBuffer outputTexture = IntBuffer.allocate(1);
		GLES20.glGenTextures(1, outputTexture);
		mTexture = outputTexture.get(0);
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, mTextureOptions.minFilter);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, mTextureOptions.magFilter);
	    // This is necessary for non-power-of-two textures
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, mTextureOptions.wrapS);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, mTextureOptions.wrapT);
	}
	
	public GPUTextureOptions getmTextureOptions() {
		return mTextureOptions;
	}
	
	public AndroidSize getmSize() {
		return mSize;
	}
	public int getmTexture() {
		return mTexture;
	}
	public boolean ismMissingFramebuffer() {
		return mMissingFramebuffer;
	}
	
	public void lock() {
		if (mReferenceCountingDisabled) {
			return ;
		}
		
	    mFramebufferReferenceCount ++;
	}
	
	public void unlock() {
	    if (mReferenceCountingDisabled) {
	        return;
	    }

	    mFramebufferReferenceCount--;
	    if (mFramebufferReferenceCount < 1) {
	    	GPUImageContext.sharedFramebufferCache().returnFramebufferToCache(this);
	    }
	}
	
	public void lockForReading() {
		
	}
	
	public void unlockAfterReading() {
		
	}
	
	public void clearAllLocks() {
		mFramebufferReferenceCount = 0;
	}
	
	public void disableReferenceCounting() {
		mReferenceCountingDisabled = true;
	}
	
	public void enableReferenceCounting() {
		mReferenceCountingDisabled = false;
	}
	
	public static class GPUTextureOptions {
	    public int minFilter;
	    public int magFilter;
	    public int wrapS;
	    public int wrapT;
	    public int internalFormat;
	    public int format;
	    public int type;
	};
	
	private GPUTextureOptions mTextureOptions = null;
	private AndroidSize mSize;
	private int mTexture = -1;
	private int mFramebuffer = -1;
	private boolean mMissingFramebuffer;
	private boolean mReferenceCountingDisabled;
	private int mFramebufferReferenceCount;
}
