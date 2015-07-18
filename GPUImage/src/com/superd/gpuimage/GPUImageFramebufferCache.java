package com.superd.gpuimage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.opengl.GLES20;

import com.superd.gpuimage.GPUImageFramebuffer.GPUTextureOptions;
import com.superd.gpuimage.android.AndroidConstants;
import com.superd.gpuimage.android.AndroidSize;

public class GPUImageFramebufferCache {
	
	GPUImageFramebufferCache init() {
		mFramebufferCache = new HashMap<String, GPUImageFramebuffer>();
		mFramebufferTypeCounts = new HashMap<String, Integer>();
		mActiveImageCaptureList = new ArrayList<GPUImageFramebuffer>();
		return this;
	}
	
	GPUImageFramebuffer fetchFramebufferForSize(final AndroidSize framebufferSize, final GPUTextureOptions textureOptions, final boolean onlyTexture) {
		final List<GPUImageFramebuffer> outputList = new ArrayList<GPUImageFramebuffer>();
		GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				GPUImageFramebuffer framebufferFromCache = null;
				
				String lookupHash = hashKeyString(framebufferSize, textureOptions, onlyTexture);
				
				int numberOfMatchingTextures = 0;
				Integer numberOfMatchingTexturesInCache = mFramebufferTypeCounts.get(lookupHash);
				if (numberOfMatchingTexturesInCache != null)
					numberOfMatchingTextures = numberOfMatchingTexturesInCache.intValue();
				if (numberOfMatchingTextures < 1) {
					framebufferFromCache = new GPUImageFramebuffer().initWithSize(framebufferSize, textureOptions, onlyTexture);
				}else {
					int currentTextureID = numberOfMatchingTextures - 1;
					while ((framebufferFromCache == null) && (currentTextureID >= 0)) {
						
						String textureHash = lookupHash;
						textureHash += currentTextureID;
						
						framebufferFromCache = mFramebufferCache.get(textureHash);
						if (framebufferFromCache != null) {
							mFramebufferCache.remove(framebufferFromCache);
						}
						
						--currentTextureID;
					}
					
					++currentTextureID;
					
					mFramebufferTypeCounts.put(lookupHash, Integer.valueOf(currentTextureID));
					
					if (framebufferFromCache == null) {
						framebufferFromCache = new GPUImageFramebuffer().initWithSize(framebufferSize, textureOptions, onlyTexture);
					}
				}
				
				outputList.add(framebufferFromCache);
			}
		});
		
		GPUImageFramebuffer resultFrameBuffer = null;
		if (outputList.size() > 0) {
			resultFrameBuffer = outputList.get(0);
		}

		resultFrameBuffer.lock();
		
		return resultFrameBuffer;
	}
	
	GPUImageFramebuffer fetchFramebufferForSize(AndroidSize framebufferSize, boolean onlyTexture) {
		GPUTextureOptions defaultTextureOptions = new GPUTextureOptions();
	    defaultTextureOptions.minFilter = GLES20.GL_LINEAR;
	    defaultTextureOptions.magFilter = GLES20.GL_LINEAR;
	    defaultTextureOptions.wrapS = GLES20.GL_CLAMP_TO_EDGE;
	    defaultTextureOptions.wrapT = GLES20.GL_CLAMP_TO_EDGE;
	    defaultTextureOptions.internalFormat = GLES20.GL_RGBA;
	    defaultTextureOptions.format = AndroidConstants.GL_BGRA;	//This is important
	    defaultTextureOptions.type = GLES20.GL_UNSIGNED_BYTE;
		return fetchFramebufferForSize(framebufferSize, defaultTextureOptions, onlyTexture);
	}
	
	public void returnFramebufferToCache(final GPUImageFramebuffer framebuffer) {
		framebuffer.clearAllLocks();
		
		GPUImageOutput.runAsynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				AndroidSize framebufferSize = framebuffer.getmSize();
				GPUTextureOptions framebufferTextureOptions = framebuffer.getmTextureOptions();
			
				String lookupHash = hashKeyString(framebufferSize, framebufferTextureOptions, framebuffer.ismMissingFramebuffer());
			
				int numberOfMatchingTextures = mFramebufferTypeCounts.get(lookupHash).intValue();
				String textureHash = lookupHash;
				textureHash += numberOfMatchingTextures;
				
				mFramebufferCache.put(textureHash, framebuffer);
				mFramebufferTypeCounts.put(lookupHash, Integer.valueOf(numberOfMatchingTextures + 1));
			}
		});
	}
	
	public void purgeAllUnassignedFramebuffers() {
		GPUImageOutput.runAsynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				mFramebufferCache.clear();
				mFramebufferTypeCounts.clear();
			}
		});
	}
	
	public void addFramebufferToActiveImageCaptureList(final GPUImageFramebuffer framebuffer) {
		GPUImageOutput.runAsynchronouslyOnVideoProcessingQueue(new Runnable(){
			@Override
			public void run() {
				mActiveImageCaptureList.add(framebuffer);
			}
		});
	}
	
	public void removeFramebufferFromActiveImageCaptureList(final GPUImageFramebuffer framebuffer) {
		GPUImageOutput.runAsynchronouslyOnVideoProcessingQueue(new Runnable() {
			@Override
			public void run() {
				mActiveImageCaptureList.remove(framebuffer);
			}
		});
	}
	
	private String hashKeyString(AndroidSize size, GPUTextureOptions textureOptions, boolean onlyTexture) {
		String hashString = "";
	
		hashString += size.width;
		hashString += size.height;
		hashString += "-";
		hashString += textureOptions.minFilter;
		hashString += textureOptions.magFilter;
		hashString += textureOptions.wrapS;
		hashString += textureOptions.wrapT;
		hashString += textureOptions.internalFormat;
		hashString += textureOptions.format;
		hashString += textureOptions.type;
		
		if (onlyTexture) {
			hashString += "-NOFB";
		}
		
		return hashString;
	}
	
	private Map<String, GPUImageFramebuffer> mFramebufferCache = null;
	private Map<String, Integer> mFramebufferTypeCounts = null;
	private List<GPUImageFramebuffer> mActiveImageCaptureList = null;
}
