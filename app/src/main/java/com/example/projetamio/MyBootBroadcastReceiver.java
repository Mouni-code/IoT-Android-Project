package com.example.projetamio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class MyBootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_START_AT_BOOT = "start_at_boot";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Vérifier que c'est bien l'action BOOT_COMPLETED
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            Log.d("boot", " BOOT_COMPLETED reçu !");

            // Lire la préférence SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean startAtBoot = prefs.getBoolean(KEY_START_AT_BOOT, false);

            Log.d("pref", "Préférence 'start_at_boot' = " + startAtBoot);

            // Démarrer le service seulement si l'utilisateur a coché la checkbox
            if (startAtBoot) {
                Log.d("start", " Démarrage automatique du service...");

                Intent serviceIntent = new Intent(context, MainService.class);

                // Démarrer le service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }

                Log.d("demarrage", " Service démarré avec succès !");
            } else {
                Log.d("demarrage1", " Démarrage automatique désactivé par l'utilisateur");
            }
        }
    }
}