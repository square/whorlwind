package com.squareup.whorlwind.sample;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import io.reactivex.functions.Consumer;
import java.util.Collections;
import java.util.List;
import okio.ByteString;

final class SampleAdapter extends BaseAdapter implements Consumer<List<Pair<String, ByteString>>> {
  private final LayoutInflater inflater;
  private final Consumer<String> readConsumer;

  private List<Pair<String, ByteString>> data;

  public SampleAdapter(Context context, Consumer<String> readConsumer) {
    this.inflater = LayoutInflater.from(context);
    this.readConsumer = readConsumer;

    data = Collections.emptyList();
  }

  @Override public void accept(List<Pair<String, ByteString>> data) {
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
      itemView.setReadConsumer(readConsumer);
    } else {
      itemView = (ItemView) convertView;
    }

    itemView.setItem(getItem(position));
    return itemView;
  }
}
