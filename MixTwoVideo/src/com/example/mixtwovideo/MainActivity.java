package com.example.mixtwovideo;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.superd.mediacodecmixer.SDMediaCodecMixer;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mLinearLayout = (LinearLayout)this.findViewById(R.id.previewLayout);
        
        mMediaCodecMixer = new SDMediaCodecMixer().initWithPreviewContainerView(mLinearLayout, getBaseContext());
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(SDMediaCodecMixer.PICTURE_IMAGE_ASSETS_FILENAME_KEY, "images/eminem.jpg");
        
        mMediaCodecMixer.setParameters(params);
        mMediaCodecMixer.setCompletionBlock(new Runnable(){
			@Override
			public void run() {
				
			}
        });
        mMediaCodecMixer.startProcess();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private LinearLayout mLinearLayout = null;
    private SDMediaCodecMixer mMediaCodecMixer = null;
}
