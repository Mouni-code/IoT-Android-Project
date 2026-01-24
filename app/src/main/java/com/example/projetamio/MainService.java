package com.example.projetamio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainService extends Service {

    private static final String CHANNEL_ID = "MainServiceChannel";
    private static final String NOTIFICATION_CHANNEL_ID = "LightChangeChannel";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    private static final String SERVER_URL = "http://iotlab.telecomnancy.eu:8080/iotlab/rest/data/1/light1/last";
    private Timer timer;
    private TimerTask timerTask;
    private NotificationManager notificationManager;
    private Map<String, Boolean> previousLightStates = new HashMap<>();

    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    public void onDestroy() {
        super.onDestroy();
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }

        stopForeground(true);
        Log.d("DESTROYED", "App destroyed");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(FOREGROUND_NOTIFICATION_ID,  createForegroundNotification());

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.d("Timer", "Tâche périodique exécutée à : " + System.currentTimeMillis());
                Log.d("Périodique", "run() - Tâche périodique en cours d'exécution");
                checkLightStatus();
            }
        };
        timer.schedule(timerTask, 0, 30000);
        Log.d("START_STICKY", "Start stickyy");
        return START_STICKY;
    }

    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void checkLightStatus() {
        new Thread(() -> {
            HttpURLConnectionTask task = new HttpURLConnectionTask(
                    SERVER_URL,
                    new HttpURLConnectionTask.HttpResponseListener() {
                        @Override
                        public void onResponse(String response, int httpCode) {
                            Log.d("MainService", "Données reçues (code " + httpCode + ")");
                            try {
                                detectLightChanges(response);
                            } catch (Exception e) {
                                Log.e("MainService", " Erreur parsing : " + e.getMessage());
                            }
                        }

                        @Override
                        public void onError(String error, int httpCode) {
                            Log.e("MainService", "Erreur HTTP : " + error);
                        }
                    }
            );
            task.run();
        }).start();
    }

    private void detectLightChanges(String jsonString) throws Exception {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);

            String mote = item.getString("mote");
            double value = item.getDouble("value");
            String label = item.getString("label");

            boolean isLightOn = (value > 500);
            Boolean previousState = previousLightStates.get(mote);

            if (previousState != null && previousState != isLightOn) {
                Log.d("MainService", "🔔 CHANGEMENT : Mote " + mote + " → " + (isLightOn ? "ALLUMÉE" : "ÉTEINTE"));
                sendLightChangeNotification(mote, label, isLightOn, value);
            }

            previousLightStates.put(mote, isLightOn);
            Log.d("MainService", "Mote " + mote + " : " + value + " → " + (isLightOn ? "🟢" : "⚫"));
        }
    }

    private void sendLightChangeNotification(String mote, String label, boolean isLightOn, double value) {
        int currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);

        if (currentHour < 18 || currentHour >= 23) {
            Log.d("MainService", "⏰ Hors intervalle horaire (18h-23h), notification non envoyée");
            return;
        }

        Log.d("MainService", "🔔 Envoi de notification pour Mote " + mote);

        String title = isLightOn ? "💡 Lumière allumée" : "⚫ Lumière éteinte";
        String message = "Mote " + mote + " (" + label + ") : " + value;

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(mote.hashCode(), notification);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Service de surveillance",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationChannel alertChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Alertes lumières",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Notifications de changement d'état des lumières");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(alertChannel);
            }
        }
    }

    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🔍 Surveillance active")
                .setContentText("Vérification périodique des lumières...")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}