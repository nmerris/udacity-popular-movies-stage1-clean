package com.nate.popmoviess1;

import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;


/**
 * When created, checks MovieTheater to see if the list of genres and certifications is empty,
 * if they are empty, calls FetchGenresTask and FetchCertificationsTask to get a list of available
 * genres and certs from themoviedb.  Themoviedb API calls that use a cert param just use the cert's name,
 * but the API call that uses a genre param use the genre id.  Thus, certs are stored as Strings
 * in MovieTheater's cert List, and genres are stored as key-value pairs in MovieTheater's genre
 * List.
 *
 * <p>
 * The ListPreference's for genres and certs are also programmatically populated so that they
 * reflect the most current list from themoviedb.  It is confusing that both sharedPreferences and
 * MovieTheater are used to store genre and cert related data, and I intend to consolidate all
 * that into one mechanism in stage 2.
 * </p>
 *
 * <p>
 * After the initial fetch operation for genres and certs, this fragment monitors for user changes
 * to any movie filtering preference.  When a preference is changed, it is immediately stored in
 * sharedPrefs, before onPreferenceChange even returns, because MovieGridFragment updates the movie
 * grid posters in real time.  Android does not actually write the new values to sharedPrefs until
 * after onPreferenceChange returns.  When a preference is changed by user, this fragment's
 * onMovieFilterChange callback method is called so that it's hosting Activity (MovieGridFragment)
 * can update the movie grid instantly.
 * </p>
 *
 * @author Nathan Merris
 * @see MovieGridFragment
 * @see com.nate.popmoviess1.MovieTheater.Genre
 * @see MovieTheater#setGenres(List)
 * @see com.nate.popmoviess1.MovieTheater.Certification
 * @see MovieTheater#setCertifications(List)
 */
