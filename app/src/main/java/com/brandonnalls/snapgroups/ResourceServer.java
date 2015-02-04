package com.brandonnalls.snapgroups;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.CheckBox;

import java.lang.ref.WeakReference;

import de.robv.android.xposed.XposedBridge;

public class ResourceServer {
    private static WeakReference<Resources> snapchatResources, snapgroupResources;

    public static Resources getSnapGroupResources(Context anyContext) {
        try {
            if(snapgroupResources == null || snapgroupResources.get() == null) {
                snapgroupResources = new WeakReference<Resources>(anyContext.getPackageManager().getResourcesForApplication(SnapGroupsHooks.class.getPackage().getName()));
            }
            return snapgroupResources.get();
        } catch (PackageManager.NameNotFoundException nmne) {
            XposedBridge.log("SnapGroups GetSGResources exception:\n" + nmne.getStackTrace());
            return null;
        }
    }

    public static Drawable getSnapchatCheckboxDrawable(Context anyContext) {
        try {
            if(snapchatResources == null || snapchatResources.get() == null) {
                snapchatResources = new WeakReference<Resources>(anyContext.getPackageManager().getResourcesForApplication("com.snapchat.android"));
            }
            return snapchatResources.get()
                    .getDrawable(snapchatResources.get().getIdentifier("send_to_button_selector", "drawable", "com.snapchat.android"));
        } catch (PackageManager.NameNotFoundException nmne) {
            XposedBridge.log("SnapGroups GetSCCheckbox exception:\n" + nmne.getStackTrace());
            return null;
        }
    }
}
