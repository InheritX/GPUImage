package com.superd.gpuimage;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import com.superd.gpuimage.GPUImageFramebuffer.GPUTextureOptions;
import com.superd.gpuimage.android.AndroidDispatchQueue;
import com.superd.gpuimage.android.AndroidImageOrientation;
import com.superd.gpuimage.android.AndroidSize;

public class GPUImageOutput {
	
	public GPUImageOutput() {
		mTargets = new ArrayList<GPUImageInput>();
		mTargetTextureIndices = new ArrayList<Integer>();
	}
	
	public void setInputFramebufferForTarget(GPUImageInput target, int inputTextureIndex) {
		
	}
	
	public GPUImageFramebuffer framebufferForOutput() {
		return mOutputFramebuffer;
	}
	
	public void removeOutputFramebuffer() {
		mOutputFramebuffer = null;
	}
	
	public void notifyTargetsAboutNewOutputTexture() {
	    for (int i = 0; i < mTargets.size(); ++i)
	    {
	    	GPUImageInput currentTarget = mTargets.get(i);
	    	
	        int textureIndex = mTargetTextureIndices.get(i).intValue();
	        
	        setInputFramebufferForTarget(currentTarget, textureIndex);
	    }
	}

	public List<GPUImageInput> getTargets() {
		return mTargets;
	}

	public void addTarget(GPUImageInput newTarget) {
		int nextAvailableTextureIndex = newTarget.nextAvailableTextureIndex();
		addTarget(newTarget, nextAvailableTextureIndex);
		
		if (newTarget.shouldIgnoreUpdatesToThisTarget()) {
			mTargetToIgnoreForUpdates = newTarget;
		}
	}

	public void addTarget(final GPUImageInput newTarget, final int textureLocation) {
		for (GPUImageInput target : mTargets) {
			if (target == newTarget) {
				return ;
			}
		}
		
		mCachedMaximumOutputSize = null;
		runSynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				setInputFramebufferForTarget(newTarget, textureLocation);
				mTargets.add(newTarget);
				mTargetTextureIndices.add(Integer.valueOf(textureLocation));
				mAllTargetsWantMonochromeData = mAllTargetsWantMonochromeData && newTarget.wantsMonochromeInput();
			}
		});
	}
	
	public void removeTarget(final GPUImageInput targetToRemove) {
		int index = 0;
		for (; index < mTargets.size(); ++index) {
			if (targetToRemove == mTargets.get(index)) {
				break;
			}
		}
		
		if (index == mTargets.size()) {
			return ;
		}
		
		if (mTargetToIgnoreForUpdates == targetToRemove) {
			mTargetToIgnoreForUpdates = null;
		}
		
		mCachedMaximumOutputSize = null;
		
		final int finalIndex = index;
		final int textureIndexOfTarget = mTargetTextureIndices.get(index).intValue();
		runSynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				targetToRemove.setInputSize(null, textureIndexOfTarget);
				targetToRemove.setInputRotation(GPUImageRotationMode.kGPUImageNoRotation, textureIndexOfTarget);
			
				mTargetTextureIndices.remove(finalIndex);
				mTargets.remove(targetToRemove);
				targetToRemove.endProcessing();
			}
		});
	}

	public void removeAllTargets() {
		mCachedMaximumOutputSize = null;
		runSynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				for(int i = 0; i < mTargets.size(); ++i) {
					GPUImageInput targetToRemove = mTargets.get(i);
					
					int textureIndexOfTarget = mTargetTextureIndices.get(i).intValue();
					
					targetToRemove.setInputSize(null, textureIndexOfTarget);
					targetToRemove.setInputRotation(GPUImageRotationMode.kGPUImageNoRotation, textureIndexOfTarget);
				}
				
				mTargets.clear();
				mTargetTextureIndices.clear();
				
				mAllTargetsWantMonochromeData = true;
			}
		});
	}

	public void forceProcessingAtSize(AndroidSize frameSize) {
		
	}
	
	public void forceProcessingAtSizeRespectingAspectRatio(AndroidSize frameSize) {
		
	}
	
	public void useNextFrameForImageCapture() {
		
	}
	
	public Bitmap newCGImageFromCurrentlyProcessedOutput() {
		return null;
	}
	
	public Bitmap newCGImageByFilteringCGImage(Bitmap imageToFilter) {
		return null;
	}
	
	public boolean providesMonochromeOutput() {
		return false;
	}
	
	public Bitmap imageFromCurrentFramebuffer() {
		return null;
	}
	
	public Bitmap imageFromCurrentFramebufferWithOrientation(AndroidImageOrientation imageOrientation) {
		return null;
	}

	public Bitmap imageByFilteringImage(Bitmap imageToFilter) {
		return null;
	}
	
	protected GPUImageFramebuffer mOutputFramebuffer = null;
	protected List<GPUImageInput> mTargets = null;
	protected List<Integer> mTargetTextureIndices = null;
	protected AndroidSize mInputTextureSize = null;
	private AndroidSize mCachedMaximumOutputSize = null;
	protected AndroidSize mForcedMaximumSize = null;
	protected boolean mOverrideInputSize = false;
	private boolean mAllTargetsWantMonochromeData = false;
	private boolean mUsingNextFrameForImageCapture = false;
	
	private boolean mShouldSmoothlyScaleOutput = false;
	private boolean mShouldIgnoreUpdatesToThisTarget = false;
	private GPUImageInput mTargetToIgnoreForUpdates = null;
	
	private boolean mEnabled = false;
	private GPUTextureOptions mOutputTextureOptions = null;
	
	public static void runOnMainQueueWithoutDeadlocking(Runnable runnable) {
		if (AndroidDispatchQueue.isMainThread()) {
			runnable.run();
		}else {
			AndroidDispatchQueue.getMainDispatchQueue().dispatchSync(runnable);
		}
	}
	
	public static void runSynchronouslyOnVideoProcessingQueue(Runnable runnable) {
		AndroidDispatchQueue dispatchQueue = GPUImageContext.sharedImageProcessingContexts().getContextQueue();
		if (AndroidDispatchQueue.isSameDispatchQueue(dispatchQueue)) {
			runnable.run();
		}else {
			dispatchQueue.dispatchSync(runnable);
		}
	}

	public static void runAsynchronouslyOnVideoProcessingQueue(Runnable runnable) {
		AndroidDispatchQueue dispatchQueue = GPUImageContext.sharedImageProcessingContexts().getContextQueue();
		if (AndroidDispatchQueue.isSameDispatchQueue(dispatchQueue)) {
			runnable.run();
		}else {
			dispatchQueue.dispatchAsync(runnable);
		}
	}

	public static void runSynchronouslyOnContextQueue(GPUImageContext context, Runnable runnable) {
		AndroidDispatchQueue dispatchQueue = context.getContextQueue();
		if (AndroidDispatchQueue.isSameDispatchQueue(dispatchQueue)) {
			runnable.run();
		}else {
			dispatchQueue.dispatchSync(runnable);
		}
	}
	
	public static void runAsynchronouslyOnContextQueue(GPUImageContext context, Runnable runnable) {
		AndroidDispatchQueue dispatchQueue = context.getContextQueue();
		if (AndroidDispatchQueue.isSameDispatchQueue(dispatchQueue)) {
			runnable.run();
		}else {
			dispatchQueue.dispatchAsync(runnable);
		}
	}
	
	public static void reportAvailableMemoryForGPUImage(String tag) {
		
	}
}
