package com.squareup.whorlwind;

import android.annotation.SuppressLint;
import android.content.ContextWrapper;
import android.hardware.fingerprint.FingerprintManager;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import okio.ByteString;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowContextWrapper;
import rx.Observable;
import rx.functions.Action1;

import static android.Manifest.permission.USE_FINGERPRINT;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class) //
@Config(manifest = Config.NONE) //
@SuppressLint("NewApi") //
@SuppressWarnings("ResourceType") //
public final class RealWhorlwindTest {
  private final ContextWrapper context = new ContextWrapper(RuntimeEnvironment.application);
  private final ShadowContextWrapper shadowContext =
      ((ShadowContextWrapper) ShadowExtractor.extract(context));
  private final FingerprintManager fingerprintManager = mock(FingerprintManager.class);
  private final Storage storage = spy(new TestStorage());
  private final KeyStore keyStore = mock(KeyStore.class);
  private final KeyPairGenerator keyGenerator = mock(KeyPairGenerator.class);
  private final KeyFactory keyFactory = mock(KeyFactory.class);
  private final RealWhorlwind whorlwind = new RealWhorlwind(context, fingerprintManager, storage, //
      "test", keyStore, keyGenerator, keyFactory);

  @Test public void cannotStoreSecurelyWithNoPermission() {
    shadowContext.denyPermissions(USE_FINGERPRINT);
    when(fingerprintManager.isHardwareDetected()).thenReturn(true);
    when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
    assertThat(whorlwind.canStoreSecurely()).isFalse();
    verifyZeroInteractions(storage);
  }

  @Test public void cannotStoreSecurelyWithNoHardware() {
    shadowContext.grantPermissions(USE_FINGERPRINT);
    when(fingerprintManager.isHardwareDetected()).thenReturn(false);
    when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
    assertThat(whorlwind.canStoreSecurely()).isFalse();
    verifyZeroInteractions(storage);
  }

  @Test public void cannotStoreSecurelyWithNoFingerprints() {
    shadowContext.grantPermissions(USE_FINGERPRINT);
    when(fingerprintManager.isHardwareDetected()).thenReturn(true);
    when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
    assertThat(whorlwind.canStoreSecurely()).isFalse();
    verifyZeroInteractions(storage);
  }

  @Test public void canStoreSecurelyWithPermissionAndHardwareAndFingerprints() {
    shadowContext.grantPermissions(USE_FINGERPRINT);
    when(fingerprintManager.isHardwareDetected()).thenReturn(true);
    when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
    assertThat(whorlwind.canStoreSecurely()).isTrue();
    verifyZeroInteractions(storage);
  }

  @Test public void writeThrowsWhenCannotStoreSecurely() {
    shadowContext.denyPermissions(USE_FINGERPRINT);

    try {
      whorlwind.write("test", ByteString.encodeUtf8("test"));
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage(
          "Can't store securely. Check canStoreSecurely() before attempting to read/write.");
    }

    verifyZeroInteractions(storage);
  }

  @Test public void readThrowsWhenCannotStoreSecurely() {
    shadowContext.denyPermissions(USE_FINGERPRINT);

    try {
      whorlwind.read("test");
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage(
          "Can't store securely. Check canStoreSecurely() before attempting to read/write.");
    }

    verifyZeroInteractions(storage);
  }

  @Test public void readThrowsOnSubscribeWhenCannotStoreSecurely() {
    shadowContext.grantPermissions(USE_FINGERPRINT);
    when(fingerprintManager.isHardwareDetected()).thenReturn(true);
    when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

    Observable<ReadResult> read = whorlwind.read("a");
    shadowContext.denyPermissions(USE_FINGERPRINT);

    try {
      read.toBlocking().forEach(new Action1<ReadResult>() {
        @Override public void call(ReadResult ignored) {
          fail();
        }
      });
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected).hasMessage(
          "Can't store securely. Check canStoreSecurely() before attempting to read/write.");
    }

    verifyZeroInteractions(storage);
  }

  private static class TestStorage implements Storage {
    private final Map<String, ByteString> storage = new LinkedHashMap<>();

    @Override public void clear() {
      storage.clear();
    }

    @Override public void remove(@NonNull String name) {
      storage.remove(name);
    }

    @Override public void put(@NonNull String name, @Nullable ByteString value) {
      storage.put(name, value);
    }

    @CheckResult @Override public ByteString get(@NonNull String name) {
      return storage.get(name);
    }

    @CheckResult @Override public Set<String> names() {
      return Collections.unmodifiableSet(new LinkedHashSet<>(storage.keySet()));
    }
  }
}
