package py.com.smarttraffic.gpslocator;

import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;

public class Polygon{
    public final String name;
    public final Point point1;
    public final Point point2;
    public final Point point3;
    public final Point point4;
    //TODO: another atribute for the status of the spot...
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
        List<LatLng> resultList = new ArrayList<LatLng>() ;
        resultList.add(new LatLng(this.point1.latitud,this.point1.longitud));
        resultList.add(new LatLng(this.point2.latitud,this.point2.longitud));
        resultList.add(new LatLng(this.point3.latitud,this.point3.longitud));
        resultList.add(new LatLng(this.point4.latitud,this.point4.longitud));
        resultList.add(new LatLng(this.point1.latitud,this.point1.longitud));
        return resultList;
    }
}
