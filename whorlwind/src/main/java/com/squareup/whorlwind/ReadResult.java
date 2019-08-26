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

import android.hardware.fingerprint.FingerprintManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okio.ByteString;

/** @see Whorlwind#read(String) */
public final class ReadResult {
  @NonNull public final ReadState readState;
  /**
   * A help/error code provided by Android. See {@link FingerprintManager} for possible values.
   * Will be -1 if a code was not provided.
   */
  public final int code;
  /**
   * A help/error message provided by Android. Will only be populated if {@code readState} is
   * {@link ReadState#UNRECOVERABLE_ERROR} or {@link ReadState#RECOVERABLE_ERROR}.
   */
  @Nullable public final CharSequence message;
  /**
   * The decrypted value. Will be null if {@code readState} is not {@link ReadState#READY}. Will
   * also be null if there was no encrypted value in storage.
   */
  @Nullable public final ByteString value;

  private ReadResult(@NonNull ReadState readState, int code, @Nullable CharSequence message,
      @Nullable ByteString value) {
    this.readState = readState;
    this.code = code;
    this.message = message;
    this.value = value;
  }

  public static ReadResult create(@NonNull ReadState readState, int code,
      @Nullable CharSequence message, @Nullable ByteString value) {
    if (readState == null) {
      throw new IllegalArgumentException("ReadState cannot be null.");
    }
    return new ReadResult(readState, code, message, value);
  }

  public enum ReadState {
    /**
     * A value was found but it needs authorization to be returned. The fingerprint reader has been
     * activated.
     */
    NEEDS_AUTH,

    /**
     * An unrecoverable error has occurred (ex: too many attempts, time out, canceled). An error
     * string may be provided in {@code message} to display to the user. The fingerprint reader is
     * no longer active.
     */
    UNRECOVERABLE_ERROR,

    /** Fingerprint was read but is not authorized. The fingerprint reader is still active. */
    AUTHORIZATION_ERROR,

    /**
     * Fingerprint could not be read. A help string may be provided in {@code message} to display
     * to the user. The fingerprint reader is still active.
     */
    RECOVERABLE_ERROR,

    /**
     * Value is ready to be consumed. If the value is null, it was not found in secure storage. The
     * fingerprint reader is no longer active.
     */
    READY,
  }
}
