package com.superd.gpuimage;

import com.superd.gpuimage.android.AndroidSize;

public interface GPUImageInput {
	
	public void newFrameReadyAtTime(long frameTime, int textureIndex);
	
	public void setInputFramebuffer(GPUImageFramebuffer newInputFramebuffer, int textureIndex);

	public int nextAvailableTextureIndex();
	
	public void setInputSize(AndroidSize newSize, int index);
	
	public void setInputRotation(GPUImageRotationMode newInputRotation, int textureIndex);
	
	public AndroidSize maximumOutputSize();
	
	public void endProcessing();
	
	public boolean shouldIgnoreUpdatesToThisTarget();
	
	public boolean enabled();
	
	public boolean wantsMonochromeInput();
	
	public void setCurrentlyReceivingMonochromeInput(boolean newValue);
}
