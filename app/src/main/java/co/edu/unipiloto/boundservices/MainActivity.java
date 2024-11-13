package co.edu.unipiloto.boundservices;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private OdometerService odometro;
    private boolean enlazado = false;
    private EditText precisionEditText, tiempoEditText;
    private double precisionMovimiento = 1; // Valor predeterminado en metros
    private long tiempoActualizacion = 2000; // Valor predeterminado en milisegundos

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            OdometerService.OdometerBinder odometerBinder = (OdometerService.OdometerBinder) iBinder;
            odometro = odometerBinder.getOdometer();
            enlazado = true;
            updateConfiguration();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            enlazado = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        precisionEditText = findViewById(R.id.precision);
        tiempoEditText = findViewById(R.id.tiempo);

        Button updateButton = findViewById(R.id.update_button);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateConfiguration();
            }
        });

        // Leer distancia persistida
        SharedPreferences sharedPref = getSharedPreferences("OdometroPrefs", Context.MODE_PRIVATE);
        float distanciaRecorrida = sharedPref.getFloat("distancia_recorrida", 0);

        TextView verDistancia = findViewById(R.id.distancia);
        String distanciaStr = String.format(Locale.getDefault(), "%1$,.2f Km", distanciaRecorrida);
        verDistancia.setText(distanciaStr);

        mostrarDistancia();
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, OdometerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (enlazado) {
            unbindService(connection);
            enlazado = false;
        }
    }

    private void mostrarDistancia() {
        TextView verDistancia = findViewById(R.id.distancia);
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                double distancia = 0.0;
                if (enlazado && odometro != null) {
                    distancia = odometro.getDistancia();
                }

                String distanciaStr = String.format(Locale.getDefault(), "%1$,.2f Km", distancia);
                verDistancia.setText(distanciaStr);

                // Guardar distancia en SharedPreferences
                SharedPreferences sharedPref = getSharedPreferences("OdometroPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putFloat("distancia_recorrida", (float) distancia);
                editor.apply();

                handler.postDelayed(this, tiempoActualizacion);
            }
        });
    }

    private void updateConfiguration() {
        try {
            precisionMovimiento = Double.parseDouble(precisionEditText.getText().toString());
            tiempoActualizacion = Long.parseLong(tiempoEditText.getText().toString()) * 1000;
            if (odometro != null) {
                odometro.setConfiguration(precisionMovimiento, tiempoActualizacion);
            }
        } catch (NumberFormatException e) {
            // Handle number format exceptions if the input is invalid
        }
    }
}
