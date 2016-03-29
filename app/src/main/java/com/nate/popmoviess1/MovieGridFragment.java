package com.nate.popmoviess1;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;


/**
 * Displays a scrolling grid of movies that can be clicked to see a movie detail view.
 * Images are displayed using a RecyclerView.
 * Movie poster images are loaded as needed by Picasso.
 *
 * @author Nathan Merris
 */
public class MovieGridFragment extends Fragment {

    private final String LOGTAG = SingleFragmentActivity.N8LOG + getClass().getSimpleName();

    private static final String ARG_SHOULD_FETCH_MOVIES = "com.nate.popmoviess1.moviegridfragment.fetch_movies";

    private MovieTheater mMovieTheater; // refers to the singleton class that houses this app's movie thumbnail list
    private RecyclerView mMoviePosterRecyclerView; // displays a grid of movie posters
    private MoviePosterAdapter mMoviePosterAdapter; // adapter between data in MovieTheater and mMoviePosterRecyclerView
    private Callbacks mCallbacks; // hosting activity will define what the method(s) inside Callback interface should do
    private SharedPreferences mSharedPrefs;
    private TextView mNoMoviesTextView; // holds a msg informing users that no movies could be displayed



    public MovieGridFragment() {
    }


    /**
     * Call from a hosting Activity to get a new fragment for a fragment transaction.  The fragment
     * will display a list of movie posters in grid form: 2 columns in portrait, 3 in landscape.
     * Clicking on a movie will start a call to MovieGridFragment's onMovieSelected callback, which
     * the hosting activity must implement.  Independence: it's not just an awesome US holiday.
     *
     * @param shouldFetchMovies set to true if caller wants to create a new MovieGridFragment that
     *                          will make an API call to fetch a new list of movies, if true, the
     *                          view will be refreshed to reflect the new movies
     * @return new MovieGridFragment <code>fragment</code>
     */
    public static MovieGridFragment newInstance(boolean shouldFetchMovies) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOULD_FETCH_MOVIES, shouldFetchMovies);
        MovieGridFragment fragment = new MovieGridFragment();
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * Required interface for any activity that hosts this fragment
     */
    public interface Callbacks {

        /**
         * Hosting Activity should determine what happens when a movie is tapped from the movie grid.
         * @param movie the movie that was just tapped by user from the grid view
         */
        void onMovieSelected(Movie movie);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //Log.i(LOGTAG, "just entered onAttach()");

        // associate the fragment's mCallbacks object with the activity it was just attached to
        mCallbacks = (Callbacks) getActivity();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        //Log.i(LOGTAG, "just entered onDetach()");

        mCallbacks = null; // need to make sure this member variable is up to date with the correct activity
        // so nullify it every time this fragment gets detached from it's hosting activity
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOGTAG, "just entered onCreate()");

        mMovieTheater = MovieTheater.get(getActivity());
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if(savedInstanceState == null) {
            Log.i(LOGTAG, "  and savedInstanceState is NULL, may or may not perform a FetchMoviesTask...");

            if(getArguments().getBoolean(ARG_SHOULD_FETCH_MOVIES)) {
                Log.i(LOGTAG, "    and got fragment arg boolean extra to fetch new movies");
                new FetchMoviesTask().execute(); // update MovieTheater with a new movie list
            }
        }
        else {
            Log.i(LOGTAG, "  and savedInstanceState is *NOT* null");
        }

    }


    /**
     * Make the movie posters look nice, with even padding all around.
     *
     * @author edwardaa on Stackoverflow
     * @see <a>http://stackoverflow.com/questions/28531996/android-recyclerview-gridlayoutmanager-column-spacing</a>
     */
    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Log.i(LOGTAG, "just entered onCreateView()");

        View rootView = inflater.inflate(R.layout.fragment_movie_grid, container, false);

        // get a reference to the RecyclerView
        mMoviePosterRecyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_movie_grid_recycler_view);
        mNoMoviesTextView = (TextView) rootView.findViewById(R.id.fragment_movie_grid_no_movies);

        // define the layout that the RecyclerView will use
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            mMoviePosterRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2,
                    getResources().getDimensionPixelSize(R.dimen.movie_grid_poster_margin), true));

            mMoviePosterRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                    2, GridLayoutManager.VERTICAL, false)); // 2 columns for portrait
        }
        else {

            mMoviePosterRecyclerView.addItemDecoration(new GridSpacingItemDecoration(3,
                    getResources().getDimensionPixelSize(R.dimen.movie_grid_poster_margin), true));

            mMoviePosterRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                    3, GridLayoutManager.VERTICAL, false)); // 3 columns for landscape
        }

        updateUI();

        return rootView;
    }


    public void updateUI() {
        //Log.i(LOGTAG, "just entered updateUI()");

        // get a ref to the app global MovieTheater, which houses the movies list
        //MovieTheater movieTheater = MovieTheater.get(getActivity());

        // get the list of movies.. this does not update the list in any way
        List<Movie> movies = mMovieTheater.getMovies();

        // no movies in MovieTheater, don't care about the reason for S1 of this project, will customize msg in S2
        if(mMovieTheater.getMovieListSize() < 1) {
            // show no movies msg, hide recyclerview
            mMoviePosterRecyclerView.setVisibility(View.GONE);
            mNoMoviesTextView.setVisibility(View.VISIBLE);
        }
        else {
            // show recyclerview, hide no movies msg
            mMoviePosterRecyclerView.setVisibility(View.VISIBLE);
            mNoMoviesTextView.setVisibility(View.GONE);

            // set up and refresh the adapter with the new movie list
            mMoviePosterAdapter = new MoviePosterAdapter(movies);
            mMoviePosterRecyclerView.setAdapter(mMoviePosterAdapter);
            mMoviePosterAdapter.notifyDataSetChanged();
        }

    }
    

    // a ViewHolder holds on to a View, which in this case is just a simple ImageView
    // there is a performance benefit to using RecyclerView, namely the Views do not need to be
    // 'found' every time the movie grid fragment creates it's views..
    // they are found once and then stored in the ViewHolder
    // the onClickListener is also implemented here.. nice and tidy
    private class MoviePosterHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {

        private Movie mMovie;
        private ImageView mPosterImageView;

        public MoviePosterHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this); // so each poster image can respond to clicks

            mPosterImageView = (ImageView) itemView.findViewById(R.id.fragment_moviegrid_poster_image_view);

            // could have many other view references cached here
        }


        public void bindMovie(Movie movie) {
            mMovie = movie;
        }


        @Override
        public void onClick(View view) {
            mCallbacks.onMovieSelected(mMovie);
        }

    } // end inner class


    // MoviePosterAdapter does what any adapter does: it is the controller that sits between
    // the list of movies in MovieTheater singleton and the RecyclerView that displays them
    // the List passed to it's constructor is the list of movies in MovieTheater
    private class MoviePosterAdapter extends RecyclerView.Adapter<MoviePosterHolder> {

        private List<Movie> mMovies; // local reference to the app global movie list

        public MoviePosterAdapter(List<Movie> movies) {
            mMovies = movies;
        }


        @Override
        public MoviePosterHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            // get a ref to hosting activities LayoutInflator
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());

            // for now the view just needs to hold a single ImageView.. (the movie thumbnail pic)
            View view = layoutInflater.inflate(R.layout.moviegrid_poster, viewGroup, false);
            return new MoviePosterHolder(view);
        }


        @Override
        public void onBindViewHolder(MoviePosterHolder holder, int position) {

            holder.bindMovie(mMovies.get(position));

            // good Lord, it looks like a LOT of work to do this with stock android Handlers, Loopers, Messages, etc..
            // Picasso does all the grunt work for me
            Picasso.with(getActivity())
                    .load(mMovies.get(position).getPosterUrl()) // the fully formed image URL
                    //.placeholder(R.drawable.movie_placeholder) // probably don't need this
                    .into(holder.mPosterImageView);

        }


        @Override
        public int getItemCount() {
            return mMovies.size();
        }

    } // end inner class


    /**
     * Fetches movie metadata from themoviedb based on the filter parameters currently stored
     * in sharePrefs.  When this task is started the first time this app is launched after install,
     * the sharedPrefs key-value pairs will not yet exist if the user has not navigated to PreferencesFragment
     * yet, which would only be possible on phones that run in single pane mode. To ensure that this
     * task returns the most popular movies of any year in any genre in that case,
     * the movie year getBoolean method defaults to false (so TheMovieDbFetcher will ignore
     * the releasedate param), the certification getString call will default to "Any Year", and the
     * sort order getString call defaults to 'popularity.desc'.  The genreId is set to -1 right here
     * in doInBackground when a number format exception is thrown, so theMovieDbFetcher will know to ignore the genre id.
     *
     * <p>
     * I do not like how this works, it's kind of confusing and scattered.  Like many of the obnoxious
     * design issues I have ended up with in this app, this will be corrected in stage 2 when I
     * use some kind of initialization service that will ensure that EVERYTHING in this app is
     * ready to go before any Activities even start.  That will include MovieTheater's movie list,
     * and also the list of genre id-name pairs, and the certifications.  The problem is I have
     * things initializing in different places.. it's just difficult to manage.  Chalk it up to
     * me being so green as a coder!  Programmatically adding user selectable lists of genres and
     * certifications using async tasks ended up being much trickier than I thought!!
     * </p>
     *
     * In postExecute, the metadata is stored in MovieTheater's list of Movies to be used elsewhere.
     * This fragments updateUI method is called in onPostExecute.  If the fetch returns no movies
     * for any reason, a msg is shown to the user indicating so.
     *
     * @see MovieTheater#updateMovies(List)
     * @see MovieGridFragment#updateUI()
     * @see TheMovieDbFetcher#fetchMovies(String, String, int, String, boolean)
     */
    private class FetchMoviesTask extends AsyncTask<Void, Void, List<Movie>> {

        @Override
        protected List<Movie> doInBackground(Void... params) {
            Log.i(LOGTAG, "just entered FetchMoviesTask.doInBackground");

            int genreId;

            try {
                genreId = Integer.valueOf(mSharedPrefs.getString(getString(R.string.pref_movieinfo_genre_key), ""));
            }
            catch (NumberFormatException nfe) {
                genreId = -1;
            }

            return new TheMovieDbFetcher(getActivity()).fetchMovies(

                    // certificaiton param
                    mSharedPrefs.getString(getString(R.string.pref_movieinfo_cert_key),
                            // default to 'Any Rating'
                            getString(R.string.themoviedb_any_certification_filter_name_value)),

                    //releasedate param
                    mSharedPrefs.getString(getString(R.string.pref_movieinfo_year_key),
                            ""),   // default does not matter because year switch boolean defaults to false below

                    // genre id param
                    genreId, // defaults to -1 or 'Any Genre' above

                    // sort order param
                    mSharedPrefs.getString(getString(R.string.pref_movieinfo_sortby_key),
                            "popularity.desc"), // default to 'Most Popular'

                    // movie release date toggle switch state is
                    // used to determine if should query by all years or a specific year
                    mSharedPrefs.getBoolean(getString(R.string.pref_movieinfo_year_switch_key),
                            false)); // default to 'search any year'

        }

        @Override
        protected void onPostExecute(List<Movie> items) {
            Log.i(LOGTAG, "EXITING FetchMoviesTask.onPostExecute");

            mMovieTheater.updateMovies(items); // update the list of Movies in MovieTheater singleton

            updateUI();
        }

    } // end inner class


}// end class