public class PreferencesFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    private final String LOGTAG = SingleFragmentActivity.N8LOG + getClass().getSimpleName();

    // one MovieTheater to rule them all, and in the darkness bind their preferences (get the reference?)
    private MovieTheater mMovieTheater;
    private SharedPreferences mSharedPrefs;
    private Callbacks mCallbacks;


    /**
     * Required interface for any activity that hosts this fragment.
     *
     * Called when any movie filter related preference is changed.  The data in sharedPrefs will
     * already be updated when this method is called.
     */
    public interface Callbacks {
        void onMovieFilterChanged();
    }


    @Override
    public void onSaveInstanceState(Bundle outstate) {
        super.onSaveInstanceState(outstate);
        //Log.i(LOGTAG, "just entered onSaveInstanceState()");

        //setTargetFragment(null, -1);
    }






    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // the following line does this: it casts this fragment's hosting activities context
        // to a Callbacks interface, which will be implemented by this fragment's hosting activity
        // this keeps the fragment locked to it's hosting activity, but to make sure this fragment
        // does not get accidentally tied to an activity that you don't expect, also need to
        // nullify mCallbacks in onDetach()
        mCallbacks = (Callbacks) getActivity();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOGTAG, "just entered onCreate");


        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mMovieTheater = MovieTheater.get(getActivity());

        addPreferencesFromResource(R.xml.preference_movie_filters);

        updateReleaseDatePref(); // sets release date pref to current year when app run for first time


        // if the list of genres or certs are zero
        if (mMovieTheater.getCertificationListSize() == 0) // check if this app session has an up to date list of certs
            new FetchCertificationsTask().execute(); // gets a list of all available themoviedb certs, and calls updateCertificationListPref() when done
        else
            updateCertificationListPref(); // use whatever certs list is in MovieTheater, no need to call themoviedb API on every orientation change

        if(mMovieTheater.getGenreListSize() == 0) // check if this app session has an up to date list of genres
            new FetchGenresTask().execute(); // gets a list of all available themoviedb genres and calls updateGenreListPref when done
        else
            updateGenreListPref(); // use whatever genre list is in MovieTheater, no need to call themoviedb API every orientation change



        findPreference(getString(R.string.pref_movieinfo_genre_key)).setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.pref_movieinfo_cert_key)).setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.pref_movieinfo_year_key)).setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.pref_movieinfo_sortby_key)).setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.pref_movieinfo_year_switch_key)).setOnPreferenceChangeListener(this);


        setPreferenceSummaries(findPreference(getString(R.string.pref_movieinfo_genre_key)),
                mSharedPrefs.getString(getString(R.string.pref_movieinfo_genre_key), ""));

        setPreferenceSummaries(findPreference(getString(R.string.pref_movieinfo_cert_key)),
                mSharedPrefs.getString(getString(R.string.pref_movieinfo_cert_key), ""));

        setPreferenceSummaries(findPreference(getString(R.string.pref_movieinfo_year_key)),
                mSharedPrefs.getString(getString(R.string.pref_movieinfo_year_key), ""));

        setPreferenceSummaries(findPreference(getString(R.string.pref_movieinfo_sortby_key)),
                mSharedPrefs.getString(getString(R.string.pref_movieinfo_sortby_key), ""));


    }



    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        //Log.i(LOGTAG, "just entered onCreatePreferences");

    }



    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        //Log.i(LOGTAG, "just entered onPreferenceChange");

        String stringValue = value.toString();



        SharedPreferences.Editor editor = mSharedPrefs.edit();

        // Android does not write the value to sharePrefs until after this method
        // returns, but I need them immediately over in MovieGridFragment, so I am
        // writing them in myself.  I tried using Android's built in callback
        // interface (Preference.onPreferenceStartFragment) in this fragment's hosting
        // activity, but it does the same thing, it's actually called before
        // onPreferenceChange is called
        if(preference.getKey().equals(getString(R.string.pref_movieinfo_cert_key))) {
            editor.putString(getString(R.string.pref_movieinfo_cert_key),
                    stringValue);
        }
        else if(preference.getKey().equals(getString(R.string.pref_movieinfo_year_key))) {
            editor.putString(getString(R.string.pref_movieinfo_year_key),
                    stringValue);
        }
        else if(preference.getKey().equals(getString(R.string.pref_movieinfo_genre_key))) {
            editor.putString(getString(R.string.pref_movieinfo_genre_key),
                    stringValue);
        }
        else if(preference.getKey().equals(getString(R.string.pref_movieinfo_sortby_key))) {
            editor.putString(getString(R.string.pref_movieinfo_sortby_key),
                    stringValue);
        }
        else if(preference.getKey().equals(getString(R.string.pref_movieinfo_year_switch_key))) {
            editor.putBoolean(getString(R.string.pref_movieinfo_year_switch_key),
                    Boolean.valueOf(stringValue));

        }

        editor.commit();

        setPreferenceSummaries(preference, value);


        // now tell the hosting activity movie filter prefs have changed.. it's going to
        // perform a fragment transaction to update the grid of movies
        mCallbacks.onMovieFilterChanged();

        return true;
    }


    private void bindPreferenceSummaryToValue(Preference preference) {
        //Log.i(LOGTAG, "just entered bindPreferenceSummaryToValue: " + preference.toString());

        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // update the preference summary
        setPreferenceSummaries(preference, mSharedPrefs.getString(preference.getKey(), ""));
    }


    private void setPreferenceSummaries(Preference preference, Object value) {
        String stringValue = value.toString();

        EditTextPreference yearEditTextPref = (EditTextPreference) findPreference(getString(R.string.pref_movieinfo_year_key));

        // when year toggle switch is turned ON, set year edit text title to whatever is in shared prefs for year
        // when year toggle switch is turned OFF, set year edit text title to 'Any Year'
        if(preference instanceof SwitchPreferenceCompat) {

            if(Boolean.valueOf(stringValue)) { // year toggle switch was just turned ON
                yearEditTextPref.setTitle(mSharedPrefs.getString(getString(R.string.pref_movieinfo_year_key), ""));

            }
            else { // year toggle switch was just turned OFF
                yearEditTextPref.setTitle(getString(R.string.pref_movieinto_year_anyyear_title));
                yearEditTextPref.setShouldDisableView(false); // prevent it from 'graying out' because it depends on the toggle switch in the XML
            }

            return;
        }

        // set the year edittext pref title, instead of the summary, depending on the toggle switch state
        if(preference.getKey().equals(getString(R.string.pref_movieinfo_year_key))) {

            //SwitchPreferenceCompat yearToggleSwitch = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_movieinfo_year_switch_key));

            // year toggle switch was ON when this method was called, so set the year EditText title
            if(mSharedPrefs.getBoolean(getString(R.string.pref_movieinfo_year_switch_key), true)) {
                yearEditTextPref.setTitle(stringValue);
            }
            else { // year toggle switch was OFF whan this method was called
                yearEditTextPref.setTitle(getString(R.string.pref_movieinto_year_anyyear_title));
                yearEditTextPref.setShouldDisableView(false); // prevent it from 'graying out' because it depends on the toggle switch in the XML
            }

            return;
        }

        // update preference summaries for everything else as normal..
        if(preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0)
                preference.setSummary(listPreference.getEntries()[prefIndex]);
        }
        else {
            preference.setSummary(stringValue);
        }

    }


    private class FetchGenresTask extends AsyncTask<Void, Void, List<MovieTheater.Genre>> {

        @Override
        protected List<MovieTheater.Genre> doInBackground(Void... params) {
            Log.i(LOGTAG, "just entered FetchGenresTask.doInBackground");
            return new TheMovieDbFetcher(getActivity()).fetchAvailableGenres();
        }

        @Override
        protected void onPostExecute(List<MovieTheater.Genre> items) {
            mMovieTheater.setGenres(items); // update the list of available genres in MovieTheater singleton

            if(mMovieTheater.getGenreListSize() > 1)
                updateGenreListPref();

            Log.i(LOGTAG,"EXITING FetchGenresTask.onPostExecute");
        }

    }


    // task will query themoviedb for ALL available movie certifications
    private class FetchCertificationsTask extends AsyncTask<Void, Void, List<MovieTheater.Certification>> {

        @Override
        protected List<MovieTheater.Certification> doInBackground(Void... params) {
            Log.i(LOGTAG, "just entered FetchCertificationsTask.doInBackground");
            return new TheMovieDbFetcher(getActivity()).fetchAvailableCertifications();
        }

        @Override
        protected void onPostExecute(List<MovieTheater.Certification> items) {
            mMovieTheater.setCertifications(items); // update the list of available certs in MovieTheater singleton

            if(mMovieTheater.getCertificationListSize() > 1)
                updateCertificationListPref();

            Log.i(LOGTAG,"EXITING FetchCertificationsTask.onPostExecute");
        }

    }


    private void updateReleaseDatePref() {

        // the following code will programmatically populate the year pref ONLY if their is
        // currently no year pref saved in sharedPrefs, ie the first time the user installs this app,
        // the year pref defaults to the current year, after that the user's pref is retained and shown
        String movieYearValue = mSharedPrefs.getString(getString(R.string.pref_movieinfo_year_key), "");
        if(TextUtils.isEmpty(movieYearValue)) {
            Log.i(LOGTAG, "  movieYearValue isEmpty(), defaulting to the current year");
            Calendar calendar = Calendar.getInstance();
            String year = String.valueOf(calendar.get(Calendar.YEAR));
            SharedPreferences.Editor editor = mSharedPrefs.edit();
            editor.putString(getString(R.string.pref_movieinfo_year_key), year);
            editor.commit();
            EditTextPreference yearEditTextPref = (EditTextPreference) findPreference(getString(R.string.pref_movieinfo_year_key));
            yearEditTextPref.setText(year);
        }

        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_movieinfo_year_key)));

    }


    private void updateCertificationListPref() {

        boolean currentlySelectedPrefValueHasChanged = true;
        String currentCertificationPrefValue = mSharedPrefs.getString(getString(R.string.pref_movieinfo_cert_key), "");
        Log.i(LOGTAG, "  inside updateCertificationListPref and current certificationPrefValue: " + currentCertificationPrefValue);

        // the following code populates the movie certs filter prefs with data that has
        // already been fetched from themoviedb, it also takes care of what happens when the app is
        // installed for the first time, and also if themoviedb happens to change a genre name
        // AND the user coincidentally happens to have had that genre name selected
        ListPreference certificationListPref = (ListPreference) findPreference(getString(R.string.pref_movieinfo_cert_key));
        List<MovieTheater.Certification> certificationList = mMovieTheater.getCertifications();
        CharSequence[] cs = new CharSequence[certificationList.size()];


        MovieTheater.Certification[] certificationObjectArray = certificationList
                .toArray(new MovieTheater.Certification[certificationList.size()]);
        for(int i = 0; i < certificationList.size(); i++) {
            cs[i] = certificationObjectArray[i].name;

            // need to know if themoviedb has changed or removed the certification name that is currently selected by user
            // so set boolean to false if at least one of the cs entries matches the current user selected certification prefs value
            // if ANY other certification name than the one the user currently has selected is changed, it does not matter
            // the list will simply update to the most recent certifications just returned from themoviedb
            if(cs[i].equals(currentCertificationPrefValue))
                currentlySelectedPrefValueHasChanged = false; // no change that matters

            //Log.i(LOGTAG, "  cs[" + i + "]: " + cs[i]);
        }

        certificationListPref.setEntries(cs);
        certificationListPref.setEntryValues(cs);

        // check if certificationListPref has a default value, if not that means this app is being run
        // for the first time after install, in which case default to 'Any Rating'
        if(certificationListPref.getValue() == null) {
            Log.i(LOGTAG, "  resetting cert list pref to 'Any Rating' because no previous cert list entry was found");
            certificationListPref.setValue(cs[0].toString());
        }

        // if the user happened to have had a movie genre selected that themoviedb happened to
        // just have changed, simply reset the users pref to 'Any Genre'
        // not sure how else to handle this.. seems to be an acceptable result from a user's
        // perspective, considering that it will rarely ever happen
        if(currentlySelectedPrefValueHasChanged) {
            Log.i(LOGTAG, "  resetting certification list pref to 'Any Rating'");
            certificationListPref.setValue(cs[0].toString()); // csValue[0] is always the default entry
        }

        bindPreferenceSummaryToValue(certificationListPref);

    }


    private void updateGenreListPref() {

        boolean currentlySelectedPrefValueHasChanged = true;
        String currentGenrePrefValue = mSharedPrefs.getString(getString(R.string.pref_movieinfo_genre_key), "");
        Log.i(LOGTAG, "  inside updateGenreListPref and current genrePrefValue (-1 means 'Any Genre'): " + currentGenrePrefValue);

        // the following code populates the movie genre filter prefs with data that has
        // already been fetched from themoviedb, it also takes care of what happens when the app is
        // installed for the first time, and also if themoviedb happens to change a genre name
        // AND the user coincidentally happens to have had that genre name selected
        ListPreference genreListPref = (ListPreference) findPreference(getString(R.string.pref_movieinfo_genre_key));
        List<MovieTheater.Genre> genreList = mMovieTheater.getGenres();
        CharSequence[] csEntries = new CharSequence[genreList.size()];
        CharSequence[] csValues = new CharSequence[genreList.size()];

        MovieTheater.Genre[] genreObjectArray = genreList
                .toArray(new MovieTheater.Genre[genreList.size()]);
        for(int i = 0; i < genreList.size(); i++) {
            csEntries[i] = genreObjectArray[i].name;
            csValues[i] = String.valueOf(genreObjectArray[i].id);

            // need to know if themoviedb has changed or removed the genre name that is currently selected by user
            // so set boolean to false if at least one of the cs entries matches the current user selected genre prefs value
            // if ANY other genre name than the one the user currently has selected is changed, it does not matter
            // the list will simply update to the most recent genres just returned from themoviedb
            if(csValues[i].equals(currentGenrePrefValue))
                currentlySelectedPrefValueHasChanged = false; // no change that matters

            //Log.i(LOGTAG, "  cs[" + i + "]: " + cs[i]);
        }

        genreListPref.setEntries(csEntries);
        genreListPref.setEntryValues(csValues);

        // check if genreListPref has a default value, if not that means this app is being run
        // for the first time after install, in which case default to 'Any Genre'
        if(genreListPref.getValue() == null) {
            Log.i(LOGTAG, "  resetting genre list pref to 'Any Genre' because no previous genre list entry was found");
            genreListPref.setValue(csValues[0].toString()); // cs[0] is always the default entry
        }

        // if the user happened to have had a movie genre selected that themoviedb happened to
        // just have changed, simply reset the users pref to 'Any Genre'
        // not sure how else to handle this.. seems to be an acceptable result from a user's
        // perspective, considering that it will rarely ever happen
        if(currentlySelectedPrefValueHasChanged) {
            Log.i(LOGTAG, "  resetting genre list pref to 'Any Genre'");
            genreListPref.setValue(csValues[0].toString()); // csValue[0] is always the default entry
        }

        bindPreferenceSummaryToValue(genreListPref);

    }


}
