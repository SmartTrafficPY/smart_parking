package py.com.smarttraffic.gpslocator;

import com.google.android.gms.maps.model.LatLng;

public class Point {

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
