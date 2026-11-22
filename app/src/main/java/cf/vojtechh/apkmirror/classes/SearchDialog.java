package cf.vojtechh.apkmirror.classes;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.net.URLEncoder;

import cf.vojtechh.apkmirror.R;

/** The search window (used by the search button and by the launcher shortcut). */
public final class SearchDialog {

    public interface Callback {
        void onSearch(String searchUrl);
    }

    private SearchDialog() {
    }

    public static void show(Context context, final Callback callback, DialogInterface.OnDismissListener onDismiss) {
        final EditText input = new EditText(context);
        input.setHint(R.string.search);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        int pad = (int) (24 * context.getResources().getDisplayMetrics().density);
        FrameLayout box = new FrameLayout(context);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(pad, pad / 2, pad, 0);
        box.addView(input, lp);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.search)
                .setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            callback.onSearch("https://www.apkmirror.com/?s=" + URLEncoder.encode(text, "UTF-8"));
                        } catch (java.io.UnsupportedEncodingException ignored) {
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(onDismiss)
                .show();
    }
}
