Change Log
==========

Version 2.1.0 *(2020-11-23)*
----------------------------

* AndroidX
* Add `USE_BIOMETRIC` permission for API 28

Version 2.0.0 *(2018-07-19)*
----------------------------

* RxJava 2
* `ReadResult` now has a public `create` method
* `Whorlwind.write()` now returns a `Completable`
* Fix: Lots of common exceptions have been caught and handled internally
* Fix: Remove key when `IllegalBlockSizeException` exception is thrown due to Android 8.0 bug

Version 1.0.1 *(2016-05-31)*
----------------------------

* Fix: Do not bother calling authenticate if subscriber has already unsubscribed.
* Fix: Lazily initialized `SharedPreferencesStorage` on first use to avoid hitting the filesystem
   on startup (and avoid potentially tripping StrictMode as well).


Version 1.0.0 *(2016-03-09)*
----------------------------

Initial version.
