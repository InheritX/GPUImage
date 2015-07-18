package com.example.mixtwovideo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

public class MyCube {
	private float mAngleX = 0.0f;
	private float mAngleY = 0.0f;
	private IntBuffer mVertexBuffer; // 顶点
	private IntBuffer mColorBuffer; // 颜色
	private ByteBuffer mIndexBuffer; // 索引

	// private IntBuffer mTextureBuffer;//贴图坐标

	public MyCube() {
		int one = 0x10000;
		/*
		 * 后 3-2 0-1 前 7-6 4-5
		 */
		// 顶点坐标数据
		int vertices[] = { -one, -one, -one, one, -one, -one, one, one, -one,
				-one, one, -one, -one, -one, one, one, -one, one, one, one,
				one, -one, one, one, };

		// 顶点颜色　
		int colors[] = { 0, 0, 0, one, one, 0, 0, one, one, one, 0, one, 0,
				one, 0, one, 0, 0, one, one, one, 0, one, one, one, one, one,
				one, 0, one, one, one, };

		/*
		 * //顶点贴图坐标 int texCoords[] ={ 0,0, 0,0, 0,0, 0,0, 0,0, one,0, one,one,
		 * 0,one };
		 */

		// 面的索引
		byte indices[] = { 0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2, 2, 6, 7, 2, 7,
				3, 3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3, 0, 1, 3, 1, 2 };

		// 建立顶点缓存mVertexBuffer
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4); // * 4
																			// becuase
																			// of
																			// int
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asIntBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);

		// 建立颜色缓存mColorBuffer
		ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4); // * 4
																		// becuase
																		// of
																		// int
		cbb.order(ByteOrder.nativeOrder());
		mColorBuffer = cbb.asIntBuffer();
		mColorBuffer.put(colors);
		mColorBuffer.position(0);

		// 建立索引缓存mIndexBuffer　
		mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
		mIndexBuffer.put(indices);
		mIndexBuffer.position(0);

		/*
		 * //建立贴图坐标缓存mTextureBuffer ByteBuffer tbb =
		 * ByteBuffer.allocateDirect(texCoords.length * 4);
		 * tbb.order(ByteOrder.nativeOrder()); mTextureBuffer =
		 * tbb.asIntBuffer(); mTextureBuffer.put(texCoords);
		 * mTextureBuffer.position(0);
		 */
	}

	public void draw(GL10 gl) {
		gl.glEnable(GL10.GL_DITHER);

		gl.glPushMatrix();

		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(0, 0, -3.0f);
		gl.glScalef(0.5f, 0.5f, 0.5f);
		gl.glRotatef(mAngleX, 1, 0, 0);
		gl.glRotatef(mAngleY, 0, 1, 0);

		gl.glColor4f(0.7f, 0.7f, 0.7f, 1.0f);
		gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
		gl.glEnableClientState(gl.GL_COLOR_ARRAY);
		gl.glEnable(gl.GL_CULL_FACE);

		gl.glFrontFace(gl.GL_CW);
		gl.glVertexPointer(3, gl.GL_FIXED, 0, mVertexBuffer);
		gl.glColorPointer(4, gl.GL_FIXED, 0, mColorBuffer);

		// gl.glNormalPointer(gl.GL_FIXED,mNormalBuffer);
		// gl.glTexCoordPointer(2, gl.GL_FIXED, 0, mTextureBuffer);
		// gl.glBindTexture(GL10.GL_TEXTURE_2D, blueButtonTexture);

		gl.glDrawElements(gl.GL_TRIANGLES, 36, gl.GL_UNSIGNED_BYTE,
				mIndexBuffer);

		gl.glPopMatrix();

		mAngleX += 2;
		mAngleY += 2;
	}

}
