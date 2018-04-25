/*
 * SimpleCalendarExportPlugin for TV-Browser for Android
 * Copyright (C) 2014 René Mach (rene@tvbrowser.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify or merge the Software,
 * furthermore to publish and distribute the Software free of charge without modifications and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.simplecalendarexportplugin;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;

import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginManager;
import org.tvbrowser.devplugin.PluginMenu;
import org.tvbrowser.devplugin.Program;
import org.tvbrowser.devplugin.ReceiveTarget;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A service class that provides a calendar export functionality for TV-Browser for Android.
 * 
 * @author René Mach
 */
public class SimpleCalendarExportPlugin extends Service {
  /* The id for the calendar export PluginMenu */
  private static final int PLUGIN_MENU_EXPORT_CALENDAR = 1;
  /* The id for the remove marking PluginMenu*/
  private static final int PLUGIN_MENU_REMOVE_MARKING = 2;
  
  /* The preferences key for the marking set */
  private static final String PREF_MARKINGS = "PREF_MARKINGS";
  
  /* The plugin manager of TV-Browser */
  private PluginManager mPluginManager;
  
  /* The set with the marking ids */
  private Set<String> mMarkingProgramIds;
  
  /**
   * At onBind the Plugin for TV-Browser is loaded.
   */
  @Override
  public IBinder onBind(Intent intent) {
    return getBinder;
  }
  
  @Override
  public boolean onUnbind(Intent intent) {
    /* Don't keep instance of plugin manager*/
    mPluginManager = null;
    
    stopSelf();
    
    return false;
  }
  
  @Override
  public void onDestroy() {
    /* Don't keep instance of plugin manager*/
    mPluginManager = null;
    
    super.onDestroy();
  }
  
  private void save() {
    Editor edit = PreferenceManager.getDefaultSharedPreferences(SimpleCalendarExportPlugin.this).edit();
    
    edit.putStringSet(PREF_MARKINGS, mMarkingProgramIds);
    edit.commit();
  }

  private Plugin.Stub getBinder = new Plugin.Stub() {
    private long mRemovingProgramId = -1;
    @Override
    public void openPreferences(List<Channel> subscribedChannels) throws RemoteException {
      Intent startPref = new Intent(SimpleCalendarExportPlugin.this, SimpleCalendarExportPluginPreferencesActivity.class);
      startPref.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      
      if(mPluginManager != null) {
        startPref.putExtra(SimpleCalendarExportPluginPreferencesActivity.DARK_THEME_EXTRA_KEY, mPluginManager.getTvBrowserSettings().isUsingDarkTheme());
      }
      
      startActivity(startPref);
    }
    
    @Override
    public boolean onProgramContextMenuSelected(Program program, PluginMenu pluginMenu) throws RemoteException {
      if(pluginMenu.getId() == PLUGIN_MENU_EXPORT_CALENDAR) {
        // Create a new insertion Intent.
        Intent addCalendarEntry = new Intent(Intent.ACTION_EDIT);
        
        addCalendarEntry.setType(getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
        
        String desc = null;
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SimpleCalendarExportPlugin.this);
        
        String type = pref.getString(getString(R.string.PREF_CALENDAR_EXPORT_DESCRIPTION_TYPE), getString(R.string.pref_export_description_type_default));
        
        if(type.equals(getResources().getStringArray(R.array.pref_simple_string_value_array2)[1])) {
          if(program.getDescription() != null) {
            desc = program.getDescription();
          }
        }
        
        if(desc == null && program.getShortDescription() != null) {
          desc = program.getShortDescription();
          
          if(desc == null && program.getDescription() != null) {
            if(program.getDescription().trim().length() > 160) {
              desc = program.getDescription().trim().substring(0,160)+"\u2026";
            }
            else if(program.getDescription().trim().length() > 0) {
              desc = program.getDescription();
            }
          }
        }
        
        String title = program.getTitle();
        
        boolean channelInTitle = pref.getBoolean(getString(R.string.PREF_CALENDAR_EXPORT_TITLE_CONTAINS_CHANNEL), getResources().getBoolean(R.bool.pref_calendar_export_title_contains_channel_default));
        
        if(channelInTitle) {
          if(pref.getString(getString(R.string.PREF_CALENDAR_EXPORT_TITLE_CHANNEL_POSITION), getString(R.string.pref_calendar_export_title_channel_position_default)).equals("0")) {
            title = program.getChannel().getChannelName() + ": " + title;
          }
          else {
            title = title + " (" + program.getChannel().getChannelName() + ")";
          }
        }
        
        if(!channelInTitle || pref.getBoolean(getString(R.string.PREF_CALENDAR_EXPORT_LOCATION_CONTAINS_CHANNEL), getResources().getBoolean(R.bool.pref_calendar_export_location_contains_channel_default))) {
          addCalendarEntry.putExtra(Events.EVENT_LOCATION, program.getChannel().getChannelName());
        }
        
        // Add the calendar event details
        addCalendarEntry.putExtra(Events.TITLE, title);
        
        String description = null;
        
        if(program.getEpisodeTitle() != null) {
          description = program.getEpisodeTitle();
        }
        
        if(desc != null) {
          if(description != null) {
            description += "\n\n" + desc;
          }
          else {
            description = desc;
          }
        }
        
        if(description != null) {
          addCalendarEntry.putExtra(Events.DESCRIPTION, description);
        }
        
        addCalendarEntry.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, program.getStartTimeInUTC());
        addCalendarEntry.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,program.getEndTimeInUTC());
        
        try {
          // Use the Calendar app to add the new event.
          addCalendarEntry.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(addCalendarEntry);
          mMarkingProgramIds.add(String.valueOf(program.getId()));
          save();
        }catch(ActivityNotFoundException anfe) {
          return false;
        }
        
        return true;
      }
      else {
        if(mMarkingProgramIds.contains(String.valueOf(program.getId()))) {
          mRemovingProgramId = program.getId();

          boolean unmarked = false;

          if(mPluginManager.getTvBrowserSettings().getTvbVersionCode() >= 308) {
            unmarked = mPluginManager.unmarkProgramWithIcon(program,SimpleCalendarExportPlugin.class.getCanonicalName());
          }
          else {
            unmarked = mPluginManager.unmarkProgram(program);
          }

          if(unmarked) {
            mMarkingProgramIds.remove(String.valueOf(program.getId()));
            save();
          }

          mRemovingProgramId = -1;
        }
      }
      
