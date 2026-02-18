package com.example.projetamio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainService extends Service {

    public static final String ACTION_RESULT = "com.example.projetamio.ACTION_RESULT";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MainServiceChannel";
    private static final String NOTIFICATION_CHANNEL_ID = "LightChangeChannel";
    private static final String SERVER_URL = "http://iotlab.telecomnancy.eu:8080/iotlab/rest/data/1/light1/last";
    private Timer timer;
    private TimerTask timerTask;
    private NotificationManager notificationManager;
    private Map<String, Boolean> previousLightStates = new HashMap<>();
    private Vibrator vibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
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
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());

        //  Lire l'intervalle depuis les préférences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int checkInterval = Integer.parseInt(prefs.getString("check_interval", "30"));
        long intervalMs = checkInterval * 1000L;

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.d("Timer", "Tâche périodique exécutée à : " + System.currentTimeMillis());
                Log.d("Périodique", "run() - Tâche périodique en cours");
                checkLightStatus();
            }
        };
        timer.schedule(timerTask, 0, intervalMs);
        Log.d("START_STICKY", "Service démarré avec intervalle de " + checkInterval + "s");
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
                                Log.e("MainService", "Erreur parsing : " + e.getMessage());
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //  Lire le seuil de luminosité configuré
        int lightThreshold = Integer.parseInt(prefs.getString("light_threshold", "500"));

        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);

            String mote = item.getString("mote");
            double value = item.getDouble("value");
            String label = item.getString("label");

            boolean isLightOn = (value > lightThreshold);
            Boolean previousState = previousLightStates.get(mote);

            if (previousState != null && previousState != isLightOn) {
                Log.d("MainService", " CHANGEMENT : Mote " + mote + " → " + (isLightOn ? "ALLUMÉE" : "ÉTEINTE"));
                vibratePhone();

                sendLightChangeNotification(mote, label, isLightOn, value);
                sendEmail(mote, label, isLightOn, value);

            }

            previousLightStates.put(mote, isLightOn);
            sendResultToActivity(mote, label, value, isLightOn);
            Log.d("MainService", "Mote " + mote + " : " + value + " → " + (isLightOn ? "🟢" : "⚫"));
        }
    }

    private void sendEmail(String mote, String label, boolean isLightOn, double value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //  Lire les options email
        boolean emailWeekend = prefs.getBoolean("email_weekend", true);
        boolean emailNight = prefs.getBoolean("email_night", true);

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        boolean isWeekend = (dayOfWeek == java.util.Calendar.SATURDAY ||
                dayOfWeek == java.util.Calendar.SUNDAY);

        boolean shouldSendEmail = false;

        if (isWeekend && hour >= 19 && hour < 23 && emailWeekend) {
            shouldSendEmail = true;
            Log.d("MainService", " Week-end 19h-23h → Email");
        } else if (!isWeekend && (hour >= 23 || hour < 6) && emailNight) {
            shouldSendEmail = true;
            Log.d("MainService", " Semaine nuit → Email");
        } else {
            Log.d("MainService", "Pas d'email");
        }

        if (!shouldSendEmail) {
            return;
        }

        Log.d("MainService", "📧 Préparation email pour Mote " + mote);

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");

        //Lire l'email depuis les préférences
        String destinataire = prefs.getString("email_address", "brstuvh@gmail.com");
        String sujet = isLightOn ? "💡 Lumière allumée" : " Lumière éteinte";
        String message = "Détection de changement d'état :\n\n" +
                "Mote : " + mote + "\n" +
                "Label : " + label + "\n" +
                "Valeur : " + value + "\n" +
                "État : " + (isLightOn ? "ALLUMÉE " : "ÉTEINTE ") + "\n\n" +
                "Détecté à : " + new java.util.Date();

        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{destinataire});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, sujet);
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);
        emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(Intent.createChooser(emailIntent, "Envoyer email..."));
            Log.d("MainService", "Intent email envoyé");
        } catch (Exception e) {
            Log.e("MainService", " Erreur envoi email : " + e.getMessage());
        }
    }

    private void sendResultToActivity(String mote, String label, double value, boolean isLightOn) {
        Intent intent = new Intent(ACTION_RESULT);
        intent.putExtra("mote", mote);
        intent.putExtra("label", label);
        intent.putExtra("value", value);
        intent.putExtra("isLightOn", isLightOn);
        sendBroadcast(intent);
        Log.d("MainService", " Intent envoyé à l'activité : Mote=" + mote + " Light=" + value);
    }

    private void sendLightChangeNotification(String mote, String label, boolean isLightOn, double value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int startHour = Integer.parseInt(prefs.getString("notif_start_hour", "19"));
        int endHour = Integer.parseInt(prefs.getString("notif_end_hour", "23"));

        int currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);

        if (currentHour < startHour || currentHour >= endHour) {
            Log.d("MainService", "Hors intervalle (" + startHour + "h-" + endHour + "h)");
            return;
        }

        Log.d("MainService", "Envoi notification pour Mote " + mote);

        String title = isLightOn ? " Lumière allumée" : " Lumière éteinte";
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
            alertChannel.setDescription("Notifications de changement d'état");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(alertChannel);
            }
        }
    }

    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(" Surveillance active")
                .setContentText("Vérification périodique...")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // Faire vibrer le téléphone
    private void vibratePhone() {
        if (vibrator != null && vibrator.hasVibrator()) {
            // Vibrer pendant 500 millisecondes (0.5 secondes)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ (API 26+)
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // Anciennes versions Android
                vibrator.vibrate(500);
            }
            Log.d("MainService", " Téléphone vibré !");
        } else {
            Log.w("MainService", " Vibreur non disponible");
        }
    }
}