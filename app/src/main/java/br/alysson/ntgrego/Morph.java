package br.alysson.ntgrego;

import java.util.ArrayList;
import java.util.List;

/** Decodifica os códigos morfológicos do MorphGNT para português. */
public final class Morph {

    public static String pos(String code) {
        switch (code) {
            case "A-": return "Adjetivo";
            case "C-": return "Conjunção";
            case "D-": return "Advérbio";
            case "I-": return "Interjeição";
            case "N-": return "Substantivo";
            case "P-": return "Preposição";
            case "RA": return "Artigo";
            case "RD": return "Pronome demonstrativo";
            case "RI": return "Pronome interrogativo/indefinido";
            case "RP": return "Pronome pessoal";
            case "RR": return "Pronome relativo";
            case "V-": return "Verbo";
            case "X-": return "Partícula";
            default: return code;
        }
    }

    /** Linhas "rótulo: valor" da análise (parse = 8 posições: pessoa, tempo, voz, modo, caso, número, gênero, grau). */
    public static List<String[]> details(String parse) {
        List<String[]> out = new ArrayList<>();
        if (parse == null || parse.length() < 8) return out;
        add(out, "Tempo", tense(parse.charAt(1)));
        add(out, "Voz", voice(parse.charAt(2)));
        add(out, "Modo", mood(parse.charAt(3)));
        add(out, "Pessoa", person(parse.charAt(0)));
        add(out, "Caso", kase(parse.charAt(4)));
        add(out, "Número", number(parse.charAt(5)));
        add(out, "Gênero", gender(parse.charAt(6)));
        add(out, "Grau", degree(parse.charAt(7)));
        return out;
    }

    /** Resumo numa linha, ex.: "aoristo ativo indicativo · 3ª sing." */
    public static String summary(String pos, String parse) {
        StringBuilder sb = new StringBuilder(pos(pos));
        for (String[] d : details(parse)) sb.append(" · ").append(d[1]);
        return sb.toString();
    }

    private static void add(List<String[]> l, String k, String v) { if (v != null) l.add(new String[]{k, v}); }

    private static String person(char c) {
        switch (c) { case '1': return "1ª"; case '2': return "2ª"; case '3': return "3ª"; default: return null; }
    }
    private static String tense(char c) {
        switch (c) {
            case 'P': return "presente"; case 'I': return "imperfeito"; case 'F': return "futuro";
            case 'A': return "aoristo"; case 'X': return "perfeito"; case 'Y': return "mais-que-perfeito";
            default: return null;
        }
    }
    private static String voice(char c) {
        switch (c) { case 'A': return "ativa"; case 'M': return "média"; case 'P': return "passiva"; default: return null; }
    }
    private static String mood(char c) {
        switch (c) {
            case 'I': return "indicativo"; case 'D': return "imperativo"; case 'S': return "subjuntivo";
            case 'O': return "optativo"; case 'N': return "infinitivo"; case 'P': return "particípio";
            default: return null;
        }
    }
    private static String kase(char c) {
        switch (c) {
            case 'N': return "nominativo"; case 'G': return "genitivo"; case 'D': return "dativo";
            case 'A': return "acusativo"; case 'V': return "vocativo"; default: return null;
        }
    }
    private static String number(char c) {
        switch (c) { case 'S': return "singular"; case 'P': return "plural"; default: return null; }
    }
    private static String gender(char c) {
        switch (c) { case 'M': return "masculino"; case 'F': return "feminino"; case 'N': return "neutro"; default: return null; }
    }
    private static String degree(char c) {
        switch (c) { case 'C': return "comparativo"; case 'S': return "superlativo"; default: return null; }
    }

    private Morph() {}
}