      return false;
    }
    
    @Override
    public void onDeactivation() throws RemoteException {
      /* Don't keep instance of plugin manager*/
      mPluginManager = null;
    }
    
    @Override
    public void onActivation(PluginManager pluginManager) throws RemoteException {
      mPluginManager = pluginManager;
      
      mMarkingProgramIds = PreferenceManager.getDefaultSharedPreferences(SimpleCalendarExportPlugin.this).getStringSet(PREF_MARKINGS, new HashSet<String>());
    }
    
    @Override
    public boolean hasPreferences() throws RemoteException {
      return true;
    }
    
    @Override
    public void handleFirstKnownProgramId(long programId) throws RemoteException {
      if(programId == -1) {
        mMarkingProgramIds.clear();
      }
      else {
        String[] knownIds = mMarkingProgramIds.toArray(new String[mMarkingProgramIds.size()]);
        
        for(int i = knownIds.length-1; i >= 0; i--) {
          if(Long.parseLong(knownIds[i]) < programId) {
            mMarkingProgramIds.remove(knownIds[i]);
          }
        }
      }
    }
    
    @Override
    public String getVersion() throws RemoteException {
      String version = "UNKONW";

      try {
        PackageInfo pInfo = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
        version = pInfo.versionName;
      } catch (PackageManager.NameNotFoundException e) {
        e.printStackTrace();
      }
      return version;
    }
    
    @Override
    public String getName() throws RemoteException {
      return getString(R.string.service_simple_calendar_export_name);
    }
    
    @Override
    public long[] getMarkedPrograms() throws RemoteException {
      long[] markings = new long[mMarkingProgramIds.size()];
      
      Iterator<String> values = mMarkingProgramIds.iterator();
      
      for(int i = 0; i < markings.length; i++) {
        markings[i] = Long.parseLong(values.next());
      }
      
      return markings;
    }
    
    @Override
    public byte[] getMarkIcon() throws RemoteException {
      Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_event_white);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      icon.compress(Bitmap.CompressFormat.PNG, 100, stream);

      return stream.toByteArray();
    }
    
    @Override
    public String getLicense() throws RemoteException {
      return getString(R.string.license);
    }
    
    @Override
    public String getDescription() throws RemoteException {
      return getString(R.string.service_simple_calendar_export_description);
    }
    
    @Override
    public PluginMenu[] getContextMenuActionsForProgram(Program program) throws RemoteException {
      PluginMenu calendarExport = null;
      
      if(!mMarkingProgramIds.contains(String.valueOf(program.getId()))) {
        boolean isFutureCalendar = program.getStartTimeInUTC() > System.currentTimeMillis();
        
        boolean showCalender = false;
        
        try {
          Class.forName("android.provider.CalendarContract$Events");
          showCalender = true;
        } catch (ClassNotFoundException e) {}
        
        if(isFutureCalendar && showCalender) {
          calendarExport = new PluginMenu(PLUGIN_MENU_EXPORT_CALENDAR, getString(R.string.service_simple_calendar_export_context_title));
        }
      }
      else {
        calendarExport = new PluginMenu(PLUGIN_MENU_REMOVE_MARKING, getString(R.string.service_simple_calendar_export_context_unmark));
      }
      
      return calendarExport != null ? new PluginMenu[] {calendarExport} : null;
    }
    
    @Override
    public String getAuthor() throws RemoteException {
      return "René Mach";
    }

    @Override
    public boolean isMarked(long programId) throws RemoteException {
      return programId != mRemovingProgramId && mMarkingProgramIds.contains(String.valueOf(programId));
    }
    
    @Override
    public ReceiveTarget[] getAvailableProgramReceiveTargets() throws RemoteException {
      return null;
    }

    @Override
    public void receivePrograms(Program[] programs, ReceiveTarget target) throws RemoteException {}
  };
}
