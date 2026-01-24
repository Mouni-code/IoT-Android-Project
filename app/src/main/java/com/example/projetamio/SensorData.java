package com.example.projetamio;

public class SensorData {
    private String mote;
    private String label;
    private double value;
    private long timestamp;
    private boolean isLightOn;  // Lumière allumée ou éteinte

    public SensorData(String mote, String label, double value, long timestamp) {
        this.mote = mote;
        this.label = label;
        this.value = value;
        this.timestamp = timestamp;

        //  Détection : lumière allumée si value > 250
        this.isLightOn = (value > 250);
    }

    // Getters
    public String getMote() { return mote; }
    public String getLabel() { return label; }
    public double getValue() { return value; }
    public long getTimestamp() { return timestamp; }
    public boolean isLightOn() { return isLightOn; }

    @Override
    public String toString() {
        return "Mote " + mote + " (" + label + ") : " + value +
                " → Lumière " + (isLightOn ? "🟢 ALLUMÉE" : "⚫ ÉTEINTE");
    }
}