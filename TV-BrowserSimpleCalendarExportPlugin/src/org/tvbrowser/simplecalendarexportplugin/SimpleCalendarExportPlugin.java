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

import java.util.List;

import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginMenu;
import org.tvbrowser.devplugin.Program;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;

/**
 * A service class that provides a calendar export functionality for TV-Browser for Android.
 * 
 * @author René Mach
 */
public class SimpleCalendarExportPlugin extends Service {
  /* The id for the calendar export PluginMenu */
  private static final int PLUGIN_MENU_EXPORT_CALENDAR = 1;
  
  /* The version of this Plugin */
  private static final String VERSION = "0.1";
  
  @Override
  public IBinder onBind(Intent intent) {
    return getBinder;
  }

  private Plugin.Stub getBinder = new Plugin.Stub() {
    @Override
    public void openPreferences(List<Channel> subscribedChannels) throws RemoteException {
      Intent startPref = new Intent(SimpleCalendarExportPlugin.this, SimpleCalendarExportPluginPreferencesActivity.class);
      startPref.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(startPref);
    }
    
    @Override
    public boolean onProgramContextMenuSelected(Program program,
        PluginMenu pluginMenu) throws RemoteException {
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
      
      if(pref.getBoolean(getString(R.string.PREF_CALENDAR_EXPORT_TITLE_CONTAINS_CHANNEL), getResources().getBoolean(R.bool.pref_calendar_export_title_contains_channel_default))) {
        title = program.getChannel().getChannelName() + ": " + title;
      }
      
      addCalendarEntry.putExtra(Events.EVENT_LOCATION, program.getChannel().getChannelName());
      
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
      }catch(ActivityNotFoundException anfe) {}
      
      return false;
    }
    
    @Override
    public void onDeactivation() throws RemoteException {}
    
    @Override
    public void onActivation() throws RemoteException {}
    
    @Override
    public boolean hasPreferences() throws RemoteException {
      return true;
    }
    
    @Override
    public void handleFirstKnownProgramId(long programId) throws RemoteException {}
    
    @Override
    public String getVersion() throws RemoteException {
      return VERSION;
    }
    
    @Override
    public String getName() throws RemoteException {
      return getString(R.string.service_simple_calendar_export_name);
    }
    
    @Override
    public long[] getMarkedPrograms() throws RemoteException {
      return null;
    }
    
    @Override
    public byte[] getMarkIcon() throws RemoteException {
      return null;
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
      boolean isFutureCalendar = program.getStartTimeInUTC() > System.currentTimeMillis();
      
      boolean showCalender = false;
      
      try {
        Class.forName("android.provider.CalendarContract$Events");
        showCalender = true;
      } catch (ClassNotFoundException e) {}
      
      if(isFutureCalendar && showCalender) {
        PluginMenu calendarExport = new PluginMenu(PLUGIN_MENU_EXPORT_CALENDAR, getString(R.string.service_simple_calendar_export_context_title));
        
        return new PluginMenu[] {calendarExport};
      }
      
      return null;
    }
    
    @Override
    public String getAuthor() throws RemoteException {
      return "René Mach";
    }
  };
}
