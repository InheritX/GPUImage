package com.superd.gpuimage.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.graphics.Bitmap;

public class AndroidUtils {
	public static FloatBuffer directFloatBufferFromFloatArray(float []data) {
		FloatBuffer buffer = null;
		
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 4);  
        
        byteBuffer.order(ByteOrder.nativeOrder());  
         
        buffer = byteBuffer.asFloatBuffer();  
        buffer.put(data);  
        buffer.position(0);

        return buffer;  
	}
	
	public static void saveBitmap(String picPath, Bitmap bitmap) {
		File f = new File(picPath);
		if (f.exists()) {
			f.delete();
		}
		
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(f);
			bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
			out.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
