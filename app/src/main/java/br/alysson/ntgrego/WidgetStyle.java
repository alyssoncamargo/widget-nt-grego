package br.alysson.ntgrego;

import android.content.Context;
import android.content.SharedPreferences;

/** Aparência do widget (global para todos os widgets). */
public final class WidgetStyle {
    public int bgColor = 0xFF1E1A16;   // sem alfa (alfa vem de opacity)
    public int opacity = 90;           // 0-100 %
    public int textColor = 0xFFF5EEDC;
    public int refColor = 0xFFD4A95A;
    public int fontSize = 17;          // sp

    public static final int[] BG_COLORS = {
            0xFF1E1A16, 0xFF000000, 0xFF263238, 0xFF1A2A44, 0xFF2E3B2C,
            0xFF4A1C1C, 0xFFFAF6EE, 0xFFFFFFFF, 0xFFE8DCC4, 0xFFDDE7F0};
    public static final int[] TEXT_COLORS = {
            0xFFF5EEDC, 0xFFFFFFFF, 0xFFD4A95A, 0xFFB0BEC5,
            0xFF1E1A16, 0xFF000000, 0xFF5D4037, 0xFF1A2A44};

    public static WidgetStyle load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("ntgrego", Context.MODE_PRIVATE);
        WidgetStyle s = new WidgetStyle();
        s.bgColor = sp.getInt("w_bg", s.bgColor);
        s.opacity = sp.getInt("w_opacity", s.opacity);
        s.textColor = sp.getInt("w_text", s.textColor);
        s.fontSize = sp.getInt("w_font", s.fontSize);
        s.refColor = sp.getInt("w_ref", defaultRef(s.bgColor));
        return s;
    }

    public void save(Context ctx) {
        ctx.getSharedPreferences("ntgrego", Context.MODE_PRIVATE).edit()
                .putInt("w_bg", bgColor).putInt("w_opacity", opacity)
                .putInt("w_text", textColor).putInt("w_font", fontSize)
                .putInt("w_ref", refColor).apply();
    }

    /** Cor da referência: dourado em fundo escuro, marrom em fundo claro. */
    public static int defaultRef(int bg) {
        return isLight(bg) ? 0xFF8A5A1A : 0xFFD4A95A;
    }

    public static boolean isLight(int c) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        return (0.299 * r + 0.587 * g + 0.114 * b) > 150;
    }

    public int alpha255() { return Math.round(opacity * 2.55f); }

    public int subtleColor() { return (textColor & 0x00FFFFFF) | 0xB3000000; }
}
