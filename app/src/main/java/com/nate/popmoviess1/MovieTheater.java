package com.nate.popmoviess1;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * A singleton class that stores the list of <code>Movie</code>, <code>Genre</code>, and
 * <code>Certificaiton</code> (G, PG, R, etc) objects for this app.  These objects and this
 * MovieTheater make up the 'Model' layer of this app.  I have attempted to keep MovieTheater from
 * relying on any details of any other classes in this app, the only thing it needs is a Context
 * reference so it can get a handle on sharedPrefs.
 *
 * <p>
 * When <code>MovieTheater.updateMovies</code> is called, the list is
 * updated both in this class locally, and also written to sharedPreferences using Gson.
 * This is necessary to ensure that any Activity at any time can call <code>MovieTheater.get</code>
 * and rely on it to have a valid list of movies.  So if Android kills MovieTheater, it will come
 * back to life, like a zombie.  In stage 2 of this project, I will have the list of Genres and Certs
 * also be written to persistent memory, and MovieTheater will be initialized before any Activity
 * can get it's hands on it.  It's a bit of a confusing initialization mess at this time.
 * </p>
 *
 * Movie and Certification objects live inside this class as inner classes, mostly because they are
 * very simple.  The Movie class is a separate class.
 *
 * @author Nathan Merris
 */
public class MovieTheater {
    private final String LOGTAG = SingleFragmentActivity.N8LOG + getClass().getSimpleName();
    private final String MOVIE_LIST_SHAREDPREFS_KEY = "movietheater_movie_list_key";

    private SharedPreferences mSharedPrefs;
    private static MovieTheater sMovieTheater; // there can be only one and it will never change
    private List<Movie> mMoviesList; // the list of movies, metadata only here, images are downloaded in real time elsewhere
    private List<Genre> mGenresList; // this list of most available themoviedb genres
    private List<Certification> mCertifications; // the list of all available themoviedb certifications (G, PG, R, etc)


    /**
     * Represents a single themoviedb genre, which has an id and a name.  A single Movie object
     * contains an array of genre ids.  A single Movie can have multiple genres associated with it.
     */
    public static class Genre {
        int id; // the id to used in themoviedb API calls
        String name;
        public Genre(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }


    /**
     * Represents a single themoviedb certification, which has a name, a meaning (such as 'suitable
     * for all ages') and an order.  The order is necessary because themoviedb does put the certs
     * in their logical order in their json response.. as in: you want the order to be NR, G, PG, PG-13, R, NC-17.
     */
    public static class Certification implements Comparable<Certification> {
        String name; // the name to display in this app's movie filter list
        String meaning; // a summary of any particular certification, like: 'All ages permitted', etc
        int order; // the order in which the certs should be displayed: NR, G, PG, PG-13..

        public Certification(String name, String meaning, int order) {
            this.name = name;
            this.meaning = meaning;
            this.order = order;
        }

        public int compareTo(@NonNull Certification cert) { return (cert.order >= order) ? -1 : 1; }
    }


    // private singleton constructor..
    private MovieTheater(Context context) {
        Log.i(LOGTAG, "just entered private SINGLETON CONSTRUCTOR");

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mMoviesList = new ArrayList<>(); // ArrayList<Movie> is inferred from mMoviesList declaration
        mGenresList = new ArrayList<>();
        mCertifications = new ArrayList<>();

        if(!mSharedPrefs.contains(MOVIE_LIST_SHAREDPREFS_KEY)) {
            // do nothing: the list will be updated the first time MovieGridFragment calls FetchMoviesTask
            // every time after that, this singleton's mMovies list will be loaded with the
            // movie list stored in sharedPrefs, so this empty code block should only
            // ever be reached the first time this app is installed.  Note that is is okay for the
            // list to exist and have zero entries.. this can happen if the user has selected movie
            // filters that are too restrictive, just need to avoid a null pointer exception when
            // setting mMoviesList below

            Log.i(LOGTAG, "  and there was no persistent movie list found, so app is being installed" +
                    " for the first time, or user has persistence turned off, which may break this app" +
                    " if it is running in phone mode and they left the app while in the movie detail" +
                    " screen and Android killed this singleton.  At this time this app is not intended" +
                    " to be run on devices with persistence turned off.");
        }
        else {
            // this will typically happen once per 'app session'
            Log.i(LOGTAG, "  and the local mMovies list is being loaded with whatever movie" +
                    " list the user had in their sharedPrefs when this singleton was killed");
            // set the local movie list to whatever was in sharedPrefs
            mMoviesList = loadMovieList();
        }

    }


