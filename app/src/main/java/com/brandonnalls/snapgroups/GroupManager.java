package com.brandonnalls.snapgroups;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;

/**
 * This is a helper class that loads data from SharedPreferences & Manages the groups.
 * loadToCache reads from SharedPreferences, and saveCacheToDisk writes all changes.
 */
public class GroupManager {
    static final String PREF_ID_GROUP_LIST = "group_list";
    static final String PREF_ID_GROUP_PREFIX = "group_named_";

    private Activity mContext;
    private SharedPreferences mPrefs;
    private HashMap<String, HashSet<String>> mGroups; /* Maps Group Names to a set of UserNames */
    private CacheChangedListener mListener;

    public GroupManager(Activity context, CacheChangedListener listener) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mListener = listener;
        loadToCache();
    }

    public void createGroup(String userEnteredName) {
        String name = userEnteredName.trim();
        if(name.length() == 0){
            Toast.makeText(mContext, R.string.no_length_group_name, Toast.LENGTH_LONG).show();
        } else if(mGroups.containsKey(name) || mPrefs.contains(PREF_ID_GROUP_PREFIX + name)) {
            Toast.makeText(mContext, R.string.group_already_exists, Toast.LENGTH_SHORT).show();
        } else {
            mGroups.put(name, new HashSet<String>(0));
            saveCacheToDisk();
            mListener.onCacheChange();
        }
    }

    public void deleteGroup(String groupName) {
        if(mPrefs.contains(PREF_ID_GROUP_PREFIX + groupName) && mGroups.containsKey(groupName)) {
            mGroups.remove(groupName);
            saveCacheToDisk();
            mListener.onCacheChange();
        } else {
            Toast.makeText(mContext, mContext.getResources().getString(R.string.failed_to_delete_group, groupName), Toast.LENGTH_LONG).show();
            Log.e("SnapGroups", "Failed to delete group " + groupName + " in deleteGroup()");
            XposedBridge.log("SnapGroups : Failed to delete group "+groupName+" in deleteGroup()");
        }
    }

    public void renameGroup(String oldName, String newUserEnteredName) {
        String newName = newUserEnteredName.trim();
        if(newName.length() == 0){
            Toast.makeText(mContext, R.string.no_length_group_name, Toast.LENGTH_LONG).show();
        } else if(mPrefs.contains(PREF_ID_GROUP_PREFIX + oldName) && mGroups.containsKey(oldName)) {
            HashSet<String> usernames = mGroups.get(oldName);
            mGroups.remove(oldName);
            mGroups.put(newName, usernames);
            saveCacheToDisk();
            mListener.onCacheChange();
        } else {
            Toast.makeText(mContext, mContext.getResources().getString(R.string.failed_to_rename_group, oldName), Toast.LENGTH_LONG).show();
            Log.e("SnapGroups", "Failed to rename group " + oldName);
            XposedBridge.log("SnapGroups : Failed to rename group "+oldName);
        }
    }

    public void setGroupsUsers(String group, HashSet<String> usernames) {
        if(usernames != null) {
            mGroups.put(group, usernames);
            saveCacheToDisk();
            mListener.onCacheChange();
        } else {
            Log.e("SnapGroups", "Null set can't be set to groupList in setGroupUsers()");
            XposedBridge.log("SnapGroups : Null set can't be set to groupList in setGroupUsers()");
        }
    }

    public ArrayList<String> getGroupNames() {
        ArrayList<String> names = new ArrayList<>(mGroups.keySet());
        Collections.sort(names);
        return names;
    }

    public int getGroupUserCount(String group) {
        return mGroups.get(group).size();
    }

    public HashSet<String> getGroupsUsers(String groupName) {
        HashSet<String> userNames = mGroups.get(groupName);
        if(userNames != null) {
            return userNames;
        } else {
            Log.e("SnapGroups", "Failed to load group users " + groupName + "from cache.");
            XposedBridge.log("SnapGroups : Failed to load group users " + groupName + "from cache.");
            return new HashSet<String>(0);
        }
    }

    private void loadToCache() {
        //Set from getStringSet is READ ONLY
        Set<String> loadedGroups = mPrefs.getStringSet(PREF_ID_GROUP_LIST, new HashSet<String>(0));
        mGroups = new HashMap<>(loadedGroups.size());

        for(String curGroup : loadedGroups) {
            //Assert
            if (!mPrefs.contains(PREF_ID_GROUP_PREFIX + curGroup)) {
                Log.e("SnapGroups", "Failed to find group "+curGroup+" in loadToCache()");
                XposedBridge.log("SnapGroups : Failed to find group "+curGroup+" in loadToCache()");
            }

            Set<String> loadedUsernames = mPrefs.getStringSet(PREF_ID_GROUP_PREFIX + curGroup, new HashSet<String>(0));
            if(loadedGroups != null) {
                HashSet<String> usernameSet = new HashSet<>(loadedUsernames.size());
                usernameSet.addAll(loadedUsernames);
                mGroups.put(curGroup, usernameSet);
            } else {
                Log.e("SnapGroups", "Failed to load users from "+curGroup+" in loadToCache()");
                XposedBridge.log("SnapGroups : Failed to load users from "+curGroup+" in loadToCache()");
                mGroups.put(curGroup, new HashSet<String>(0));
            }
        }
    }

    /**
     * Writes Group List Cache to disk.
     * Note: It clears all SharedPreferences for this app before saving groups.
     */
    private void saveCacheToDisk() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.clear();

        Set<String> groupNames = mGroups.keySet();
        editor.putStringSet(PREF_ID_GROUP_LIST, groupNames);

        for(String curGroup : groupNames) {
            HashSet<String> usernames = mGroups.get(curGroup);

            //Assert
            if(usernames == null) {
                Log.e("SnapGroups", "Failed to read users from "+curGroup+" in saveCacheToDisk()");
                XposedBridge.log("SnapGroups : Failed to read users from " + curGroup + " in saveCacheToDisk()");
                editor.putStringSet(PREF_ID_GROUP_PREFIX + curGroup, new HashSet<String>(0));
            } else {
                editor.putStringSet(PREF_ID_GROUP_PREFIX + curGroup, usernames);
            }
        }
        editor.apply();
    }


}
