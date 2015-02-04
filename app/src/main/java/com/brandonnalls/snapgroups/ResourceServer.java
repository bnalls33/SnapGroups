package com.brandonnalls.snapgroups;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import de.robv.android.xposed.XposedBridge;

public class ResourceServer {
    public static Resources getSnapGroupResources(Context context) {
        try {
            final Resources res = context.getPackageManager().getResourcesForApplication(SnapGroupsHooks.class.getPackage().getName());
            return res;
        } catch (PackageManager.NameNotFoundException nmne) {
            XposedBridge.log("SnapGroups GetSGResources exception:\n" + nmne.getStackTrace());
            return null;
        }
    }

    //public static
}
