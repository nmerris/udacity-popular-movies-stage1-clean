package com.nate.popmoviess1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.support.v4.app.Fragment;
import android.view.MenuItem;


/**
 * Home activity for this app.  Clicking UP will always go here in both single and dual pane modes.
 * Hosts a MovieGridFragment in both single and dual pane modes.
 * Hosts both a MovieGridFragment (left pane) and a PreferencesFragment (right pane) in dual pane mode.
 * Fragment independence is maintained by implementing their Callbacks interfaces.
 *
 * @author Nathan Merris
 */
public class HomeActivity extends SingleFragmentActivity
    implements MovieGridFragment.Callbacks, PreferencesFragment.Callbacks {

    private final String LOGTAG = SingleFragmentActivity.N8LOG + getClass().getSimpleName();
    private static final int MOVIE_FILTER_CHANGED_INTENT_REQUEST_CODE = 1;

    // return a reference to this Activities layout.. the ref is in a resource qualified folder,
    // so Android will pick the correct ref depending on the device
    protected int getLayoutResourceId() { return R.layout.activity_home_ref; }


    /**
     * Returns a MovieGridFragment for SingleFragmentActivity to put it 'fragment_container'.
     * To avoid unnecessary moviedb API calls, the intent that started this Activity is checked
     * for an extra that tells it whether any movie filters have been changed by the user.  If so,
     * createFragment returns a new MovieGridFragment that will make a new API call and
     * update the movie grid to reflect whatever is in users movie filter criteria in sharedPrefs, otherwise a
     * MovieGridFragment will be returned that will display movies from whatever is currently in
     * MovieTheater's Movie list.  If there is no intent extra found, the returned fragment will
     * assume an API call should be made, which will happen every time this app is started from dead.
     *
     * @return a new MovieGridFragment, the contents of which will depend on the intent extra that
     * that started this Activity
     * @see MovieGridFragment#onCreate(Bundle)
     */
    @Override
    protected Fragment createFragment() {
        Log.i(LOGTAG, "just entered createFragment()");

        // if this Activity is started but not via an intent from PreferencesActivity,
        // must default to true when getting the intent extra, so that the new MovieGridFragment
        // will perform a FetchMoviesTask and the movie list will be up to date
        boolean updateMovieGrid = getIntent().
                getBooleanExtra(getString(R.string.EXTRA_MOVIE_FILTERS_HAVE_CHANGED), true);

        Log.i(LOGTAG, "  and the fragment being returned to SingleFragmentActivity will fetch new movies: " + updateMovieGrid);

        // create and return a new MGF, depending on the incoming intent boolean extra
        return MovieGridFragment.newInstance(updateMovieGrid);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(LOGTAG, "just entered onCreate(), about to replace PrefsFragment with frag txn IF savedInstance state is NULL");

        // if you don't check for savedInstanceState == null here, app crashes when preference
        // dialog is OPEN and device is rotated, you get a 'target not in fragment manager' exception!
        // does not crash when dialog is NOT open though...
        if(savedInstanceState == null) {
            if (findViewById(R.id.container_second_pane) != null) { // app is in two pane mode
                Fragment newPrefsFragment = new PreferencesFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container_second_pane, newPrefsFragment).commit();
            }
        }

    }

    /**
     * Launches an Intent to start a new MovieDetailPagerActivity.
     * It does not matter if app is in single or dual pane mode.
     *
     * @see MovieDetailPagerActivity
     */
    @Override
    public void onMovieSelected(Movie movie) {
        Intent intent = MovieDetailPagerActivity.newIntent(this, movie.id);
        startActivity(intent);
    }


    /**
     * Performs a fragment transaction in fragment_container (left pane) so that the grid of
     * movies is updated to reflect the movie filter change user just made.  This can only be
     * called in dual pane mode because there are no movie filters on screen in single pane mode.
     * At the time this is called, PreferencesFragment (which is where movie filters changes are
     * monitored), has already updated the movie metadata list in MovieTheater, so when
     * MovieGridFragment is replaced, it will reflect the current movie list.
     *
     * @see MovieGridFragment
     */
    @Override
    public void onMovieFilterChanged() {
        Log.i(LOGTAG, "in onMovieFilterChanged(), replacing MovieGridFragment with fragment txn, adding arg to fetch new movies");

        if(findViewById(R.id.fragment_container) != null) {
            // always tell MovieGridFragment to fetch new movies from here
            Fragment newMovieGridFragment = MovieGridFragment.newInstance(true);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, newMovieGridFragment)
                    .commit();
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.action_movie_filters) {
            Log.i(LOGTAG, "in onOptionsItemSelected, movie filter button pressed, about to startActivityForResult");
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivityForResult(intent, MOVIE_FILTER_CHANGED_INTENT_REQUEST_CODE);
        }

        super.onOptionsItemSelected(item);
        return true;
    }


    /**
     * Receives the intent extra that PreferencesActivity creates when one or more movie filters
     * are changed by user.  The extra is not actually being used here right now.
     * At this time, the mere existence of the intent requestCode from Prefs
     * Activity is enough to justify updating the movie grid recycler view in MovieGridFragment.
     *
     * @param requestCode will only be MOVIE_FILTER_CHANGED_INTENT_REQUEST_CODE at this time
     * @param data always contains <code>true</code> at this time
     * @see PreferencesActivity#onMovieFilterChanged()
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(LOGTAG, "in onActivityResult, no request code yet...");

        if(requestCode == MOVIE_FILTER_CHANGED_INTENT_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                /*Boolean updateMovieGrid = data.
                        getBooleanExtra(getString(R.string.EXTRA_MOVIE_FILTERS_HAVE_CHANGED), false);*/
                Log.i(LOGTAG, "  and received an intent with request code MOVIE_FILTER_CHANGED_INTENT_REQUEST_CODE");

                // always tell the new MovieGridFragment to fetch new movies from here
                if(findViewById(R.id.fragment_container) != null) {
                    Fragment newMovieGridFragment = MovieGridFragment.newInstance(true);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, newMovieGridFragment)
                            .commit();
                }
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // show the 'About App' menu item
        menu.findItem(R.id.action_about_app).setVisible(true);

        if(findViewById(R.id.container_second_pane) != null) { // app is in dual pane mode
            // do not show the movie filters menu item because it's already visible in second pane..
            menu.findItem(R.id.action_movie_filters).setVisible(false);
        }
        return true;
    }


}
