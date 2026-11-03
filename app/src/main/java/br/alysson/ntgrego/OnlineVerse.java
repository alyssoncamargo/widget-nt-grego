package br.alysson.ntgrego;

import android.text.Html;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Busca o texto de um versículo de uma versão protegida (ex.: NVI) direto do bible.com,
 * lendo a descrição da página (a mesma que o site exibe ao compartilhar o link).
 * Nada é armazenado em disco; só um cache em memória durante a sessão.
 */
public final class OnlineVerse {

    private static final Map<String, String> cache = new ConcurrentHashMap<>();
    private static final Pattern[] METAS = {
            Pattern.compile("<meta[^>]+property=\"og:description\"[^>]+content=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<meta[^>]+content=\"([^\"]*)\"[^>]+property=\"og:description\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<meta[^>]+name=\"description\"[^>]+content=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<meta[^>]+content=\"([^\"]*)\"[^>]+name=\"description\"", Pattern.CASE_INSENSITIVE),
    };

    public static String url(int versionId, String usfm, String abbr) {
        return "https://www.bible.com/pt/bible/" + versionId + "/" + usfm + "." + abbr;
    }

    /** Bloqueante — chamar fora da UI thread. Retorna null se falhar. */
    public static String fetch(int versionId, String usfm, String abbr, String reference) {
        String u = url(versionId, usfm, abbr);
        String hit = cache.get(u);
        if (hit != null) return hit;
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) NTGrego/1.1");
            c.setRequestProperty("Accept-Language", "pt-BR,pt");
            if (c.getResponseCode() != 200) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buf = new char[8192];
                int n;
                // a descrição fica no <head>; não precisa ler a página inteira
                while ((n = r.read(buf)) > 0 && sb.length() < 400_000) {
                    sb.append(buf, 0, n);
                    if (sb.indexOf("</head>") >= 0) break;
                }
            }
            String html = sb.toString();
            for (Pattern p : METAS) {
                Matcher m = p.matcher(html);
                if (m.find()) {
                    String t = clean(Html.fromHtml(m.group(1), Html.FROM_HTML_MODE_LEGACY).toString(), reference, abbr);
                    if (!t.isEmpty()) {
                        cache.put(u, t);
                        return t;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** Remove prefixos como "João 3:16 NVI" ou sufixos "(NVI)" caso o site os inclua. */
    private static String clean(String t, String reference, String abbr) {
        t = t.trim();
        if (reference != null && t.startsWith(reference)) t = t.substring(reference.length()).trim();
        if (t.startsWith(abbr)) t = t.substring(abbr.length()).trim();
        if (t.startsWith("-") || t.startsWith("–") || t.startsWith(":")) t = t.substring(1).trim();
        String suf = "(" + abbr + ")";
        if (t.endsWith(suf)) t = t.substring(0, t.length() - suf.length()).trim();
        return t;
    }

    private OnlineVerse() {}
}
