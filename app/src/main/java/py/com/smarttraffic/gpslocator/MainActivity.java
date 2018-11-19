package py.com.smarttraffic.gpslocator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements OccupationDialogFragment.NoticeDialogListener,DisoccupiedDialogFragment.NoticeDialogListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private LocationManager locationManager;
    private final Context MainContext = MainActivity.this;
    private LocationListener locationListener;
    private TextView coordinates;
    private TextView txtActivity, txtConfidence;
    private ImageView imgActivity;
    private TextView parkingSpot;
    private Button startGps;
    private Button stopGps;
    private User me = new User("User", "Profile",13);
    private int indexOfParkingSpot;
    private int typeOfActivity;
    private TextView userParkStatus;
    private boolean dialogOccupationShow = false;
    private boolean dialogDisoccupationShow = false;
    private ArrayList<ParkingSpot> finalParkingSpots = new ArrayList<ParkingSpot>();

    private Location ucaCampus = new Location("dummyprovider");

    BroadcastReceiver broadcastReceiver;
    private NotificationHandler notificationHandler;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startGps = (Button) findViewById(R.id.startGPS);
        stopGps = (Button) findViewById(R.id.stopGPS);
        coordinates = (TextView) findViewById(R.id.coordenates);
        parkingSpot = (TextView) findViewById(R.id.parkingspot);
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        notificationHandler = new NotificationHandler(this);
        txtActivity = (TextView) findViewById(R.id.txt_activity);
        userParkStatus = (TextView) findViewById(R.id.user_system_activity);
        txtConfidence = (TextView) findViewById(R.id.txt_confidence);
        imgActivity = (ImageView) findViewById(R.id.img_activity);
        // Point of the UCA campus to get the distance to the place in consideration...
        //TODO: Change to geofencing location tech...
        ucaCampus.setLatitude(-25.323740);
        ucaCampus.setLongitude(-57.638405);

        startTracking();


        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                    typeOfActivity = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(typeOfActivity, confidence);
                }
            }
        };
        // Saves Parking Spots in a list...
        ArrayList<ParkingSpot> parkingSpots = new ArrayList<ParkingSpot>();
        try {
            parkingSpots = kmlOpenPolygonFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        finalParkingSpots = parkingSpots;

        locationListener = new LocationListener() {
            //TODO: this can be module by declaring a Global Location variable, that is changed...
            //TODO... with the Location at the user, then we can corroborated if has parked or not.
            @SuppressLint("MissingPermission")
            @Override
            public void onLocationChanged(Location location) {
                coordinates.setText("Ubicacion Actual" +
                        "\nLatitud: " + location.getLatitude() +
                        "\nLongitud: " + location.getLongitude() +
                        "\nPrecision: " + location.getAccuracy() +
                        "\nDistancia a la facultad: " + location.distanceTo(ucaCampus) +
                        "\nProvider :" + location.getProvider());
                savePointInGpxFile(location);
                indexOfParkingSpot = isPointInsideParkingSpot(finalParkingSpots, location);
                //ALERT: :::TROUBLE HERE, WHEN EXPLODES, ENTER HERE...
                if(indexOfParkingSpot != Constants.NOT_IN_PARKINGSPOT){
                    // if return something diferent that the -1, is because have found a parking spot...
                    // It found a the location of user inside a polygon ...
                    if(typeOfActivity == DetectedActivity.IN_VEHICLE){
                        // if is inside the vehicle... will capture as a occupation...
                        if(finalParkingSpots.get(indexOfParkingSpot).setOccupied(me.userID)){
                            userParkStatus.setText("Estas ocupando el lugar" + finalParkingSpots.get(indexOfParkingSpot).getPolygon().name);
                        }
                        if(!dialogOccupationShow){
                            showOccupationDialogMessage(
                                    getResources().getString(R.string.dialog_occupation_confirmation));
                            dialogOccupationShow = true;
                        }
                    }else{
                        //unify by IN_VEHICLE...
                        if(finalParkingSpots.get(indexOfParkingSpot).setFree(me.userID)){
                            userParkStatus.setText("Estas liberando el lugar" + finalParkingSpots.get(indexOfParkingSpot).getPolygon().name);
                        }else{
                            userParkStatus.setText("No puedes liberar el lugar" + finalParkingSpots.get(indexOfParkingSpot).getPolygon().name + "si no has estacionado aqui !");
                        }
                        if(!dialogOccupationShow){
                            showDisoccupationDialogMessage(
                                    getResources().getString(R.string.dialog_liberation_confirmation));
                            dialogOccupationShow = true;
                        }
                    }
                }

//                locationListenerHighFrequency();
                //FOR CHANGE NEAR THE DESTINY OF THE USER...(UCA example, add home for testing)
//                //TODO:Change to GEOFENCING....
//                if (location.distanceTo(ucaCampus) < 300 || location.distanceTo(home < 150)) {
//                    //notification manager  ...
//                    if(notificationSent == false){
//                        notificationToApp();
//                    }
//                    locationListenerHighFrequency();
//                } else {
//                    if(notificationSent == true){
//                        notificationSent = false;
//                    }
//                    // work with less frequency...
//                    locationListenerLowFrequency();
//                    notificationSent = false;
//                }


            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //nothing need to do...
            }
            @Override
            public void onProviderEnabled(String provider) {
                //nothing need to do...
            }
            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };

        //TODO: this button will have to stop working, cause this work will start after the app is...
        //TODO: ... installed.
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
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.SECOND_IN_MILLISECONDS * 1, 0, locationListener);
                }
                startGpxFile();
            }
        });

        //TODO: the stop button, stop the GPS sensing, and close the location file...
        //TODO: how to do it later(in server)?
        stopGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationManager.removeUpdates(locationListener);
                closeGpxFile();
            }
        });
    }

    //For the activity mode recognition system...
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
        startTracking();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        stopTracking();
    }

    private void startTracking() {
        Intent trackerOfTransitions = new Intent(MainActivity.this, BackgroundDetected.class);
        startService(trackerOfTransitions);
    }

    private void stopTracking() {
        Intent stopTrackerActivity = new Intent(MainActivity.this, BackgroundDetected.class);
        stopService(stopTrackerActivity);
    }

    //Manage the Activity Recognized...
    //... and sets the labels and graphics

    private void handleUserActivity(int type, int confidence) {
        String label = getString(R.string.activity_unknown);
        int icon = R.drawable.icons_unkown26;
        switch (type) {
            case DetectedActivity.IN_VEHICLE: {
                label = getString(R.string.activity_in_vehicle);
                icon = R.drawable.icons_driving40;
                break;
            }
            case DetectedActivity.ON_FOOT: {
                label = getString(R.string.activity_on_foot);
                icon = R.drawable.icons_walking40;
                break;
            }
            case DetectedActivity.WALKING: {
                label = getString(R.string.activity_walking);
                icon = R.drawable.walking;
                break;
            }
            case DetectedActivity.RUNNING: {
                label = getString(R.string.activity_running);
                icon = R.drawable.icons_running40;
                break;
            }
            case DetectedActivity.STILL: {
                label = getString(R.string.activity_still);
                icon = R.drawable.icons_still64;
                break;
            }
            case DetectedActivity.TILTING:{
                label = getString(R.string.activity_tilting);
                icon = R.drawable.icons_tilting;
                break;
            }
            case DetectedActivity.UNKNOWN: {
                label = getString(R.string.activity_unknown);
                icon = R.drawable.icons_unknown;
                break;
            }
        }

        Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);
        // if the confidence of the activity is grater than 70, then is shown the
        //activity that were detected...
        if (confidence > Constants.MIN_CONFIDENCE) {
            txtActivity.setText(label);
            txtConfidence.setText("Confidence: " + confidence);
            imgActivity.setImageResource(icon);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.SECOND_IN_MILLISECONDS, 0, locationListener);
            }
        }
    }
    //Notification manager...
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void notificationToApp() {
        Notification.Builder nb = notificationHandler.createNotification(
                "You are near the Campus",
                "The GPS now will take data more frequently"
        );
        Intent activityIntent = new Intent(this, MainActivity.class);
        activityIntent.putExtra(Constants.EXTRA_MESSAGE,true);
        PendingIntent contentIntent = PendingIntent.getActivity(this,0,
                activityIntent, 0);
        nb.setContentIntent(contentIntent);
        notificationHandler.getManager().notify(1,nb.build());
    }

    //TODO:Make it work with a Parse you i....!!
    //--------------------GPX MANAGER----------------------//
    public void startGpxFile(){
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy-HH");
        Date currentTime = Calendar.getInstance().getTime();
        String fileName = formatter.format(currentTime) + ".gpx";
        Timestamp ts = new Timestamp(currentTime.getTime());
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
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy-HH");
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
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy-HH");
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

    //ADDS THE SHARE BUTTON ON TOP OF THE BAR............................................//
    //Activity Bar shareButton............................................................

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.shareactionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_item_share:
                this.shareFiles();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    //This will try to open de .kml file to get the
    // polygons for each parking spots...
    public ArrayList<ParkingSpot> kmlOpenPolygonFile() throws IOException {
        //Open the file and then pass it to the xml analyzer to get the data information ...
        String fileName = "Parking Spots.kml";
        String Directory = "PolyMap";
        File kmlDir = new File(this.getFilesDir(), Directory);
        ArrayList<Polygon> Polygons = new ArrayList<Polygon>();
        ArrayList<ParkingSpot> Spots = new ArrayList<ParkingSpot>();
        ArrayList Places = new ArrayList();
        ArrayList coordinates = new ArrayList();

        // Parser for kml files JSOUP...
        String xmlContent = readFile(kmlDir + "/" + fileName);
        // find a way to read the file and store it in a string
        Document doc = Jsoup.parse(xmlContent, "", Parser.xmlParser());
        for(Element p : doc.select("Placemark").select("name")){
            // the contents
            Places.add(p.text());
        }
        for(Element c : doc.select("Placemark").select("coordinates")){
            // the contents
            coordinates.add(c.text());
        }

        for (int i = 0; i < Places.size(); i++){
            Point [] pX = new Point[4];
            String [] polygonCoordinates = coordinates.get(i).toString().split(" ");
            for(int j =0; j < polygonCoordinates.length-1; j++ ){
                String [] pointCoordinates = polygonCoordinates[j].split(",");
                pX[j] = new Point(Double.valueOf(pointCoordinates[1]),Double.valueOf(pointCoordinates[0]));
            }
            Polygons.add(new Polygon(Places.get(i).toString(),pX[0],pX[1],pX[2],pX[3]));
            Spots.add(new ParkingSpot(Constants.UNKNOWN_STATE, Constants.UNKNOWN_STATE, new Polygon(Places.get(i).toString(),pX[0],pX[1],pX[2],pX[3]),null,Constants.NOT_USER));
        }
        return Spots;
    }

    //read a file and return a string with the content of it...
    public String readFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }

    //--------------FOR THE DETECTION OF POINT INSIDE A SPOT LOGIC.........
    //TODO: this need to be modulated, into another java.class with there own methods...

    public boolean isPointInsidePolygon(Polygon polygon, Point Point){
        //If the point is inside of the polygon in question...
        return PolyUtil.containsLocation(Point.latitud,Point.longitud,toLatLngList(polygon),
                true);
    }


    public int isPointInsideParkingSpot(ArrayList<ParkingSpot> ParkingSpot, Location Point){
        // Determine if the Point is inside a Polygon of all the list in Polygons...
        for(int i = 0; i < ParkingSpot.size(); i++){
            if (isPointInsidePolygon(ParkingSpot.get(i).getPolygon(),locationToPoint(Point))){
                parkingSpot.setText("You are inside the spot" + ParkingSpot.get(i).getPolygon().name);
                return i;
            }else{
                continue;
            }
        }
        parkingSpot.setText("You are not in a parking spot");
        return Constants.NOT_IN_PARKINGSPOT;
    }

    public List<LatLng> toLatLngList(Polygon polygon){
        List<LatLng> resultList = new ArrayList<LatLng>() ;
        resultList.add(new LatLng(polygon.point1.latitud,polygon.point1.longitud));
        resultList.add(new LatLng(polygon.point2.latitud,polygon.point2.longitud));
        resultList.add(new LatLng(polygon.point3.latitud,polygon.point3.longitud));
        resultList.add(new LatLng(polygon.point4.latitud,polygon.point4.longitud));
        resultList.add(new LatLng(polygon.point1.latitud,polygon.point1.longitud));
        return resultList;
    }

    public Point locationToPoint(Location location){
        Point point = new Point(location.getLatitude(),location.getLongitude());
        return point;
    }

    //..........END OF THE LOGIC OF DETECTION A POINT INSIDE A POLYGON-----------

    // Frequency of the Location Listener...
    public void locationListenerHighFrequency(){
        locationManager.removeUpdates(locationListener);
        if (ActivityCompat.checkSelfPermission(MainContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.SECOND_IN_MILLISECONDS, 0, locationListener);
//        Toast.makeText(this,"Change to High GPS data frequency recolection modo", Toast.LENGTH_LONG).show();
    }

    public void locationListenerLowFrequency(){
        locationManager.removeUpdates(locationListener);
        if (ActivityCompat.checkSelfPermission(MainContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.SECOND_IN_MILLISECONDS * 30, 0, locationListener);
        Toast.makeText(this,"Change to Low GPS data frequency recolection modo", Toast.LENGTH_LONG).show();
    }

    //Dialog of OCCUPATION of the spot...


    public void showOccupationDialogMessage(String message) {
        // Create an instance of the dialog fragment and show it
        // its possible that would be need to create more than just one type of
        ///...dialog fragment, cause the answer would be different...
        final DialogFragment dialog = new OccupationDialogFragment(message);
        dialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                dialog.dismiss();
                timer.cancel();
                dialogOccupationShow = false;
                // If user do not respond, take as a parking action...
            }
        }, 10000);
    }

    @Override
    public void onDialogOccupiedPositiveClick(DialogFragment dialog) {
        //Saved the parking spot to the user that has occupied(already done before)...
        dialog.dismiss();
        dialogOccupationShow = false;
        //just wipe out the dialog
    }

    @Override
    public void onDialogOccupiedNegativeClick(DialogFragment dialog) {
        finalParkingSpots.get(indexOfParkingSpot).setUnknown();
        dialogOccupationShow = false;
        dialog.dismiss();
    }

    //Dialog of DISOCCUPATION of the spot...

    public void showDisoccupationDialogMessage(String message) {
        // Create an instance of the dialog fragment and show it
        // its possible that would be need to create more than just one type of
        ///...dialog fragment, cause the answer would be different...
        final DialogFragment dialog = new DisoccupiedDialogFragment(message);
        dialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                dialog.dismiss();
                timer.cancel();
                dialogDisoccupationShow = false;
                // If user do not respond, take as a parking action...
            }
        }, 10000);
    }

    @Override
    public void onDialogDisoccupiedPositiveClick(DialogFragment dialog) {
        dialogDisoccupationShow = false;
        dialog.dismiss();
    }

    @Override
    public void onDialogDisoccupiedNegativeClick(DialogFragment dialog) {
        finalParkingSpots.get(indexOfParkingSpot).setOccupied(me.getUserID());
        dialogDisoccupationShow = false;
        dialog.dismiss();
    }

    // This could be another option of dialog builder, but is not modulated...
    private void showAlertForCreatingBoard(String title, String message){
        AlertDialog.Builder builder  = new AlertDialog.Builder(this);

        if(title != null)builder.setTitle(title);
        if(message != null)builder.setMessage(message);

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_board,null);
        builder.setView(viewInflated);

        Button yesButton = (Button) viewInflated.findViewById(R.id.button_yes);
        Button noButton = (Button) viewInflated.findViewById(R.id.button_no);

//        builder.setPositiveButton("")
    }

}
