package com.example.mixtwovideo;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.util.Log;

public class GLThread extends Thread {
	private final GLView view;
	private boolean done = false;

	GLThread(GLView view) {
		this.view = view;
	}

	EGLContext glc;
	EGL10 egl;
	EGLDisplay display;
	GL10 gl;
	EGLSurface surface;

	// Initialize OpenGL...
	private void init_gl() {

		egl = (EGL10) EGLContext.getEGL();
		display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

		int[] version = new int[2];
		egl.eglInitialize(display, version);

		int[] configSpec = { EGL10.EGL_RED_SIZE, 5, EGL10.EGL_GREEN_SIZE, 6,
				EGL10.EGL_BLUE_SIZE, 5, EGL10.EGL_DEPTH_SIZE, 16,
				EGL10.EGL_NONE };

		EGLConfig[] configs = new EGLConfig[1];
		int[] numConfig = new int[1];
		egl.eglChooseConfig(display, configSpec, configs, 1, numConfig);
		EGLConfig config = configs[0];

		glc = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, null);

		surface = egl.eglCreateWindowSurface(display, config, view.getHolder(),
				null);

		egl.eglMakeCurrent(display, surface, surface, glc);

		gl = (GL10) (glc.getGL());
	}

	private void exit_gl() {
		// Free OpenGL resources
		egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
				EGL10.EGL_NO_CONTEXT);

		egl.eglDestroySurface(display, surface);
		egl.eglDestroyContext(display, glc);
		egl.eglTerminate(display);
	}

	private void endframe() {
		egl.eglSwapBuffers(display, surface);
		/*
		 * // Error handling if (egl.eglGetError() == EGL11.EGL_CONTEXT_LOST) {
		 * Context c = view.getContext(); if (c instanceof Activity) {
		 * ((Activity) c).finish(); } }
		 */
	}

	@Override
	public void run() {
		init_gl(); // 初试化,取得gl句柄
		final MyDraw mydraw = new MyDraw(gl);
		mydraw.change(gl, view.getWidth(), view.getHeight());
		
		Log.d("GLThread", "GLThread init");
		
		egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
				EGL10.EGL_NO_CONTEXT);
		
		Thread thread = new Thread(){
			@Override
			public void run() {
				// Loop until asked to quit
				Log.d("DrawThread", "DrawThread init");
				
				while (!done) {
					if(egl.eglMakeCurrent(display, surface, surface, glc) == false) {
						Log.e("DrawThread", "Can't make current context");
					}
					
					gl = (GL10) (glc.getGL());
					
					mydraw.drawframe(gl);
					endframe(); // 绘制完毕后,将后台页面推到屏幕上
				}
			}
		};
		thread.start();
		
		Log.d("GLThread", "GLThread start");
		
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		exit_gl(); // 关闭opengl
	}

	public void requestExitAndWait() {
		// Tell the thread to quit
		done = true;
		try {
			join();
		} catch (InterruptedException ex) {
			// Ignore
		}
	}

}
