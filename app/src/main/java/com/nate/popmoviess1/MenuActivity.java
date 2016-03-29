package com.nate.popmoviess1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * A convenience class that avoids having to repeatedly handle inflating the menu and processing
 * menu clicks.  It's probably overkill for this app due to it's simplicity, but it still seemed like
 * a good idea.  On the other hand, I had to hide menu items in other Activities when they are not
 * needed, but that is easy and very straight forward.  Inheritably delicious!
 *
 * @author Nathan Merris
 */
public class MenuActivity extends AppCompatActivity{

    public static final String N8LOG = "N8LOG "; // logtag prefix to use for entire app
    private final String LOGTAG = N8LOG + "MenuActivity";


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_about_app).setVisible(false);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.action_about_app:
                startActivity(new Intent(this, AboutAppActivity.class));
                return true;

            // may add more menu items in stage 2

        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * I need an intent extra so that HomeActivity knows whether or not it should tell it's
     * MovieGridFragment to make an API call to refresh the movie grid after the user changes a movie
     * filter, so I need to override the
     * default UP button behavior because I could not figure out how to attach a message to whatever
     * Android was doing when up was pressed.  The default behavior here will tell MovieGridFragment
     * not to make an API call by adding a false boolean extra.  Any subclass can override this
     * and tell HomeActivity to refresh the movie grid or not, which as of now only happens in
     * PreferencesActivity.
     *
     * @see PreferencesActivity#onSupportNavigateUp()
     */
    @Override
    public boolean onSupportNavigateUp() {
        Log.i(LOGTAG, "just entered onSupportNavigateUP()");

        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(getString(R.string.EXTRA_MOVIE_FILTERS_HAVE_CHANGED), false);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        return true;
    }


}
