package com.squareup.whorlwind.sample;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.functions.Consumer;
import okio.ByteString;

public final class ItemView extends LinearLayout {
  @BindView(R.id.key) TextView keyView;
  @BindView(R.id.value) TextView valueView;

  private Consumer<String> readConsumer;

  public ItemView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);
  }

  public void setReadConsumer(Consumer<String> readConsumer) {
    this.readConsumer = readConsumer;
  }

  public void setItem(Pair<String, ByteString> item) {
    keyView.setText(item.first);
    valueView.setText(item.second.hex());
  }

  @OnClick(R.id.read) void read() {
    try {
      readConsumer.accept(keyView.getText().toString());
    } catch (Exception e) {
      Log.e("ItemView", "Error passing read value to Consumer");
    }
  }
}
