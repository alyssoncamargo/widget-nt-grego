package br.alysson.ntgrego;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Tela de aparência do widget: fundo, transparência, cor e tamanho da fonte. */
public class ConfigActivity extends Activity {

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetStyle style;
    private GradientDrawable previewBg;
    private TextView pText, pRef;
    private TextView opLabel, fontLabel;
    private float dp;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        dp = getResources().getDisplayMetrics().density;
        Bundle ex = getIntent().getExtras();
        if (ex != null) widgetId = ex.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

        style = WidgetStyle.load(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = px(20);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        root.addView(title("Aparência do widget"));

        // Pré-visualização sobre um "papel de parede" neutro
        FrameLayout wall = new FrameLayout(this);
        wall.setBackgroundColor(0xFF7A8A99);
        wall.setPadding(px(16), px(16), px(16), px(16));
        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(px(14), px(14), px(14), px(14));
        previewBg = new GradientDrawable();
        previewBg.setCornerRadius(px(22));
        preview.setBackground(previewBg);
        pText = new TextView(this);
        pText.setTypeface(Typeface.SERIF);
        pText.setLineSpacing(0, 1.15f);
        pText.setText(VerseRepo.today(this).text);
        pText.setMaxLines(6);
        pRef = new TextView(this);
        pRef.setTypeface(Typeface.DEFAULT_BOLD);
        pRef.setTextSize(13);
        pRef.setText(VerseRepo.today(this).reference());
        pRef.setPadding(0, px(6), 0, 0);
        preview.addView(pText);
        preview.addView(pRef);
        wall.addView(preview);
        root.addView(wall, new LinearLayout.LayoutParams(-1, -2));

        root.addView(label("Cor de fundo"));
        root.addView(swatches(WidgetStyle.BG_COLORS, true));

        opLabel = label("");
        root.addView(opLabel);
        root.addView(seek(0, 100, style.opacity, v -> { style.opacity = v; refresh(); }));

        root.addView(label("Cor da fonte"));
        root.addView(swatches(WidgetStyle.TEXT_COLORS, false));

        fontLabel = label("");
        root.addView(fontLabel);
        root.addView(seek(12, 28, style.fontSize, v -> { style.fontSize = v; refresh(); }));

        Button save = new Button(this);
        save.setText("Salvar");
        save.setOnClickListener(v -> finishSave());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = px(24);
        root.addView(save, lp);

        setContentView(scroll);
        refresh();
    }

    private void finishSave() {
        style.save(this);
        VerseWidget.updateAll(this);
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
        }
        finish();
    }

    private void refresh() {
        previewBg.setColor((style.bgColor & 0x00FFFFFF) | (style.alpha255() << 24));
        pText.setTextColor(style.textColor);
        pText.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize);
        pRef.setTextColor(style.refColor);
        opLabel.setText("Opacidade do fundo: " + style.opacity + "%");
        fontLabel.setText("Tamanho da fonte: " + style.fontSize + " sp");
    }

    private interface IntCb { void on(int v); }

    private SeekBar seek(int min, int max, int value, IntCb cb) {
        SeekBar s = new SeekBar(this);
        s.setMin(min);
        s.setMax(max);
        s.setProgress(value);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int p, boolean u) { cb.on(p); }
            @Override public void onStartTrackingTouch(SeekBar b) {}
            @Override public void onStopTrackingTouch(SeekBar b) {}
        });
        return s;
    }

    private View swatches(int[] colors, boolean background) {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (int c : colors) {
            View sw = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(c);
            d.setStroke(px(1), 0x55888888);
            sw.setBackground(d);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(40), px(40));
            lp.rightMargin = px(10);
            sw.setOnClickListener(v -> {
                if (background) {
                    boolean refWasDefault = style.refColor == WidgetStyle.defaultRef(style.bgColor);
                    style.bgColor = c;
                    if (refWasDefault) style.refColor = WidgetStyle.defaultRef(c);
                    // ajusta a fonte automaticamente se ficaria ilegível
                    if (WidgetStyle.isLight(c) == WidgetStyle.isLight(style.textColor)) {
                        style.textColor = WidgetStyle.isLight(c) ? 0xFF1E1A16 : 0xFFF5EEDC;
                    }
                } else {
                    style.textColor = c;
                }
                refresh();
            });
            row.addView(sw, lp);
        }
        hs.addView(row);
        hs.setPadding(0, px(4), 0, px(4));
        return hs;
    }

    private TextView title(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(22);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 0, 0, px(16));
        return t;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setPadding(0, px(18), 0, px(6));
        return t;
    }

    private int px(int v) { return Math.round(v * dp); }
}
