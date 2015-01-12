package com.brandonnalls.snapgroups;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;

public class FriendManager {

    static final String USER_TO_DISPLAY_MAP_FIELD_DISPLAYNAME = "displayname";
    static final String USER_TO_DISPLAY_MAP_FIELD_USERNAME = "username";


    private HashMap<String, String> mUserToDisplayNameMap; /* Map that links usernames to display names. For read-only display purposes. */
    private Activity mContext;

    public FriendManager(Activity context) {
        mContext = context;
    }

    /**
     * Turns the display name map into a list of maps containing friend info.
     * This info is used for a ListView's SimpleAdapter.
     * Precondition: readFriendsFromBundle populates the display name map.
     * @return
     */
    public List<Map<String, String>> generateFriendListviewData() {
        //TODO: Sort by Display Name. Might already be done by SnapChat
        int totalFriends = mUserToDisplayNameMap.size();
        List<String> unsortedUsernames = new ArrayList(mUserToDisplayNameMap.keySet());

        ArrayList<Map<String, String>> finalList = new ArrayList<>(totalFriends);
        Map<String, String> currentMap;
        String currentUsername;
        for(int i = 0; i < totalFriends; i++) {
            currentMap = new HashMap<String, String>();
            currentUsername = unsortedUsernames.get(i);
            currentMap.put(USER_TO_DISPLAY_MAP_FIELD_USERNAME, unsortedUsernames.get(i));
            currentMap.put(USER_TO_DISPLAY_MAP_FIELD_DISPLAYNAME, mUserToDisplayNameMap.get(currentUsername));
            finalList.add(currentMap);
        }

        // Sort Friend List Data Map by DisplayName.
        // I can't just map from displayname to username since it's not 1:1 necessarily
        Collections.sort(finalList, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> map1, Map<String, String> map2) {
                return map1.get(USER_TO_DISPLAY_MAP_FIELD_DISPLAYNAME)
                        .compareTo(map2.get(USER_TO_DISPLAY_MAP_FIELD_DISPLAYNAME));
            }
        });

        return finalList;

    }

    public void readFriendsFromBundle(Bundle extras) {
        ArrayList<String> userNames;
        ArrayList<String> displayNames;
        if(extras.containsKey(GroupListActivity.BUNDLE_IN_EXTRA_USERNAME_LIST) && extras.containsKey(GroupListActivity.BUNDLE_IN_EXTRA_USERDISPLAYNAME_LIST) &&
                !(userNames = extras.getStringArrayList(GroupListActivity.BUNDLE_IN_EXTRA_USERNAME_LIST)).isEmpty() &&
                !(displayNames = extras.getStringArrayList(GroupListActivity.BUNDLE_IN_EXTRA_USERDISPLAYNAME_LIST)).isEmpty()) {
            if(userNames.size() == displayNames.size()) {
                HashMap<String, String> userToDisplayNameMap = new HashMap<String, String>(userNames.size());
                for (int i = 0; i < userNames.size(); i++) {
                    String userName = userNames.get(i);
                    String displayName = displayNames.get(i);

                    //Map the username to the display name if the display name is set.
                    userToDisplayNameMap.put(userName, displayName);
                }
                mUserToDisplayNameMap = userToDisplayNameMap;
            } else {
                Toast.makeText(mContext, "Friends not loaded correctly. SnapGroups needs an update...", Toast.LENGTH_LONG).show();
                XposedBridge.log("SnapGroups: Username and display name intent lists are different sizes. Need 1:1");
                Log.e("SnapGroups","Username and display name intent lists are different sizes. Need 1:1");
            }
        } else {
            Toast.makeText(mContext, "Friends not found. SnapGroups needs an update...", Toast.LENGTH_LONG).show();
            XposedBridge.log("SnapGroups: Friends not found in the intent...");
            Log.e("SnapGroups","Friends not found in the intent...");
        }
    }

    public int getFriendCount() {
        return mUserToDisplayNameMap.size();
    }
}