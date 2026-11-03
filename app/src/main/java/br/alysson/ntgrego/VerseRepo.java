package br.alysson.ntgrego;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Texto grego: SBLGNT (Society of Biblical Literature / Logos), via MorphGNT. */
public final class VerseRepo {

    public static final class Verse {
        public final int index;
        public final String book;   // código USFM (JHN, ROM...)
        public final int chapter, verse;
        public final String text;

        Verse(int index, String book, int chapter, int verse, String text) {
            this.index = index; this.book = book; this.chapter = chapter; this.verse = verse; this.text = text;
        }

        public String reference() {
            String name = BOOK_NAMES.get(book);
            return (name != null ? name : book) + " " + chapter + ":" + verse;
        }

        public String usfm() { return book + "." + chapter + "." + verse; }
    }

    private static final Map<String, String> BOOK_NAMES = new HashMap<>();
    static {
        String[][] b = {
            {"MAT","Mateus"},{"MRK","Marcos"},{"LUK","Lucas"},{"JHN","João"},{"ACT","Atos"},
            {"ROM","Romanos"},{"1CO","1 Coríntios"},{"2CO","2 Coríntios"},{"GAL","Gálatas"},
            {"EPH","Efésios"},{"PHP","Filipenses"},{"COL","Colossenses"},{"1TH","1 Tessalonicenses"},
            {"2TH","2 Tessalonicenses"},{"1TI","1 Timóteo"},{"2TI","2 Timóteo"},{"TIT","Tito"},
            {"PHM","Filemom"},{"HEB","Hebreus"},{"JAS","Tiago"},{"1PE","1 Pedro"},{"2PE","2 Pedro"},
            {"1JN","1 João"},{"2JN","2 João"},{"3JN","3 João"},{"JUD","Judas"},{"REV","Apocalipse"}};
        for (String[] p : b) BOOK_NAMES.put(p[0], p[1]);
    }

    private static List<Verse> cache;

    public static synchronized List<Verse> all(Context ctx) {
        if (cache != null) return cache;
        List<Verse> list = new ArrayList<>(8000);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                ctx.getAssets().open("nt.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|", 4);
                if (p.length < 4) continue;
                list.add(new Verse(list.size(), p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), p[3]));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        cache = list;
        return list;
    }

    public static Verse get(Context ctx, int index) {
        List<Verse> l = all(ctx);
        if (index < 0) index = 0;
        if (index >= l.size()) index = l.size() - 1;
        return l.get(index);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("ntgrego", Context.MODE_PRIVATE);
    }

    /** Versículo do dia: determinístico pela data; pode ser trocado manualmente no widget. */
    public static Verse today(Context ctx) {
        int n = all(ctx).size();
        long day = LocalDate.now().toEpochDay();
        SharedPreferences sp = prefs(ctx);
        if (sp.getLong("override_day", -1) == day) {
            return get(ctx, sp.getInt("override_index", 0));
        }
        long h = (day * 2654435761L) ^ 0x5DEECE66DL;
        int idx = (int) (Math.floorMod(h, (long) n));
        return get(ctx, idx);
    }

    public static Verse shuffleToday(Context ctx) {
        int idx = new Random().nextInt(all(ctx).size());
        prefs(ctx).edit()
                .putLong("override_day", LocalDate.now().toEpochDay())
                .putInt("override_index", idx)
                .apply();
        return get(ctx, idx);
    }

    // Versões do bible.com (configuráveis pelo app)
    public static int versionId(Context ctx, String abbr) {
        int def = "NVI".equals(abbr) ? 129 : 277;
        return prefs(ctx).getInt("vid_" + abbr, def);
    }

    public static void setVersionId(Context ctx, String abbr, int id) {
        prefs(ctx).edit().putInt("vid_" + abbr, id).apply();
    }

    private VerseRepo() {}
}
