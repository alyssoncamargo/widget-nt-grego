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

    /** Trecho de 1 a 3 versículos que forma uma frase completa. */
    public static final class Passage {
        public final Verse first, last;
        public final String text;
        Passage(Verse first, Verse last, String text) { this.first = first; this.last = last; this.text = text; }
        public String reference() {
            return first.reference() + (last.index > first.index ? "-" + last.verse : "");
        }
        public String usfm() {
            return first.usfm() + (last.index > first.index ? "-" + last.verse : "");
        }
    }

    public static Passage passage(Context ctx, int start, int end) {
        List<Verse> l = all(ctx);
        start = Math.max(0, Math.min(start, l.size() - 1));
        end = Math.max(start, Math.min(end, l.size() - 1));
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(l.get(i).text);
        }
        return new Passage(l.get(start), l.get(end), sb.toString());
    }

    private static int[][] units;

    /** Trechos selecionados: frases completas (até 3 vv.) com mais referências cruzadas (OpenBible.info). */
    private static synchronized int[][] units(Context ctx) {
        if (units != null) return units;
        List<int[]> list = new ArrayList<>(1600);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                ctx.getAssets().open("units.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length == 2) list.add(new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])});
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        units = list.toArray(new int[0][]);
        return units;
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

    /** Trecho do dia: determinístico pela data; pode ser trocado manualmente no widget. */
    public static Passage today(Context ctx) {
        int[][] u = units(ctx);
        long day = LocalDate.now().toEpochDay();
        SharedPreferences sp = prefs(ctx);
        int idx;
        if (sp.getLong("override_day", -1) == day) {
            idx = sp.getInt("override_unit", 0);
        } else {
            // permutação: percorre todos os trechos antes de repetir
            idx = (int) Math.floorMod(day * 617L + 13L, (long) u.length);
        }
        idx = Math.floorMod(idx, u.length);
        return passage(ctx, u[idx][0], u[idx][1]);
    }

    public static Passage shuffleToday(Context ctx) {
        int[][] u = units(ctx);
        int idx = new Random().nextInt(u.length);
        prefs(ctx).edit()
                .putLong("override_day", LocalDate.now().toEpochDay())
                .putInt("override_unit", idx)
                .apply();
        return passage(ctx, u[idx][0], u[idx][1]);
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
