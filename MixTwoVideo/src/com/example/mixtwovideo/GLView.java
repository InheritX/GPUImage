package com.example.mixtwovideo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;

public class GLView extends GLSurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = GLView.class.getSimpleName();
	
	SurfaceHolder holder;

	public GLView(Context context) {
		super(context);

		holder = this.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
		
		setFocusable(true);
		
		Log.e(TAG, "GLView init");
	}

	GLThread thread;

	// @Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.e(TAG, "GLView surfaceCreated");
		
		thread = new GLThread(this);
		thread.start();
	}

	// @Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	// @Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		thread.requestExitAndWait();
		thread = null;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		// return thread.doKeyDown(keyCode, msg);
		return false;
	}

}
