package com.danielkim.soundrecorder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.astuetz.PagerSlidingTabStrip;
import com.danielkim.soundrecorder.R;
import com.danielkim.soundrecorder.fragments.FolderViewerFragment;
import com.danielkim.soundrecorder.fragments.HelpPageFragment;
import com.danielkim.soundrecorder.fragments.RecordFragment;
import com.danielkim.soundrecorder.fragments.VideoRecordFragment;


public class MainActivity extends ActionBarActivity{

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private PagerSlidingTabStrip tabs;
    private ViewPager pager;

    private MyAdapter mAdapter;

    private boolean recordAudio = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        pager = (ViewPager) findViewById(R.id.pager);

        mAdapter = new MyAdapter(getSupportFragmentManager());
        pager.setAdapter(mAdapter);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(pager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        pager.setCurrentItem(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem toggleItem = menu.findItem(R.id.toggleMode);

        toggleMode(toggleItem);
        return true;
    }

    private void toggleMode(MenuItem item) {

        if (recordAudio) {
            item.setIcon(R.drawable.ic_mic_white_36dp);
            item.setTitle("Audio");
        } else {
            item.setIcon(R.drawable.ic_videocam_white_36dp);
            item.setTitle("Video");
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;

            case R.id.toggleMode:
                recordAudio = !recordAudio;
                toggleMode(item);

                pager.setAdapter(null);

                pager.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
                pager.setCurrentItem(0);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class MyAdapter extends FragmentStatePagerAdapter {
        private String[] titles = { getString(R.string.tab_title_record), getString(R.string.tab_title_folders), getString(R.string.tab_title_help)};

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position){
                case 0:{
                    if (recordAudio) {
                        return RecordFragment.newInstance(position);
                    }
                    return VideoRecordFragment.newInstance(position);
                }
                case 1:{
                    return FolderViewerFragment.newInstance(position);
                }
                case 2:{
                    return HelpPageFragment.newInstance(position);
                }
            }
            return null;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
            //return super.getItemPosition(object);
        }


        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
    }

    public MainActivity() {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
