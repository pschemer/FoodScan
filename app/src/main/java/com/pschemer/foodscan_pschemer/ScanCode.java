package com.pschemer.foodscan_pschemer;

import androidx.appcompat.app.AppCompatActivity;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.google.zxing.Result;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

public class ScanCode extends AppCompatActivity implements ZXingScannerView.ResultHandler{

    // Web endpoints
	// TODO - Temporary hard code for ID/Key redacted from public Git while development continues.
    private static final String APIGET	= "https://api.edamam.com/api/food-database/v2/parser?",
            APIPOST = "https://api.edamam.com/api/food-database/v2/nutrients?",
            ID  	= "CONTACT PSCHEMER",
            KEY 	= "CONTACT PSCHEMER";

    // DB
    final static String TABLE_NAME = "foodscans";
    final static String _ID = "_id";
    final static String UPC = "upc";
    final static String NAME = "name";
    final static String LABELS = "labels";
    final private static String [] columns = { _ID, UPC, NAME, LABELS};
    private SQLiteDatabase db = null;
    // First Item is Product Name followed by associated health labels.
    // On invalid lookout, [0] is "Error" and size is 1.private DataBaseOpenHelper dbHelper = null;
    private static String strSeparator = "__,__";
    private ArrayList<String> results;
    private String upc;
    ZXingScannerView sv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Database holds past scan information to reduce network traffic, processing, and battery power
        DataBaseOpenHelper dbHelper = new DataBaseOpenHelper(this);
        db = dbHelper.getWritableDatabase();

        sv = new ZXingScannerView(this);
        setContentView(sv);
    }

    @Override
    public void handleResult(Result result) {
        // Prepare results list
        results = new ArrayList<>();
        upc = result.getText();
        Cursor c = db.query(TABLE_NAME, columns, UPC + " = ?", new String[] {upc}, null, null, null);
        if (c.getCount() > 0) {
            // Found this UPC id in database, skipping api calls
            c.moveToFirst();
            String col;
            col = c.getString(2);
            results.add(0, col);
            Log.v(getLocalClassName(), String.format("------- FOUND IN DB : Name : %s", col));
            col = c.getString(3);
            Log.v(getLocalClassName(), String.format("------- FOUND IN DB : Labels : %s", col));
            String[] label_list = convertStringToArray(col);
            Collections.addAll(results, label_list);

            Log.v(getLocalClassName(), "------ DB sending results: " + results.toString());

            // Return to main
            c.close();
            scanComplete();
        }
        else {
            c.close();
            Log.v(getLocalClassName(), "---- NOT FOUND IN DB : PROCEED WITH API CALLS");
            // HTTP GET scan item from FOOD DB for further query in NUTRIENTS DB
            StringBuilder sb = new StringBuilder();
            sb.append(APIGET)
                    .append("upc=").append(upc)
                    .append("&app_id=").append(ID)
                    .append("&app_key=").append(KEY);
            new ScanCode.HttpsGetTask().execute(sb.toString());
        }
        // Finished, go back to main
        onBackPressed();
    }

    public static String convertArrayToString(String[] array){
        StringBuilder str = new StringBuilder();
        for (int i = 0;i<array.length; i++) {
            str.append(array[i]);
            // Do not append comma at the end of last element
            if(i<array.length-1){
                str.append(strSeparator);
            }
        }
        return str.toString();
    }
    public static String[] convertStringToArray(String str){
        return str.split(strSeparator);
    }
    @Override
    protected void onPause() {
        super.onPause();
        sv.stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sv.setResultHandler(this);
        sv.startCamera();
    }

private void onFinishGetRequest(String result) {
    Log.v(getLocalClassName(), "---------- RETURN FROM GET");
    Log.v(getLocalClassName(), result);
    if (result.equals("error")) {
        results.add(0, "error");
        // Scan commplete
        scanComplete();
        return;
    }
    try {
        JSONObject food_scan = (new JSONObject(result));
        JSONObject food = food_scan
                .getJSONArray("hints")
                .getJSONObject(0)
                .getJSONObject("food");
        String foodId = food.getString("foodId");
        String label = food.getString("label");
        results.add(0, label);

        new HttpsPostTask().execute(foodId);
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    private void onFinishPostRequest(String result) {
        Log.v(getLocalClassName(), "---------- RETURN FROM POST");
        Log.v(getLocalClassName(), result);
        try {
            JSONObject nutrient_scan = (new JSONObject(result));
            JSONArray health_labels = nutrient_scan.getJSONArray("healthLabels");
            if (health_labels != null) {
                for (int i = 0; i < health_labels.length(); i++) {
                    results.add(health_labels.getString(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Add new food to db
        List<String> label_list = results.subList(2, results.size());
        String[] labels = label_list.toArray(new String[label_list.size()]);
        String labelstr = convertArrayToString(labels);
        ContentValues values = new ContentValues();
        values.put(UPC, upc);
        values.put(NAME, results.get(0));
        values.put(LABELS, labelstr);
        long i = db.insert(TABLE_NAME, null, values);
        if (i > 0)
            Log.v(getLocalClassName(), String.format("------- DB INSERT SUCCESS\n\tUPC\t%s\n\tNAME\t%s\n\tLABELS (Size: %d)\t%s\n", upc, results.get(0), labels.length, labelstr));
        // Scan complete
        scanComplete();
    }

    /**
     * Notify MainActivity of results and quit
     */
    private void scanComplete() {
        MainActivity.checkHealthLabels(results);
    }

    /**
     * HTTP GET Request
     */
    private class HttpsGetTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Log.v(getLocalClassName(), "---------- START GET");
            StringBuffer data = new StringBuffer();
            BufferedReader br = null;
            try {
                HttpsURLConnection conn = (HttpsURLConnection) new
                        URL(params[0]).openConnection();
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String rawData;
                while ((rawData = br.readLine()) != null) {
                    data.append(rawData);
                }
            } catch (java.io.FileNotFoundException e) {
                data = new StringBuffer();
                data.append("error");
                e.printStackTrace();
            } catch (MalformedURLException e1) {e1.printStackTrace();
            } catch (Exception e1) {e1.printStackTrace();
            } finally {
                if (br != null)
                    try {  br.close();
                    } catch (IOException e) {e.printStackTrace();}
            }
            return data.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            onFinishGetRequest(result);
        }
    }

    /**
     * HTTP POST Request
     */
    private class HttpsPostTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            Log.v(getLocalClassName(), "---------- STARTING POST");
            StringBuilder response = new StringBuilder(),
                    urlBuilder = new StringBuilder();
            urlBuilder.append(APIPOST)
                    .append("app_id=").append(ID)
                    .append("&app_key=").append(KEY);
            String urlString = urlBuilder.toString();
            try {
                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; utf-8");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);

                StringBuilder sb = new StringBuilder();
                sb.append("{\"ingredients\": [{\"quantity\":1,\"measureURI\": \"http://www.edamam.com/ontologies/edamam.owl#Measure_unit\",\"foodId\": \"")
                        .append(params[0]).append("\"}]}");
                String jsonInputString = sb.toString();

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"))) {
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return response.toString();

        }

        @Override
        protected void onPostExecute(String result) {
            onFinishPostRequest(result);
        }
    }
}