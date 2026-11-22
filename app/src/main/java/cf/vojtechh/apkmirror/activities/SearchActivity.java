package cf.vojtechh.apkmirror.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import cf.vojtechh.apkmirror.classes.SearchDialog;

public class SearchActivity extends AppCompatActivity {

    private boolean searched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SearchDialog.show(this, url -> {
            searched = true;
            Intent i = new Intent(SearchActivity.this, MainActivity.class);
            i.putExtra("url", url);
            startActivity(i);
            finish();
        }, dialog -> {
            if (!searched) finish();
        });
    }
}
