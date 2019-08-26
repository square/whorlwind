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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.util.Log;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Cipher;
import okio.ByteString;

import static android.Manifest.permission.USE_BIOMETRIC;
import static android.Manifest.permission.USE_FINGERPRINT;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

@RequiresApi(Build.VERSION_CODES.M)
final class RealWhorlwind extends Whorlwind {
  private final Context context;
  private final FingerprintManager fingerprintManager;
  @SuppressWarnings("WeakerAccess") // Used in nested class. Removing synthetic accessor.
  final Storage storage;
  private final String keyAlias;
  private final KeyStore keyStore;
  private final KeyPairGenerator keyGenerator;
  private final KeyFactory keyFactory;
  private final AtomicBoolean readerScanning;
  @SuppressWarnings("WeakerAccess") // Used in nested class. Removing synthetic accessor.
  final Object dataLock = new Object();

  RealWhorlwind(Context context, FingerprintManager fingerprintManager, Storage storage,
      String keyAlias, KeyStore keyStore, KeyPairGenerator keyGenerator, KeyFactory keyFactory) {
    this.context = context;
    this.fingerprintManager = fingerprintManager;
    this.storage = storage;
    this.keyAlias = keyAlias;
    this.keyStore = keyStore;
    this.keyGenerator = keyGenerator;
    this.keyFactory = keyFactory;

    readerScanning = new AtomicBoolean();
  }

  @Override public boolean canStoreSecurely() {
    return hasPermission()
        && isHardwareDetected(fingerprintManager)
        && hasEnrolledFingerprints(fingerprintManager);
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return checkSelfPermission(USE_FINGERPRINT) == PERMISSION_GRANTED;
    } else {
      return checkSelfPermission(USE_BIOMETRIC) == PERMISSION_GRANTED;
    }
  }

  private int checkSelfPermission(String permission) {
    return context.checkPermission(permission, android.os.Process.myPid(),
        android.os.Process.myUid());
  }

  void checkCanStoreSecurely() {
    if (!canStoreSecurely()) {
      throw new IllegalStateException(
          "Can't store securely. Check canStoreSecurely() before attempting to read/write.");
    }
  }

  @Override public Completable write(@NonNull final String name, @Nullable final ByteString value) {
    return Completable.fromAction(new Action() {
      @Override public void run() throws Exception {
        checkCanStoreSecurely();

        synchronized (dataLock) {
          if (value == null) {
            storage.remove(name);
            return;
          }

          prepareKeyStore();

          Cipher cipher = createCipher();
          cipher.init(Cipher.ENCRYPT_MODE, getPublicKey());

          storage.put(name, ByteString.of(cipher.doFinal(value.toByteArray())));
        }
      }
    });
  }

  @Override public Observable<ReadResult> read(@NonNull String name) {
    return Observable.create(new FingerprintAuthOnSubscribe(fingerprintManager, storage, name, //
        readerScanning, dataLock, this));
  }

  /**
   * Prepares the key store and our keys for encrypting/decrypting. Keys will be generated if we
   * haven't done so yet, and keys will be re-generated if the old ones have been invalidated. In
   * both cases, our K/V store will be cleared before continuing.
   */
  void prepareKeyStore() {
    try {
      Key key = keyStore.getKey(keyAlias, null);
      Certificate certificate = keyStore.getCertificate(keyAlias);
      if (key != null && certificate != null) {
        try {
          createCipher().init(Cipher.DECRYPT_MODE, key);

          // We have a keys in the store and they're still valid.
          return;
        } catch (KeyPermanentlyInvalidatedException e) {
          Log.d(TAG, "Key invalidated.");
        }
      }

      storage.clear();

      keyGenerator.initialize(new KeyGenParameterSpec.Builder(keyAlias,
          KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT) //
          .setBlockModes(KeyProperties.BLOCK_MODE_ECB) //
          .setUserAuthenticationRequired(true) //
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1) //
          .build());

      keyGenerator.generateKeyPair();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  static Cipher createCipher() throws GeneralSecurityException {
    return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_RSA
        + "/"
        + KeyProperties.BLOCK_MODE_ECB
        + "/"
        + KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1);
  }

  @SuppressWarnings("WeakerAccess") PublicKey getPublicKey() throws GeneralSecurityException {
    PublicKey publicKey = keyStore.getCertificate(keyAlias).getPublicKey();

    // In contradiction to the documentation, the public key returned from the key store is only
    // unlocked after the user has authenticated with their fingerprint. This is unnecessary
    // (and broken) for encryption using asynchronous keys, so we work around this by re-creating
    // our own copy of the key. See known issues at
    // http://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html
    KeySpec spec = new X509EncodedKeySpec(publicKey.getEncoded());
    return keyFactory.generatePublic(spec);
  }

  PrivateKey getPrivateKey() throws GeneralSecurityException {
    return (PrivateKey) keyStore.getKey(keyAlias, null);
  }

  void removeKey() {
    try {
      keyStore.deleteEntry(keyAlias);
    } catch (Exception e) {
      Log.d(TAG, "Remove key failed", e);
    }
  }
}
