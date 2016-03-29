package com.nate.popmoviess1;

import android.support.v4.app.Fragment;
import android.view.Menu;

/**
 * Hosts a single fragment that displays the About page for this app
 *
 * @author Nathan Merris
 */
public class AboutAppActivity extends SingleFragmentActivity {

    private final String LOGTAG = SingleFragmentActivity.N8LOG + getClass().getSimpleName();


    @Override
    protected Fragment createFragment() {
        return new AboutAppFragment();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // do not show menu items in both single and dual pane modes
        menu.findItem(R.id.action_movie_filters).setVisible(false);
        menu.findItem(R.id.action_about_app).setVisible(false);

        return true;
    }


}
