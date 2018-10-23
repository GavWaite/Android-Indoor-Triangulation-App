package com.ewireless.s1208506.navigationinside;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * Author: Gavin Waite
 * An Android Fragment representing the debug tab of the navigation application
 * The full database is viewable in a scrollable TextView so that readings can be verified
 * and analysed. Buttons exist to allow the user to clear the database or refresh the TextView.
 * The database uses the Room API.
 *
 * This tab would only be hidden by default in a final application but is useful for debugging
 * during the development phase
 */
public class DatabaseFragment extends Fragment implements View.OnClickListener{

    // Handles to the UI elements
    private Button refreshBut;
    private Button clearBut;
    private TextView databaseOutput;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Setup the UI view for the Database Tab based upon the layout constraints file
        View rootView =  inflater.inflate(R.layout.database_fragment, container,false);

        // Acquire handles for the UI elements and perform initial set-up
        linkInterface(rootView);

        // Load from the database into the local copy
        loadDatabase();

        return rootView;
    }

    /**
     * Perform the initialisation of the buttons and scrolling TextView
     * Link the references to each element to this Fragment
     * Also sets up listeners for the two Buttons s that this Fragment can implement an onClick
     * Callback.
     * The TextView is set to be scrollable in the layout file and here the Scrolling behaviour is
     * implemented so that the user can drag the list to move it up or down.
     * @param rootView - the root View for the tab
     */
    private void linkInterface(View rootView){
        refreshBut = (Button) rootView.findViewById(R.id.refresh);
        refreshBut.setOnClickListener(this);

        clearBut = (Button) rootView.findViewById(R.id.clear);
        clearBut.setOnClickListener(this);

        databaseOutput = (TextView) rootView.findViewById(R.id.dbText);
        databaseOutput.setMovementMethod(new ScrollingMovementMethod());
    }

    /**
     * Provides the on-click functionality for the two buttons and links them to teh correct handler
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refresh:
                loadDatabase();
                break;
            case R.id.clear:
                clearDatabase();
                break;
            default:
                // Do nothing
        }
    }

    /**
     * The implementation of the Room database task: load
     * Executes an asynchronous task as required by the Room API
     *
     * Queries the database for all entries and updates the UI
     */
    private void loadDatabase(){
        new LoadDatabaseTask().execute();
    }

    private class LoadDatabaseTask extends AsyncTask<Void, Void, List<LocData>>{
        @Override
        protected List<LocData> doInBackground(Void... params){
            Log.d("DB","Starting background task");
            return ((MainActivity)getActivity()).db.locDao().getAll();
        }

        @Override
        protected void onPostExecute(List<LocData> locations){
            Log.d("DB","In post execute");

            String databaseText= "";
            for (LocData element : locations){
                Log.d("DB", "Found an element");
                databaseText += element.uid;
                databaseText += "\n" + element.latitude + " : " + element.longitude;
                databaseText += "\n" + element.BSSID_1 + " - " + element.dB_1;
                databaseText += "\n" + element.BSSID_2 + " - " + element.dB_2;
                databaseText += "\n" + element.BSSID_3 + " - " + element.dB_3;
                databaseText += "\n\n";
            }

            databaseOutput.setText(databaseText);

        }
    }

    /**
     * The implementation of the Room database task: clear
     * Executes an asynchronous task as required by the Room API
     *
     * Deletes all entries from the database
     * This might be useful for demonstration purposes or if data is corrupted or no longer valid
     * (Consider that a location may change its WiFi infrastructure at a later date)
     */
    private void clearDatabase(){
        new ClearDatabaseTask().execute();
    }

    private class ClearDatabaseTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... args){
            Log.d("DB","Nuking the db");
            ((MainActivity)getActivity()).db.locDao().deleteAll();
            return null;
        }
    }
}
