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

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import java.util.Set;
import okio.ByteString;

public interface Storage {
  void clear();
  void remove(@NonNull String name);
  void put(@NonNull String name, @NonNull ByteString value);
  @CheckResult ByteString get(@NonNull String name);
  @CheckResult Set<String> names();
}
