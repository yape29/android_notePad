/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import static com.example.android.notepad.NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE;

import com.example.android.notepad.NotePad;
import com.getbase.floatingactionbutton.FloatingActionButton;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 *
 * 显示注释列表。将显示来自传入 Intent 中提供的 {@link Uri} 的注释（如果有），否则默认显示 {@link NotePadProvider} 的内容。
 * 注意：请注意，此 Activity 中的提供程序操作在 UI 线程上进行。这不是一个好的做法。此处仅为了使代码更具可读性而执行此操作。
 * 实际应用程序应使用 {@link android.content.AsyncQueryHandler} 或 {@link android.os.AsyncTask} 对象在单独的线程上异步执行操作。
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            COLUMN_NAME_MODIFICATION_DATE,
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;

    /**
     * onCreate is called when Android starts this Activity from scratch.
     * onCreate 在 Android 从头开始启动此活动时调用
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // list布局加载
        setContentView(R.layout.notelist_main);
        // 新增note
        FloatingActionButton fabAddNote = (FloatingActionButton) findViewById(R.id.add_note);
        fabAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch NoteEditor to add a new note
                Intent intent = new Intent(Intent.ACTION_INSERT, NotePad.Notes.CONTENT_URI);
                startActivity(intent);
            }
        });

        // The user does not need to hold down the key to use menu shortcuts.
        // 用户无需按住该键即可使用菜单快捷方式。
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         *
         * 如果启动此活动的 Intent 中没有提供任何数据，则表示当 Intent 筛选条件与 MAIN 操作匹配时，
         * 此 Activity 已启动。我们应该使用默认的提供者 URI。
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView, and the context menu is handled by a method in NotesList.
         */
        getListView().setOnCreateContextMenuListener(this);

        /* Performs a managed query. The Activity handles closing and requerying the cursor
         * when needed.
         *
         * Please see the introductory note about performing provider operations on the UI thread.
         */
        Cursor cursor = managedQuery(
            getIntent().getData(),            // Use the default content URI for the provider.
            PROJECTION,                       // Return the note ID and title for each note.
            null,                             // No where clause, return all records.
            null,                             // No where clause, therefore no where column values.
            NotePad.Notes.DEFAULT_SORT_ORDER  // Use the default sort order.
        );

        /*
         * The following two arrays create a "map" between columns in the cursor and view IDs
         * for items in the ListView. Each element in the dataColumns array represents
         * a column name; each element in the viewID array represents the ID of a View.
         * The SimpleCursorAdapter maps them in ascending order to determine where each column
         * value will appear in the ListView.
         */

        // The names of the cursor columns to display in the view, initialized to the title column
        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE,
                COLUMN_NAME_MODIFICATION_DATE } ;

        // The view IDs that will display the cursor columns, initialized to the TextView in
        // noteslist_item.xml
        int[] viewIDs = { R.id.textTitle,
                R.id.textDate};

        // Creates the backing adapter for the ListView.
        final MyAdapter adapter
            = new MyAdapter(
                      this,                             // The Context for the ListView
                      R.layout.notelist_item4,          // Points to the XML for a list item
                      cursor,                           // The cursor to get items from
                      dataColumns,
                      viewIDs
              );

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        setListAdapter(adapter);

        // 设置多选模式
        final ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                adapter.setItemSelected(position, checked);

                int checkedCount = listView.getCheckedItemCount();
                if (checkedCount > 0) {
                    findViewById(R.id.delete_notes).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.delete_notes).setVisibility(View.GONE);
                }

                mode.setTitle(checkedCount + " selected");
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true; // 不需要显示菜单
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });

        final FloatingActionButton fabDelete = (FloatingActionButton) findViewById(R.id.delete_notes);
        // 设置删除按钮点击事件
        fabDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
                for (int i = 0; i < checkedItems.size(); i++) {
                    int position = checkedItems.keyAt(i);
                    if (checkedItems.valueAt(i)) {
                        Cursor selectedCursor = (Cursor) adapter.getItem(position);
                        long noteId = selectedCursor.getLong(selectedCursor.getColumnIndex(NotePad.Notes._ID));
                        Uri uri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, noteId);
                        getContentResolver().delete(uri, null, null);
                    }
                }
                adapter.clearSelection(); // 清空选中状态
                fabDelete.setVisibility(View.GONE);
            }
        });
        // 设置自定义视图绑定器，用于修改时间格式显示。这里使用内部类来实现SimpleCursorAdapter.ViewBinder接口。
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                // 检查视图是否是TextView类型，因为我们要修改的是文本显示。
                if (view instanceof TextView) {
                    // 获取当前列的列名，以便判断是否需要特殊处理。
                    String columnName = cursor.getColumnName(columnIndex);

                    // 判断当前列是否是修改日期列（假设NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE是修改日期的列名）。
                    if (columnName.equals(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
                        // 从Cursor中获取修改日期的时间戳（毫秒为单位）。
                        long dateInMillis = cursor.getLong(columnIndex);

                        // 将视图转换为TextView，以便设置文本。
                        TextView textView = (TextView) view;

                        // 创建一个SimpleDateFormat对象，用于格式化日期。这里使用"yyyy-MM-dd HH:mm:ss"格式，并指定默认语言环境。
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                        // 使用SimpleDateFormat格式化时间戳，并将结果设置为TextView的文本。
                        textView.setText(dateFormat.format(new Date(dateInMillis)));

                        // 返回true表示我们已经处理了该视图的值，不需要进一步处理。
                        return true;
                    }
                }

                // 如果视图不是TextView或者当前列不是修改日期列，返回false表示我们没有处理该视图的值，可能需要其他处理。
                return false;
            }
        });
    }


    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * Sets up a menu that provides the Insert option plus a list of alternative actions for
     * this Activity. Other applications that want to handle notes can "register" themselves in
     * Android by providing an intent filter that includes the category ALTERNATIVE and the
     * mimeTYpe NotePad.Notes.CONTENT_TYPE. If they do this, the code in onCreateOptionsMenu()
     * will add the Activity that contains the intent filter to its list of options. In effect,
     * the menu will offer the user other applications that can handle notes.
     * @param menu A Menu object, to which menu items should be added.
     * @return True, always. The menu should be displayed.
     * 当用户首次单击此 Activity 的 Menu 按钮时调用。
     * Android 传入一个填充了项目的 Menu 对象。
     * 设置一个菜单，该菜单提供 Insert 选项以及此活动的替代操作列表。
     * 其他想要处理便笺的应用程序可以通过提供包含类别 ALTERNATIVE 和 mimeTYpe NotePad.Notes.CONTENT_TYPE的 intent 过滤器，
     * 在 Android 中“注册”自身。如果他们这样做，onCreateOptionsMenu（） 中的代码会将包含 intent 过滤器的 Activity 添加到其选项列表中。
     * 实际上，该菜单将为用户提供其他可以处理笔记的应用程序。@param menu 一个 Menu 对象，应将菜单项添加到该对象中。@return 确实如此。应显示菜单。
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                Menu.NONE,                  // A unique item ID is not required.
                Menu.NONE,                  // The alternatives don't need to be in order.
                null,                       // The caller's name is not excluded from the group.
                specifics,                  // These specific options must appear first.
                intent,                     // These Intent objects map to the options in specifics.
                Menu.NONE,                  // No flags are required.
                items                       // The menu items generated from the specifics-to-
                                            // Intents mapping
            );
                // If the Edit menu item exists, adds shortcuts for it.
                if (items[0] != null) {

                    // Sets the Edit menu item shortcut to numeric "1", letter "e"
                    items[0].setShortcut('1', 'e');
                }
            } else {
                // If the list is empty, removes any existing alternative actions from the menu
                menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
            }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user selects an option from the menu, but no item
     * in the list is selected. If the option was INSERT, then a new Intent is sent out with action
     * ACTION_INSERT. The data from the incoming Intent is put into the new Intent. In effect,
     * this triggers the NoteEditor activity in the NotePad application.
     *
     * If the item was not INSERT, then most likely it was an alternative option from another
     * application. The parent method is called to process the item.
     * @param item The menu item that was selected by the user
     * @return True, if the INSERT menu item was selected; otherwise, the result of calling
     * the parent method.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add:
          /*
           * Launches a new Activity using an Intent. The intent filter for the Activity
           * has to have action ACTION_INSERT. No category is set, so DEFAULT is assumed.
           * In effect, this starts the NoteEditor Activity in NotePad.
           */
           startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
           return true;
        case R.id.menu_paste:
          /*
           * Launches a new Activity using an Intent. The intent filter for the Activity
           * has to have action ACTION_PASTE. No category is set, so DEFAULT is assumed.
           * In effect, this starts the NoteEditor Activity in NotePad.
           */
          startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
          return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

