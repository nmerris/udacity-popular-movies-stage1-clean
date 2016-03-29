package com.nate.popmoviess1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;

import java.util.List;

/**
 * Hosting Activity for a movie detail screen.  A ViewPager is used to allow the user to swipe
 * left and right to see adjacent movies in their movie grid.  Movie data is obtained from
 * MovieTheater, which always has an updated list of movies, even after Android kills it to reclaim
 * memory.  Picasso is used to download movie backdrops in real time, which different and more
 * elaborate than the poster images used in MovieGridFragment.
 *
 * At this time, only <code>MovieTheater.getMovies</code> is safe to call here.  Genre and
 * Certification data are not safe to call because they are not persisted in MovieTheater.  That will
 * be corrected in stage 2.
 *
 * @author Nathan Merris
 * @see MovieTheater#getMovie(int)
 */
public class MovieDetailPagerActivity extends MenuActivity {

    private final String LOGTAG = SingleFragmentActivity.N8LOG + getClass().getSimpleName();
    private static final String EXTRA_MOVIE_ID = "com.nate.popmoviess1.movie_id";

    private ViewPager mViewPager;
    private MovieTheater mMovieTheater;
    private List<Movie> mMovies;
    private int mMovieId;


    /**
     * Creates a new Intent that should be used to start this Activity.  The intent must contain
     * the movie id so that MovieDetailPagerActivity knows which movie to display.  This app uses
     * the same movie id to keep track of movies that themoviedb does.
     *
     * @param movieId the moviedb id of the Movie to display
     * @return a fresh and delicious Intent, packed with movie id goodness
     */
    public static Intent newIntent(Context packageContext, int movieId) {
        Intent intent = new Intent(packageContext, MovieDetailPagerActivity.class);
        intent.putExtra(EXTRA_MOVIE_ID, movieId);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(LOGTAG, "just entered onCreate");

        setContentView(R.layout.activity_movie_detail_pager);


        // can't call mMovieTheater.getGenres or .getCertifications yet as they may be null

        mViewPager = (ViewPager) findViewById(R.id.activity_movie_detail_view_pager);
        mMovieTheater = MovieTheater.get(this);
        mMovies = mMovieTheater.getMovies();
        mMovieId = getIntent().getIntExtra(EXTRA_MOVIE_ID, 0);
        FragmentManager fragmentManager = getSupportFragmentManager();

        mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {

            @Override
            public Fragment getItem(int position) {
                Movie movie = mMovies.get(position);
                return MovieDetailFragment.newInstance(movie.id);
            }

            @Override
            public int getCount() {
                return mMovies.size();
            }
        });

        // set the viewpager to start at the movieId of whatever was sent in the intent
        // that started this Activity (that would be MovieGridActivity)
        for(int i = 0; i < mMovies.size(); i++) {
            if(mMovies.get(i).id == mMovieId) { // cycle through the list of movies
                mViewPager.setCurrentItem(i);
                break;
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.findItem(R.id.action_movie_filters).setVisible(false); // hide movie filters menu item because it's more intuitive to just click the up button from here

        return true;
    }


}
