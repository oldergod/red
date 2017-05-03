package io.oldering.tvfoot.red.app.common;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.ImageView;

public class DataBindingAdapters {

  @BindingAdapter("imageResource")
  public static void setImageResource(ImageView imageView, String resource) {
    Context context = imageView.getContext();
    int identifier =
        context.getResources().getIdentifier(resource, "drawable", context.getPackageName());
    imageView.setImageResource(identifier);
  }

  @BindingAdapter("tvfootTeamImageResource")
  public static void setTvFootTeamImageResource(ImageView imageView, String resource) {
    setImageResource(imageView, "tvfoot_team_" + resource);
  }

  @BindingAdapter("visible") public static void setVisibility(View view, boolean isVisible) {
    view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
  }
}