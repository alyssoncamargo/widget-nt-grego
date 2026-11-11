using System.IO;
using System.Reflection;
using System.Text;

namespace NTGrego;

public sealed record Verse(int Index, string Book, int Chapter, int Number, string Text)
{
    public string Reference => $"{Data.BookName(Book)} {Chapter}:{Number}";
    public string Usfm => $"{Book}.{Chapter}.{Number}";
}

public sealed record Passage(Verse First, Verse Last)
{
    public bool Multi => Last.Index > First.Index;
    public string Reference => First.Reference + (Multi ? "-" + Last.Number : "");
    public string Usfm => First.Usfm + (Multi ? "-" + Last.Number : "");
    public string Text => string.Join(" ", Enumerable.Range(First.Index, Last.Index - First.Index + 1).Select(i => Data.Verses[i].Text));
}

public sealed record Word(string Text, string Pos, string Parse, string Lemma);

/// <summary>
/// Mesmos dados e mesmo algoritmo do app Android (assets embutidos como recursos):
/// nt.txt (SBLGNT), morph.txt (MorphGNT), tb.txt (TB), lex.txt (Dodson), units.txt (trechos do dia).
/// </summary>
public static class Data
{
    static readonly Dictionary<string, string> Books = new()
    {
        ["MAT"] = "Mateus", ["MRK"] = "Marcos", ["LUK"] = "Lucas", ["JHN"] = "João", ["ACT"] = "Atos",
        ["ROM"] = "Romanos", ["1CO"] = "1 Coríntios", ["2CO"] = "2 Coríntios", ["GAL"] = "Gálatas",
        ["EPH"] = "Efésios", ["PHP"] = "Filipenses", ["COL"] = "Colossenses", ["1TH"] = "1 Tessalonicenses",
        ["2TH"] = "2 Tessalonicenses", ["1TI"] = "1 Timóteo", ["2TI"] = "2 Timóteo", ["TIT"] = "Tito",
        ["PHM"] = "Filemom", ["HEB"] = "Hebreus", ["JAS"] = "Tiago", ["1PE"] = "1 Pedro", ["2PE"] = "2 Pedro",
        ["1JN"] = "1 João", ["2JN"] = "2 João", ["3JN"] = "3 João", ["JUD"] = "Judas", ["REV"] = "Apocalipse",
    };

    public static string BookName(string usfm) => Books.TryGetValue(usfm, out var n) ? n : usfm;

    static List<string> Lines(string resource)
    {
        using var s = Assembly.GetExecutingAssembly().GetManifestResourceStream(resource)
                      ?? throw new FileNotFoundException(resource);
        using var r = new StreamReader(s, Encoding.UTF8);
        var list = new List<string>(8000);
        string l;
        while ((l = r.ReadLine()) != null) list.Add(l);
        return list;
    }

    static List<Verse> verses;
    public static List<Verse> Verses => verses ??= Lines("nt.txt")
        .Select(l => l.Split('|', 4)).Where(p => p.Length == 4)
        .Select((p, i) => new Verse(i, p[0], int.Parse(p[1]), int.Parse(p[2]), p[3])).ToList();

    static int[][] units;
    static int[][] Units => units ??= Lines("units.txt")
        .Select(l => l.Split('|')).Where(p => p.Length == 2)
        .Select(p => new[] { int.Parse(p[0]), int.Parse(p[1]) }).ToArray();

    static string[] tb, morph;
    static Dictionary<string, string> lex;

    public static string Tb(int i) { tb ??= Lines("tb.txt").ToArray(); return i >= 0 && i < tb.Length ? tb[i] : ""; }

    public static List<Word> Words(int i)
    {
        morph ??= Lines("morph.txt").ToArray();
        if (i < 0 || i >= morph.Length) return new();
        return morph[i].Split('\t').Select(t => t.Split('|')).Where(p => p.Length == 4)
            .Select(p => new Word(p[0], p[1], p[2], p[3])).ToList();
    }

    public static string Gloss(string lemma)
    {
        lex ??= Lines("lex.txt").Select(l => l.Split('\t', 2)).Where(p => p.Length == 2)
            .GroupBy(p => p[0]).ToDictionary(g => g.Key, g => g.First()[1]);
        return lex.TryGetValue(lemma, out var g) ? g : null;
    }

    public static Passage Get(int start, int end)
    {
        int n = Verses.Count;
        start = Math.Clamp(start, 0, n - 1);
        end = Math.Clamp(end, start, n - 1);
        return new Passage(Verses[start], Verses[end]);
    }

    static long EpochDay => DateOnly.FromDateTime(DateTime.Now).DayNumber - new DateOnly(1970, 1, 1).DayNumber;

    static long FloorMod(long a, long m) => ((a % m) + m) % m;

    /// <summary>Trecho do dia — idêntico ao Android: (dia × 617 + 13) mod 1500.</summary>
    public static Passage Today()
    {
        var u = Units;
        var st = Settings.Current;
        long day = EpochDay;
        int idx = st.OverrideDay == day ? st.OverrideUnit : (int)FloorMod(day * 617L + 13L, u.Length);
        idx = (int)FloorMod(idx, u.Length);
        return Get(u[idx][0], u[idx][1]);
    }

    public static Passage Shuffle()
    {
        var st = Settings.Current;
        st.OverrideDay = EpochDay;
        st.OverrideUnit = Random.Shared.Next(Units.Length);
        st.Save();
        return Today();
    }
}

/// <summary>Códigos morfológicos do MorphGNT em português.</summary>
public static class Morph
{
    public static string Pos(string c) => c switch
    {
        "A-" => "Adjetivo", "C-" => "Conjunção", "D-" => "Advérbio", "I-" => "Interjeição",
        "N-" => "Substantivo", "P-" => "Preposição", "RA" => "Artigo", "RD" => "Pronome demonstrativo",
        "RI" => "Pronome interrogativo/indefinido", "RP" => "Pronome pessoal", "RR" => "Pronome relativo",
        "V-" => "Verbo", "X-" => "Partícula", _ => c
    };

    public static List<(string Label, string Value)> Details(string p)
    {
        var o = new List<(string, string)>();
        if (p == null || p.Length < 8) return o;
        void Add(string k, string v) { if (v != null) o.Add((k, v)); }
        Add("Tempo", p[1] switch { 'P' => "presente", 'I' => "imperfeito", 'F' => "futuro", 'A' => "aoristo", 'X' => "perfeito", 'Y' => "mais-que-perfeito", _ => null });
        Add("Voz", p[2] switch { 'A' => "ativa", 'M' => "média", 'P' => "passiva", _ => null });
        Add("Modo", p[3] switch { 'I' => "indicativo", 'D' => "imperativo", 'S' => "subjuntivo", 'O' => "optativo", 'N' => "infinitivo", 'P' => "particípio", _ => null });
        Add("Pessoa", p[0] switch { '1' => "1ª", '2' => "2ª", '3' => "3ª", _ => null });
        Add("Caso", p[4] switch { 'N' => "nominativo", 'G' => "genitivo", 'D' => "dativo", 'A' => "acusativo", 'V' => "vocativo", _ => null });
        Add("Número", p[5] switch { 'S' => "singular", 'P' => "plural", _ => null });
        Add("Gênero", p[6] switch { 'M' => "masculino", 'F' => "feminino", 'N' => "neutro", _ => null });
        Add("Grau", p[7] switch { 'C' => "comparativo", 'S' => "superlativo", _ => null });
        return o;
    }
}
