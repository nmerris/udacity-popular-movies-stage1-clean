package com.nate.popmoviess1;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Used by ansync tasks to fetch movie metadata in json format.  The json is parsed and returned to
 * the caller.  The data is manipulated here as needed: for example, the list of genres that are
 * fetched have the 'Foreign' genre stripped out because I only designed this app for US movies.  Thus,
 * this class should probably not be used in other apps because it does more than simply fetch
 * moviedb json.
 *
 * @author Nathan Merris
 */
public class TheMovieDbFetcher {
    private final String LOGTAG = SingleFragmentActivity.N8LOG + getClass().getSimpleName();

    private Context mContext; // used to retrieve String resources for API queries

    public TheMovieDbFetcher(Context context) { mContext = context; }


    // boilerplate networking code taken from Big Nerd Ranch Android Programming, 2nd ed
    // use getUrlBytes when downloading pics or other non-string data
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }


    // returns the URL fetch as a string, use when parsing json with async tasks
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }


    /**
     * Fetches json data based on input paramaters provided.  The json is parsed and packed into
     * Movie objects via parseMovies, and then stored in a List.  As of stage 1 of this project, all these params
     * are being read in from sharedPrefs, but in stage 2 I want everything to be in MovieTheater.
     * It would be less confusing if everything was in MovieTheater.  I ran into trouble when I
     * realized that Android doesn't write values to sharePrefs until after onPreferenceChange
     * returns, so I just wrote them all in myself, but I think that's kind of odd and I don't like it.
     *
     * @param cert the Certification used by themoviedb API to search by a given cert (like 'G', 'PG', etc),
     *             pass in 'Any Rating' to fetch movies with any certification
     * @param releaseDate the release date to search by, use four digits, like '1989', this will be ignored
     *                    if querySpecificYear is <code>false</code>
     * @param genreId the moviedb numerical genre id to search by, pass in -1 to search by 'Any Genre'
     * @param sortby the moviedb sortby parameter, this is required, although the call will not fail
     *               if ignored, but the resulting json will not be what you are expecting
     * @param querySpecificYear if <code>true</code>, must also provide a releaseDate, otherwise all
     *                          years will be searched
     * @return an updated list of Movies ready for the MovieTheater
     * @see Movie
     * @see TheMovieDbFetcher#parseMovies(List, JSONObject)
     */
    public List<Movie> fetchMovies(String cert, String releaseDate,
                                   int genreId, String sortby, boolean querySpecificYear) {

        List<Movie> movies = new ArrayList<>();

        try { // build the URL for themoviedb GET for 'discover movies'

            Uri.Builder builder = new Uri.Builder();

            // build a URL to send to themoviedb
            builder.scheme("https")
                    .authority("api.themoviedb.org")
                    .appendPath("3")
                    .appendPath("discover")
                    .appendPath("movie") // https://api.themoviedb.org/3/discover/movie
                    .appendQueryParameter("certification_country", "US"); // US movies only

            // when cert country is specificed, API also then requires a cert or a 'less than or equal to cert'
            if(!cert.equals(mContext.getString(R.string
                    .themoviedb_any_certification_filter_name_value))) {
                // only query for movies with user's selected certification
                builder.appendQueryParameter("certification", cert);
            }
            else {
                // if user has 'Any Rating' selected, search them all
                // could add parental lockout features
                builder.appendQueryParameter("certification.lte", "R");
            }

            if(querySpecificYear) { // year toggle switch is ON (true)
                // query for movies made only in the year the user has selected
                // could add a double sliding scale to be able to select a range of years
                builder.appendQueryParameter("primary_release_year", releaseDate);
            }

            if(genreId != -1) { // -1 means no genre selected
                // query for movies only with the genre the user has selected
                // could change to check box pref to be able to select multiple genres
                builder.appendQueryParameter("with_genres", String.valueOf(genreId));
            }

            // if you don't specify a min number of votes, you end up with really bogus
            // results, esp when querying by highest rated, because even a single vote of
            // 10/10 for some oddball movie will be returned..
            if(cert.equals("NC-17")) // there are few NC-17 movies, so lower min vote count
                builder.appendQueryParameter("vote_count.gte", "15");
            else // arbitrarily set the min num votes for all other
                builder.appendQueryParameter("vote_count.gte", "20");

            // every query will have a sort by parameter
            builder.appendQueryParameter("sort_by", sortby);

            // every query will have an API key
            builder.appendQueryParameter("api_key",
                mContext.getResources().getString(R.string.themoviedb_api_key));

            String url = builder.build().toString();
            Log.i(LOGTAG, "just built URL: " + url);

            String jsonString = getUrlString(url); // call getUrlString, which will query themoviedb API
            //Log.i(LOGTAG, "  and Received JSON: " + jsonString);

            JSONObject jsonBody = new JSONObject(jsonString); // convert the returned data to a JSON object

            parseMovies(movies, jsonBody); // parse it and fill movies List

            Log.i(LOGTAG, "  num movies after TheMovieDbFetcher.fetchMovies: " + movies.size());

        } catch (IOException ioe) {
            Log.e(LOGTAG, "Failed to fetch items", ioe);
        } catch (JSONException je) {
            Log.e(LOGTAG, "Failed to parse JSON", je);
        }

        return movies;

    }


    /**
     * Converts a json body to a list of Movies using Gson.  It is critical that Movie contains
     * fields that match the json precisely.
     *
     * @param movies the list of Movies
     * @param jsonBody the json to parse
     * @see Movie
     * @see TheMovieDbFetcher#parseMovies(List, JSONObject)
     */
    private void parseMovies(List<Movie> movies, JSONObject jsonBody)
            throws /*IOException,*/ JSONException {

        JSONArray moviesJsonArray = jsonBody.getJSONArray("results"); // get movies array
        Gson gson = new Gson();

        for (int i = 0; i < moviesJsonArray.length(); i++) {
            //Log.i(LOGTAG, "  inside parseMovies, parsing move# " + i);

            String str = moviesJsonArray.getJSONObject(i).toString();

            Movie movieObj = gson.fromJson(str, Movie.class); // create the Movie object

            movies.add(movieObj); // add the just created object to the List
            //Log.i(LOGTAG, "just put genre-id: " + certificationJsonObject.getInt("id") + ", and genre-name: " + certificationJsonObject.getString("name"));

        }

    }


    /**
     * Fetches all of the available genres from themoviedb.  The resulting json body is passed to
     * parseGenres, which converts it to a list of Genre objects.  That list is then returned to caller.
     *
     * @return the list of most of the available moviedb genres user can filter by
     * @see com.nate.popmoviess1.MovieTheater.Genre
     * @see TheMovieDbFetcher#parseGenres(List, JSONObject)
     */
    public List<MovieTheater.Genre> fetchAvailableGenres() {

        List<MovieTheater.Genre> availableGenres = new ArrayList<>();

        try { // build the URL for themoviedb GET for genres

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority("api.themoviedb.org")
                    .appendPath("3")
                    .appendPath("genre")
                    .appendPath("movie")
                    .appendPath("list") // https://api.themoviedb.org/3/genre/movie/list
                    .appendQueryParameter("api_key",
                            mContext.getResources().getString(R.string.themoviedb_api_key));

            String url = builder.build().toString();
            String jsonString = getUrlString(url); // call getUrlString, which will query themoviedb API
            //Log.i(LOGTAG, "Received JSON: " + jsonString);

            JSONObject jsonBody = new JSONObject(jsonString); // convert the returned data to a JSON object

            // parseGenres fills availableGenres, all it needs is a reference to it and a JSONObject
            parseGenres(availableGenres, jsonBody);

        } catch (IOException ioe) {
            Log.e(LOGTAG, "Failed to fetch items", ioe);
        } catch (JSONException je) {
            Log.e(LOGTAG, "Failed to parse JSON", je);
        }

        return availableGenres;
    }

    /**
     * Takes a json body containing all available themoviedb genres, parses it, and packages it all
     * into a list of Genre objects.  A Genre object representing 'Any Genre' is created here.  The
     * moviedb genre for 'Foreign' is stripped out here.
     *
     * @param availableGenres the list of Genres that this method should put the data after parsing it
     * @param jsonBody the unparsed json body to devour
     */
    private void parseGenres(List<MovieTheater.Genre> availableGenres, JSONObject jsonBody)
            throws IOException, JSONException {

        // themoviedb doesn't give a 'search all genres' json object, so make one here
        MovieTheater.Genre anyGenreObject = new MovieTheater.Genre(
                -1, // -1 is the id for 'Any Genre'
                mContext.getString(R.string.themoviedb_any_genre_filter_name_value));
        availableGenres.add(anyGenreObject);


        JSONArray genresJsonArray = jsonBody.getJSONArray("genres");

        for (int i = 0; i < genresJsonArray.length(); i++) {
            // get a single moviedb genre JSON object from jsonBody
            JSONObject genreJsonObject = genresJsonArray.getJSONObject(i);
            // create a new MovieTheater.Genre object and provide it with a genre id and name
            MovieTheater.Genre genreObject = new MovieTheater.Genre(
                    genreJsonObject.getInt("id"),
                    genreJsonObject.getString("name"));

            // I'm only searching for US movies in this app, so don't add the Foreign genre to the list
            if(genreObject.name.equals("Foreign"))
                continue;

            // add the just created object to the List
            availableGenres.add(genreObject);
            //Log.i(LOGTAG, "just put genre-id: " + genreJsonObject.getInt("id") + ", and genre-name: " + genreJsonObject.getString("name"));
        }


        // UDACITY REVIEWER, READ THIS AND TEST IF YOU WISH:
        // to test what happens if themoviedb were to change a genre name, AND it happened
        // to be the same genre that the user had currently selected in their sharedPrefs,
        // uncomment the following block of code, rerun the app, select one of the bogus genre names
        // that are now present, kill the app, recomment the lines, and run the app again..
        // the genre pref should default back to 'Any Rating'
        // if the user had any other pref selected, their selection is retained and the list
        // simply updates with the new genres from themoviedb
        
/*
        MovieTheater.Genre testGenreObj1 = new MovieTheater.Genre(
                "test moviedb id",
                "test if themoviedb changed this genre name");
        MovieTheater.Genre testGenreObj2 = new MovieTheater.Genre(
                "test moviedb id",
                "test if themoviedb changed this other genre name");
        availableGenres.add(testGenreObj1);
        availableGenres.add(testGenreObj2);
*/

        
    }


    /**
     * Fetches all of the available certifications from themoviedb.  The resulting json body is passed to
     * parseCertifications, which converts it to a list of Certification objects.  That list is then returned to caller.
     *
     * @return the list of all available certs that user can filter by
     * @see com.nate.popmoviess1.MovieTheater.Certification
     * @see TheMovieDbFetcher#parseCertifications(List, JSONObject)
     */
    public List<MovieTheater.Certification> fetchAvailableCertifications() {

        List<MovieTheater.Certification> availableCertifications = new ArrayList<>();

        try { // build the URL for themoviedb GET for certifications

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority("api.themoviedb.org")
                    .appendPath("3")
                    .appendPath("certification")
                    .appendPath("movie")
                    .appendPath("list") // https://api.themoviedb.org/3/certification/movie/list
                    .appendQueryParameter("api_key",
                            mContext.getResources().getString(R.string.themoviedb_api_key));

            String url = builder.build().toString();
            String jsonString = getUrlString(url); // call getUrlString, which will query themoviedb API
            //Log.i(LOGTAG, "Received JSON: " + jsonString);

            JSONObject jsonBody = new JSONObject(jsonString); // convert the returned data to a JSON object

            // parseGenres fills availableGenres, all it needs is a reference to it and a JSONObject
            parseCertifications(availableCertifications, jsonBody);

        } catch (IOException ioe) {
            Log.e(LOGTAG, "Failed to fetch items", ioe);
        } catch (JSONException je) {
            Log.e(LOGTAG, "Failed to parse JSON", je);
        }

        return availableCertifications;
    }


    private void parseCertifications(List<MovieTheater.Certification> availableCertifications, JSONObject jsonBody)
            throws IOException, JSONException {


        // themoviedb doesn't give a 'search all certifications' json object, so make one here
        MovieTheater.Certification anyCertificationObject = new MovieTheater.Certification(
                mContext.getString(R.string.themoviedb_any_certification_filter_name_value),
                mContext.getString(R.string.themoviedb_any_certification_filter_meaning_value),
                -1); // sort order is always -1 for 'Any Rating' b/c themoviedb starts at 0
        // oddly, themoviedb starts at 1 for other countries certs lists
        availableCertifications.add(anyCertificationObject);


        JSONArray certificationsJsonArray = jsonBody
                .getJSONObject("certifications").getJSONArray("US"); // get certs for USA

        for (int i = 0; i < certificationsJsonArray.length(); i++) {
            // get a single moviedb genre JSON object from jsonBody
            JSONObject certificationJsonObject = certificationsJsonArray.getJSONObject(i);
            // create a new MovieTheater.Certification object and provide it with a genre id and name
            MovieTheater.Certification certificationObject = new MovieTheater.Certification(
                    certificationJsonObject.getString("certification"),
                    certificationJsonObject.getString("meaning"),
                    Integer.valueOf(certificationJsonObject.getString("order")));

            // add the just created object to the List
            availableCertifications.add(certificationObject);
            //Log.i(LOGTAG, "just put genre-id: " + certificationJsonObject.getInt("id") + ", and genre-name: " + certificationJsonObject.getString("name"));

        }

        
        // UDACITY REVIEWER, READ THIS AND TEST IF YOU WISH:
        // to test what happens if themoviedb were to change a certification name, AND it happened
        // to be the same cert that the user had currently selected in their sharedPrefs,
        // uncomment the following block of code, rerun the app, select one of the bogus cert names
        // that are now present, kill the app, recomment the lines, and run the app again..
        // the certification pref should default back to 'Any Rating'
        // if the user had any other pref selected, their selection is retained and the list
        // simply updates with the new certs from themoviedb
        

/*
        MovieTheater.Certification testCertObj1 = new MovieTheater.Certification(
                "test if themoviedb changed this certification name",
                "test moviedb 'meaning'", 98);
        MovieTheater.Certification testCertObj2 = new MovieTheater.Certification(
                "test if themoviedb changed this other cert name",
                "test moviedb 'meaning'", 99);
        availableCertifications.add(testCertObj1);
        availableCertifications.add(testCertObj2);
*/


        Collections.sort(availableCertifications); // sort by 'order'
        // the List of Certifications should now be ready to go and sorted for fetchAvailableCertifications to return
    }


}
