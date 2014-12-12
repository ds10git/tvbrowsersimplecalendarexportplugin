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

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

/**
 * The preferences fragment for the SimpleCalenderExportPlugin.
 * 
 * @author René Mach
 */
public class SimpleCalendarExportPluginPreferencesFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences_calendar_export);
    
    onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(getActivity()), getString(R.string.PREF_CALENDAR_EXPORT_TITLE_CONTAINS_CHANNEL));
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    
    PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(this);
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    
    PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
  }
  
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if(key.equals(getString(R.string.PREF_CALENDAR_EXPORT_TITLE_CHANNEL_POSITION)) ||
        key.equals(getString(R.string.PREF_CALENDAR_EXPORT_DESCRIPTION_TYPE))) {
      ListPreference lp = (ListPreference) findPreference(key);
      
      if(lp != null) {
        lp.setSummary("dummy"); // required or will not update
        
        String value = String.valueOf(lp.getEntry());
        
        if(value.endsWith("%")) {
          value += "%";
        }
        
        lp.setSummary(value);
      }
    }
  
    if(key.equals(getString(R.string.PREF_CALENDAR_EXPORT_TITLE_CONTAINS_CHANNEL))) {
      CheckBoxPreference channelInTitle = (CheckBoxPreference)findPreference(key);
      CheckBoxPreference channelInLocation = (CheckBoxPreference)findPreference(getString(R.string.PREF_CALENDAR_EXPORT_LOCATION_CONTAINS_CHANNEL));
      ListPreference channelPosition = (ListPreference)findPreference(getString(R.string.PREF_CALENDAR_EXPORT_TITLE_CHANNEL_POSITION));
      
      if(channelInTitle != null && channelInLocation != null && channelPosition != null) {
        if(!channelInTitle.isChecked()) {
          channelInLocation.setChecked(true);
        }
        
        channelInLocation.setEnabled(channelInTitle.isChecked());
        channelPosition.setEnabled(channelInTitle.isChecked());
      }
    }
  }
}
