package py.com.smarttraffic.gpslocator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static android.support.v4.content.FileProvider.getUriForFile;

public class MainActivity extends AppCompatActivity {
    private LocationManager locationManager;
    private LocationListener locationListener;
    private TextView coordenates;
    private Button startGps;
    private Button stopGps;
    private ImageButton share;
    private ActionProvider shareAction;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startGps = (Button) findViewById(R.id.startGPS);
        stopGps = (Button) findViewById(R.id.stopGPS);
        share = (ImageButton)findViewById(R.id.share);
        coordenates = (TextView) findViewById(R.id.coordenates);
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                coordenates.setText("\n " + location.getLatitude() +" " + location.getLongitude());
                savePointInGpxFile(location);
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };
        startGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    return;
                }else{
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
                }
                startGpxFile();
            }
        });

        stopGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationManager.removeUpdates(locationListener);
                closeGpxFile();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareFiles();
            }
        });
        }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }
    }

    //TODO:Make it with a Parse work you i....!!
    //--------------------GPX MANAGER----------------------//
    public void startGpxFile(){
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy");
        Date currentTime = Calendar.getInstance().getTime();
        String fileName = formatter.format(currentTime) + ".gpx";
        Timestamp ts = new Timestamp(currentTime.getTime());
//        File mydir = this.getDir("myCoordinates", Context.MODE_PRIVATE); //Creating an internal dir;
//        File fileWithinMyDir = new File(mydir, fileName); //Getting a file within the dir.
        String Message = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><gpx version=\"1.0\" creator=\"SmartTraffic - http://smarttraffic.com.py/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\"><time>" + ts + "</time>";
        try {
            FileOutputStream fileOutputStream = openFileOutput(fileName,MODE_PRIVATE);
            fileOutputStream.write(Message.getBytes());
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void savePointInGpxFile(Location location){
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy");
        Date currentTime = Calendar.getInstance().getTime();
        String fileName = formatter.format(currentTime) + ".gpx";
        String Message = "<wpt lat=\"" + location.getLatitude() + "\" lon=\"" + location.getLongitude() + "\"><ele>" + location.getAltitude() + "</ele><time>" + new Timestamp(currentTime.getTime()) + "</time><src>gps</src></wpt>";
        try {
            FileOutputStream fileOutputStream = openFileOutput(fileName,MODE_APPEND);
            fileOutputStream.write(Message.getBytes());
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeGpxFile(){
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy");
        Date currentTime = Calendar.getInstance().getTime();
        String fileName = formatter.format(currentTime) + ".gpx";
        String Message = "</gpx>";
        try {
            FileOutputStream fileOutputStream = openFileOutput(fileName,MODE_APPEND);
            fileOutputStream.write(Message.getBytes());
            fileOutputStream.close();
            Toast.makeText(getApplicationContext(),"Archivo cerrado...", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//**************** CLOSE FILE WITH CORRESPONDING xml **************************//

    //NEED TO EXPLAIN?>>>
    public void shareFiles(){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);

        builderSingle.setTitle("Select One File: ");
        final ArrayAdapter<Uri> arrayAdapter = getAllFilesToShare();
        builderSingle.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Uri strName = arrayAdapter.getItem(which);
                final int elementPos = which;
                AlertDialog.Builder builderInner = new AlertDialog.Builder(MainActivity.this);
                builderInner.setMessage(strName.toString());
                builderInner.setTitle("Your Selected Item is");
                builderInner.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.setType("*/*");
                        sendIntent.putExtra(Intent.EXTRA_STREAM, arrayAdapter.getItem(elementPos));
                        sendIntent.putExtra(Intent.EXTRA_TEXT, "Send you this coordinates in GPX file");
                        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Gps coordinates from Smarttraffic");
                        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(sendIntent,"Share gpx file"));
                        dialog.dismiss();
                    }
                });
                builderInner.show();
            }
        });
        builderSingle.show();
    }

    public void shareAllfiles(){
        ArrayList<Uri> gpxFiles = getUrisFilesToShare();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        sendIntent.setType("*/*");
        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,gpxFiles);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Send you this coordinates in GPX file");
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(sendIntent,"Share gpx file"));
    }

    //ADDS THE SHARE BUTTON ON TOP OF THE BAR............................................//
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shareactionbar, menu);
        MenuItem item = menu.findItem(R.id.menu_item_share);
        shareAction = MenuItemCompat.getActionProvider(item);
        return true;
    }

    public ArrayList<Uri> getUrisFilesToShare(){
        ArrayList<Uri> gpxFiles = new ArrayList<>();
        File[] files = getFilesDir().listFiles();
        for(int i = 0; i < files.length; i++){
            Uri path = FileProvider.getUriForFile(this,"py.com.smarttraffic.gpslocator",files[i]);
            gpxFiles.add(path);
        }
        return gpxFiles;
    }

    public ArrayAdapter<Uri> getAllFilesToShare(){
        ArrayAdapter<Uri> gpxFiles = new ArrayAdapter<Uri>(MainActivity.this, android.R.layout.select_dialog_singlechoice);
        File[] files = getFilesDir().listFiles();
        for(int i = 0; i < files.length; i++){
            Uri path = FileProvider.getUriForFile(this,"py.com.smarttraffic.gpslocator",files[i]);
            gpxFiles.add(path);
        }
        return gpxFiles;
    }

    //--------------*************************************
    //Just an old version of the first GPX creator....
    //**************************************-------------
    public void saveGpxFile(Location location){
        Date currentTime = Calendar.getInstance().getTime();
        Timestamp ts = new Timestamp(currentTime.getTime());
        String Message = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><gpx version=\"1.0\" creator=\"SmartTraffic - http://smarttraffic.com.py/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\"><time>" + ts + "</time><wpt lat=\"" + location.getLatitude() + "\" lon=\"" + location.getLongitude() + "\"><ele>\"" + location.getAltitude() + "\"</ele><time>" + new Timestamp(currentTime.getTime()) + "</time><src>gps</src></wpt></gpx>";
        String fileName = currentTime.toString();
        try {
            FileOutputStream fileOutputStream = openFileOutput(fileName,MODE_PRIVATE);
            fileOutputStream.write(Message.getBytes());
            fileOutputStream.close();
            Toast.makeText(getApplicationContext(),"Informacion guardada en" + getFilesDir(), Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}