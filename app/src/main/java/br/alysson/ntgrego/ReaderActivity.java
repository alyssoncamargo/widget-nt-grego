package br.alysson.ntgrego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderActivity extends Activity {

    public static final String EXTRA_START = "start";
    public static final String EXTRA_END = "end";

    private int start, end;
    private int loadToken = 0;
    private TextView ref, greek, tb, nvi, nviOpen;
    private View panel;
    private TextView pWord, pLemma, pGloss, pPos, pDetails;
    private BackgroundColorSpan highlight;
    private SpannableStringBuilder greekSpans;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_reader);

        ref = findViewById(R.id.ref);
        greek = findViewById(R.id.greek);
        tb = findViewById(R.id.tb);
        nvi = findViewById(R.id.nvi);
        nviOpen = findViewById(R.id.nvi_open);
        panel = findViewById(R.id.panel);
        pWord = findViewById(R.id.p_word);
        pLemma = findViewById(R.id.p_lemma);
        pGloss = findViewById(R.id.p_gloss);
        pPos = findViewById(R.id.p_pos);
        pDetails = findViewById(R.id.p_details);

        greek.setMovementMethod(LinkMovementMethod.getInstance());
        greek.setHighlightColor(0x00000000);
        highlight = new BackgroundColorSpan(getColor(R.color.hl));

        findViewById(R.id.prev).setOnClickListener(v -> show(start - 1, start - 1));
        findViewById(R.id.next).setOnClickListener(v -> show(end + 1, end + 1));
        findViewById(R.id.today).setOnClickListener(v -> showToday());
        findViewById(R.id.menu).setOnClickListener(v -> openMenu());
        findViewById(R.id.p_close).setOnClickListener(v -> closePanel());
        nviOpen.setOnClickListener(v -> openWeb("NVI"));

        fromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        fromIntent(intent);
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (panel.getVisibility() == View.VISIBLE) closePanel();
        else super.onBackPressed();
    }

    private void fromIntent(Intent i) {
        if (i != null && i.hasExtra(EXTRA_START)) {
            int s = i.getIntExtra(EXTRA_START, 0);
            show(s, i.getIntExtra(EXTRA_END, s));
        } else {
            showToday();
        }
    }

    private void showToday() {
        VerseRepo.Passage p = VerseRepo.today(this);
        show(p.first.index, p.last.index);
    }

    private void show(int s, int e) {
        VerseRepo.Passage p = VerseRepo.passage(this, s, e);
        start = p.first.index;
        end = p.last.index;
        ref.setText(p.reference());
        closePanel();
        buildGreek();
        tb.setText(buildTb());
        loadNvi();
    }

    // ---------- Grego clicável ----------

    private void buildGreek() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean multi = end > start;
        for (int vi = start; vi <= end; vi++) {
            if (multi) appendVerseNumber(sb, VerseRepo.get(this, vi).verse);
            for (TextData.Word w : TextData.words(this, vi)) {
                int[] core = letterRange(w.text);
                int base = sb.length();
                sb.append(w.text);
                if (core != null) {
                    sb.setSpan(new WordSpan(w), base + core[0], base + core[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                sb.append(' ');
            }
        }
        greekSpans = sb;
        greek.setText(sb, TextView.BufferType.SPANNABLE);
    }

    private void appendVerseNumber(SpannableStringBuilder sb, int n) {
        int a = sb.length();
        sb.append(String.valueOf(n));
        sb.setSpan(new SuperscriptSpan(), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new RelativeSizeSpan(0.55f), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(getColor(R.color.accent)), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(' ');
    }

    /** Parte "letra" do token (sem pontuação nas pontas). */
    private static int[] letterRange(String t) {
        int a = 0, b = t.length();
        while (a < b && !Character.isLetter(t.charAt(a))) a++;
        while (b > a && !Character.isLetter(t.charAt(b - 1))) b--;
        return a < b ? new int[]{a, b} : null;
    }

    private final class WordSpan extends ClickableSpan {
        final TextData.Word w;
        WordSpan(TextData.Word w) { this.w = w; }

        @Override public void onClick(View v) {
            Spanned sp = (Spanned) greek.getText();
            int a = sp.getSpanStart(this), b = sp.getSpanEnd(this);
            if (greek.getText() instanceof android.text.Spannable) {
                android.text.Spannable s = (android.text.Spannable) greek.getText();
                s.removeSpan(highlight);
                s.setSpan(highlight, a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            showWord(w);
        }

        @Override public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false); // mantém a cor normal do texto
        }
    }

    private void showWord(TextData.Word w) {
        int[] core = letterRange(w.text);
        pWord.setText(core != null ? w.text.substring(core[0], core[1]) : w.text);
        pLemma.setText("lema: " + w.lemma);
        String g = TextData.gloss(this, w.lemma);
        pGloss.setText(g != null ? g : "(sem glosa no léxico)");
        pPos.setText(Morph.pos(w.pos));
        StringBuilder sb = new StringBuilder();
        for (String[] d : Morph.details(w.parse)) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(d[0]).append(": ").append(d[1]);
        }
        pDetails.setText(sb);
        pDetails.setVisibility(sb.length() > 0 ? View.VISIBLE : View.GONE);
        panel.setVisibility(View.VISIBLE);
    }

    private void closePanel() {
        panel.setVisibility(View.GONE);
        if (greek.getText() instanceof android.text.Spannable) {
            ((android.text.Spannable) greek.getText()).removeSpan(highlight);
        }
    }

    // ---------- Traduções ----------

    private CharSequence buildTb() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean multi = end > start;
        for (int vi = start; vi <= end; vi++) {
            String t = TextData.tb(this, vi);
            if (t.isEmpty()) continue;
            if (multi) appendVerseNumber(sb, VerseRepo.get(this, vi).verse);
            sb.append(t).append(' ');
        }
        if (sb.length() == 0) sb.append("(versículo ausente na TB)");
        return sb;
    }

    private void loadNvi() {
        final int token = ++loadToken;
        final int s = start, e = end;
        final int vid = VerseRepo.versionId(this, "NVI");
        nvi.setText("carregando…");
        nvi.setAlpha(0.5f);
        io.submit(() -> {
            SpannableStringBuilder sb = new SpannableStringBuilder();
            boolean ok = true;
            for (int vi = s; vi <= e; vi++) {
                VerseRepo.Verse v = VerseRepo.get(this, vi);
                String t = OnlineVerse.fetch(vid, v.usfm(), "NVI", v.reference());
                if (t == null) { ok = false; break; }
                if (e > s) appendVerseNumber(sb, v.verse);
                sb.append(t).append(' ');
            }
            final boolean fine = ok;
            ui.post(() -> {
                if (token != loadToken) return;
                nvi.setAlpha(1f);
                if (fine) {
                    nvi.setText(sb);
                } else {
                    SpannableStringBuilder msg = new SpannableStringBuilder("Não foi possível carregar a NVI (sem internet?). Toque abaixo para abrir no site.");
                    msg.setSpan(new StyleSpan(Typeface.ITALIC), 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    nvi.setText(msg);
                }
            });
        });
    }

    private void openWeb(String abbr) {
        VerseRepo.Passage p = VerseRepo.passage(this, start, end);
        String u = OnlineVerse.url(VerseRepo.versionId(this, abbr), p.usfm(), abbr);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
    }

    // ---------- Menu ----------

    private void openMenu() {
        String[] items = {"Aparência do widget", "Abrir no YouVersion (TB)", "Abrir no YouVersion (NVI)", "Versão online da NVI…"};
        new AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: startActivity(new Intent(this, ConfigActivity.class)); break;
                        case 1: openWeb("TB"); break;
                        case 2: openWeb("NVI"); break;
                        case 3: openVersionSettings(); break;
                    }
                })
                .show();
    }

    /** Permite colar um link do bible.com (ou só o número) para trocar a versão usada na seção NVI. */
    private void openVersionSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        TextView hint = new TextView(this);
        hint.setText("Cole um link do bible.com da versão (ex.: bible.com/pt/bible/129/JHN.3.16.NVI) ou só o número.");
        box.addView(hint);
        EditText field = new EditText(this);
        field.setHint("atual: " + VerseRepo.versionId(this, "NVI"));
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        box.addView(field);
        new AlertDialog.Builder(this)
                .setTitle("Versão NVI")
                .setView(box)
                .setPositiveButton("Salvar", (d, w) -> {
                    Integer id = parseId(field.getText().toString());
                    if (id != null) {
                        VerseRepo.setVersionId(this, "NVI", id);
                        Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show();
                        loadNvi();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static Integer parseId(String s) {
        s = s.trim();
        if (s.isEmpty()) return null;
        if (s.matches("\\d+")) return Integer.parseInt(s);
        Matcher m = Pattern.compile("/(?:bible|versions)/(\\d+)").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
}
