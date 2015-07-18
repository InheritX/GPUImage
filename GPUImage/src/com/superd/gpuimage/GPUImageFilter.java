package com.superd.gpuimage;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import android.annotation.SuppressLint;
import android.opengl.GLES20;
import android.util.Log;

import com.superd.gpuimage.android.AndroidUtils;
import com.superd.gpuimage.android.AndroidConstants;
import com.superd.gpuimage.android.AndroidPoint;
import com.superd.gpuimage.android.AndroidResourceManager;
import com.superd.gpuimage.android.AndroidSize;

public class GPUImageFilter extends GPUImageOutput implements GPUImageInput {
	public static final String TAG = GPUImageFilter.class.getSimpleName();
	
    public static final String kGPUImageVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    public static final String kGPUImagePassthroughFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";
    
    @SuppressLint("UseSparseArrays")
    public GPUImageFilter initWithVertexVShaderFromStringFShaderFromString(String vertexShaderString, String fragmentShaderString) {
    	mUniformStateRestorationBlocks = new HashMap<Integer, Runnable>();
    	mPreventRendering = false;
    	mCurrentlyReceivingMonochromeInput = false;
    	mInputRotation = GPUImageRotationMode.kGPUImageNoRotation;
    	mBackgroundColorRed = 0.0f;
    	mBackgroundColorGreen = 0.0f;
    	mBackgroundColorBlue = 0.0f;
    	mBackgroundColorAlpha = 0.0f;
    	mImageCaptureSemaphore = new Semaphore(0);
    	mImageCaptureSemaphore.release();
    	
    	GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				GPUImageContext.useImageProcessingContext();
				mFilterProgram = GPUImageContext.sharedImageProcessingContexts().programForVertexShaderStringFragmentShaderString(kGPUImageVertexShaderString, kGPUImagePassthroughFragmentShaderString);
				if (!mFilterProgram.ismInitialized()) {
					initializeAttributes();
					
					if (!mFilterProgram.link())
		            {
		                Log.e(TAG, "Program link log: " + mFilterProgram.getmProgramLog());
		                Log.e(TAG, "Fragment shader compile log: " + mFilterProgram.getmFragmentShaderLog());
		                Log.e(TAG, "Vertex shader compile log: "+ mFilterProgram.getmVertexShaderLog());
		                mFilterProgram = null;
		            }
				}
				
				mFilterPositionAttribute = mFilterProgram.attributeIndex("position");
				mFilterTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate");
				mFilterInputTextureUniform = mFilterProgram.uniformIndex("inputImageTexture");
				
				GPUImageContext.setActiveShaderProgram(mFilterProgram);
				
		        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
		        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);  
			}
    	});
    	
    	return this;
    }
    
    public GPUImageFilter initWithFragmentShaderFromString(String fragmentShaderString) {
    	return initWithVertexVShaderFromStringFShaderFromString(kGPUImageVertexShaderString, fragmentShaderString);
    }
    
    public GPUImageFilter initWithFragmentShaderFromFile(String fragmentShaderFilename) {
    	String fragmentShaderString = AndroidResourceManager.getAndroidResourceManager().readStringFromAssets(fragmentShaderFilename);
    	return initWithFragmentShaderFromString(fragmentShaderString);
    }
    
    public GPUImageFilter init() {
    	return initWithFragmentShaderFromString(kGPUImagePassthroughFragmentShaderString);
    }

    public void initializeAttributes() {
		mFilterProgram.addAttribute("position");
		mFilterProgram.addAttribute("inputTextureCoordinate");
	}
    
    public void setupFilterForSize(AndroidSize filterFrameSize) {
    	
    }
   
    public AndroidSize rotatedSize(AndroidSize sizeToRotate, int textureIndex) {
    	return null;
    }
   
    public AndroidPoint rotatedPoint(AndroidPoint pointToRotate, GPUImageRotationMode rotation) {
    	return null;
    }

    public AndroidSize sizeOfFBO() {
		AndroidSize outputSize = maximumOutputSize();
		if (outputSize == null || outputSize.width < mInputTextureSize.width) {
			return mInputTextureSize;
		}

		return outputSize;
	}
    
    public static FloatBuffer textureCoordinatesForRotation(GPUImageRotationMode rotationMode) {
        switch(rotationMode)
        {
            case kGPUImageNoRotation: return AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.noRotationTextureCoordinates);
            case kGPUImageRotateLeft: return AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.rotateLeftTextureCoordinates);
            case kGPUImageRotateRight: return AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.rotateRightTextureCoordinates);
            case kGPUImageFlipVertical: return AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.verticalFlipTextureCoordinates);
            case kGPUImageFlipHorizonal: return AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.horizontalFlipTextureCoordinates);
            case kGPUImageRotateRightFlipVertical: return AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.rotateRightVerticalFlipTextureCoordinates);
            case kGPUImageRotateRightFlipHorizontal: return AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.rotateRightHorizontalFlipTextureCoordinates);
            case kGPUImageRotate180: return AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.rotate180TextureCoordinates);
        }
        return AndroidUtils.directFloatBufferFromFloatArray(AndroidConstants.noRotationTextureCoordinates);
    }
    
    private GPUImageFramebuffer mFirstInputFramebuffer = null;
    private GLProgram mFilterProgram = null;
    private int mFilterPositionAttribute = -1;
    private int mFilterTextureCoordinateAttribute = -1;
    private int mFilterInputTextureUniform = -1;
    private float mBackgroundColorRed = 0.0f;
    private float mBackgroundColorGreen = 0.0f;
    private float mBackgroundColorBlue = 0.0f;
    private float mBackgroundColorAlpha = 0.0f;
    private boolean mIsEndProcessing = false;
    private AndroidSize mCurrentFilterSize = null;
    private GPUImageRotationMode mInputRotation = null;
    private boolean mCurrentlyReceivingMonochromeInput = false;
    private Map<Integer, Runnable> mUniformStateRestorationBlocks = null;
    private Semaphore mImageCaptureSemaphore = null;
    private boolean mPreventRendering = false;
	
	public void forceProcessingAtSize(AndroidSize frameSize) {
		if (frameSize == null) {
			mOverrideInputSize = false;
		}else {
			mOverrideInputSize = true;
			mInputTextureSize = frameSize;
			mForcedMaximumSize = null;
		}
	}
	
	public void forceProcessingAtSizeRespectingAspectRatio(AndroidSize frameSize) {
		if (frameSize == null) {
			mOverrideInputSize = false;
			mInputTextureSize = null;
			mForcedMaximumSize = null;
		}else {
			mOverrideInputSize = true;
			mForcedMaximumSize = frameSize;
		}
	}
	
	@Override
	public void newFrameReadyAtTime(long frameTime, int textureIndex) {
		
	}

	@Override
	public void setInputFramebuffer(GPUImageFramebuffer newInputFramebuffer,
			int textureIndex) {
		
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
		return null;
	}

	@Override
	public void endProcessing() {
		if (!mIsEndProcessing) {
			mIsEndProcessing = true;
			for (GPUImageInput currentTarget : mTargets) {
				currentTarget.endProcessing();
			}
		}
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

}
