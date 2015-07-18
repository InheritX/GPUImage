package com.example.mixtwovideo;

import javax.microedition.khronos.opengles.GL10;

public class MyDraw {
	MyCube mycube;

	public MyDraw(GL10 gl) {
		init(gl);
		mycube = new MyCube(); // 建立一个盒子
	}

	public void init(GL10 gl) {
		// 初始化OpenGL ES

		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

		gl.glClearColor(0.0f, 0.0f, 1.0f, 1);
		gl.glShadeModel(GL10.GL_SMOOTH);
		// gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glDisable(GL10.GL_DITHER);
		gl.glDisable(GL10.GL_LIGHTING);
		// gl.glEnable(GL10.GL_LIGHT0);

	}

	// OpenGL ES窗口变化处理,设定透视模式
	public void change(GL10 gl, int w, int h) {
		gl.glViewport(0, 0, w, h);

		float ratio = (float) w / h;
		gl.glMatrixMode(gl.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glFrustumf(-ratio, ratio, -1, 1, 2, 12);

		gl.glDisable(gl.GL_DITHER);

		gl.glClearColor(1, 1, 1, 1);
		gl.glEnable(gl.GL_SCISSOR_TEST);
		gl.glScissor(0, 0, w, h);
		gl.glClear(gl.GL_COLOR_BUFFER_BIT);

		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(0, 0, -3.0f);
		gl.glScalef(0.5f, 0.5f, 0.5f);

		gl.glColor4f(0.7f, 0.7f, 0.7f, 1.0f);

		gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
		gl.glEnableClientState(gl.GL_COLOR_ARRAY);
		// gl.glEnableClientState(gl.GL_NORMAL_ARRAY);
		// gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);

		gl.glEnable(gl.GL_CULL_FACE);
	}

	// OpenGL ES帧绘制
	public void drawframe(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glClearColor(0, 0, 0, 1.0f);

		mycube.draw(gl); // 绘制盒子
	}
}
