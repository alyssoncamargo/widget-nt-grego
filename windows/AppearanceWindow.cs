using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Shapes;

namespace NTGrego;

/// <summary>Aparência do widget: fundo, opacidade, cor e tamanho da fonte. Mudanças aparecem ao vivo no widget.</summary>
public sealed class AppearanceWindow : Window
{
    static readonly string[] BgColors = { "#1E1A16", "#000000", "#263238", "#1A2A44", "#2E3B2C", "#4A1C1C", "#FAF6EE", "#FFFFFF", "#E8DCC4", "#DDE7F0" };
    static readonly string[] TextColors = { "#F5EEDC", "#FFFFFF", "#D4A95A", "#B0BEC5", "#1E1A16", "#000000", "#5D4037", "#1A2A44" };

    readonly Settings draft;
    readonly Settings original;
    readonly TextBlock opLabel = new(), fontLabel = new();

    public AppearanceWindow()
    {
        Title = "Aparência do widget";
        Width = 460;
        SizeToContent = SizeToContent.Height;
        ResizeMode = ResizeMode.NoResize;
        WindowStartupLocation = WindowStartupLocation.CenterScreen;
        Background = (Brush)Application.Current.Resources["Paper"];

        original = Settings.Current.Clone();
        draft = Settings.Current.Clone();

        var root = new StackPanel { Margin = new Thickness(24) };
        root.Children.Add(new TextBlock { Text = "Aparência do widget", FontSize = 20, FontWeight = FontWeights.Bold, Margin = new Thickness(0, 0, 0, 8) });
        root.Children.Add(new TextBlock { Text = "As mudanças aparecem no widget enquanto você ajusta.", Foreground = (Brush)Application.Current.Resources["Muted"] });

        root.Children.Add(Label("Cor de fundo"));
        root.Children.Add(Swatches(BgColors, c =>
        {
            bool refDefault = draft.RefColor == DefaultRef(draft.BgColor);
            draft.BgColor = c;
            if (refDefault) draft.RefColor = DefaultRef(c);
            if (IsLight(c) == IsLight(draft.TextColor)) draft.TextColor = IsLight(c) ? "#1E1A16" : "#F5EEDC";
        }));

        root.Children.Add(opLabel);
        root.Children.Add(Slider(0, 100, draft.Opacity, v => draft.Opacity = v));

        root.Children.Add(Label("Cor da fonte"));
        root.Children.Add(Swatches(TextColors, c => draft.TextColor = c));

        root.Children.Add(fontLabel);
        root.Children.Add(Slider(13, 34, draft.FontSize, v => draft.FontSize = v));

        var buttons = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right, Margin = new Thickness(0, 24, 0, 0) };
        var cancel = new Button { Content = "Cancelar", Padding = new Thickness(16, 6, 16, 6), Margin = new Thickness(0, 0, 8, 0), IsCancel = true };
        var save = new Button { Content = "Salvar", Padding = new Thickness(20, 6, 20, 6), IsDefault = true };
        cancel.Click += (_, _) => Close();
        save.Click += (_, _) =>
        {
            var s = Settings.Current;
            s.BgColor = draft.BgColor; s.Opacity = draft.Opacity; s.TextColor = draft.TextColor;
            s.RefColor = draft.RefColor; s.FontSize = draft.FontSize;
            s.Save();
            saved = true;
            Close();
        };
        buttons.Children.Add(cancel);
        buttons.Children.Add(save);
        root.Children.Add(buttons);

        Content = root;
        Closed += (_, _) => { if (!saved) App.Widget?.ApplyStyle(original); };
        Update();
    }

    bool saved;

    void Update()
    {
        opLabel.Text = $"Opacidade do fundo: {draft.Opacity}%";
        fontLabel.Text = $"Tamanho da fonte: {draft.FontSize}";
        opLabel.Margin = fontLabel.Margin = new Thickness(0, 18, 0, 6);
        opLabel.FontSize = fontLabel.FontSize = 14;
        App.Widget?.ApplyStyle(draft);
    }

    static TextBlock Label(string t) => new() { Text = t, FontSize = 14, Margin = new Thickness(0, 18, 0, 6) };

    Slider Slider(int min, int max, int value, Action<int> set)
    {
        var s = new Slider { Minimum = min, Maximum = max, Value = value, IsSnapToTickEnabled = true, TickFrequency = 1 };
        s.ValueChanged += (_, e) => { set((int)e.NewValue); Update(); };
        return s;
    }

    WrapPanel Swatches(string[] colors, Action<string> pick)
    {
        var p = new WrapPanel();
        foreach (var c in colors)
        {
            var dot = new Ellipse
            {
                Width = 32, Height = 32, Margin = new Thickness(0, 0, 10, 8),
                Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString(c)),
                Stroke = new SolidColorBrush(Color.FromArgb(0x55, 0x88, 0x88, 0x88)), StrokeThickness = 1,
                Cursor = System.Windows.Input.Cursors.Hand, ToolTip = c,
            };
            dot.MouseLeftButtonDown += (_, _) => { pick(c); Update(); };
            p.Children.Add(dot);
        }
        return p;
    }

    static bool IsLight(string hex)
    {
        var c = (Color)ColorConverter.ConvertFromString(hex);
        return 0.299 * c.R + 0.587 * c.G + 0.114 * c.B > 150;
    }

    static string DefaultRef(string bg) => IsLight(bg) ? "#8A5A1A" : "#D4A95A";
}
