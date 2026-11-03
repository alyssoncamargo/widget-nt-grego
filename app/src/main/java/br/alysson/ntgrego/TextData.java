package br.alysson.ntgrego;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dados embutidos, alinhados linha a linha com nt.txt:
 *  - tb.txt    : Tradução Brasileira (uso pessoal)
 *  - morph.txt : palavras do versículo com morfologia MorphGNT (texto|classe|análise|lema), separadas por TAB
 *  - lex.txt   : lema TAB glosa (Dodson Greek Lexicon, CC0)
 */
public final class TextData {

    public static final class Word {
        public final String text, pos, parse, lemma;
        Word(String text, String pos, String parse, String lemma) {
            this.text = text; this.pos = pos; this.parse = parse; this.lemma = lemma;
        }
    }

    private static String[] tb, morph;
    private static Map<String, String> lex;

    private static List<String> lines(Context ctx, String asset) {
        List<String> out = new ArrayList<>(8000);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                ctx.getAssets().open(asset), StandardCharsets.UTF_8), 1 << 16)) {
            String l;
            while ((l = r.readLine()) != null) out.add(l);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    public static synchronized String tb(Context ctx, int verseIndex) {
        if (tb == null) tb = lines(ctx, "tb.txt").toArray(new String[0]);
        return verseIndex >= 0 && verseIndex < tb.length ? tb[verseIndex] : "";
    }

    public static synchronized List<Word> words(Context ctx, int verseIndex) {
        if (morph == null) morph = lines(ctx, "morph.txt").toArray(new String[0]);
        List<Word> out = new ArrayList<>();
        if (verseIndex < 0 || verseIndex >= morph.length) return out;
        for (String tok : morph[verseIndex].split("\t")) {
            String[] p = tok.split("\\|");
            if (p.length == 4) out.add(new Word(p[0], p[1], p[2], p[3]));
        }
        return out;
    }

    public static synchronized String gloss(Context ctx, String lemma) {
        if (lex == null) {
            lex = new HashMap<>(6000);
            for (String l : lines(ctx, "lex.txt")) {
                int t = l.indexOf('\t');
                if (t > 0) lex.put(l.substring(0, t), l.substring(t + 1));
            }
        }
        return lex.get(lemma);
    }

    private TextData() {}
}
