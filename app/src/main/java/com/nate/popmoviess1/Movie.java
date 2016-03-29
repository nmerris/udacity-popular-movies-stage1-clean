package com.nate.popmoviess1;

import org.json.JSONObject;

import java.util.List;

/**
 * Contains relevant data to contain a single movie's metadata.
 * The members must exactly match themoviedb API json return data because
 * this app uses Gson to parse it.
 *
 * @author Nathan Merris
 * @see TheMovieDbFetcher#parseMovies(List, JSONObject)
 */
@SuppressWarnings("unused")
public class Movie {

    private String poster_path;        // Movie poser image path
    public boolean adult;              // adult movie or not? themoviedb defaults to false
    public String overview;            // aka a plot synopsis
    public String release_date;        // year the movie was released

    // all the genre id's associated with this movie, Integer[] so you can get it's length elsewhere
    public Integer[] genre_ids = new Integer[]{};
    public int id;                     // themoviedb movie ID number
    public String original_title;
    public String original_language;
    public String title;
    private String backdrop_path;       // image that can be used as a larger backdroop, not same as poster
    public float popularity;
    public int vote_count;
    public boolean video;               // does the movie have a video that can be linked to?
    public float vote_average;


    // you can actually query themoviedb 'configuration' endpoint to get lists of things like
    // available poster and backdrop sizes, changes, etc

    public String getPosterUrl(/*String size*/) {
        // w185 is the size, could add parameter to select larger size for tablets
        // w342 is next size up, need to do this for stage 2
        String url = "https://image.tmdb.org/t/p/" + "w185/" + poster_path;
        return url;
    }

    public String getBackdropUrl() {
        // w780 is the size, it's second from smallest as of 3.19.16
        String url = "https://image.tmdb.org/t/p/" + "w780/" + backdrop_path;
        return url;
    }


}
