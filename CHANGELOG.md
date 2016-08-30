Change Log
==========

Version 1.0.1 *(2016-05-31)*
----------------------------

 * Fix: Do not bother calling authenticate if subscriber has already unsubscribed.
 * Fix: Lazily initialized `SharedPreferencesStorage` on first use to avoid hitting the filesystem
   on startup (and avoid potentially tripping StrictMode as well).


Version 1.0.0 *(2016-03-09)*
----------------------------

Initial version.
