package com.example.google.playservices.placecomplete;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener,OnMapReadyCallback {

    private GoogleMap mMap;

    private Context context;
    String lang;
    String location1;
    String location2;
    JSONArray stepsArray;

    static String LANGUAGE_SPANISH = "es";
    static String LANGUAGE_ENGLISH = "en";
    static String LANGUAGE_FRENCH = "fr";
    static String LANGUAGE_GERMAN = "de";
    static String LANGUAGE_CHINESE_SIMPLIFIED = "zh-CN";
    static String LANGUAGE_CHINESE_TRADITIONAL = "zh-TW";

    static String TRANSPORT_DRIVING = "driving";
    static String TRANSPORT_WALKING = "walking";
    static String TRANSPORT_BIKE = "bicycling";
    static String TRANSPORT_TRANSIT = "transit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        context = MapsActivity.this;
        Bundle bundle = getIntent().getExtras();
        location1 = bundle.getString("source");
        location2 = bundle.getString("dest");

    /*
        // Add a marker in Sydney, Australia, and move the camera.
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        Polyline line = mMap.addPolyline(new PolylineOptions()
                .add(new LatLng(-37.81319, 144.96298), new LatLng(-31.95285, 115.85734))
                .width(25)
                .color(Color.BLUE)
                .geodesic(true));
                */

        //LatLng sydney = new LatLng(-37.81319, 144.96298);
        //LatLng perth = new LatLng(-31.95285, 115.85734);

        if(Geocoder.isPresent()){
            try {

                Geocoder gc = new Geocoder(this);
                List<Address> addresses1= gc.getFromLocationName(location1, 5); // get the found Address Objects
                List<Address> addresses2= gc.getFromLocationName(location2, 5);


                ArrayList<LatLng> ll = new ArrayList<LatLng>(addresses1.size()+addresses2.size()); // A list to save the coordinates if they are available
                for(Address a : addresses1){
                    if(a.hasLatitude() && a.hasLongitude()){
                        ll.add(new LatLng(a.getLatitude(), a.getLongitude()));
                    }
                }
                for(Address a : addresses2){
                    if(a.hasLatitude() && a.hasLongitude()){
                        ll.add(new LatLng(a.getLatitude(), a.getLongitude()));
                    }
                }
                drawRoute(mMap,context,ll,LANGUAGE_ENGLISH,true);
            } catch (IOException e) {
                // handle the exception
            }
        }
        //ArrayList<LatLng> pts = new ArrayList<>();
       // pts.add(sydney);
       // pts.add(perth);
       // drawRoute(mMap,context,pts,LANGUAGE_ENGLISH,true);

        mMap.setOnMarkerClickListener(this);

    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        String name="";
        int duration=0;
        if(Geocoder.isPresent()) {
            try {

                Geocoder gc = new Geocoder(this);
                List<Address> addresses = gc.getFromLocation(marker.getPosition().latitude, marker.getPosition().longitude, 1);
                Address obj = addresses.get(0);
                name = obj.getLocality();
                LatLng clickedLatLng = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
            try {
                for (int i = 0; i < stepsArray.length(); i++) {
                    Step step = new Step(stepsArray.getJSONObject(i));
                    String s[] = step.duration.split(" ");
                    duration = duration + Integer.parseInt(s[0]);
                    if(clickedLatLng.equals(step.location)){
                        break;
                    }

                }
            }
            catch (JSONException e) {

            }
            } catch (IOException e) {
                // handle the exception
            }
        }

        String markerLat = String.valueOf(marker.getPosition().latitude);
        String markerLng = String.valueOf(marker.getPosition().longitude);
        String markerLatLng = markerLat+","+markerLng;

        Intent intent = new Intent(MapsActivity.this, WeatherAndEventsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putInt("duration",duration);
        bundle.putString("markerLatLng", markerLatLng);
        intent.putExtras(bundle);
        startActivity(intent);
        return false;
    }

        public boolean drawRoute(GoogleMap map, Context c, ArrayList<LatLng> points, boolean withIndications, String language, boolean optimize)
    {
        mMap = map;
        context = c;
        lang = language;
        if(points.size() == 2)
        {
            String url = makeURL(points.get(0).latitude,points.get(0).longitude,points.get(1).latitude,points.get(1).longitude,"driving");
            new connectAsyncTask(url,withIndications).execute();
            return true;
        }
        else if(points.size() > 2)
        {
            String url = makeURL(points,"driving",optimize);
            new connectAsyncTask(url,withIndications).execute();
            return true;
        }

        return false;

    }

    public boolean drawRoute(GoogleMap map, Context c, ArrayList<LatLng> points, String language, boolean optimize)
    {
        mMap = map;
        context = c;
        lang = language;
        if(points.size() == 2)
        {
            String url = makeURL(points.get(0).latitude,points.get(0).longitude,points.get(1).latitude,points.get(1).longitude,"driving");
            new connectAsyncTask(url,true).execute();
            return true;
        }
        else if(points.size() > 2)
        {
            String url = makeURL(points,"driving",optimize);
            new connectAsyncTask(url,true).execute();
            return true;
        }

        return false;

    }


    public boolean drawRoute(GoogleMap map, Context c, ArrayList<LatLng> points, String mode, boolean withIndications, String language, boolean optimize)
    {
        mMap = map;
        context = c;
        lang = language;
        if(points.size() == 2)
        {
            String url = makeURL(points.get(0).latitude,points.get(0).longitude,points.get(1).latitude,points.get(1).longitude,mode);
            new connectAsyncTask(url,withIndications).execute();
            return true;
        }
        else if(points.size() > 2)
        {
            String url = makeURL(points,mode,optimize);
            new connectAsyncTask(url,withIndications).execute();
            return true;
        }

        return false;

    }

    //


    public void drawRoute(GoogleMap map, Context c, LatLng source, LatLng dest, boolean withIndications, String language)
    {
        mMap = map;
        context = c;

        String url = makeURL(source.latitude,source.longitude,dest.latitude,dest.longitude,"driving");
        new connectAsyncTask(url,withIndications).execute();
        lang = language;

    }


    public void drawRoute(GoogleMap map, Context c, LatLng source, LatLng dest, String language)
    {
        mMap = map;
        context = c;

        String url = makeURL(source.latitude,source.longitude,dest.latitude,dest.longitude,"driving");
        new connectAsyncTask(url,false).execute();
        lang = language;

    }


    public void drawRoute(GoogleMap map, Context c, LatLng source, LatLng dest, String mode, boolean withIndications, String language)
    {
        mMap = map;
        context = c;

        String url = makeURL(source.latitude,source.longitude,dest.latitude,dest.longitude,mode);
        new connectAsyncTask(url,withIndications).execute();
        lang = language;

    }

    private String makeURL (ArrayList<LatLng> points, String mode, boolean optimize){
        StringBuilder urlString = new StringBuilder();

        if(mode == null)
            mode = "driving";

        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append( points.get(0).latitude);
        urlString.append(',');
        urlString.append(points.get(0).longitude);
        urlString.append("&destination=");
        urlString.append(points.get(points.size()-1).latitude);
        urlString.append(',');
        urlString.append(points.get(points.size()-1).longitude);

        urlString.append("&waypoints=");
        if(optimize)
            urlString.append("optimize:true|");
        urlString.append( points.get(1).latitude);
        urlString.append(',');
        urlString.append(points.get(1).longitude);

        for(int i=2;i<points.size()-1;i++)
        {
            urlString.append('|');
            urlString.append( points.get(i).latitude);
            urlString.append(',');
            urlString.append(points.get(i).longitude);
        }


        urlString.append("&sensor=true&mode="+mode);


        return urlString.toString();
    }

    private String makeURL (double sourcelat, double sourcelog, double destlat, double destlog,String mode){
        StringBuilder urlString = new StringBuilder();

        if(mode == null)
            mode = "driving";

        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString
                .append(Double.toString( sourcelog));
        urlString.append("&destination=");// to
        urlString
                .append(Double.toString( destlat));
        urlString.append(",");
        urlString.append(Double.toString( destlog));
        urlString.append("&sensor=false&mode="+mode+"&alternatives=true&language="+lang);
        return urlString.toString();
    }




    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }




    private class connectAsyncTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;
        String url;
        boolean steps;
        connectAsyncTask(String urlPass, boolean withSteps){
            url = urlPass;
            steps = withSteps;

        }
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Fetching route, Please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }
        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.hide();
            if(result!=null){
                drawPath(result,steps);
            }
        }
    }

    private void drawPath(String  result, boolean withSteps) {

        try {
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);

           /* Polyline line = mMap.addPolyline(new PolylineOptions()
                    .addAll(list)
                    .width(12)
                    .color(Color.parseColor("#05b1fb"))//Google maps blue color
                    .geodesic(true)
            );*/

            for(int z = 0; z<list.size()-1;z++){
                LatLng src= list.get(z);
                LatLng dest= list.get(z+1);
                Polyline line = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude))
                        .width(10)
                        .color(Color.parseColor("#05b1fb")).geodesic(true));

            }
            mMap.addMarker(new MarkerOptions().position(list.get(0)).title(location1));
            mMap.addMarker(new MarkerOptions().position(list.get(list.size()-1)).title(location2));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(list.get(0),5));


            if(withSteps)
            {
                JSONArray arrayLegs = routes.getJSONArray("legs");
                JSONObject legs = arrayLegs.getJSONObject(0);
                stepsArray = legs.getJSONArray("steps");
                //put initial point

                for(int i=stepsArray.length()/10;i<stepsArray.length();i=i+(stepsArray.length()/10))
                {
                    Step step = new Step(stepsArray.getJSONObject(i));
                    mMap.addMarker(new MarkerOptions()
                            .position(step.location)
                            .title(step.distance)
                            .snippet(step.instructions)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                }

                mMap.addMarker(new MarkerOptions().position(list.get(0)).title(location1));
                mMap.addMarker(new MarkerOptions().position(list.get(list.size()-1)).title(location2));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(list.get(0),5));

            }

        }
        catch (JSONException e) {

        }
    }


    /**
     * Class that represent every step of the directions. It store distance, location and instructions
     */
    private class Step
    {
        public String distance;
        public String duration;
        public LatLng location;
        public String instructions;

        Step(JSONObject stepJSON)
        {
            JSONObject startLocation;
            try {

                distance = stepJSON.getJSONObject("distance").getString("text");
                duration = stepJSON.getJSONObject("duration").getString("text");
                startLocation = stepJSON.getJSONObject("start_location");
                location = new LatLng(startLocation.getDouble("lat"),startLocation.getDouble("lng"));
                try {
                    instructions = URLDecoder.decode(Html.fromHtml(stepJSON.getString("html_instructions")).toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                };

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }




}
