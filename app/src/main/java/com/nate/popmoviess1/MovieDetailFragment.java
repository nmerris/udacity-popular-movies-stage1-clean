package com.nate.popmoviess1;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


/**
 * Displays a movie detail screen containing interesting movie data.
 * Due the way I designed this app, it is currently not showing movie genre's, even though
 * I have both the genre id's attached to every movie and the movie genre id-name associations
 * stored in MovieTheater.  I plan on correcting that in stage 2.
 *
 * Movie backdrop images are loaded as needed by Picasso.
 *
 * <p>
 * For now it shows: the movie backdrop image, vote avg, release date, and plot synopsis
 * </p>
 *
 * @author Nathan Merris
 */
public class MovieDetailFragment extends Fragment {

    private final String LOGTAG = SingleFragmentActivity.N8LOG + getClass().getSimpleName();

    // when MovieDetailPagerActivity needs to perform a fragment transaction to update the movie
    // that this fragment is showing, it will call this fragment's newInstance method and include
    // a fragment argument that specifies the movie id to show
    private static final String ARG_MOVIE_ID = "movie_thumb_id";
    private static final String BUNDLE_MOVIE_ID_KEY = "bundle_movie_id_key";

    private MovieTheater mMovieTheater;
    private Movie mMovie; // the specific movie thumbnail object that this fragment is working with
    private int mMovieId; // the id of the specific movie that this fragment is working with


    public MovieDetailFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(LOGTAG, "just entered onCreate()");

        mMovieTheater = MovieTheater.get(getActivity());

        if(savedInstanceState == null) {
            // get the movie id from MovieDetailPagerActivity, which just performed a fragment
            // transaction on this fragment
            mMovieId = getArguments().getInt(ARG_MOVIE_ID);
            Log.i(LOGTAG, "  savedInstanceState was null, so just got mMovieID via intent: " + mMovieId);

            mMovie = mMovieTheater.getMovie(mMovieId);
        }
        else {
            // get the last used movie id that was previously stored in onSaveInstanceState
            mMovieId = savedInstanceState.getInt(BUNDLE_MOVIE_ID_KEY);
            Log.i(LOGTAG, "  savedInstanceState was NOT null, so just retrieved mMovieID from Bundle: " + mMovieId);

            mMovie = mMovieTheater.getMovie(mMovieId);
        }

    }



    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // save the movie id
        savedInstanceState.putInt(BUNDLE_MOVIE_ID_KEY, mMovieId);
        Log.i(LOGTAG, "in onSaveInstanceState, just stored in Bundle: mMovieId # " + mMovieId);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Log.i(LOGTAG, "just entered onCreateView()");

        View rootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        ImageView posterImgView = (ImageView) rootView.findViewById(R.id.fragment_movie_detail_poster_imageview);


        Picasso.with(getActivity())
                .load(mMovie.getBackdropUrl()) // the fully formed image URL
                //.placeholder(R.drawable.movie_placeholder) // probably don't need this
                .into(posterImgView);

        TextView movieTitleTxtView = (TextView) rootView.findViewById(R.id.fragment_movie_detail_movie_title_textview);
        TextView releaseDateValueTxtView = (TextView) rootView.findViewById(R.id.fragment_movie_detail_releasedate_value_textview);
        TextView voteAvgValueTxtView = (TextView) rootView.findViewById(R.id.fragment_movie_detail_vote_average_value_textview);
        TextView overviewContentTxtView = (TextView) rootView.findViewById(R.id.fragment_movie_detail_overview_content_textview);

        movieTitleTxtView.setText(mMovie.title);
        releaseDateValueTxtView.setText(parseDate(mMovie.release_date));


        // I'll get to this is stage two... need to have some kind of process initialize MovieTheater on app start,
        // that way anything can call on it and it will be initialized
/*
        int numGenreIds = mMovie.genre_ids.length;
        for(int i = 0; i < numGenreIds; i++) {
            genreNamesTxtView.append(mMovieTheater.getGenreName(mMovie.genre_ids[i]));

            if(i == numGenreIds)
                continue;
            genreNamesTxtView.append(", ");
        }
*/


        voteAvgValueTxtView.setText(String.valueOf(mMovie.vote_average));
        overviewContentTxtView.setText(mMovie.overview);

        return rootView;
    }



    /**
     * Parses a date so it looks nice on screen.
     *
     * @param dateString the date to parse, format must be 'yyyy-mm-dd'
     * @return the formated date, like July 4, 1776
     */
    private String parseDate(String dateString) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = dateFormat.parse(dateString);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            String month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
            String year = String.valueOf(calendar.get(Calendar.YEAR));
            String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

            return month + " " + day + ", " + year;

        } catch (ParseException e) {
            Log.i(LOGTAG, "  dateFormat.parse error: " + e);
            e.printStackTrace();
            return "Invalid release date";
        }
    }


    /**
     * Creates a new instance of a MovieDetailFragment, which requires a movie id to know
     * what movie details to show.
     *
     * @param movieId the moviedb movie id of the movie to show
     * @return a new <code>Fragment</code> instance
     */
    public static MovieDetailFragment newInstance(int movieId) {
        Bundle args = new Bundle();
        args.putInt(ARG_MOVIE_ID, movieId);
        MovieDetailFragment fragment = new MovieDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

}
