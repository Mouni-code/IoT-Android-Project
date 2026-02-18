package com.example.projetamio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_START_AT_BOOT = "start_at_boot";
    private static final String SERVER_URL = "http://iotlab.telecomnancy.eu:8080/iotlab/rest/data/1/light1/last";

    // Variables UI
    private TextView tv1, tv2, tv3, tv4, tv5, tv6;
    private SwitchCompat btn1, btn2;
    private Button btnHttpRequest;
    private CheckBox checkboxStartAtBoot;

    // Variables pour les mesures (historique)
    private LinearLayout containerMeasures;
    private TextView tvNoData;

    // Variables pour les capteurs actifs
    private LinearLayout containerActiveSensors;
    private TextView tvNoSensors;

    private Map<String, SensorInfo> activeSensors = new HashMap<>();

    private static class SensorInfo {
        String mote;
        String label;
        double value;
        boolean isLightOn;
        long timestamp;

        SensorInfo(String mote, String label, double value, boolean isLightOn, long timestamp) {
            this.mote = mote;
            this.label = label;
            this.value = value;
            this.isLightOn = isLightOn;
            this.timestamp = timestamp;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Afficher l'ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
        }

        Log.d(TAG, "Création de l'activité");

        // Initialiser les vues
        tv1 = findViewById(R.id.tv1);
        tv2 = findViewById(R.id.tv2);
        tv3 = findViewById(R.id.tv3);
        tv4 = findViewById(R.id.tv4);
        tv5 = findViewById(R.id.tv5);
        tv6 = findViewById(R.id.tv6);
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btnHttpRequest = findViewById(R.id.btnHttpRequest);
        checkboxStartAtBoot = findViewById(R.id.checkboxStartAtBoot);

        // Initialiser les containers
        containerMeasures = findViewById(R.id.containerMeasures);
        tvNoData = findViewById(R.id.tvNoData);
        containerActiveSensors = findViewById(R.id.containerActiveSensors);
        tvNoSensors = findViewById(R.id.tvNoSensors);

        // Setup des listeners
        setupSwitchListeners();
        setupCheckBoxListener();
        setupHttpButton();

        // Enregistrer le BroadcastReceiver
        IntentFilter filter = new IntentFilter(MainService.ACTION_RESULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serviceReceiver, filter);
        }

        Log.d(TAG, "BroadcastReceiver enregistré");

        // Insets pour les bords de l'écran
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // BroadcastReceiver pour recevoir les données du service
    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String mote = intent.getStringExtra("mote");
            String label = intent.getStringExtra("label");
            double value = intent.getDoubleExtra("value", 0);
            boolean isLightOn = intent.getBooleanExtra("isLightOn", false);

            Log.d(TAG, "Données reçues : Mote=" + mote + " Light=" + value);

            //  Mettre à jour la map des capteurs actifs
            activeSensors.put(mote, new SensorInfo(mote, label, value, isLightOn, System.currentTimeMillis()));
            updateActiveSensorsDisplay();

            // Mettre à jour les TextViews
            tv4.setText("Light: " + value);
            tv6.setText("Mote: " + mote);

            if (isLightOn) {
                tv2.setText("ALLUMÉE");
                tv2.setTextColor(getResources().getColor(R.color.light_on, null));
            } else {
                tv2.setText("ÉTEINTE");
                tv2.setTextColor(getResources().getColor(R.color.light_off, null));
            }
        }
    };

    // Mettre à jour l'affichage des capteurs actifs
    private void updateActiveSensorsDisplay() {
        containerActiveSensors.removeAllViews();

        if (activeSensors.isEmpty()) {
            tvNoSensors.setVisibility(View.VISIBLE);
        } else {
            tvNoSensors.setVisibility(View.GONE);

            // Trier par numéro de mote
            List<String> sortedMotes = new ArrayList<>(activeSensors.keySet());
            Collections.sort(sortedMotes);

            // Ajouter chaque capteur
            for (String mote : sortedMotes) {
                SensorInfo sensor = activeSensors.get(mote);
                if (sensor != null) {
                    addSensorItem(sensor);
                }
            }
        }
    }

    //  Ajouter un item de capteur
    private void addSensorItem(SensorInfo sensor) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View sensorView = inflater.inflate(R.layout.item_sensor, containerActiveSensors, false);

        View sensorIndicator = sensorView.findViewById(R.id.sensorIndicator);
        TextView tvSensorName = sensorView.findViewById(R.id.tvSensorName);
        TextView tvSensorLabel = sensorView.findViewById(R.id.tvSensorLabel);
        TextView tvSensorValue = sensorView.findViewById(R.id.tvSensorValue);
        TextView tvSensorStatus = sensorView.findViewById(R.id.tvSensorStatus);

        tvSensorName.setText("Capteur " + sensor.mote);
        tvSensorLabel.setText(sensor.label);
        tvSensorValue.setText(String.valueOf(sensor.value));

        if (sensor.isLightOn) {
            tvSensorStatus.setText("ON");
            tvSensorStatus.setTextColor(getResources().getColor(R.color.light_on, null));
            tvSensorStatus.setBackgroundColor(0xFFE8F5E9);
            sensorIndicator.setBackgroundColor(getResources().getColor(R.color.light_on, null));
        } else {
            tvSensorStatus.setText("OFF");
            tvSensorStatus.setTextColor(getResources().getColor(R.color.light_off, null));
            tvSensorStatus.setBackgroundColor(0xFFF5F5F5);
            sensorIndicator.setBackgroundColor(getResources().getColor(R.color.light_off, null));
        }

        containerActiveSensors.addView(sensorView);
    }

    //  Bouton HTTP
    private void setupHttpButton() {
        btnHttpRequest.setOnClickListener(v -> {
            Log.d(TAG, "Bouton HTTP cliqué");
            Toast.makeText(this, "Envoi de la requête...", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                HttpURLConnectionTask task = new HttpURLConnectionTask(
                        SERVER_URL,
                        new HttpURLConnectionTask.HttpResponseListener() {
                            @Override
                            public void onResponse(String response, int httpCode) {
                                Log.d(TAG, "Réponse JSON reçue (code " + httpCode + ")");
                                runOnUiThread(() -> {
                                    try {
                                        parseAndDisplayJSON(response);
                                        Toast.makeText(MainActivity.this, "Succès ! Code " + httpCode, Toast.LENGTH_SHORT).show();
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Erreur parsing JSON : " + e.getMessage());
                                        Toast.makeText(MainActivity.this, "JSON invalide", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onError(String error, int httpCode) {
                                Log.e(TAG, "Erreur HTTP (code " + httpCode + ") : " + error);
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "Erreur " + httpCode + " : " + error, Toast.LENGTH_LONG).show();
                                    tv4.setText("Erreur " + httpCode);
                                });
                            }
                        }
                );
                task.run();
            }).start();
        });
    }

    //  Parser et afficher le JSON
    private void parseAndDisplayJSON(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        Log.d(TAG, "Nombre de mesures : " + dataArray.length());

        if (dataArray.length() > 0) {
            tvNoData.setVisibility(View.GONE);
            containerMeasures.removeAllViews();

            activeSensors.clear();

            double lastValue = 0;
            long lastTimestamp = 0;
            String lastLabel = "";
            String lastMote = "";

            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject item = dataArray.getJSONObject(i);

                long timestampMs = item.getLong("timestamp");
                String label = item.getString("label");
                double value = item.getDouble("value");
                String mote = item.getString("mote");

                String dateFormatted = formatTimestamp(timestampMs);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                int lightThreshold = Integer.parseInt(prefs.getString("light_threshold", "500"));
                boolean isLightOn = (value > lightThreshold);

                activeSensors.put(mote, new SensorInfo(mote, label, value, isLightOn, timestampMs));

                // Créer une card pour l'historique
                addMeasureCard(i + 1, value, label, mote, dateFormatted, isLightOn);

                lastValue = value;
                lastTimestamp = timestampMs;
                lastLabel = label;
                lastMote = mote;
            }

            updateActiveSensorsDisplay();

            String lastDate = formatTimestamp(lastTimestamp);
            tv6.setText("Date: " + lastDate);

            Log.d(TAG, "Dernière luminosité : " + lastValue);
            Log.d(TAG, "Date : " + lastDate);
        } else {
            Log.w(TAG, "Tableau 'data' vide !");
            tvNoData.setVisibility(View.VISIBLE);
            containerMeasures.removeAllViews();
        }
    }

    private void addMeasureCard(int number, double lightValue, String label, String mote, String date, boolean isLightOn) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.item_measure, containerMeasures, false);

        View statusIndicator = cardView.findViewById(R.id.statusIndicator);
        TextView tvMeasureNumber = cardView.findViewById(R.id.tvMeasureNumber);
        TextView tvStatus = cardView.findViewById(R.id.tvStatus);
        TextView tvLightValue = cardView.findViewById(R.id.tvLightValue);
        TextView tvMote = cardView.findViewById(R.id.tvMote);
        TextView tvLabel = cardView.findViewById(R.id.tvLabel);
        TextView tvDate = cardView.findViewById(R.id.tvDate);

        tvMeasureNumber.setText("Mesure #" + number);
        tvLightValue.setText(String.valueOf(lightValue));
        tvMote.setText(mote);
        tvLabel.setText(label);
        tvDate.setText(date);

        if (isLightOn) {
            tvStatus.setText("ALLUMÉE");
            tvStatus.setTextColor(getResources().getColor(R.color.light_on, null));
            tvStatus.setBackgroundColor(0xFFE8F5E9);
            statusIndicator.setBackgroundColor(getResources().getColor(R.color.light_on, null));
        } else {
            tvStatus.setText("ÉTEINTE");
            tvStatus.setTextColor(getResources().getColor(R.color.light_off, null));
            tvStatus.setBackgroundColor(0xFFF5F5F5);
            statusIndicator.setBackgroundColor(getResources().getColor(R.color.light_off, null));
        }

        containerMeasures.addView(cardView);
    }

    private String formatTimestamp(long timestampMs) {
        try {
            Date date = new Date(timestampMs);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE);
            return sdf.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Erreur formatage date : " + e.getMessage());
            return String.valueOf(timestampMs);
        }
    }

    // CheckBox listener
    private void setupCheckBoxListener() {
        checkboxStartAtBoot.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Log.d(TAG, "CheckBox cochée : Start at boot activé");
                saveStartAtBootPreference(true);
            } else {
                Log.d(TAG, "CheckBox décochée : Start at boot désactivé");
                saveStartAtBootPreference(false);
            }
        });
    }

    // Sauvegarder préférence
    private void saveStartAtBootPreference(boolean enabled) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_START_AT_BOOT, enabled)
                .apply();
        Log.d(TAG, "Préférence 'start_at_boot' sauvegardée : " + enabled);
    }

    // Switch listeners
    private void setupSwitchListeners() {
        btn1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startMainService();
                tv2.setText("En cours");
                tv2.setTextColor(getResources().getColor(R.color.light_on, null));
            } else {
                Intent serviceIntent = new Intent(this, MainService.class);
                stopService(serviceIntent);
                tv2.setText("Arrêté");
                tv2.setTextColor(getResources().getColor(R.color.error, null));
            }
        });

        btn2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tv4.setText("Actives");
                tv4.setTextColor(getResources().getColor(R.color.light_on, null));
            } else {
                tv4.setText("Désactivées");
                tv4.setTextColor(getResources().getColor(R.color.light_off, null));
            }
        });
    }

    // Démarrer le service
    private void startMainService() {
        Intent serviceIntent = new Intent(this, MainService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(serviceReceiver);
    }
}