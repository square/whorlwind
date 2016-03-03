package com.squareup.whorlwind.sample;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import com.squareup.whorlwind.Storage;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okio.ByteString;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

/**
 * An in-memory storage implementation that allows observing of values. You should have no reason to
 * use this in your app.
 */
final class SampleStorage implements Storage {
  private final Map<String, ByteString> storage = new LinkedHashMap<>();
  private final BehaviorSubject<List<Pair<String, ByteString>>> subject =
      BehaviorSubject.create(Collections.emptyList() );

  @Override public void clear() {
    storage.clear();
    emit();
  }

  @Override public void remove(@NonNull String name) {
    storage.remove(name);
    emit();
  }

  @Override public void put(@NonNull String name, @Nullable ByteString value) {
    storage.put(name, value);
    emit();
  }

  @CheckResult @Override public ByteString get(@NonNull String name) {
    return storage.get(name);
  }

  @CheckResult @Override public Set<String> names() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(storage.keySet()));
  }

  public Observable<List<Pair<String, ByteString>>> entries() {
    return subject;
  }

  private void emit() {
    Observable.from(storage.entrySet()) //
        .map(entry -> Pair.create(entry.getKey(), entry.getValue())) //
        .toSortedList((a, b) -> a.first.compareTo(b.first)) //
        .subscribeOn(Schedulers.io()) //
        .subscribe(subject::onNext);
  }
}
