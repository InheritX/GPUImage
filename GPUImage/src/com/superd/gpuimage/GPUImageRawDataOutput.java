package com.superd.gpuimage;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import com.superd.gpuimage.android.AndroidUtils;
import com.superd.gpuimage.android.AndroidConstants;
import com.superd.gpuimage.android.AndroidSize;

public class GPUImageRawDataOutput implements GPUImageInput {
	public static final String TAG = GPUImageRawDataOutput.class.getSimpleName();
	
    public static final String kGPUImageColorSwizzlingFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate).bgra;\n" +
            "}";

	public GPUImageRawDataOutput initWithImageSize(AndroidSize newImageSize, boolean resultsInBGRAFormat) {
		mEnabled = true;
		mLockNextFramebuffer = false;
		mOutputBGRA = resultsInBGRAFormat;
		mImageSize = newImageSize;
		mHasReadFromTheCurrentFrame = false;
		mInputRotation = GPUImageRotationMode.kGPUImageNoRotation;
		
		GPUImageContext.useImageProcessingContext();
		if (mOutputBGRA) {
			mDataProgram = GPUImageContext.sharedImageProcessingContexts().programForVertexShaderStringFragmentShaderString(GPUImageFilter.kGPUImageVertexShaderString, kGPUImageColorSwizzlingFragmentShaderString);
		}else {
			mDataProgram = GPUImageContext.sharedImageProcessingContexts().programForVertexShaderStringFragmentShaderString(GPUImageFilter.kGPUImageVertexShaderString, GPUImageFilter.kGPUImagePassthroughFragmentShaderString);
		}
		
		if (!mDataProgram.ismInitialized()) {
			mDataProgram.addAttribute("position");
			mDataProgram.addAttribute("inputTextureCoordinate");
			
			if(!mDataProgram.link()) {
				Log.e(TAG, "Program link log: " + mDataProgram.getmProgramLog());
                Log.e(TAG, "Fragment shader compile log: " + mDataProgram.getmFragmentShaderLog());
                Log.e(TAG, "Vertex shader compile log: "+ mDataProgram.getmVertexShaderLog());
                mDataProgram = null;
			}
		}
		
		mDataPositionAttribute = mDataProgram.attributeIndex("position");
		mDataTextureCoordinateAttribute = mDataProgram.attributeIndex("inputTextureCoordinate");
		mDataInputTextureUniform = mDataProgram.uniformIndex("inputImageTexture");
		
		return this;
	}
	
	public void setmNewFrameAvailableBlock(Runnable mNewFrameAvailableBlock) {
		this.mNewFrameAvailableBlock = mNewFrameAvailableBlock;
	}
	
	public void lockFramebufferForReading() {
		mLockNextFramebuffer = true;
	}
	
	public void unlockFramebufferAfterReading() {
		mRetainedFramebuffer.unlockAfterReading();
		mRetainedFramebuffer.unlock();
		mRetainedFramebuffer = null;
	}
	
	public void setImageSize(AndroidSize newImageSize) {
		mImageSize = newImageSize;
	}
	
	public Bitmap copyFrameToBitmap() {
		
		final List<Bitmap> arrayBitmap = new ArrayList<Bitmap>();
		
		GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				GPUImageContext.useImageProcessingContext();
				
				renderAtInternalSize();
				
//				int[] array = new int[mImageSize.width * mImageSize.height];
				IntBuffer outputBuffer = IntBuffer.allocate(mImageSize.width * mImageSize.height);
				outputBuffer.position(0);
				GLES20.glReadPixels(0, 0, mImageSize.width, mImageSize.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, outputBuffer);
				
				Bitmap bitmap = Bitmap.createBitmap(mImageSize.width, mImageSize.height, Bitmap.Config.ARGB_8888);
				bitmap.copyPixelsFromBuffer(outputBuffer);
				
				arrayBitmap.add(bitmap);
			}
		});
		
		Bitmap resultBitmap = null;
		if (arrayBitmap.size() > 0) {
			resultBitmap = arrayBitmap.get(0);
		}
		
		return resultBitmap;
	}
	
	private void renderAtInternalSize() {
		GPUImageContext.useImageProcessingContext();
		
		mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(mImageSize, false);
		mOutputFramebuffer.activateFramebuffer();
		
		if (mLockNextFramebuffer) {
			mRetainedFramebuffer = mOutputFramebuffer;
			mRetainedFramebuffer.lock();
			mRetainedFramebuffer.lockForReading();
			mLockNextFramebuffer = false;
		}
		
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getmTexture());
		GLES20.glUniform1i(mDataInputTextureUniform, 4);	
	    
		GLES20.glVertexAttribPointer(mDataPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.squareVertices));
		GLES20.glVertexAttribPointer(mDataTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.textureCoordinates));
	    
		GLES20.glEnableVertexAttribArray(mDataPositionAttribute);
		GLES20.glEnableVertexAttribArray(mDataTextureCoordinateAttribute);
	    
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		mFirstInputFramebuffer.unlock();
	}
	
	@Override
	public void newFrameReadyAtTime(long frameTime, int textureIndex) {
		mHasReadFromTheCurrentFrame = true;
		
		if (mNewFrameAvailableBlock != null) {
			mNewFrameAvailableBlock.run();
		}
	}

	@Override
	public void setInputFramebuffer(GPUImageFramebuffer newInputFramebuffer,
			int textureIndex) {
		mFirstInputFramebuffer = newInputFramebuffer;
		mFirstInputFramebuffer.lock();
	}

	@Override
	public int nextAvailableTextureIndex() {
		return 0;
	}

	@Override
	public void setInputSize(AndroidSize newSize, int index) {
		
	}

	@Override
	public void setInputRotation(GPUImageRotationMode newInputRotation,
			int textureIndex) {
		mInputRotation = newInputRotation;
	}

	@Override
	public AndroidSize maximumOutputSize() {
		return mImageSize;
	}

	@Override
	public void endProcessing() {
		
	}

	@Override
	public boolean shouldIgnoreUpdatesToThisTarget() {
		return false;
	}

	@Override
	public boolean enabled() {
		return false;
	}

	@Override
	public boolean wantsMonochromeInput() {
		return false;
	}

	@Override
	public void setCurrentlyReceivingMonochromeInput(boolean newValue) {
		
	}
	
	public static class GPUByteColorVector {
		byte red;
		byte green;
		byte blue;
		byte alpha;
	}
	
	private AndroidSize mImageSize = null;
	private GPUImageRotationMode mInputRotation = null;
	private boolean mOutputBGRA = false;
	private boolean mEnabled = false;
	private boolean mLockNextFramebuffer = false;
	private boolean mHasReadFromTheCurrentFrame = false;
	private GLProgram mDataProgram = null;
	private int mDataPositionAttribute = -1;
	private int mDataTextureCoordinateAttribute = -1;
	private int mDataInputTextureUniform = -1;
	private GPUImageFramebuffer mFirstInputFramebuffer = null;
	private GPUImageFramebuffer mOutputFramebuffer = null;
	private GPUImageFramebuffer mRetainedFramebuffer = null;
	private Runnable mNewFrameAvailableBlock = null;
}
