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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;

import com.squareup.whorlwind.ReadResult.ReadState;

import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Cancellable;
import okio.ByteString;

@TargetApi(Build.VERSION_CODES.M) final class FingerprintAuthOnSubscribe
    implements ObservableOnSubscribe<ReadResult> {
  private final FingerprintManager fingerprintManager;
  private final Storage storage;
  private final String name;
  private final AtomicBoolean readerScanning;
  private final Object dataLock;
  private final RealWhorlwind whorlwind;

  public FingerprintAuthOnSubscribe(FingerprintManager fingerprintManager, Storage storage,
      String name, AtomicBoolean readerScanning, Object dataLock, RealWhorlwind whorlwind) {
    this.fingerprintManager = fingerprintManager;
    this.storage = storage;
    this.name = name;
    this.readerScanning = readerScanning;
    this.dataLock = dataLock;
    // TODO: Come up with a better way to access the required data without passing this in.
    this.whorlwind = whorlwind;
  }

  // Lint doesn't think we're checking permissions before calling FingerprintManager APIs, but we
  // are in checkCanStoreSecurely()
  @SuppressWarnings("ResourceType") @SuppressLint("MissingPermission") //
  @Override public void subscribe(final ObservableEmitter<ReadResult> emitter) {
    whorlwind.checkCanStoreSecurely();

    final ByteString encrypted;
    Cipher cipher = null;

    // Results to emit to the subscriber after the lock is released.
    ReadResult emitResult;
    boolean emitComplete = false;
    Throwable emitError = null;

    synchronized (dataLock) {
      whorlwind.prepareKeyStore();

      encrypted = storage.get(name);
      if (encrypted == null) {
        emitResult = ReadResult.create(ReadState.READY, -1, null, null);
        emitComplete = true;
      } else {
        emitResult = ReadResult.create(ReadState.NEEDS_AUTH, -1, null, null);
      }

      try {
        cipher = RealWhorlwind.createCipher();
        cipher.init(Cipher.DECRYPT_MODE, whorlwind.getPrivateKey());
      } catch (GeneralSecurityException e) {
        Log.i(Whorlwind.TAG, "Failed to initialize cipher for decryption.", e);
        emitError = e;
      }
    }

    if (emitError != null) {
      emitter.onError(emitError);
      return;
    }

    emitter.onNext(emitResult);

    if (emitComplete) {
      emitter.onComplete();
      return;
    }

    // http://b.android.com/192513
    if (!readerScanning.compareAndSet(false, true)) {
      emitter.onError(new IllegalStateException("Already attempting to read another value."));
      return;
    }

    final CancellationSignal cancellationSignal = new CancellationSignal();
    emitter.setCancellable(new Cancellable() {
      @Override public void cancel() throws Exception {
        cancellationSignal.cancel();
      }
    });

    if (emitter.isDisposed()) {
      readerScanning.set(false);
      return;
    }

    fingerprintManager.authenticate(new FingerprintManager.CryptoObject(cipher), cancellationSignal,
        0, new FingerprintManager.AuthenticationCallback() {
          @Override public void onAuthenticationError(int errorCode, CharSequence errString) {
            emitter.onNext(
                ReadResult.create(ReadState.UNRECOVERABLE_ERROR, errorCode, errString, null));
            emitter.onComplete();
            readerScanning.set(false);
          }

          @Override public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            emitter.onNext(
                ReadResult.create(ReadState.RECOVERABLE_ERROR, helpCode, helpString, null));
          }

          @Override
          public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            if (!emitter.isDisposed()) {
              try {
                Cipher cipher = result.getCryptoObject().getCipher();
                byte[] decrypted = cipher.doFinal(encrypted.toByteArray());

                emitter.onNext(
                    ReadResult.create(ReadState.READY, -1, null, ByteString.of(decrypted)));
                emitter.onComplete();
              } catch (IllegalBlockSizeException | BadPaddingException e) {
                Log.i(Whorlwind.TAG, "Failed to decrypt.", e);
                emitter.onError(e);
              }
            }
            readerScanning.set(false);
          }

          @Override public void onAuthenticationFailed() {
            emitter.onNext(ReadResult.create(ReadState.AUTHORIZATION_ERROR, -1, null, null));
          }
        }, null);
  }
}
