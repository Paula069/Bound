package co.edu.unipiloto.boundservices;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class OdometerService extends Service {

    private final IBinder binder = new OdometerBinder();
    private static double distanciaEnMetros;
    private static Location ultimaLocalizacion = null;
    private LocationManager locManager;
    private LocationListener listener;  // Declaración del listener
    private double precisionMovimiento = 1; // Valor predeterminado en metros
    private long tiempoActualizacion = 2000; // Valor predeterminado en milisegundos

    public class OdometerBinder extends Binder {
        OdometerService getOdometer() {
            return OdometerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public double getDistancia() {
        return distanciaEnMetros / 1000;
    }

    public void setConfiguration(double precision, long tiempo) {
        this.precisionMovimiento = precision;
        this.tiempoActualizacion = tiempo;
        if (locManager != null && listener != null) {
            locManager.removeUpdates(listener);
            String proveedor = locManager.getBestProvider(new Criteria(), true);
            if (proveedor != null &&
                    ActivityCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locManager.requestLocationUpdates(proveedor, tiempoActualizacion, (float) precisionMovimiento, listener);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (ultimaLocalizacion != null) {
                    distanciaEnMetros += location.distanceTo(ultimaLocalizacion);
                }
                ultimaLocalizacion = location;
            }

            @Override
            public void onProviderDisabled(String arg0) {}

            @Override
            public void onProviderEnabled(String arg0) {}

            @Override
            public void onStatusChanged(String arg0, int arg1, Bundle bundle) {}
        };

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String proveedor = locManager.getBestProvider(new Criteria(), true);

        if (proveedor != null &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locManager.requestLocationUpdates(proveedor, tiempoActualizacion, (float) precisionMovimiento, listener);
        } else {
            // Handle permission request if needed
        }
    }

    @Override
    public void onDestroy() {
        if (locManager != null && listener != null) {
            locManager.removeUpdates(listener);
        }
        locManager = null;
        listener = null;
    }
}
