package com.nate.popmoviess1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;


/**
 * Hosts a single PreferencesFragment.  This Activity will only be reachable on phones that run
 * in single pane mode because devices that run in dual pane mode will always have a prefs fragment
 * visible on the right pane.
 *
 * @author Nathan Merris
 */
public class PreferencesActivity extends SingleFragmentActivity implements
        PreferencesFragment.Callbacks {

    private final String LOGTAG = SingleFragmentActivity.N8LOG + getClass().getSimpleName();

    private static final String MOVIE_FILTERS_HAVE_CHANGED_BUNDLE_KEY = "prefs_filters_changed_bundle_key";

    private boolean mMovieFilterWasChanged; // true when one or more movie filters have changed

    @Override
    protected Fragment createFragment() {
        return new PreferencesFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // default to false because only want true when PreferencesFragment.onMovieFilterChanged
        // callback is made
        if(savedInstanceState == null) {
            mMovieFilterWasChanged = false;
        }
        // otherwise, if the screen was just rotated or Android killed this activity for some reason,
        // want to restore the state of mMovieFilterChanged.  for example: if user: 1) made a filter change,
        // 2) pressed the home button, 3) waited a really long time to come back to this app,
        // then in that case when they press back or UP, HomeActivity will still know to tell
        // MovieGridFragment to make an API call to refresh the movie grid
        else {
            mMovieFilterWasChanged = savedInstanceState.getBoolean(MOVIE_FILTERS_HAVE_CHANGED_BUNDLE_KEY);
        }

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(MOVIE_FILTERS_HAVE_CHANGED_BUNDLE_KEY, mMovieFilterWasChanged);
    }

    /**
     * Puts a boolean extra in the intent that HomeActivity started via startActivityForResult when
     * the user clicked the movie filter icon in the toolbar on devices that run in single pane mode.
     * HomeActivity will make an API call and refresh the movie grid to display updated movies.
     */
    @Override
    public void onMovieFilterChanged() {
        Log.i(LOGTAG, "in onMovieFilterChanged()");

        mMovieFilterWasChanged = true;

        Intent intent = new Intent();
        // always set the boolean extra to true in this case
        intent.putExtra(getString(R.string.EXTRA_MOVIE_FILTERS_HAVE_CHANGED), true);
        setResult(RESULT_OK, intent);
    }


    @Override
    public boolean onSupportNavigateUp() {
        Log.i(LOGTAG, "just entered onSupportNavigateUP()");

        Intent intent = new Intent(this, HomeActivity.class);

        // tell HomeActivity whether or not a movie filter was changed
        intent.putExtra(getString(R.string.EXTRA_MOVIE_FILTERS_HAVE_CHANGED), mMovieFilterWasChanged);

        // it is more intuitive that the app just exits when the user presses back from HomeActivity, so..
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if(findViewById(R.id.container_second_pane) == null) { // app is in single pane mode
            // do not show the movie filters menu item because it's already visible in single pane..
            menu.findItem(R.id.action_movie_filters).setVisible(false);
        }
        else { // app is in dual pane mode

            if(getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(false); // hide UP button

            menu.findItem(R.id.action_movie_filters).setVisible(false);
        }
        return true;
    }



}
