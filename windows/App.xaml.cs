using System.Windows;

namespace NTGrego;

public partial class App : Application
{
    static Mutex single;
    public static WidgetWindow Widget { get; private set; }

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        single = new Mutex(true, "NTGrego.Desktop.SingleInstance", out bool first);
        if (!first) { Shutdown(); return; }

        DispatcherUnhandledException += (s, ex) =>
        {
            MessageBox.Show(ex.Exception.Message, "NT Grego", MessageBoxButton.OK, MessageBoxImage.Warning);
            ex.Handled = true;
        };

        Widget = new WidgetWindow();
        Widget.Show();
        if (e.Args.Contains("--ler")) ReaderWindow.Open(Data.Today());
    }

    public static void Quit()
    {
        Widget?.SavePlacement();
        Current.Shutdown();
    }
}
