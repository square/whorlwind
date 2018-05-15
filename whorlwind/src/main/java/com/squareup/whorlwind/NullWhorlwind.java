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

import io.reactivex.Completable;
import io.reactivex.Observable;
import okio.ByteString;

class NullWhorlwind extends Whorlwind {
  @Override public boolean canStoreSecurely() {
    return false;
  }

  @Override public Completable write(String name, ByteString value) {
    return Completable.error(new UnsupportedOperationException());
  }

  @Override public Observable<ReadResult> read(String name) {
    return Observable.error(new UnsupportedOperationException());
  }
}
