package br.alysson.ntgrego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class ReaderActivity extends Activity {

    public static final String EXTRA_START = "start";
    public static final String EXTRA_END = "end";

    private int start, end;
    private TextView ref, greek, tb;
    private View panel;
    private TextView pWord, pLemma, pGloss, pPos, pDetails;
    private BackgroundColorSpan highlight;
    private SpannableStringBuilder greekSpans;


    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_reader);

        ref = findViewById(R.id.ref);
        greek = findViewById(R.id.greek);
        tb = findViewById(R.id.tb);
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

        fromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        fromIntent(intent);
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

    private void openWeb(String abbr) {
        VerseRepo.Passage p = VerseRepo.passage(this, start, end);
        String u = "https://www.bible.com/pt/bible/" + VerseRepo.versionId(this, abbr) + "/" + p.usfm() + "." + abbr;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
    }

    // ---------- Menu ----------

    private void openMenu() {
        String[] items = {"Aparência do widget", "Abrir no YouVersion"};
        new AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    if (which == 0) startActivity(new Intent(this, ConfigActivity.class));
                    else openWeb("TB");
                })
                .show();
    }

}
