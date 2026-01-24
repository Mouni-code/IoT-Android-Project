package com.example.projetamio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_START_AT_BOOT = "start_at_boot";
    private static final String SERVER_URL = "http://iotlab.telecomnancy.eu:8080/iotlab/rest/data/1/light1/last";
    private TextView tv1, tv2, tv3, tv4, tv5, tv6;
    private SwitchCompat btn1, btn2;
    private Button btnHttpRequest;
    private CheckBox checkboxStartAtBoot;
    private TextView tvAllMeasures;
    private Map<String, SensorData> latestSensorData = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Création de l'activité");
        //startMainService();
        // Lier les TextViews avec le XML
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
        tvAllMeasures = findViewById(R.id.tvAllMeasures);
        loadStartAtBootPreference();
        setupSwitchListeners();
        setupCheckBoxListener();
        setupHttpButton();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void setupHttpButton() {
        btnHttpRequest.setOnClickListener(v -> {
            Log.d("btnhttp", "Bouton HTTP cliqué");
            Toast.makeText(this, "Envoi de la requête...", Toast.LENGTH_SHORT).show();

            // Exécuter la requête dans un thread séparé
            new Thread(() -> {
                HttpURLConnectionTask task = new HttpURLConnectionTask(
                        SERVER_URL,
                        new HttpURLConnectionTask.HttpResponseListener() {
                            @Override
                            public void onResponse(String response, int httpCode) {
                                Log.d("repJson", "Réponse JSON reçue (code " + httpCode + ")");
                                Log.d("repData", "Corps de la réponse : " + response);
                                // Mettre à jour l'UI dans le thread principal
                                runOnUiThread(() -> {
                                    try {
                                        parseAndDisplayJSON(response);
                                        Toast.makeText(MainActivity.this, " Succès ! Code " + httpCode, Toast.LENGTH_SHORT).show();

                                    } catch (JSONException e) {
                                        Log.e("errJson", "Erreur parsing JSON : " + e.getMessage());
                                        tv4.setText(response.substring(0, Math.min(response.length(), 50)));
                                        Toast.makeText(MainActivity.this, "JSON invalide", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onError(String error, int httpCode) {
                                Log.e("erreur", " Erreur HTTP (code " + httpCode + ") : " + error);
                                runOnUiThread(() -> {
                                    Toast.makeText(
                                            MainActivity.this,
                                            " Erreur " + httpCode + " : " + error,
                                            Toast.LENGTH_LONG  // Toast long pour laisser le temps de lire
                                    ).show();

                                    tv4.setText("Erreur " + httpCode);
                                });
                            }
                        }
                );
                task.run(); // Exécuter la tâche
            }).start();
        });
    }

    private String formatTimestamp(long timestampMs) {
        try {
            Date date = new Date(timestampMs);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE);
            return sdf.format(date);
        } catch (Exception e) {
            Log.e("errDate", "Erreur formatage date : " + e.getMessage());
            return String.valueOf(timestampMs);
        }
    }

    private void parseAndDisplayJSON(String jsonString) throws JSONException { //Flemme d'utiliser JSonReader C TROP LONG....
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        Log.d("nbr", "📊 Nombre de mesures : " + dataArray.length());

        if (dataArray.length() > 0) {
            StringBuilder allMeasures = new StringBuilder();
            allMeasures.append("═══════════════════════════\n");
            allMeasures.append("  📊 TOUTES LES MESURES\n");
            allMeasures.append("═══════════════════════════\n\n");

            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject item = dataArray.getJSONObject(i);

                long timestampMs = item.getLong("timestamp");
                String label = item.getString("label");
                double value = item.getDouble("value");
                String mote = item.getString("mote");

                SensorData sensorData = new SensorData(mote, label, value, timestampMs);

                latestSensorData.put(mote, sensorData);

                String dateFormatted = formatTimestamp(timestampMs);

                allMeasures.append("📍 Mesure #").append(i + 1).append("\n");
                allMeasures.append("  💡 Light: ").append(value);

                if (sensorData.isLightOn()) {
                    allMeasures.append(" 🟢 ALLUMÉE\n");
                } else {
                    allMeasures.append(" ⚫ ÉTEINTE\n");
                }

                allMeasures.append("  🏷️  Label: ").append(label).append("\n");
                allMeasures.append("  📍 Mote: ").append(mote).append("\n");
                allMeasures.append("  🕒 Date: ").append(dateFormatted).append("\n\n");
            }


            allMeasures.append("🔦 LUMIÈRES ALLUMÉES :\n");
            allMeasures.append("━━━━━━━━━━━━━━━━━━━━━\n");

            boolean hasLightsOn = false;
            for (SensorData sensor : latestSensorData.values()) {
                if (sensor.isLightOn()) {
                    allMeasures.append("  🟢 Mote ").append(sensor.getMote())
                            .append(" : ").append(sensor.getValue()).append("\n");
                    hasLightsOn = true;
                }
            }

            if (!hasLightsOn) {
                allMeasures.append("  ⚫ Aucune lumière allumée\n");
            }

            tvAllMeasures.setText(allMeasures.toString());

            JSONObject lastItem = dataArray.getJSONObject(dataArray.length() - 1);

            long timestampMs = lastItem.getLong("timestamp");
            String label = lastItem.getString("label");
            double value = lastItem.getDouble("value");
            String mote = lastItem.getString("mote");

            String dateFormatted = formatTimestamp(timestampMs);

            Log.d("lum", "💡 Luminosité : " + value);
            Log.d("date", "🕒 Date : " + dateFormatted);
            Log.d("label", "🏷️ Label : " + label);
            Log.d("Mote", "📍 Mote : " + mote);

            tv6.setText("Date: " + dateFormatted);

        } else {
            Log.w("empty", "⚠️ Tableau 'data' vide !");
            tvAllMeasures.setText("Aucune donnée");
        }
    }
    private void loadStartAtBootPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean startAtBoot = prefs.getBoolean(KEY_START_AT_BOOT, false); // false = valeur par défaut

        // Appliquer l'état à la checkbox
        checkboxStartAtBoot.setChecked(startAtBoot);

        Log.d("pref", " Préférence chargée au démarrage : start_at_boot = " + startAtBoot);
    }
    private void setupCheckBoxListener(){
        checkboxStartAtBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Afficher un log quand l'état de la checkbox change
                if (isChecked) {
                    Log.d("checkbox", "CheckBox cochée : Start at boot activé");
                    // Tu peux sauvegarder cette préférence ici
                    saveStartAtBootPreference(true);
                } else {
                    Log.d("checkbox", "CheckBox décochée : Start at boot désactivé");
                    saveStartAtBootPreference(false);
                }
            }
        });
    }
    private void saveStartAtBootPreference(boolean enabled) {
        // Sauvegarder la préférence dans SharedPreferences
        getSharedPreferences("AppPreferences", MODE_PRIVATE)
                .edit()
                .putBoolean("start_at_boot", enabled)
                .apply();

        Log.d("pref", "Préférence 'start_at_boot' sauvegardée : " + enabled);
    }
    private void setupSwitchListeners() {
        btn1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startMainService();
                tv2.setText("en cours");
            } else {
                Intent serviceIntent = new Intent(this, MainService.class);
                stopService(serviceIntent);
                tv2.setText("arrêté");
            }
        });
        btn2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tv4.setText("Active");
                tv6.setText("Enabled");
            } else {
                tv4.setText("Inactive");
                tv6.setText("Disabled");
            }
        });
    }
    private void startMainService() {
        Intent serviceIntent = new Intent(this, MainService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Optionnel : arrêter le service si nécessaire
        // stopService(new Intent(this, MainService.class));
    }
}