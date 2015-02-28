package com.brandonnalls.snapgroups;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public class GroupListActivity extends ListActivity implements CacheChangedListener {

    private GroupManager mGroupManager;
    private FriendManager mFriendManager;
    private GroupListAdapter mListAdapter;

    public static final String BUNDLE_IN_EXTRA_USERNAME_LIST = "snapchat_incoming_usernames";
    public static final String BUNDLE_IN_EXTRA_USERDISPLAYNAME_LIST = "snapchat_incoming_user_displaynames";
    public static final String BUNDLE_OUT_EXTRA_USERNAME_LIST = "snapchat_outgoing_usernames";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGroupManager = new GroupManager(this, this);
        mFriendManager = new FriendManager(this);
        mFriendManager.readFriendsFromBundle(getIntent().getExtras());
        mListAdapter = new GroupListAdapter(this);
        setListAdapter(mListAdapter);
        setContentView(R.layout.group_list_activity);
        setSendButtonClickListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_grouplistactivity_overflow, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.overflow_action_new) {
            showAddGroupDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAddGroupDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.dialog_add_group_edittext_hint);
        input.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_add_group_title)
                .setView(input)
                .setPositiveButton(R.string.dialog_button_create, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable newGroupName = input.getText();
                        mGroupManager.createGroup(newGroupName.toString());

                    }
                }).setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    private void showRenameGroupDialog(final String oldName) {
        final EditText input = new EditText(this);
        input.setText(oldName);
        input.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_rename_group_title)
                .setMessage(getResources().getString(R.string.dialog_rename_group_message, oldName))
                .setView(input)
                .setPositiveButton(R.string.dialog_button_rename, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable newName = input.getText();
                        mGroupManager.renameGroup(oldName, newName.toString());

                    }
                }).setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    private void showDeleteGroupDialog(final String groupName) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_group_title)
                .setMessage(getResources().getString(R.string.dialog_delete_group_message, groupName))
                .setPositiveButton(R.string.dialog_button_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mGroupManager.deleteGroup(groupName);
                    }
                }).setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    private static View.OnClickListener pencilClickListener(final View groupRow) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Rotate button
                final View actionBar = groupRow.findViewById(R.id.group_row_action_bar);
                final View showButton = groupRow.findViewById(R.id.group_row_show_edit_bar_button);
                final int angleChange = actionBar.getVisibility() == View.VISIBLE ? -180 : 180;
                final RotateAnimation animation = new RotateAnimation(0, angleChange, showButton.getMeasuredWidth() / 2, showButton.getMeasuredHeight() / 2);
                animation.setDuration(250);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation)  { /* NOP */  }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        //Toggle action bar visibility
                        actionBar.setVisibility(actionBar.getVisibility() == View.VISIBLE ?
                                View.GONE : View.VISIBLE);
                        showButton.setRotation(showButton.getRotation() + angleChange);
                    }

                    @Override public void onAnimationRepeat(Animation animation) { /* NOP */  }
                });
                showButton.startAnimation(animation);

            }
        };
    }

    private void showEditGroupMembersDialog(final String groupName) {
        //Create listview and adapter
        final List<Map<String, String>> dataList = mFriendManager.generateFriendListviewData();
        final FriendListAdapter adapter = new FriendListAdapter(this,
                dataList,
                R.layout.friend_row,
                new String[] {FriendManager.USER_TO_DISPLAY_MAP_FIELD_DISPLAYNAME, FriendManager.USER_TO_DISPLAY_MAP_FIELD_USERNAME},
                new int[] {R.id.friend_row_displayname, R.id.friend_row_username});
        final ListView userListView = new ListView(this);
        userListView.setFastScrollEnabled(true);
        userListView.setAdapter(adapter);
        userListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        final HashSet<String> usersAlreadyInGroup = mGroupManager.getGroupsUsers(groupName);

        //Pre-check the friends that were already in the group
        for(int i = 0; i < dataList.size(); i++) {
            Map<String, String> currentUserData = dataList.get(i);
            if(usersAlreadyInGroup
                    .contains(currentUserData.get(FriendManager.USER_TO_DISPLAY_MAP_FIELD_USERNAME))) {
                adapter.setItemActuallyChecked(i, true);
            } else {
                adapter.setItemActuallyChecked(i, false); //Likely unnecessary if default bool array isn't false
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_edit_group_title, groupName))
                .setView(userListView)
                .setPositiveButton(R.string.dialog_button_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        //This doesn't simply read the checked items and save those corresponding usernames.
                        //If thats the case, but snapchat didn't pass us a username (bug in snapgroups compatibility?)
                        // and the user hits save, we would delete that user.
                        //Instead, we use the checked item list
                        // to add & remove users from the group, without changing users that were
                        // added but not mentioned when reading snapchat friend data.
                        HashSet<String> newUserList = (HashSet<String>) usersAlreadyInGroup.clone();
                        boolean[] checkedItemList = adapter.getActuallyCheckedItems();
                        for (int i = 0; i < checkedItemList.length; i++) {
                            String username = dataList.get(i).get(FriendManager.USER_TO_DISPLAY_MAP_FIELD_USERNAME);
                            if (checkedItemList[i])
                                newUserList.add(username);
                            else
                                newUserList.remove(username);
                        }
                        mGroupManager.setGroupsUsers(groupName, newUserList);
                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        // Do nothing
                    }
                })
                .show();
    }

    /**
     * Called for any group (not friend) related changes.
     * Friend changes should only happen between activity launches since info comes from intent, and
     * this activity doesn't store friend user info except usernames in a group.
     */
    public void onCacheChange() {
        mListAdapter.refreshData();
    }

    public void setSendButtonClickListener() {
        Button sendButton = (Button) findViewById(R.id.send_to_groups_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashSet<String> userList = new HashSet<String>(mFriendManager.getFriendCount());
                GroupListAdapter adapter = (GroupListAdapter) getListView().getAdapter();
                for(String group : adapter.getCheckedGroups()) {
                    userList.addAll(mGroupManager.getGroupsUsers(group));
                }
                //Don't let the user select 200+ recipients (SnapChat Server enforced)

                if(userList.size() >= 200) {
                    Toast.makeText(GroupListActivity.this, R.string.too_many_friends_in_checked_groups, Toast.LENGTH_LONG).show();
                } else {
                    //Send username list to other activity
                    Intent responseData = new Intent();
                    responseData.putStringArrayListExtra(BUNDLE_OUT_EXTRA_USERNAME_LIST, new ArrayList<>(userList));
                    GroupListActivity.this.setResult(SnapGroupsHooks.SNAPGROUP_RESPONSE_SUCCESS_CONSTANT, responseData);
                    finish();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private class GroupListAdapter extends ArrayAdapter<String> {
        private ArrayList<String> mCurrentGroupsForList;
        private Map<String, Boolean> mGroupCheckedRecord; //Using a map (groupName -> checked?) since position of a group can change when user edits groups. We need to store checkmarks by group name

        public GroupListAdapter(Context context) {
            super(context, R.layout.group_row, R.id.group_row_name);
            refreshData();
        }

        public HashSet<String> getCheckedGroups() {
            HashSet<String> checkedGroups = new HashSet<String>(mGroupCheckedRecord.size());
            for(Map.Entry<String, Boolean> entry : mGroupCheckedRecord.entrySet()) {
                if(entry.getValue() == null ? false : entry.getValue())
                    checkedGroups.add(entry.getKey());
            }
            return checkedGroups;
        }

        public void refreshData() {
            mCurrentGroupsForList = mGroupManager.getGroupNames();
            if(mGroupCheckedRecord == null)
                mGroupCheckedRecord = new HashMap<>(mCurrentGroupsForList.size());
            clear();
            addAll(mCurrentGroupsForList);
            notifyDataSetChanged();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final View row = super.getView(position, convertView, parent);
            final String groupName = mCurrentGroupsForList.get(position);
            final String groupNameWithCount = groupName + " (" + mGroupManager.getGroupUserCount(groupName) + ")";

            //Change name string so it has the group size count
            ((TextView)row.findViewById(R.id.group_row_name)).setText(groupNameWithCount);

            //Set button listeners
            row.findViewById(R.id.group_row_delete_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDeleteGroupDialog(groupName);
                }
            });
            row.findViewById(R.id.group_row_rename_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showRenameGroupDialog(groupName);
                }
            });
            row.findViewById(R.id.group_row_edit_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showEditGroupMembersDialog(groupName);
                }
            });
            row.findViewById(R.id.group_row_show_edit_bar_button).setOnClickListener(pencilClickListener(row));

            final Drawable snapchatCBDrawable = ResourceServer.getSnapchatCheckboxDrawable(GroupListActivity.this);
            Boolean checkStatus = mGroupCheckedRecord.get(groupName);
            checkStatus = checkStatus == null ? false : checkStatus; //Maps can return null for Boolean

            CheckBox cb = (CheckBox) row.findViewById(R.id.group_row_checkbox);
            cb.setOnCheckedChangeListener(null); //Due to recycling glitches
            cb.setChecked(checkStatus);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mGroupCheckedRecord.put(groupName, b);
                }
            });

            //Make the checkbox match shapchat's appearance
            if(snapchatCBDrawable != null) {
                cb.setButtonDrawable(snapchatCBDrawable);
            }

            return row;
        }
    }

    public class FriendListAdapter extends SimpleAdapter {
        private boolean[] actuallyCheckedItems;

        public FriendListAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
            actuallyCheckedItems = new boolean[data.size()];
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final View fromSuper = super.getView(position, convertView, parent);
            final Drawable snapchatCBDrawable = ResourceServer.getSnapchatCheckboxDrawable(GroupListActivity.this);
            final CheckBox cb = (CheckBox) fromSuper.findViewById(R.id.friend_row_checkbox);
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(actuallyCheckedItems[position]);
            cb.setScaleX(1);
            cb.setScaleY(1);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    setItemActuallyChecked(position, b);
                }
            });
            if(snapchatCBDrawable != null) {
                cb.setButtonDrawable(snapchatCBDrawable);
            }
            return fromSuper;
        }

        public void setItemActuallyChecked(int position, boolean newVal) {
            actuallyCheckedItems[position] = newVal;
        }

        public boolean[] getActuallyCheckedItems() {
            return actuallyCheckedItems;
        }
    }
}

//TODO: Ensure this activity gets killed & doesn't live after it closes (to force data refresh)
//TODO: Use LogCat "verbose" with search "Snap" on app: No Filters to find hidden errors / issues.
//TODO: Bug: Open editor, hit "home", then open SnapGroups from recents. Notice: Crash. COuld be intent flags, onHooking "h", or could be snapchat bug
//TODO: Add up button navigation to snapchat