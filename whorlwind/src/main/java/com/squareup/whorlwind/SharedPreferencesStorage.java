/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.whorlwind;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import okio.ByteString;

public final class SharedPreferencesStorage implements Storage {
  private final Context context;
  private final String name;

  /** DO NOT use directly! Lazily initialized through {@link #prefs()} accessor method. */
  private SharedPreferences prefs;

  public SharedPreferencesStorage(Context context, String name) {
    this.context = context;
    this.name = name;
  }

  private synchronized SharedPreferences prefs() {
    if (prefs == null) {
      prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }
    return prefs;
  }

  @Override public void clear() {
    prefs().edit().clear().apply();
  }

  @Override public void remove(@NonNull String name) {
    prefs().edit().remove(name).apply();
  }

  @Override public void put(@NonNull String name, @NonNull ByteString value) {
    prefs().edit().putString(name, value.base64()).apply();
  }

  @CheckResult @Override public ByteString get(@NonNull String name) {
    String value = prefs().getString(name, null);
    if (value == null) {
      return null;
    }

    return ByteString.decodeBase64(value);
  }

  @CheckResult @Override public Set<String> names() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(prefs().getAll().keySet()));
  }
}
