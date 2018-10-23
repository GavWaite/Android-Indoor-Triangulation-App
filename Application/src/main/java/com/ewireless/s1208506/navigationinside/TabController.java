package com.ewireless.s1208506.navigationinside;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Author: Gavin Waite
 * FragmentStatePagerAdapter class which controls assigning the various Fragments to each Tab
 * of the overall application
 */
public class TabController extends FragmentStatePagerAdapter {

    int numberOfTabs;
    Fragment[] tabs = new Fragment[3];

    public TabController(FragmentManager fm, int numberOfTabs){
        super(fm);
        this.numberOfTabs = numberOfTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch(position){
            case 0:
                if (tabs[0] == null) {
                    TrainingFragment trainF = new TrainingFragment();
                    tabs[0] = trainF;
                    return trainF;
                }
                else {
                    return tabs[0];
                }
            case 1:
                if (tabs[1] == null) {
                    PositioningFragment positF = new PositioningFragment();
                    tabs[1] = positF;
                    return positF;
                }
                else {
                    return tabs[1];
                }
            case 2:
                if (tabs[2] == null) {
                    DatabaseFragment databF = new DatabaseFragment();
                    tabs[2] = databF;
                    return databF;
                }
                else {
                    return tabs[2];
                }
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return numberOfTabs;
    }
}
