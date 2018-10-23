package com.ewireless.s1208506.navigationinside;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Author: Gavin Waite
 * The Main Activity for the app
 * Handles the checking of permissions required for the app
 * Also initialises the Room database using the Room API
 * Sets up the Tabbed interface design
 */
public class MainActivity extends AppCompatActivity {

    // Unique request codes for checking that permissions have been granted
    private final int MY_FINE_LOCATION_REQUEST_CODE = 101;
    private final int MY_COARSE_LOCATION_REQUEST_CODE = 102;
    private final int MY_CHANGE_WIFI_STATE_REQUEST_CODE = 103;
    private final int MY_ACCESS_WIFI_STATE_REQUEST_CODE = 104;

    // Local copy of the database
    public LocationDatabase db;

    /**
     * Called on initial launch of the app - performs initial setup
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupTabs();
        setupDatabase();

    }

    /**
     * Check permissions have not changed on each Resume of the app
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkAllPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Initialise the database for indoor positioning Reference points using the Room API
     */
    private void setupDatabase(){
        // .allowMainThreadQueries() allows for small tasks such as counting the entries in the
        // database to be performed on the main thread rather than a dedicated Asynchronous Task
        // Although normally discouraged, this will be used sparingly.
        db = Room.databaseBuilder(getApplicationContext(), LocationDatabase.class, "locations").allowMainThreadQueries().build();
    }

    /**
     * Setup the tabbed layout and link it to the Fragments which will run in each Tab
     */
    private TabController tabController;
    public void setupTabs(){
        // The Tab UI in the actionbar. Setup the text labels
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Training"));
        tabLayout.addTab(tabLayout.newTab().setText("Positioning"));
        tabLayout.addTab(tabLayout.newTab().setText("Database"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        // The Control code to support moving between the tabs and running each Fragment
        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        tabController = new TabController(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(tabController);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    /**
     * Make sure all permissions that are required are granted, otherwise ask for them
     * On SDK < 23 this will have been checked on install
     */
    private void checkAllPermissions(){
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_FINE_LOCATION_REQUEST_CODE);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_COARSE_LOCATION_REQUEST_CODE);
            }
            if (checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CHANGE_WIFI_STATE},
                        MY_CHANGE_WIFI_STATE_REQUEST_CODE);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE},
                        MY_ACCESS_WIFI_STATE_REQUEST_CODE);
            }
        }
    }

    /**
     * Auto-generated stubs for supporting the options menu feature.
     * Unused in this application but left for future extension
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
