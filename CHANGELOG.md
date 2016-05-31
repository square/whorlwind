Change Log
==========

Version 1.0.1 *(2015-05-31)*
----------------------------

 * Fix: Do not call bother calling authenticate if subscriber has already unsubscribed.
 * Fix: Lazily initialized `SharedPreferencesStorage` on first use to avoid hitting the filesystem
   on startup (and avoid potentially tripping StrictMode as well).


Version 1.0.0 *(2016-03-09)*
----------------------------

Initial version.
