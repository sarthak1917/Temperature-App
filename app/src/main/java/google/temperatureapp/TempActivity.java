package google.temperatureapp;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import google.temperatureapp.model.ForecastModel;
import google.temperatureapp.model.TempAdapter;


public class TempActivity extends AppCompatActivity implements LocationListener {

    private static double lati,longt;
    public static final String TAG = TempActivity.class.getSimpleName();
    private static final String FORECAST_URL = "https://api.forecast.io/forecast/"
            + "27974c4bc33201748eaf542a6769c3b7/"
            +lati+","+longt;
            //+ "19.0176" + ","
            //+ "72.8561";

    private String countryName;
    private String localityName;

    private Views mViews;
     LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp);
        mViews = new Views();

        locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, this);

    }

    private void getForecast() {
        if (isNetworkAvailable()) {

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(FORECAST_URL)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    displayErrorMessage();
                }

                @Override
                public void onResponse(final Response response) throws IOException {
                    final String responseData = response.isSuccessful() ? response.body().string() : null;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            parseResponseData(responseData);
                        }
                    });
                }
            });
        } else {
            Toast.makeText(this, "Network Error :-(", Toast.LENGTH_LONG).show();
        }
    }

    private void parseResponseData(String responseData) {
        if (!TextUtils.isEmpty(responseData)) {
            final ForecastModel forecastModel = new GsonBuilder().create().fromJson(responseData, ForecastModel.class);
            updateDisplay(forecastModel);
        } else {
            displayErrorMessage();
        }
    }

    private void updateDisplay(ForecastModel forecastModel) {
        mViews.timezoneTextView.setText(countryName+"/"+localityName);
        mViews.currentTimeTextView.setText(getDate(forecastModel.currently.time));
        mViews.hourlySummaryTextView.setText(forecastModel.hourly.summary);
        mViews.currentSummaryTextView.setText(forecastModel.currently.summary);
        mViews.currentTemperatureTextView.setText(forecastModel.currently.temperature);
        mViews.currentHumidityTextView.setText(forecastModel.currently.humidity);

        mViews.summaryContainerView.setVisibility(View.VISIBLE);
        mViews.temperatureContainerView.setVisibility(View.VISIBLE);
        mViews.humidityContainerView.setVisibility(View.VISIBLE);
        mViews.loadingViewGroup.setVisibility(View.GONE);

        TempAdapter tempAdapter = new TempAdapter(this, forecastModel.hourly.data);

        mViews.hourlyForecastListView.setAdapter(tempAdapter);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }

        return isAvailable;
    }

    private void displayErrorMessage() {
        Toast.makeText(this, "Error fetching whether information!", Toast.LENGTH_LONG).show();
    }

    private  void geocoder(){

    }

    @Override
    protected void onPause() {

        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        lati=location.getLatitude();
        longt=location.getLongitude();

      // Toast.makeText(this,lati+","+longt,Toast.LENGTH_LONG).show();

        Geocoder coder=new Geocoder(this);

        try {
            ArrayList<Address> list=(ArrayList<Address>)coder.getFromLocation(lati, longt, 2);
            if(list!=null && list.size()>0){
                Address adr=list.get(0);
                countryName =adr.getCountryName();
                localityName=adr.getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        getForecast();

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    class Views {
        final TextView timezoneTextView;
        final TextView currentTimeTextView;
        final TextView hourlySummaryTextView;
        final TextView currentSummaryTextView;
        final TextView currentTemperatureTextView;
        final TextView currentHumidityTextView;
        final ListView hourlyForecastListView;
        final ViewGroup summaryContainerView;
        final ViewGroup temperatureContainerView;
        final ViewGroup humidityContainerView;
        final ViewGroup loadingViewGroup;

        public Views() {
            timezoneTextView = (TextView) findViewById(R.id.timezoneTextView);
            currentTimeTextView = (TextView) findViewById(R.id.currentTimeTextView);
            hourlySummaryTextView = (TextView) findViewById(R.id.hourlySummaryTextView);
            currentSummaryTextView = (TextView) findViewById(R.id.currentSummaryTextView);
            currentTemperatureTextView = (TextView) findViewById(R.id.currentTemperatureTextView);
            currentHumidityTextView = (TextView) findViewById(R.id.currentHumidityTextView);
            hourlyForecastListView = (ListView) findViewById(R.id.hourlyForecastListView);
            summaryContainerView = (ViewGroup) findViewById(R.id.summaryContainerView);
            temperatureContainerView = (ViewGroup) findViewById(R.id.temperatureContainerView);
            humidityContainerView = (ViewGroup) findViewById(R.id.humidityContainerView);
            loadingViewGroup = (ViewGroup) findViewById(R.id.loadingViewGroup);
        }
    }

    private String getDate(long date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
        return simpleDateFormat.format(date);
    }
}
