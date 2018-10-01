package py.com.smarttraffic.gpslocator;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StackOverflowXmlParser {

    private static final String ns = null;

    public List parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readKml(parser);
        } finally {
            in.close();
        }
    }

    private List readKml(XmlPullParser parser) throws XmlPullParserException, IOException {
        List entries = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "kml");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the Placemark tag
            if (name.equals("Placemark")) {
                entries.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    public static class Point{
        public final String latitud;
        public final String longitud;
        public final String altitud;

        public Point(String latitud, String longitud, String altitud) {
            this.latitud = latitud;
            this.longitud = longitud;
            this.altitud = altitud;
        }
    }

    public static class Polygon{
        public final Point point1;
        public final Point point2;
        public final Point point3;
        public final Point point4;

        public Polygon(Point point1, Point point2, Point point3, Point point4) {
            this.point1 = point1;
            this.point2 = point2;
            this.point3 = point3;
            this.point4 = point4;
        }
    }

    public static class Entry{
        public final String name;
        public final String coordinates;

        private Entry(String name, String coordinates) {
            this.name = name;
            this.coordinates = coordinates;
        }
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.

    private Entry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Placemark");
        String placemarkName = null;
        String spotPlace = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("name")) {
                placemarkName = readPlacemark(parser);
            } else if (name.equals("coordinates")) {
                spotPlace = readCoordenates(parser);
            } else {
                skip(parser);
            }
        }
        return new Entry(placemarkName, spotPlace);
    }

    // Processes namePlacemark tags in the feed.

    private String readPlacemark(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "name");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "name");
        return title;
    }

    // Processes summary tags in the feed.

//    private Polygon readPolygon(XmlPullParser parser) throws IOException, XmlPullParserException {
//        parser.require(XmlPullParser.START_TAG, ns, "coordinates");
////        Polygon spot = readCoordinates(parser);
//        parser.require(XmlPullParser.END_TAG, ns, "coordinates");
//        return spot;
//    }

    private String readCoordenates(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "coordinates");
        String spot = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "coordinates");
        return spot;
    }

    // For the tags title and summary, extracts their text values.


    // For the tags title and summary, extracts their text values.

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}
