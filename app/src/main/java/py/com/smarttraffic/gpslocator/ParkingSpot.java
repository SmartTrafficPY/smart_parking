package py.com.smarttraffic.gpslocator;

import java.util.Calendar;
import java.util.Date;

public class ParkingSpot {

    public final Polygon polygon;
    public String stateOfSpot;
    public String lastStateOfSpot;

    public Date timeOfStateChange;
    public int userID;

    public ParkingSpot(String stateOfSpot,String lastStateOfSpot, Polygon polygon, Date momentOfChange,int userID) {
        this.stateOfSpot = stateOfSpot;
        this.lastStateOfSpot = lastStateOfSpot;
        this.polygon = polygon;
        this.timeOfStateChange = momentOfChange;
        this.userID = userID;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public Date getTimeOfStateChange() {
        return timeOfStateChange;
    }

    public void setTimeOfStateChange(Date timeOfStateChange) {
        this.timeOfStateChange = timeOfStateChange;
    }

    public String getState() {
        return stateOfSpot;
    }

    public void setState(String state){
        this.stateOfSpot = state;
    }

    public String getLastStateOfSpot() {
        return lastStateOfSpot;
    }

    public void setLastStateOfSpot(String lastStateOfSpot) {
        //Should set the userID to unknown to!...
//        this.setUserID(Constants.NOT_USER);
        this.lastStateOfSpot = lastStateOfSpot;
    }

    public String getRealState(){
        Date rightNow = Calendar.getInstance().getTime();
        Date lastTimeUpdated = getTimeOfStateChange();
        if(lastTimeUpdated == null){
            setState(Constants.UNKNOWN_STATE);
            setMomentOfChange();
            setUserID(Constants.NOT_USER);
        }else{
            long differentMin = (rightNow.getTime() - lastTimeUpdated.getTime())/Constants.MILLI_IN_SECONDS;
            if(differentMin > Constants.LONG_TIME_CONSIDERED
                    && getState() == Constants.FREE_STATE){
                setState(Constants.UNKNOWN_STATE);
                setMomentOfChange();
                setUserID(Constants.NOT_USER);
            }
        }
        return getState();
    }

    private void setMomentOfChange(){
        Date momentOfChange = Calendar.getInstance().getTime();
        setTimeOfStateChange(momentOfChange);
    }

    //....................FROM HERE:::STATE MANAGEMENT............................
    //when you set occupied, change the state to OCCUPIED(if it is FREE or UNKNOWN)...
    // moment of the change and the user that is occupying the spot...
    public boolean setOccupied(int userID){
        if(getRealState() != Constants.OCCUPIED_STATE
                && getRealState() != Constants.MOMENTARY_STATE){
            setLastStateOfSpot(getState());
            setState(Constants.OCCUPIED_STATE);
            setMomentOfChange();
            setUserID(userID);
            return true;
        }
        return false;
    }
    //when you set free, change the state to FREE...
    // moment of the change and the user to the NOT OCCUPIED ID user...
    //For setting free the user that has occupied the spot must be the one that makes it free...
    public boolean setFree(int userWhoSetsFreeID){
        if((getRealState() == Constants.OCCUPIED_STATE || getRealState() == Constants.MOMENTARY_STATE)
                && getUserID() == userWhoSetsFreeID){
            setState(Constants.FREE_STATE);
            setMomentOfChange();
            setUserID(Constants.NOT_USER);
            return true;
        }else{
            return false;
        }
    }

    //when you set unknown, change the state to UNKNOWN...
    // moment of the change and the user to the NOT OCCUPIED ID user...
    public void setUnknown(){
        if(getRealState() != Constants.UNKNOWN_STATE){
            setState(Constants.UNKNOWN_STATE);
            setMomentOfChange();
            setUserID(Constants.NOT_USER);
        }
    }

    //when you set momentary, change the state to MOMENTARY(if is not occupied too)...
    // moment of the change and the user to the occupying momentary the spot
    //this may mean that the spot will be free in a short period.
    public void setMomentary(int userID){
        if(getRealState() != Constants.MOMENTARY_STATE &&
                getRealState() != Constants.OCCUPIED_STATE){
            setState(Constants.MOMENTARY_STATE);
            setMomentOfChange();
            setUserID(userID);
        }
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

}
