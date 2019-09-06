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
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyProperties;
import androidx.annotation.CheckResult;
import androidx.annotation.RequiresApi;
import android.util.Log;
import com.squareup.whorlwind.ReadResult.ReadState;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import okio.ByteString;

public abstract class Whorlwind {
  static final String TAG = "Whorlwind";

  public static Whorlwind create(Context context, Storage storage, String keyAlias) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return new NullWhorlwind();
    }

    return createRealWhorlwind(context, storage, keyAlias);
  }

  @RequiresApi(Build.VERSION_CODES.M)
  private static Whorlwind createRealWhorlwind(Context context, Storage storage, String keyAlias) {
    try {
      FingerprintManager fingerprintManager = context.getSystemService(FingerprintManager.class);
      if (fingerprintManager == null) {
        Log.w(TAG, "No fingerprint manager.");
        return new NullWhorlwind();
      }

      if (!isHardwareDetected(fingerprintManager)) return new NullWhorlwind();

      KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
      keyStore.load(null); // Ensure the key store can be loaded before continuing.

      KeyPairGenerator keyGenerator =
          KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");

      RealWhorlwind.createCipher(); // If this doesn't throw, the cipher we need is available.

      return new RealWhorlwind(context, fingerprintManager, storage, keyAlias, keyStore,
          keyGenerator, keyFactory);
    } catch (Exception e) {
      Log.w(TAG, "Cannot store securely.", e);
      return new NullWhorlwind();
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  static boolean isHardwareDetected(FingerprintManager fingerprintManager) {
    try {
      return fingerprintManager.isHardwareDetected();
    } catch (SecurityException e) {
      Log.w(TAG, "Failed detecting hardware", e);
      return false;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  static boolean hasEnrolledFingerprints(FingerprintManager fingerprintManager) {
    try {
      return fingerprintManager.hasEnrolledFingerprints();
    } catch (IllegalStateException e) {
      // see https://github.com/square/whorlwind/issues/36
      Log.w(TAG, "Cannot know if device has enrolled fingerprints", e);
      return false;
    }
  }

  Whorlwind() {
    // Prevent 3rd-party implementations.
  }

  /**
   * Returns true if the device is currently capable of reading/writing from/to secure storage.
   *
   * <p>
   * <b>Note:</b> This method must be checked before subscribing to
   * {@link #write(String, ByteString)} or {@link #read(String)}.
   */
  @CheckResult
  public abstract boolean canStoreSecurely();

  /**
   * Writes a value to secure storage. Must check {@link #canStoreSecurely()} before subscribing.
   */
  @CheckResult
  public abstract Completable write(String name, ByteString value);

  /**
   * Reads a value from secure storage. If no value is found, a result with a {@code state} of
   * {@link ReadState#READY READY} and a null {@code value} will be emitted. Otherwise, a result
   * with a {@code state} of {@link ReadState#NEEDS_AUTH NEEDS_AUTH} will be emitted and the
   * fingerprint reader will be activated. Future events from the fingerprint reader will be
   * emitted to the stream.
   *
   * Must check {@link #canStoreSecurely()} before subscribing.
   */
  @CheckResult
  public abstract Observable<ReadResult> read(String name);
}
