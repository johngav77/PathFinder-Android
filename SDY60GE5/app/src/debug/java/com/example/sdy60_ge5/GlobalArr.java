package com.example.sdy60_ge5;

import android.app.Application;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class GlobalArr extends Application {
    private ArrayList<LatLng> coordList = new ArrayList<LatLng>();

    public ArrayList<LatLng> getCoordList() {
        return coordList;
    }

    public void setCoordList( ArrayList<LatLng> d){
        this.coordList = d;
    }
}
