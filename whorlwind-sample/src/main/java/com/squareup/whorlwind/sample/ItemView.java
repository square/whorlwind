package com.squareup.whorlwind.sample;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okio.ByteString;
import rx.functions.Action1;

public final class ItemView extends LinearLayout {
  @BindView(R.id.key) TextView keyView;
  @BindView(R.id.value) TextView valueView;

  private Action1<String> readAction;

  public ItemView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);
  }

  public void setReadAction(Action1<String> readAction) {
    this.readAction = readAction;
  }

  public void setItem(Pair<String, ByteString> item) {
    keyView.setText(item.first);
    valueView.setText(item.second.hex());
  }

  @OnClick(R.id.read) void read() {
    readAction.call(keyView.getText().toString());
  }
}
