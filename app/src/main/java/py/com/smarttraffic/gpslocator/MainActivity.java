package py.com.smarttraffic.gpslocator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MainActivity extends AppCompatActivity {
    private LocationManager locationManager;
    private LocationListener locationListener;
    private TextView coordenates;
    private TextView parkingspot;
    private Button startGps;
    private Location UCAcampus = new Location("dummyprovider");
    private Location testLocation = new Location("dummprovider");
    private Button stopGps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startGps = (Button) findViewById(R.id.startGPS);
        stopGps = (Button) findViewById(R.id.stopGPS);
        coordenates = (TextView) findViewById(R.id.coordenates);
        parkingspot = (TextView) findViewById(R.id.parkingspot);
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        // Point of the UCA campus to get the distance to the place in consideration...
        UCAcampus.setLatitude(-25.323740);
        UCAcampus.setLongitude(-57.638405);

        // Saves Polygon in list...
        ArrayList<Polygon> parkingSpots = new ArrayList<Polygon>();

        try {
            parkingSpots = kmlOpenPolygonFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final ArrayList<Polygon> finalParkingSpots = parkingSpots;
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                coordenates.setText("Ubicacion Actual" + "\n Latitud: " + location.getLatitude() + "\tLongitud: " + location.getLongitude() + "\nPrecision\n" + location.getAccuracy() + "\nDistancia a la facultad\n" + location.distanceTo(UCAcampus));
                savePointInGpxFile(location);
                pointInsideParkingSpot(finalParkingSpots,location);
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

        pointInsideParkingSpot(parkingSpots,testLocation);

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
    //Activity Bar Sharebuttom............................................................

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

    //For a possible all files sharing feature...
    //-----------------------------------------------//

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

    public ArrayList<Uri> getUrisFilesToShare(){
        ArrayList<Uri> gpxFiles = new ArrayList<>();
        File[] files = getFilesDir().listFiles();
        for(int i = 0; i < files.length; i++){
            Uri path = FileProvider.getUriForFile(this,"py.com.smarttraffic.gpslocator",files[i]);
            gpxFiles.add(path);
        }
        return gpxFiles;
    }

    //This will try to open de .kml file to get the

    public ArrayList<Polygon> kmlOpenPolygonFile() throws IOException {

        //Open the file and then pass it to the xml analyzer to get the data information ...
        String fileName = "Parking Spots.kml";
        String Directory = "PolyMap";
        File kmlDir = new File(this.getFilesDir(),Directory);
        ArrayList<Polygon> Polygons = new ArrayList<Polygon>();
        ArrayList Places = new ArrayList();
        ArrayList coordinates = new ArrayList();

        // Parser for kml files JSOUP...
        String inputFileContents = readFile(kmlDir + "/" + fileName); // find a way to read the file and store it in a string
        String xmlContent = inputFileContents;
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
        }
        return Polygons;
    }

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

    public static class Point{
        public double latitud;
        public double longitud;

        public Point(double latitud, double longitud) {
            this.latitud = latitud;
            this.longitud = longitud;
        }

        public double getLatitud() {
            return latitud;
        }

        public double getLongitud() {
            return longitud;
        }

        public LatLng toLatLng(){
            return new LatLng(latitud,longitud);
        }
    }

    public static class Polygon{
        public final String name;
        public final Point point1;
        public final Point point2;
        public final Point point3;
        public final Point point4;

        public Polygon(String name, Point point1, Point point2, Point point3, Point point4) {
            this.name = name;
            this.point1 = point1;
            this.point2 = point2;
            this.point3 = point3;
            this.point4 = point4;
        }

        public Point getPoint1() {
            return point1;
        }

        public Point getPoint2() {
            return point2;
        }

        public Point getPoint3() {
            return point3;
        }

        public Point getPoint4() {
            return point4;
        }

        public String getName() {
            return name;
        }

        public List<LatLng> toLatLngList(){
            List<LatLng> returnList = new List<LatLng>() {
                @Override
                public int size() {
                    return 0;
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public boolean contains(Object o) {
                    return false;
                }

                @NonNull
                @Override
                public Iterator<LatLng> iterator() {
                    return null;
                }

                @NonNull
                @Override
                public Object[] toArray() {
                    return new Object[0];
                }

                @NonNull
                @Override
                public <T> T[] toArray(@NonNull T[] a) {
                    return null;
                }

                @Override
                public boolean add(LatLng latLng) {
                    return false;
                }

                @Override
                public boolean remove(Object o) {
                    return false;
                }

                @Override
                public boolean containsAll(@NonNull Collection<?> c) {
                    return false;
                }

                @Override
                public boolean addAll(@NonNull Collection<? extends LatLng> c) {
                    return false;
                }

                @Override
                public boolean addAll(int index, @NonNull Collection<? extends LatLng> c) {
                    return false;
                }

                @Override
                public boolean removeAll(@NonNull Collection<?> c) {
                    return false;
                }

                @Override
                public boolean retainAll(@NonNull Collection<?> c) {
                    return false;
                }

                @Override
                public void clear() {

                }

                @Override
                public LatLng get(int index) {
                    return null;
                }

                @Override
                public LatLng set(int index, LatLng element) {
                    return null;
                }

                @Override
                public void add(int index, LatLng element) {

                }

                @Override
                public LatLng remove(int index) {
                    return null;
                }

                @Override
                public int indexOf(Object o) {
                    return 0;
                }

                @Override
                public int lastIndexOf(Object o) {
                    return 0;
                }

                @NonNull
                @Override
                public ListIterator<LatLng> listIterator() {
                    return null;
                }

                @NonNull
                @Override
                public ListIterator<LatLng> listIterator(int index) {
                    return null;
                }

                @NonNull
                @Override
                public List<LatLng> subList(int fromIndex, int toIndex) {
                    return null;
                }
            };
            returnList.add(new LatLng(point1.latitud, point1.longitud));
            returnList.add(new LatLng(point2.latitud, point2.longitud));
            returnList.add(new LatLng(point3.latitud, point3.longitud));
            returnList.add(new LatLng(point4.latitud, point4.longitud));
            return returnList;
        }
    }

    public boolean isPointInsidePolygon(Polygon polygon, Point Point){
        //If the point is inside of the polygon in question...
        return PolyUtil.containsLocation(Point.latitud,Point.longitud,toLatLngList(polygon),true);
    }

    public boolean pointInsideParkingSpot(ArrayList<Polygon> Polygons, Location Point){
        // Determine if the Point is inside a Polygon of all the list in Polygons...
        for(int i = 0; i < Polygons.size(); i++){
            if (isPointInsidePolygon(Polygons.get(i),locationToPoint(Point))){
                parkingspot.setText("You are inside the spot" + Polygons.get(i).name);
                return true;
            }else{
                continue;
            }
        }
        parkingspot.setText("No estas en ningun poligono");
        return false;
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

}