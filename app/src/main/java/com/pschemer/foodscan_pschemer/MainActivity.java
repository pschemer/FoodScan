package com.pschemer.foodscan_pschemer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.RED;
import static android.graphics.Color.GREEN;

public class MainActivity extends AppCompatActivity {

    private static TextView resultTextView;
    private static Map<String, CheckBox> user_health_check;
    private static ArrayList<String> user_health_list;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if we have Camera permissions. If not, request permissions.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 100);

        // Init UI elements
        user_health_list = new ArrayList<>();
        user_health_check = new HashMap<>();
        user_health_check.put("VEGAN", (CheckBox) findViewById(R.id.VEGAN));
        user_health_check.put("VEGETARIAN", (CheckBox) findViewById(R.id.VEGETARIAN));
        user_health_check.put("PESCATARIAN", (CheckBox) findViewById(R.id.PESCATARIAN));
        user_health_check.put("KOSHER", (CheckBox) findViewById(R.id.KOSHER));
        user_health_check.put("PALEO", (CheckBox) findViewById(R.id.PALEO));
        user_health_check.put("RED_MEAT_FREE", (CheckBox) findViewById(R.id.RED_MEAT_FREE));
        user_health_check.put("PORK_FREE", (CheckBox) findViewById(R.id.PORK_FREE));
        user_health_check.put("SHELLFISH_FREE", (CheckBox) findViewById(R.id.SHELLFISH_FREE));
        user_health_check.put("LOW_SUGAR", (CheckBox) findViewById(R.id.LOW_SUGAR));
        user_health_check.put("GLUTEN_FREE", (CheckBox) findViewById(R.id.GLUTEN_FREE));
        user_health_check.put("WHEAT_FREE", (CheckBox) findViewById(R.id.WHEAT_FREE));
        user_health_check.put("LUPINE_FREE", (CheckBox) findViewById(R.id.LUPINE_FREE));
        user_health_check.put("PEANUT_FREE", (CheckBox) findViewById(R.id.PEANUT_FREE));
        user_health_check.put("TREE_NUT_FREE", (CheckBox) findViewById(R.id.TREE_NUT_FREE));
        user_health_check.put("DAIRY_FREE", (CheckBox) findViewById(R.id.DAIRY_FREE));
        user_health_check.put("SOY_FREE", (CheckBox) findViewById(R.id.SOY_FREE));
        resultTextView = (TextView) findViewById(R.id.result_text);
        String initResultText = "Ready for UPC barcode scan.";
        resultTextView.setText(initResultText);
        Button scan_btn = (Button) findViewById(R.id.btn_scan);

        // SharedPreferences to save state of checkboxes between sessions
        SharedPreferences settings = getSharedPreferences("foodscan_settings", 0);
        editor = settings.edit();
        // Load preferences
        for (String s : user_health_check.keySet()) {
            CheckBox cb = user_health_check.get(s);
            if (cb != null) {
                cb.setChecked(settings.getBoolean(s, false));
                if (cb.isChecked())
                    user_health_list.add(s);
            }
        }

        // nav to ScanCode activity
        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear label color status from previous scan
                resetLabelColors();
                startActivity(new Intent(getApplicationContext(), ScanCode.class));
            }
        });
    }

    public void onCheckboxClicked(View v) {
        CheckBox cb = (CheckBox) v;
        boolean checked = cb.isChecked();
        String label = "";
        // Get health_label associated with this CheckBox
        for (Map.Entry<String, CheckBox> entry : user_health_check.entrySet()) {
            if (entry.getValue().equals(v)) {
                label = entry.getKey();
                break;
            }
        }
        // Save preference
        editor.putBoolean(label, checked);
        editor.commit();
        // Remove health label from user list and reset color
        if(!checked) {
            user_health_list.remove(label);
            cb.setTextColor(BLACK);
        }
        // Add health label to user list
        else {
            user_health_list.add(label);
        }
    }

    public static void checkHealthLabels(List<String> res) {
        String name = res.get(0);
        // Check scan returned with valid data
        if(name == null || name.equals("error"))  {
            name = "Item not found";
            resultTextView.setText(name);
            resultTextView.setTextColor(RED);
            return;
        }
        // Display name of food item
        resultTextView.setText(name);
        resultTextView.setTextColor(GREEN);
        // color matching/conflicting labels with green/red
        CheckBox cb;

        for (String s : user_health_list) {
            cb = user_health_check.get(s);
            if (cb != null) {
                if (res.contains(s)) {
                    // user and food labels match -> GREEN
                    cb.setTextColor(GREEN);
                } else {
                    // user and food labels do not match -> RED
                    cb.setTextColor(RED);
                }
            }
            else { Log.v("CHECK HEALTH", "-------- FAILED TO FIND CheckBox!"); }
        }

    }

    private void resetLabelColors() {
        resultTextView.setTextColor(BLACK);
        for (CheckBox cb : user_health_check.values())
            cb.setTextColor(BLACK);
    }
}
