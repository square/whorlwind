Whorlwind
=========

A reactive wrapper around Android's fingerprint API that handles encrypting/decrypting sensitive
data using a fingerprint.

**DEPRECATED:** Google has released the [AndroidX Biometric Library][biometric] which supports more
forms of authentication than fingerprint and should be relied on going forward. See
[the announcement][announcement] for more information.

[biometric]: https://developer.android.com/jetpack/androidx/releases/biometric
[announcement]: https://android-developers.googleblog.com/2019/10/one-biometric-api-over-all-android.html

Usage
-----

Create an instance of `Whorlwind` by calling:

```java
Whorlwind.create(context, storage, keyAlias)
```

You control where Whorlwind saves your encrypted data by providing a `Storage`. Whorlwind ships with
a `SharedPreferencesStorage` if you want to store your data to shared preferences.

`keyAlias` is used when generating a key pair in the `KeyStore` and should not be shared with any
other key aliases in your project.

All attempts to read/write from Whorlwind must be guarded by a call to `canStoreSecurely()`. This
checks for necessary permissions and whether or not the fingerprint manager is available for use.
The state of these requirements can change over the lifetime of your activity/application so it is
not sufficient to check this once during activity/application creation.

### Writing

Whorlwind handles encrypting the value for you so writing a new value is as simple as passing it to
the `write()` method. However, be aware that Whorlwind will be performing cryptographic operations
and may also perform some disk I/O regardless of your `Storage` implementation, so `write()` should
not be called on the main thread.

```java
if (whorlwind.canStoreSecurely()) {
  Observable.just("value")
      .observeOn(Schedulers.io())
      .flatMapCompletable(value -> whorlwind.write("key", ByteString.encodeUtf8(value)))
      .subscribe();
}
```

### Reading

Whorlwind will handle activating the fingerprint reader and decrypting your data for you once you
subscribe to the stream returned from `read()`. Similar to `write()`, `read()` should not be
subscribed to on the main thread.

```java
if (whorlwind.canStoreSecurely()) {
  whorlwind.read("key")
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(result -> {
        switch (result.readState) {
            case NEEDS_AUTH:
              // An encrypted value was found, prompt for fingerprint to decrypt.
              // The fingerprint reader is active.
              promptForFingerprint();
              break;
            case UNRECOVERABLE_ERROR:
            case AUTHORIZATION_ERROR:
            case RECOVERABLE_ERROR:
              // Show an error message. One may be provided in result.message.
              // Unless the state is UNRECOVERABLE_ERROR, the fingerprint reader is still
              // active and this stream will continue to emit result updates.
              showFingerprintMessage(result.code, result.message);
              break;
            case READY:
              if (result.value != null) {
                // Value was found and has been decrypted.
                showToast(result.value.utf8());
              } else {
                // No value was found. Fall back to password or fail silently, depending on
                // your use case.
                fingerprintFallback();
              }
              break;
            default:
              throw new IllegalArgumentException("Unknown state: " + result.readState);
          }
      });
}
```

### Sample

A sample application is provided with a more comprehensive example.



Download
--------

Gradle:

```groovy
implementation 'com.squareup.whorlwind:whorlwind:2.1.0'
```


License
--------

    Copyright 2016 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

