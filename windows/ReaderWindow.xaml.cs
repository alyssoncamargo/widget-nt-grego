using System.Diagnostics;
using System.Windows;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;

namespace NTGrego;

public partial class ReaderWindow : Window
{
    static ReaderWindow instance;
    int start, end;
    Run selected;
    static readonly Brush HighlightBrush = new SolidColorBrush(Color.FromArgb(0x40, 0xA0, 0x70, 0x2A));
    static readonly Brush AccentBrush = new SolidColorBrush(Color.FromRgb(0xA0, 0x70, 0x2A));

    public static void Open(Passage p)
    {
        if (instance == null || !instance.IsLoaded)
        {
            instance = new ReaderWindow();
            instance.Closed += (_, _) => instance = null;
            instance.Show();
        }
        instance.Show(p.First.Index, p.Last.Index);
        if (instance.WindowState == WindowState.Minimized) instance.WindowState = WindowState.Normal;
        instance.Activate();
    }

    public ReaderWindow()
    {
        InitializeComponent();
        KeyDown += (_, e) =>
        {
            if (e.Key == Key.Left) Prev_Click(null, null);
            else if (e.Key == Key.Right) Next_Click(null, null);
            else if (e.Key == Key.Home) Today_Click(null, null);
            else if (e.Key == Key.Escape) Close();
        };
        ClearPanel();
    }

    void Show(int s, int e)
    {
        var p = Data.Get(s, e);
        start = p.First.Index;
        end = p.Last.Index;
        RefText.Text = p.Reference;
        Title = $"NT Grego · {p.Reference}";
        BuildGreek(p.Multi);
        BuildTb(p.Multi);
        ClearPanel();
    }

    void BuildGreek(bool multi)
    {
        GreekText.Inlines.Clear();
        selected = null;
        for (int vi = start; vi <= end; vi++)
        {
            if (multi) GreekText.Inlines.Add(VerseNumber(Data.Verses[vi].Number, 15));
            foreach (var w in Data.Words(vi))
            {
                var (a, b) = LetterRange(w.Text);
                if (a > 0) GreekText.Inlines.Add(new Run(w.Text[..a]));
                if (b > a)
                {
                    var run = new Run(w.Text[a..b]) { Cursor = Cursors.Hand };
                    var word = w;
                    run.MouseLeftButtonDown += (_, ev) => { Select(run, word); ev.Handled = true; };
                    GreekText.Inlines.Add(run);
                }
                if (b < w.Text.Length) GreekText.Inlines.Add(new Run(w.Text[b..]));
                GreekText.Inlines.Add(new Run(" "));
            }
        }
    }

    void BuildTb(bool multi)
    {
        TbText.Inlines.Clear();
        for (int vi = start; vi <= end; vi++)
        {
            var t = Data.Tb(vi);
            if (t.Length == 0) continue;
            if (multi) TbText.Inlines.Add(VerseNumber(Data.Verses[vi].Number, 11));
            TbText.Inlines.Add(new Run(t + " "));
        }
        if (TbText.Inlines.Count == 0) TbText.Inlines.Add(new Run("(versículo ausente na TB)") { FontStyle = FontStyles.Italic });
    }

    static Run VerseNumber(int n, double size) => new Run(n + " ")
    {
        BaselineAlignment = BaselineAlignment.Superscript,
        FontSize = size,
        Foreground = AccentBrush,
    };

    static (int, int) LetterRange(string t)
    {
        int a = 0, b = t.Length;
        while (a < b && !char.IsLetter(t[a])) a++;
        while (b > a && !char.IsLetter(t[b - 1])) b--;
        return (a, b);
    }

    void Select(Run run, Word w)
    {
        if (selected != null) selected.Background = null;
        selected = run;
        run.Background = HighlightBrush;
        PWord.Text = run.Text;
        PLemma.Text = "lema: " + w.Lemma;
        PGloss.Text = Data.Gloss(w.Lemma) ?? "(sem glosa no léxico)";
        PPos.Text = Morph.Pos(w.Pos);
        PDetails.Text = string.Join("\n", Morph.Details(w.Parse).Select(d => $"{d.Label}: {d.Value}"));
    }

    void ClearPanel()
    {
        PWord.Text = "";
        PLemma.Text = "";
        PGloss.Text = "";
        PPos.Text = "";
        PDetails.Text = "Clique numa palavra do texto grego para ver lema, glosa e análise morfológica.";
    }

    void Prev_Click(object s, RoutedEventArgs e) => Show(start - 1, start - 1);
    void Next_Click(object s, RoutedEventArgs e) => Show(end + 1, end + 1);
    void Today_Click(object s, RoutedEventArgs e) { var p = Data.Today(); Show(p.First.Index, p.Last.Index); }

    void Web_Click(object s, RoutedEventArgs e)
    {
        var p = Data.Get(start, end);
        var url = $"https://www.bible.com/pt/bible/277/{p.Usfm}.TB";
        try { Process.Start(new ProcessStartInfo(url) { UseShellExecute = true }); } catch { }
    }
}
