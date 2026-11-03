package br.alysson.ntgrego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderActivity extends Activity {

    public static final String EXTRA_INDEX = "index";

    private int index;
    private String mode = "AMBAS"; // TB | NVI | AMBAS
    private TextView ref, greek;
    private WebView webTb, webNvi;
    private Button bTb, bNvi, bBoth;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_reader);

        ref = findViewById(R.id.ref);
        greek = findViewById(R.id.greek);
        webTb = findViewById(R.id.web_tb);
        webNvi = findViewById(R.id.web_nvi);
        bTb = findViewById(R.id.tab_tb);
        bNvi = findViewById(R.id.tab_nvi);
        bBoth = findViewById(R.id.tab_both);

        setupWeb(webTb);
        setupWeb(webNvi);

        mode = getSharedPreferences("ntgrego", MODE_PRIVATE).getString("mode", "AMBAS");

        bTb.setOnClickListener(v -> setMode("TB"));
        bNvi.setOnClickListener(v -> setMode("NVI"));
        bBoth.setOnClickListener(v -> setMode("AMBAS"));
        findViewById(R.id.prev).setOnClickListener(v -> show(index - 1));
        findViewById(R.id.next).setOnClickListener(v -> show(index + 1));
        findViewById(R.id.today).setOnClickListener(v -> show(VerseRepo.today(this).index));
        findViewById(R.id.settings).setOnClickListener(v -> openSettings());
        findViewById(R.id.youversion).setOnClickListener(v -> openYouVersion());

        index = getIntent().getIntExtra(EXTRA_INDEX, VerseRepo.today(this).index);
        applyMode();
        show(index);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        show(intent.getIntExtra(EXTRA_INDEX, VerseRepo.today(this).index));
    }

    private void setupWeb(WebView w) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        w.setWebViewClient(new WebViewClient());
    }

    private void show(int i) {
        VerseRepo.Verse v = VerseRepo.get(this, i);
        index = v.index;
        ref.setText(v.reference());
        greek.setText(v.text);
        webTb.loadUrl(url("TB", v));
        webNvi.loadUrl(url("NVI", v));
    }

    private String url(String abbr, VerseRepo.Verse v) {
        return "https://www.bible.com/pt/bible/" + VerseRepo.versionId(this, abbr) + "/" + v.usfm() + "." + abbr;
    }

    private void setMode(String m) {
        mode = m;
        getSharedPreferences("ntgrego", MODE_PRIVATE).edit().putString("mode", m).apply();
        applyMode();
    }

    private void applyMode() {
        webTb.setVisibility("NVI".equals(mode) ? View.GONE : View.VISIBLE);
        webNvi.setVisibility("TB".equals(mode) ? View.GONE : View.VISIBLE);
        findViewById(R.id.web_divider).setVisibility("AMBAS".equals(mode) ? View.VISIBLE : View.GONE);
        bTb.setAlpha("TB".equals(mode) ? 1f : 0.45f);
        bNvi.setAlpha("NVI".equals(mode) ? 1f : 0.45f);
        bBoth.setAlpha("AMBAS".equals(mode) ? 1f : 0.45f);
    }

    private void openYouVersion() {
        VerseRepo.Verse v = VerseRepo.get(this, index);
        String abbr = "NVI".equals(mode) ? "NVI" : "TB";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url(abbr, v))));
    }

    /** Permite colar um link do bible.com (ou só o número) para trocar a versão de cada aba. */
    private void openSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        TextView hint = new TextView(this);
        hint.setText("Cole um link do bible.com da versão desejada (ex.: bible.com/pt/bible/129/JHN.3.16.NVI) ou só o número da versão.");
        box.addView(hint);

        EditText tb = field("TB  (atual: " + VerseRepo.versionId(this, "TB") + ")");
        EditText nvi = field("NVI (atual: " + VerseRepo.versionId(this, "NVI") + ")");
        box.addView(tb);
        box.addView(nvi);

        new AlertDialog.Builder(this)
                .setTitle("Versões das abas")
                .setView(box)
                .setPositiveButton("Salvar", (d, w) -> {
                    Integer a = parseId(tb.getText().toString());
                    Integer b = parseId(nvi.getText().toString());
                    if (a != null) VerseRepo.setVersionId(this, "TB", a);
                    if (b != null) VerseRepo.setVersionId(this, "NVI", b);
                    Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show();
                    show(index);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        return e;
    }

    private static Integer parseId(String s) {
        s = s.trim();
        if (s.isEmpty()) return null;
        if (s.matches("\\d+")) return Integer.parseInt(s);
        Matcher m = Pattern.compile("/(?:bible|versions)/(\\d+)").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
}
