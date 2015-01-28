package com.brandonnalls.snapgroups;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getParameterTypes;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SnapGroupsHooks implements IXposedHookLoadPackage {

    /* Arbitrary Constants */
    public static final String groupButtonContainerName = "sharedButtonContainer"; //This contains SnapAll + SnapGroups
    public static final int SNAPGROUP_RESPONSE_SUCCESS_CONSTANT = 12;
    public static final int SNAPGROUP_REQUEST_CONSTANT = 13;

    public static Activity sContextActivity;
    public static Object sContextSendToFragment;

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.snapchat.android"))
            return;

        try {
            findAndHookMethod("com.snapchat.android.LandingPageActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    sContextActivity = (Activity) methodHookParam.thisObject;
                }
            });

            findAndHookMethod("com.snapchat.android.LandingPageActivity", lpparam.classLoader, "onActivityResult", int.class, int.class, Intent.class, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    sContextActivity = (Activity) methodHookParam.thisObject; // Just in case Context changed while SnapGroups was open

                    if (methodHookParam.args.length >= 3) {
                        Integer requestCode = (Integer) methodHookParam.args[0];
                        Integer responseCode = (Integer) methodHookParam.args[1];
                        Intent responseIntent = (Intent) methodHookParam.args[2];
                        if (requestCode != null && responseCode != null
                                && requestCode == SNAPGROUP_REQUEST_CONSTANT
                                && responseCode == SNAPGROUP_RESPONSE_SUCCESS_CONSTANT) {
                            if (responseIntent != null && responseIntent.hasExtra(GroupListActivity.BUNDLE_OUT_EXTRA_USERNAME_LIST)) {
                                HashSet<String> usernamesToAdd = new HashSet<String>(responseIntent.getStringArrayListExtra(GroupListActivity.BUNDLE_OUT_EXTRA_USERNAME_LIST));
                                checkmarkSnapgroupUsers(usernamesToAdd);
                            } else {
                                XposedBridge.log("SnapGroups : Response from SnapGroups has no extras, but good response");
                                Log.e("SnapGroups", "Response from SnapGroups has no extras, but good response");
                            }

                            methodHookParam.setResult(null);
                        } else {
                            XposedBridge.log("SnapGroups : onActivityResult code not from snapgroups");
                            Log.e("SnapGroups", "onActivityResult code not from snapgroups");
                        }
                    } else {
                        XposedBridge.log("SnapGroups : Not enough response params in onActivityResult");
                        Log.e("SnapGroups", "Not enough response params in onActivityResult");
                    }
                }
            });

            /** Adds the SnapGroups Button to the SendTo Action Bar
             * Method "h" that I'm overriding does many findViewByIds on it's own buttons.
             * This will add a button, and link it to my GroupListActivity, sending the necessary
             * friend information.
             * */
            findAndHookMethod("com.snapchat.android.fragments.sendto.SendToFragment", lpparam.classLoader, "h", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (sContextActivity == null) {
                        XposedBridge.log("SnapGroups: context null, doing nothing...");
                        return;
                    }

                    //Assigning a SendToFragment to Object. Can't use "Fragment" since snapchat has appcompat roots.
                    if (param.thisObject.getClass().getCanonicalName().equals("com.snapchat.android.fragments.sendto.SendToFragment"))
                        sContextSendToFragment = param.thisObject;
                    else {
                        XposedBridge.log("SnapGroups: Didn't find SendToFragment 'Context'.. Needed for arrayadatpers.");
                        Log.e("SnapGroups", "Didn't find SendToFragment 'Context'.. Needed for arrayadatpers.");
                        return;
                    }

                    try {
                        //Otherbutton (Var b) is from R.java send_to_action_bar_search_button = 2131362379
                        ///   reverse looked that # up in method SendToFragment "h" where it findsViewById(2131362379) via alias method.
                        View otherButton = (View) getObjectField(param.thisObject, "b");

                        //Creates a container for SnapAll and SnapGroup XPosed mod buttons (if it doesn't exist already)
                        // and puts this snapGroup button into the container, next to "otherbutton" (preexisting snapchat button)
                        LinearLayout sharedButtonContainer = (LinearLayout) getAdditionalInstanceField(param.thisObject, groupButtonContainerName);
                        if(sharedButtonContainer == null || !(sharedButtonContainer instanceof LinearLayout)) {
                            RelativeLayout.LayoutParams myParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            myParams.setMargins(0, 0, 0, 0);
                            myParams.addRule(RelativeLayout.LEFT_OF, otherButton.getId());
                            myParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                            sharedButtonContainer = new LinearLayout(sContextActivity);
                            sharedButtonContainer.setOrientation(LinearLayout.HORIZONTAL);
                            sharedButtonContainer.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
                            ((RelativeLayout)otherButton.getParent()).addView(sharedButtonContainer, myParams);
                            setAdditionalInstanceField(param.thisObject, groupButtonContainerName, sharedButtonContainer);
                        }

                        ImageButton snapGroupButton = new ImageButton(sContextActivity);
                        snapGroupButton.setImageDrawable(getSnapGroupResources(sContextActivity).getDrawable(R.drawable.group_icon));
                        snapGroupButton.setBackgroundColor(Color.argb(0, 0, 0, 0));
                        snapGroupButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        snapGroupButton.setPadding(10, 10, 10, 10);
                        snapGroupButton.setAdjustViewBounds(true);
                        snapGroupButton.setOnClickListener(getSnapGroupButtonClickListener());
                        LinearLayout.LayoutParams snapGroupButtonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        snapGroupButtonParams.setMargins(0,0,30,0);
                        sharedButtonContainer.addView(snapGroupButton, snapGroupButtonParams);

                    } catch (Throwable t) {
                        XposedBridge.log("SnapGroups failed to insert SnapGroups Button." + t);
                        return;
                    }
                }
            });

            /**
             * These hide the SnapGroupButton while the search box is displayed
             */
            findAndHookMethod("com.snapchat.android.fragments.sendto.SendToFragment", lpparam.classLoader, "o", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View v = (View) getAdditionalInstanceField(param.thisObject, groupButtonContainerName);
                    v.setVisibility(View.VISIBLE);
                }
            });

            findAndHookMethod("com.snapchat.android.fragments.sendto.SendToFragment", lpparam.classLoader, "p", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View v = (View) getAdditionalInstanceField(param.thisObject, groupButtonContainerName);
                    v.setVisibility(View.INVISIBLE);
                }
            });

        } catch (Exception e) {
            XposedBridge.log("SnapGroups : Unhandled exception: "+e.getStackTrace());
            Log.e("SnapGroups", "Unhandled exception: "+e.getStackTrace());
        }
    }

    public static void checkmarkSnapgroupUsers(HashSet<String> usernamesToCheck) {
        ArrayList friends = getFriendList();

        //From SendtoFragment.. the two collections, one is a list, one set
        Object hopefullyDestinationFriendSet = (Set) getObjectField(sContextSendToFragment, "l");
        if(hopefullyDestinationFriendSet != null && hopefullyDestinationFriendSet instanceof LinkedHashSet) {
            LinkedHashSet destinationFriendSet = (LinkedHashSet) hopefullyDestinationFriendSet;
            try {
                boolean changedMade = false;
                for(Object friend : friends) {
                    //This is from com.snapchat.android.model.Friend, clearly the username
                    if(usernamesToCheck.contains((String)getObjectField(friend, "mUsername"))) {
                        destinationFriendSet.add(friend);
                        changedMade = true;
                    }
                }
                // This method "b" in SendToFragment that makes the bottom Blue SentToBar contain
                // a comma separated list of friend display names.
                // It has iterators (localInterator1) and such.
                if(changedMade) {
                    callMethod(sContextSendToFragment, "b");
                }
            } catch(Exception e) {
                XposedBridge.log("SnapGroups : Failed to add SnapGroups to Friend List. Exception: "+e.getStackTrace());
                Log.e("SnapGroups","Failed to add SnapGroups to Friend List. Exception: "+e.getStackTrace());
            }
        } else {
            XposedBridge.log("SnapGroups : Couldn't find destination friend set");
            Log.e("SnapGroups","Couldn't find destination friend set");
            Toast.makeText(sContextActivity, "SnapGroups needs an update. It couldn't write to destination friend set.", Toast.LENGTH_LONG).show();
        }

        //get destinationfriendset.. then filter & add... then call bluebottombarmethod
    }

    public static View.OnClickListener getSnapGroupButtonClickListener() {
        return new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    Intent launchIntent = new Intent("com.brandonnalls.snapgroups.mainactivity");
                    ArrayList[] users = getFriendInfo();
                    launchIntent.putStringArrayListExtra(GroupListActivity.BUNDLE_IN_EXTRA_USERNAME_LIST, (ArrayList<String>)users[0]);
                    launchIntent.putStringArrayListExtra(GroupListActivity.BUNDLE_IN_EXTRA_USERDISPLAYNAME_LIST, (ArrayList<String>)users[1]);
                    sContextActivity.startActivityForResult(launchIntent, SNAPGROUP_REQUEST_CONSTANT);
                } catch (Exception e) {
                    XposedBridge.log("SnapGroups ClickListener Exception\n"+e.getStackTrace().toString());
                    Log.e("SnapGroups", "ClickListener Exception\n" + e.getStackTrace().toString());
                }
            }
        };
    }

    /**
     * Finds the users in the friend lists.
     * @return ArrayList[0] is the usernames, and ArrayList[1] is the display names
     */
    public static ArrayList[] getFriendInfo() {

        //Source list
        ArrayList friendList = getFriendList();

        //Destination (return these) lists
        ArrayList<String> usernameList = new ArrayList(friendList.size());
        ArrayList<String> userDisplayNameList = new ArrayList(friendList.size());

        for(Object friend : friendList) {

            String username = (String) getObjectField(friend, "mUsername");
            String userDisplayName = (String) getObjectField(friend, "mDisplayName");
            if (TextUtils.isEmpty(username) && TextUtils.isEmpty(userDisplayName)) {
                XposedBridge.log("SnapGroups found empty username/displayname in getFriendInfo(). Skipping a user");
                Log.e("SnapGroups", "found empty username / displayname in getFriendInfo(). Skipping a user");
            } else if (!TextUtils.isEmpty(username) && TextUtils.isEmpty(userDisplayName)) {
                usernameList.add(username);
                userDisplayNameList.add(username);
            } else {
                usernameList.add(username);
                userDisplayNameList.add(userDisplayName);
            }
        }
        return new ArrayList[]{ usernameList, userDisplayNameList };
    }

    /**
     * Filters through arrayadapters, lists, stories, and returns pure Friends. There may be duplicates?
     * @return com.snapchat.android.model.Friend "Friend" ArrayList
     */
    public static ArrayList getFriendList() {
        //SendToAdapter : var d is the only SendToAdpater in SendToFragment
        Object hopefullyArrayAdapter =  getObjectField(sContextSendToFragment, "d");
        if(hopefullyArrayAdapter != null && hopefullyArrayAdapter instanceof ArrayAdapter){
            ArrayAdapter aa = (ArrayAdapter) hopefullyArrayAdapter;

            //ArrayList d or e from SendToAdapter, both seem to contain all the users...
            Object hopefullyFriendAndStoryList = getObjectField(aa, "d");

            if(hopefullyFriendAndStoryList != null && hopefullyFriendAndStoryList instanceof ArrayList) {
                //Source list
                ArrayList friendAndStoryList = (ArrayList) hopefullyFriendAndStoryList;
                ArrayList friendList = new ArrayList(friendAndStoryList.size());
                Class<?>[] types = getParameterTypes(friendAndStoryList.toArray());
                for (int i = 0; i < types.length; i++) {
                    Object thingToAdd = friendAndStoryList.get(i);
                    if (thingToAdd != null && types[i].getCanonicalName().equals("com.snapchat.android.model.Friend")) {
                        friendList.add(thingToAdd);
                    } else if (types[i].getCanonicalName().equals("com.snapchat.android.model.MyPostToStory")) {
                        //NOOP
                    } else {
                        XposedBridge.log("SnappGroups: Found unknown type: " + types[i].toString());
                    }
                }
                return friendList;
            } else {
                XposedBridge.log("SnapGroups couldn't find Friend List.");
            }

        } else {
            XposedBridge.log("SnapGroups couldn't find ArrayAdapter");
        }
        Toast.makeText(sContextActivity, "SnapGroups thinks you have no friends :( Tell us it needs an update.", Toast.LENGTH_LONG).show();
        return new ArrayList();
    }

    public static Resources getSnapGroupResources(Context context) {
        try {
            final Resources res = context.getPackageManager().getResourcesForApplication(SnapGroupsHooks.class.getPackage().getName());
            return res;
        } catch (PackageManager.NameNotFoundException nmne) {
            XposedBridge.log("SnapGroups exception:\n"+nmne.getStackTrace());
            return null;
        }
    }


}

//todo: factor out all obfuscated code & put somewhere it's explainable

