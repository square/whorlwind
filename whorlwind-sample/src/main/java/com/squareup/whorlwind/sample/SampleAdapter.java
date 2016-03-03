package com.squareup.whorlwind.sample;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.Collections;
import java.util.List;
import okio.ByteString;
import rx.functions.Action1;

final class SampleAdapter extends BaseAdapter implements Action1<List<Pair<String, ByteString>>> {
  private final LayoutInflater inflater;
  private final Action1<String> readAction;

  private List<Pair<String, ByteString>> data;

  public SampleAdapter(Context context, Action1<String> readAction) {
    this.inflater = LayoutInflater.from(context);
    this.readAction = readAction;

    data = Collections.emptyList();
  }

  @Override public void call(List<Pair<String, ByteString>> data) {
    this.data = data;
    notifyDataSetChanged();
  }

  @Override public int getCount() {
    return data.size();
  }

  @Override public Pair<String, ByteString> getItem(int position) {
    return data.get(position);
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public View getView(int position, View convertView, ViewGroup parent) {
    ItemView itemView;
    if (convertView == null) {
      itemView = (ItemView) inflater.inflate(R.layout.item, parent, false);
      itemView.setReadAction(readAction);
    } else {
      itemView = (ItemView) convertView;
    }

    itemView.setItem(getItem(position));
    return itemView;
  }
}