//    /**
//     * This method is called when the user context-clicks a note in the list. NotesList registers
//     * itself as the handler for context menus in its ListView (this is done in onCreate()).
//     *
//     * The only available options are COPY and DELETE.
//     *
//     * Context-click is equivalent to long-press.
//     * 当用户上下文单击列表中的注释时，将调用此方法。
//     * NotesList 在其 ListView 中将自己注册为上下文菜单的处理程序（这是在 onCreate（） 中完成的）。
//     * 唯一可用的选项是 COPY 和 DELETE。上下文单击等效于长按。
//     *
//     * @param menu A ContexMenu object to which items should be added.应将项添加到的 ContexMenu 对象
//     * @param view The View for which the context menu is being constructed.正在为其构建上下文菜单的 View。
//     * @param menuInfo Data associated with view.与 view 关联的数据。
//     * @throws ClassCastException
//     */
//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
//
//        // The data from the menu item.
//        AdapterView.AdapterContextMenuInfo info;
//
//        // Tries to get the position of the item in the ListView that was long-pressed.
//        try {
//            // Casts the incoming data object into the type for AdapterView objects.
//            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
//        } catch (ClassCastException e) {
//            // If the menu object can't be cast, logs an error.
//            Log.e(TAG, "bad menuInfo", e);
//            return;
//        }
//
//        /*
//         * Gets the data associated with the item at the selected position. getItem() returns
//         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
//         * the adapter associated all of the data for a note with its list item. As a result,
//         * getItem() returns that data as a Cursor.
//         */
//        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
//
//        // If the cursor is empty, then for some reason the adapter can't get the data from the
//        // provider, so returns null to the caller.
//        if (cursor == null) {
//            // For some reason the requested item isn't available, do nothing
//            return;
//        }
//
//        // Inflate menu from XML resource
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.list_context_menu, menu);
//
//        // Sets the menu header to be the title of the selected note.
//        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));
//
//        // Append to the
//        // menu items for any other activities that can do stuff with it
//        // as well.  This does a query on the system for any activities that
//        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
//        // for each one that is found.
//        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
//                                        Integer.toString((int) info.id) ));
//        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
//                new ComponentName(this, NotesList.class), null, intent, 0, null);
//    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see onCreateContextMenu()). The only menu items that are actually handled are DELETE and
     * COPY. Anything else is an alternative option, for which default handling should be done.
     *
     * @param item The selected menu item
     * @return True if the menu item was DELETE, and no default processing is need, otherwise false,
     * which triggers the default handling of the item.
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        switch (item.getItemId()) {
        case R.id.context_open:
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
//BEGIN_INCLUDE(copy)
        case R.id.context_copy:
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
  
            // Copies the notes URI to the clipboard. In effect, this copies the note itself
            clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
                    getContentResolver(),               // resolver to retrieve URI info
                    "Note",                             // label for the clip
                    noteUri)                            // the URI
            );
  
            // Returns to the caller and skips further processing.
            return true;
//END_INCLUDE(copy)
        case R.id.context_delete:
  
            // Deletes the note from the provider by passing in a URI in note ID format.
            // Please see the introductory note about performing provider operations on the
            // UI thread.
            getContentResolver().delete(
                noteUri,  // The URI of the provider
                null,     // No where clause is needed, since only a single note ID is being
                          // passed in.
                null      // No where clause is used, so no where arguments are needed.
            );
  
            // Returns to the caller and skips further processing.
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * This method is called when the user clicks a note in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
