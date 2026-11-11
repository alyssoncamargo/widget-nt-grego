using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Threading;

namespace NTGrego;

public partial class WidgetWindow : Window
{
    Passage current;
    DateOnly shownDate;
    readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromMinutes(1) };

    public WidgetWindow()
    {
        InitializeComponent();
        var st = Settings.Current;
        Width = st.Width;
        Height = st.Height;
        if (!double.IsNaN(st.Left) && !double.IsNaN(st.Top) && OnScreen(st.Left, st.Top))
        {
            WindowStartupLocation = WindowStartupLocation.Manual;
            Left = st.Left;
            Top = st.Top;
        }
        else
        {
            WindowStartupLocation = WindowStartupLocation.Manual;
            var wa = SystemParameters.WorkArea;
            Left = wa.Right - Width - 24;
            Top = wa.Top + 24;
        }
        Topmost = st.Topmost;

        ApplyStyle(st);
        Refresh();

        MouseLeftButtonDown += OnMouseDown;
        ContextMenu = BuildMenu();
        LocationChanged += (_, _) => SavePlacementSoon();
        SizeChanged += (_, _) => SavePlacementSoon();

        timer.Tick += (_, _) => { if (DateOnly.FromDateTime(DateTime.Now) != shownDate) Refresh(); };
        timer.Start();
    }

    static bool OnScreen(double x, double y) =>
        x > SystemParameters.VirtualScreenLeft - 50 && y > SystemParameters.VirtualScreenTop - 50 &&
        x < SystemParameters.VirtualScreenLeft + SystemParameters.VirtualScreenWidth - 50 &&
        y < SystemParameters.VirtualScreenTop + SystemParameters.VirtualScreenHeight - 50;

    public void Refresh()
    {
        current = Data.Today();
        shownDate = DateOnly.FromDateTime(DateTime.Now);
        GreekText.Text = current.Text;
        RefText.Text = current.Reference;
    }

    public void ApplyStyle(Settings st)
    {
        var bg = (Color)ColorConverter.ConvertFromString(st.BgColor);
        bg.A = (byte)Math.Round(st.Opacity * 2.55);
        Card.Background = new SolidColorBrush(bg);
        var fg = (Color)ColorConverter.ConvertFromString(st.TextColor);
        GreekText.Foreground = new SolidColorBrush(fg);
        GreekText.FontSize = st.FontSize;
        GreekText.LineHeight = st.FontSize * 1.45;
        RefText.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString(st.RefColor));
        ShuffleBtn.Foreground = new SolidColorBrush(Color.FromArgb(0xB3, fg.R, fg.G, fg.B));
    }

    // Arrastar move o widget; um clique (sem mover) abre o leitor.
    void OnMouseDown(object sender, MouseButtonEventArgs e)
    {
        if (e.OriginalSource is DependencyObject d && FindParent<Button>(d) != null) return;
        if (e.OriginalSource is System.Windows.Controls.Primitives.ResizeGrip) return;
        double x = Left, y = Top;
        try { DragMove(); } catch (InvalidOperationException) { }
        if (Math.Abs(Left - x) < 3 && Math.Abs(Top - y) < 3) ReaderWindow.Open(current);
    }

    static T FindParent<T>(DependencyObject d) where T : DependencyObject
    {
        while (d != null && d is not T) d = d is Visual || d is System.Windows.Media.Media3D.Visual3D ? VisualTreeHelper.GetParent(d) : LogicalTreeHelper.GetParent(d);
        return d as T;
    }

    void Shuffle_Click(object sender, RoutedEventArgs e)
    {
        current = Data.Shuffle();
        GreekText.Text = current.Text;
        RefText.Text = current.Reference;
    }

    ContextMenu BuildMenu()
    {
        var m = new ContextMenu();
        MenuItem Item(string h, Action a) { var i = new MenuItem { Header = h }; i.Click += (_, _) => a(); m.Items.Add(i); return i; }
        Item("Ler o trecho", () => ReaderWindow.Open(current));
        Item("Outro trecho para hoje", () => Shuffle_Click(null, null));
        Item("Voltar ao trecho do dia", () =>
        {
            Settings.Current.OverrideDay = -1;
            Settings.Current.Save();
            Refresh();
        });
        m.Items.Add(new Separator());
        Item("Aparência…", () => new AppearanceWindow { Owner = this }.ShowDialog());
        var top = Item("Sempre visível", () => { });
        top.IsCheckable = true;
        top.IsChecked = Topmost;
        top.Click += (_, _) => { Topmost = top.IsChecked; Settings.Current.Topmost = Topmost; Settings.Current.Save(); };
        var run = Item("Iniciar com o Windows", () => { });
        run.IsCheckable = true;
        m.Opened += (_, _) => run.IsChecked = Settings.StartWithWindows;
        run.Click += (_, _) =>
        {
            try { Settings.StartWithWindows = run.IsChecked; }
            catch (Exception ex) { MessageBox.Show(ex.Message, "NT Grego"); }
        };
        m.Items.Add(new Separator());
        Item("Sair", App.Quit);
        return m;
    }

    DispatcherTimer saveTimer;
    void SavePlacementSoon()
    {
        saveTimer ??= new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
        saveTimer.Stop();
        saveTimer.Tick -= SaveTick;
        saveTimer.Tick += SaveTick;
        saveTimer.Start();
    }
    void SaveTick(object s, EventArgs e) { saveTimer.Stop(); SavePlacement(); }

    public void SavePlacement()
    {
        var st = Settings.Current;
        st.Left = Left; st.Top = Top; st.Width = ActualWidth > 0 ? ActualWidth : Width; st.Height = ActualHeight > 0 ? ActualHeight : Height;
        st.Save();
    }
}