    /**
     * Use to access the MovieTheater singleton.  If it does not exist, it will be
     * created and initialized.
     *
     * @param context the context used by MovieTheater to get a reference to SharedPreferences
     * @return the single MovieTheater instance
     */
    public static MovieTheater get(Context context) {
        if(sMovieTheater == null) {
            sMovieTheater = new MovieTheater(context);
        }
        return sMovieTheater;
    }


    // returns the list of Movies for this MovieTheater
    public List<Movie> getMovies() { return mMoviesList; }

    public int getMovieListSize() { return mMoviesList.size(); }

    /**
     * Use to get a single Movie.
     *
     * @param id themoviedb id of the Movie you need
     * @return the Movie
     */
    public Movie getMovie(int id) {
        for (Movie m : mMoviesList) {
            if(m.id == id) return m;
        }
        return null;
    }

    /**
     * Updates MovieTheaters list of Movies and instantly overwrites the old list.  The list is stored
     * both locally in MovieTheater, and persisted in SharedPreferences, so there is no risk of
     * loosing the list, even if Android kills MovieTheater.
     *
     * @param movies the new list of Movies that MovieTheater will store
     */
    public void updateMovies(List<Movie> movies) {
        mMoviesList = movies;
        saveMovieList(movies);
    }


    // returns the list of movie genres for this MovieTheater
    public List<Genre> getGenres() { return mGenresList; }




    /**
     * Use to get a themoviedb genre id given a genre name.  The id is the actual id used by
     * themoviedb.  -1 is the genre id used by this app to indicate 'Any Genre' and is not something
     * themoviedb API will recognize.  Not used now because of the poor initialization process this
     * app now uses, but will use it in stage 2.
     *
     * @param name name used by themoviedb for a particular genre, like 'Horror'
     * @return themoviedb id to use for API calls that represents name, or -1 otherwise if that name
     * does can not be matched
     */
    public int getGenreId(String name) {
        for (Genre g : mGenresList) {
            if(g.name.equals(name)) return g.id;
        }
        return -1;
    }

    // get a specific genre name give a genre ID
    // not used now, will get to it in stage 2
    public String getGenreName(int id) {
        for (Genre g : mGenresList) {
            if(g.id == id) return g.name;
        }
        return null;
    }


    /**
     * Unlike MovieTheater.updateMovies, the Genre list is not persisted at this time.  The problem
     * is that I didn't take into account that PreferencesFragment may not be loaded (which is where
     * the list of genres is obtained in FetchGenresTask) on single screen devices until after a
     * user navigates to the Movie detail screen, where I would like to use the list of genres to
     * display the genre names associated with a given movie.  So there is no point in making it
     * persistent because of this scenario: a user is viewing a movie detail in MovieDetailFragment,
     * then Android kills MovieTheater, and then DetailFragment tries to call MovieTheater to get
     * the genre list, but the call fails due to a null pointer excpetion... like so many other
     * annoying design problems with stage one of this app, I will correct that in stage 2 by
     * having a cleaner initialization process.  It would technically always work on split screen
     * devices because PrefFragment always loads at app start, but I didn't want it to be
     * inconsistent for tablets and phones.
     *
     * @param genres the list of Genres that MovieTheater will store until MovieTheater dies
     */
    public void setGenres(List<Genre> genres) { mGenresList = genres; }

    public int getGenreListSize() { return mGenresList.size(); }


    public List<Certification> getCertifications() { return mCertifications; }

    /**
     * ditto explanation from MovieTheater.setGenres
     *
     * @param certs the list of Certifications that MovieTheater will store until it dies
     */
    public void setCertifications(List<Certification> certs) { mCertifications = certs; }

    public int getCertificationListSize() { return mCertifications.size(); }


    /**
     * Stores the list of Movies to sharedPreferences using Gson.
     *
     * @param movies the list to store
     */
    private void saveMovieList(List<Movie> movies) {
        Gson gson = new Gson();
        String json = gson.toJson(movies);

        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putString(MOVIE_LIST_SHAREDPREFS_KEY, json).commit();

        Log.i(LOGTAG, "    in saveMovieList: saving movies to sharedPrefs");
    }


    /**
     * Retrieves a list of Movies from sharePreferences using Gson.
     *
     * @return the list of movies
     */
    private List<Movie> loadMovieList() {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<List<Movie>>(){}.getType();

        List<Movie> movieList = gson.fromJson(mSharedPrefs.
                getString(MOVIE_LIST_SHAREDPREFS_KEY, ""), collectionType);

        Log.i(LOGTAG, "    in loadMovieList, just loaded movies from sharedPrefs, and list size is: " + movieList.size());

        return movieList;
    }






}
