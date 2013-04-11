package com.dozuki.ifixit.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.dozuki.ifixit.MainApplication;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.login.LoginEvent;
import com.dozuki.ifixit.ui.guide.create.GuideCreateActivity;
import com.dozuki.ifixit.ui.topic_view.TopicActivity;
import com.squareup.otto.Subscribe;
import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Base Activity that performs various functions that all Activities in this app
 * should do. Such as:
 * <p/>
 * Registering for the event bus. Setting the current site's theme. Finishing
 * the Activity if the user logs out but the Activity requires authentication.
 * Displaying various menu icons.
 */
public abstract class IfixitActivity extends Activity {

   private static final String STATE_ACTIVE_POSITION = "com.dozuki.ifixit.ui.ifixitActivity.activePosition";
   private static final String STATE_CONTENT_TEXT = "com.dozuki.ifixit.ui.ifixitActivity.contentText";


   /**
    * Navigation indexes
    */
   public int VIEW_GUIDES = 0;
   public int CREATE_GUIDES = 1;

   private static final int MENU_OVERFLOW = 1;


   /**
    * Slide Out Menu Drawer
    */
   private MenuDrawer mMenuDrawer;

   private MenuAdapter mAdapter;
   private ListView mList;

   private int mActivePosition = -1;

   /**
    * This is incredibly hacky. The issue is that Otto does not search for @Subscribed
    * methods in parent classes because the performance hit is far too big for
    * Android because of the deep inheritance with the framework and views.
    * Because of this
    *
    * @Subscribed methods on IfixitActivity itself don't get registered. The
    * workaround is to make an anonymous object that is registered
    * on behalf of the parent class.
    * <p/>
    * Workaround courtesy of:
    * https://github.com/square/otto/issues/26
    * <p/>
    * Note: The '@SuppressWarnings("unused")' is to prevent
    * warnings that are incorrect (the methods *are* actually used.
    */
   private Object loginEventListener = new Object() {
      @SuppressWarnings("unused")
      @Subscribe
      public void onLogin(LoginEvent.Login event) {
         // Update menu icons.
         supportInvalidateOptionsMenu();
      }

      @SuppressWarnings("unused")
      @Subscribe
      public void onLogout(LoginEvent.Logout event) {
         finishActivityIfPermissionDenied();
         // Update menu icons.
         supportInvalidateOptionsMenu();
      }

      @SuppressWarnings("unused")
      @Subscribe
      public void onCancel(LoginEvent.Cancel event) {
         finishActivityIfPermissionDenied();
      }
   };

   public enum Navigation {
      SEARCH, FEATURED_GUIDES, BROWSE_TOPICS, USER_GUIDES, NEW_GUIDE, MEDIA_GALLERY, LOGOUT,
      YOUTUBE, FACEBOOK, TWITTER, HELP, ABOUT, NOVALUE;

      public static Navigation navigate(String str) {
         try {
            return valueOf(str.toUpperCase());
         } catch (Exception ex) {
            return NOVALUE;
         }
      }
   }

   private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
         Intent intent;
         Context context = parent.getContext();
         mActivePosition = position;
         mMenuDrawer.setActiveView(view, position);

