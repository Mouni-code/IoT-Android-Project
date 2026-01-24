package com.example.projetamio;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.TimerTask;

public class HttpURLConnectionTask extends TimerTask {

    private static final String TAG = "HttpTask";
    private String urlString;
    private HttpResponseListener listener;

    // Interface pour renvoyer la réponse
    public interface HttpResponseListener {
        void onResponse(String response, int responseCode);
        void onError(String error, int responseCode);
    }

    public HttpURLConnectionTask(String urlString, HttpResponseListener listener) {
        this.urlString = urlString;
        this.listener = listener;
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            Log.d("get", " Envoi de la requête GET à : " + urlString);

            // Créer la connexion
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 secondes timeout
            connection.setReadTimeout(5000);

            // Lire la réponse
            int responseCode = connection.getResponseCode();
            Log.d("rep", " Code de réponse : " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                String result = response.toString();

                Log.d("sucess", " SUCCÈS (200) - Corps de la réponse JSON :");
                Log.d("data", result);

                // Notifier le listener
                if (listener != null) {
                    // Retourner sur le UI Thread pour mettre à jour l'interface
                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onResponse(result, responseCode);
                    });
                }


            } else {
                String error = "Erreur HTTP : " + responseCode;
                Log.e(TAG, " " + error);
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onError(error, responseCode);
                    });
                }
            }

        } catch (Exception e) {

            String error = "Exception : " + e.getMessage();
            Log.e(TAG, "" + error);
            if (listener != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onError(error, -1);  // -1 = erreur réseau, pas de code HTTP
                });
            }
        } finally {
            // Fermer les ressources
            try {
                if (reader != null) reader.close();
                if (connection != null) connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la fermeture : " + e.getMessage());
            }
        }
    }
}