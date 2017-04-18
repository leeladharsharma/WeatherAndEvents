package com.example.google.playservices.placecomplete;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;



public class WeatherAndEventsActivity extends Activity {

    private static final String API ="https://api.forecast.io/forecast/ca05a018bff6ebdd72951bb203fac735/%s";

    Typeface weatherFont;
    TextView cityField;
    TextView updatedField;
    TextView detailsField;
    TextView currentTemperatureField;
    TextView weatherIcon;
    int backgroundImageId = 0;
    int duration =0;


    String coord;
    private ImageView weatherIconImageView;

    private String eventSearchURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_and_events);

        Bundle extras = getIntent().getExtras();

        //System.out.print("LALALALA" + extras.get("CityName"));
        //Intent intent = getIntent();
        //String cityName = intent.getStringExtra("CityName");

        String cityName = extras.getString("name");
        coord = extras.getString("markerLatLng");
        duration = extras.getInt("duration");
        duration = duration*60;

        //setupNavBtn();

        //chooseCity(cityName);
        LinearLayout rl = (LinearLayout) findViewById(R.id.bglayout);
        backgroundImageId =R.drawable.waves;
        rl.setBackgroundResource(backgroundImageId);

        int alphaAmount = 30;
        rl.getBackground().setAlpha(alphaAmount);

        //spacing issue

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //declare at the top if possible
        cityField = (TextView)findViewById(R.id.city_field);
        updatedField = (TextView)findViewById(R.id.updated_field);
        //detailsField = (TextView)findViewById(R.id.details_field);
        currentTemperatureField = (TextView) findViewById(R.id.current_temperature_field);
        //weatherIcon = (TextView)findViewById(R.id.weather_icon);
        //weatherIcon.setTypeface(weatherFont);
        weatherFont = Typeface.createFromAsset(this.getAssets(), "weather.ttf");
        //weatherIcon.setText(getString(R.string.weather_sunny));
        weatherIconImageView = (ImageView)findViewById(R.id.weatherIconImageView);
        cityField.setText(cityName);

        Context myContext = this;
        //chooseCity(cityName);
        final JSONObject json = getJSON(myContext, coord);
        //Log.v("JSON:", json.toString());
        renderWeather(json);

        //Events search call
        String locationToSearch = cityName;

        //Form the event search URL
        eventSearchURL = makeURL(locationToSearch);

        //Creating Async task
        new WeatherAndEventsActivity.connectAsyncTask(eventSearchURL).execute();
    }

    private class connectAsyncTask extends AsyncTask<Void, Void, String> {

        private ProgressDialog progressDialog;
        String url;
        boolean steps;
        connectAsyncTask(String urlPass){
            url = urlPass;

        }
        /*@Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Fetching route, Please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }*/
        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //progressDialog.hide();
            if(result!=null){
                // Instantiate the JSON parser to fetch the JSON
                makeEventList(result);
            }
        }
    }
    // Forming the events URL to search
    private String makeURL (String location){
        StringBuilder urlString = new StringBuilder();

        urlString.append("http://api.evdb.com/json//events/search?password=test%40123&user=vandanak24&app_key=L7RHSzB2459qxmJz");
        urlString.append("&location=");// from
        urlString.append( location );

        return urlString.toString();
    }

    private void makeEventList(String eventsJSON){
        ArrayList<String> titleList = new ArrayList<String>();
        ArrayAdapter<String> adapter;

        //Get the list view object
        ListView eventList = (ListView)findViewById(R.id.eventList);

        //Parse JSON for events
        try{
            final JSONObject parentObject = new JSONObject(eventsJSON);
            JSONObject eventsObject = parentObject.getJSONObject("events");
            JSONArray eventArray = eventsObject.getJSONArray("event");

            for(int i =0; i<eventArray.length();i++){

                JSONObject finalObject = eventArray.getJSONObject(i);
                String title = finalObject.getString("title");
                String url = finalObject.getString("url");

                titleList.add(title);
            }
            adapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.txtitem, titleList);
            eventList.setAdapter(adapter);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void renderWeather(JSONObject json){
        try {
            String icon="";
            String temperature="";
            int currentime = Integer.parseInt(json.getJSONObject("currently").getString("time"));
            int cityTime = currentime + duration;
            JSONObject hourObject = json.getJSONObject("hourly");
            JSONArray dataArray = hourObject.getJSONArray("data");

            for (int i = 0; i < dataArray.length(); i++) {
                int apiCityTime = Integer.parseInt(dataArray.getJSONObject(i).getString("time"));
                if(cityTime < apiCityTime){
                    icon = dataArray.getJSONObject(i-1).getString("icon");
                    temperature = dataArray.getJSONObject(i-1).getString("temperature") + " \u00b0 F";
                    break;
                }

            }


            if(icon.contains("-")){
                icon = icon.replace('-','_');
            }
            int resource = getResources().getIdentifier("drawable/"+icon, null, getPackageName());
            Drawable weatherIconDrawable;
            weatherIconDrawable = getResources().getDrawable(resource);
            weatherIconImageView.setImageDrawable(weatherIconDrawable);



            currentTemperatureField.setText(temperature);
            updatedField.setText(

                    // "SUMMARY OF WEEK  : " +
                    json.getJSONObject("hourly").getString("summary")
                    // +      "\nTIME ZONE  : " + json.getString("timezone")
            );
            // weatherIcon.setText(getString(R.string.weather_sunny));


        }catch(Exception e){
            Log.e("SimpleWeather", "One or more fields not found in the JSON data");
        }
    }

    public static JSONObject getJSON(Context context, String coord){
        try {
            //coord = "40.7127,-74.0059";//debug
            URL url = new URL(String.format((API), coord));

            HttpURLConnection connection =
                    (HttpURLConnection)url.openConnection();
            connection.getInputStream();

            System.out.print("CONNECTION:::" + connection.getInputStream());

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            System.out.print("url:::");
            StringBuffer json = new StringBuffer(1024);
            String tmp="";
            while((tmp=reader.readLine())!=null)
                json.append(tmp).append("\n");
            reader.close();

            JSONObject data = new JSONObject(json.toString());

            return data;
        }catch(Exception e){
            e.printStackTrace();

            return null;
        }
    }
}