         switch(Navigation.navigate((String)view.getTag())) {
            case SEARCH:
            case FEATURED_GUIDES:
            case BROWSE_TOPICS:
               intent = new Intent(context, TopicActivity.class);
               intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
               startActivity(intent);
               break;

            case USER_GUIDES:
               intent = new Intent(context, GuideCreateActivity.class);
               intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
               startActivity(intent);
               break;

            case NEW_GUIDE:
            case MEDIA_GALLERY:
            case LOGOUT:

            case YOUTUBE:
            case FACEBOOK:
            case TWITTER:

            case HELP:
            case ABOUT:
         }
         mMenuDrawer.closeMenu();
      }
   };

   @Override
   public void onCreate(Bundle savedState) {
      /**
       * Set the current site's theme. Must be before onCreate because of
       * inflating views.
       */
      setTheme(MainApplication.get().getSiteTheme());

      super.onCreate(savedState);

      if (savedState != null) {
         mActivePosition = savedState.getInt(STATE_ACTIVE_POSITION);
      }

      mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_WINDOW, Position.RIGHT);

      List<Object> items = new ArrayList<Object>();

      items.add(new Category("Browse Content"));
      items.add(new Item("Search", R.drawable.ic_action_search, "search"));
      items.add(new Item("Featured Guides", R.drawable.ic_action_star_10, "featured_guides"));
      items.add(new Item("Browse Devices", R.drawable.ic_action_list, "browse_topics"));

      items.add(new Category("Your Account"));
      items.add(new Item("My Guides", R.drawable.ic_menu_spinner_guides, "user_guides"));
      items.add(new Item("Create New Guide", R.drawable.ic_menu_add_guide, "new_guide"));
      items.add(new Item("Media Gallery", R.drawable.ic_menu_spinner_gallery, "media_gallery"));
      items.add(new Item("Logout", R.drawable.ic_action_exit, "logout"));

      items.add(new Category("iFixit Everywhere"));
      items.add(new Item("Youtube Channel", R.drawable.ic_action_youtube, "youtube"));
      items.add(new Item("Facebook", R.drawable.ic_action_facebook, "facebook"));
      items.add(new Item("Twitter", R.drawable.ic_action_twitter, "twitter"));

      items.add(new Category("More Information"));
      items.add(new Item("Help", R.drawable.ic_action_help, "help"));
      items.add(new Item("About", R.drawable.ic_action_info, "about"));

      // A custom ListView is needed so the drawer can be notified when it's scrolled. This is to update the position
      // of the arrow indicator.
      mList = new ListView(this);
      mAdapter = new MenuAdapter(items);
      mList.setAdapter(mAdapter);
      mList.setOnItemClickListener(mItemClickListener);

      mMenuDrawer.setMenuView(mList);
      mMenuDrawer.setMenuSize(600);

      mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_FULLSCREEN);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putInt(STATE_ACTIVE_POSITION, mActivePosition);
   }

   /**
    * If the user is coming back to this Activity make sure they still have
    * permission to view it. onRestoreInstanceState is for Activities that are
    * being recreated and onRestart is for Activities who are merely being
    * restarted. Unfortunately both are needed.
    */
   @Override
   public void onRestoreInstanceState(Bundle savedState) {
      super.onRestoreInstanceState(savedState);
      finishActivityIfPermissionDenied();
   }

   @Override
   public void onRestart() {
      super.onRestart();
      finishActivityIfPermissionDenied();
   }

   @Override
   public void onResume() {
      super.onResume();

      // Invalidate the options menu in case the user has logged in or out.
      //supportInvalidateOptionsMenu();

      MainApplication.getBus().register(this);
      MainApplication.getBus().register(loginEventListener);
   }

   @Override
   public void onPause() {
      super.onPause();

      MainApplication.getBus().unregister(this);
      MainApplication.getBus().unregister(loginEventListener);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case MENU_OVERFLOW:
            mMenuDrawer.toggleMenu();
            return true;
      }

      return super.onOptionsItemSelected(item);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuItem overflowItem = menu.add(0, MENU_OVERFLOW, 0, null);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
         overflowItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
      }
      overflowItem.setIcon(R.drawable.ic_action_list_2);
      return true;
   }

   public void setCustomTitle(String title) {
      this.getSupportActionBar().setDisplayShowCustomEnabled(true);
      TextView tv = new TextView(this);
      tv.setText(title);
      tv.setTextAppearance(getApplicationContext(), R.style.TextAppearance_iFixit_ActionBar_Title);
      tv.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
      tv.setGravity(Gravity.CENTER_VERTICAL);
      this.getSupportActionBar().setCustomView(tv,
       new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
   }

  /* @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
      MenuItem logout = menu.findItem(R.id.logout_button);

      if (logout != null) {
         boolean loggedIn = MainApplication.get().isUserLoggedIn();
         logout.setVisible(loggedIn);
         if (loggedIn) {
            String username = ((MainApplication) (this).getApplication()).getUser().getUsername();
            if (username.length() > 5) {
               username = username.substring(0, 5) + "...";
            }
            logout.setTitle(getResources().getString(R.string.logout_title) + " (" + username + ")");
         }
      }

      return super.onPrepareOptionsMenu(menu);
   }*/

   /**
    * Finishes the Activity if the user should be logged in but isn't.
    */
   private void finishActivityIfPermissionDenied() {
      MainApplication app = MainApplication.get();

      /**
       * Never finish if user is logged in or is logging in.
       */
      if (app.isUserLoggedIn() || app.isLoggingIn()) {
         return;
      }

      /**
       * Finish if the site is private or activity requires authentication.
       */
      if (!neverFinishActivityOnLogout()
       && (finishActivityIfLoggedOut() || !app.getSite().mPublic)) {
         finish();
      }
   }

   /**
    * "Settings" methods for derived classes are found below. Decides when to
    * finish the Activity, what icons to display etc.
    */

   /**
    * Return true if the gallery launcher should be in the options menu.
    */
   public boolean showGalleryIcon() {
      return true;
   }

   /**
    * Returns true if the Activity should be finished if the user logs out or
    * cancels authentication.
    */
   public boolean finishActivityIfLoggedOut() {
      return false;
   }

   /**
    * Returns true if the Activity should never be finished despite meeting
    * other conditions.
    * <p/>
    * This exists because of a race condition of sorts involving logging out of
    * private Dozuki sites. SiteListActivity can't reset the current site to
    * one that is public so it is erroneously finished unless flagged
    * otherwise.
    */
   public boolean neverFinishActivityOnLogout() {
      return false;
   }

   @Override
   public void onStart() {
      this.overridePendingTransition(0, 0);
      super.onStart();
   }

   @Override
   public void onBackPressed() {
      final int drawerState = mMenuDrawer.getDrawerState();
      if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
         mMenuDrawer.closeMenu();
         return;
      }

      super.onBackPressed();
   }

   private static class Item {

      String mTitle;
      int mIconRes;
      String mTag;

      Item(String title, int iconRes, String tag) {
         mTitle = title;
         mIconRes = iconRes;
         mTag = tag;
      }
   }

   private static class Category {

      String mTitle;

      Category(String title) {
         mTitle = title;
      }
   }

   private class MenuAdapter extends BaseAdapter {

      private List<Object> mItems;

      MenuAdapter(List<Object> items) {
         mItems = items;
      }

      @Override
      public int getCount() {
         return mItems.size();
      }

      @Override
      public Object getItem(int position) {
         return mItems.get(position);
      }

      @Override
      public long getItemId(int position) {
         return position;
      }

      @Override
      public int getItemViewType(int position) {
         return getItem(position) instanceof Item ? 0 : 1;
      }

      @Override
      public int getViewTypeCount() {
         return 2;
      }

      @Override
      public boolean isEnabled(int position) {
         return getItem(position) instanceof Item;
      }

      @Override
      public boolean areAllItemsEnabled() {
         return false;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         View v = convertView;
         Object item = getItem(position);

         if (item instanceof Category) {
            if (v == null) {
               v = getLayoutInflater().inflate(R.layout.menu_row_category, parent, false);
            }

            ((TextView) v).setText(((Category) item).mTitle);

         } else {
            if (v == null) {
               v = getLayoutInflater().inflate(R.layout.menu_row_item, parent, false);
            }

            TextView tv = (TextView) v;
            tv.setText(((Item) item).mTitle);
            tv.setCompoundDrawablesWithIntrinsicBounds(((Item) item).mIconRes, 0, 0, 0);
            tv.setTag(((Item) item).mTag);
         }

         v.setTag(R.id.mdActiveViewPosition, position);

         if (position == mActivePosition) {
            mMenuDrawer.setActiveView(v, position);
         }

         return v;
      }
   }
}