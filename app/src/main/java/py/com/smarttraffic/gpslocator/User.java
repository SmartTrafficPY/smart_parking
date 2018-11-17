package py.com.smarttraffic.gpslocator;

import android.util.Log;

public class User {

    private final String firstname,lastname;
    public final int userID;
    protected static final String TAG = User.class.getSimpleName();

    public User(String firstname, String lastname, int userId) {
        if (userId == Constants.NOT_USER){
            Log.e(TAG, String.valueOf(R.string.USER_ID_RESERVED));
        }
        this.firstname = firstname;
        this.lastname = lastname;
        this.userID = userId;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public int getUserID() {
        return userID;
    }
}
