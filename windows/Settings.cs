using System.IO;
using System.Text.Json;
using Microsoft.Win32;

namespace NTGrego;

/// <summary>Preferências salvas em %APPDATA%\NTGrego\settings.json.</summary>
public sealed class Settings
{
    // Aparência (mesmos padrões do Android)
    public string BgColor { get; set; } = "#1E1A16";
    public int Opacity { get; set; } = 90;          // %
    public string TextColor { get; set; } = "#F5EEDC";
    public string RefColor { get; set; } = "#D4A95A";
    public int FontSize { get; set; } = 19;          // px WPF (~14 pt)

    // Janela do widget
    public double Left { get; set; } = double.NaN;
    public double Top { get; set; } = double.NaN;
    public double Width { get; set; } = 380;
    public double Height { get; set; } = 190;
    public bool Topmost { get; set; }

    // Trecho sorteado manualmente
    public long OverrideDay { get; set; } = -1;
    public int OverrideUnit { get; set; }

    static readonly string Dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "NTGrego");
    static readonly string FilePath = Path.Combine(Dir, "settings.json");

    static Settings current;
    public static Settings Current => current ??= Load();

    static Settings Load()
    {
        try
        {
            if (File.Exists(FilePath))
                return JsonSerializer.Deserialize<Settings>(File.ReadAllText(FilePath)) ?? new Settings();
        }
        catch { /* arquivo corrompido: volta ao padrão */ }
        return new Settings();
    }

    public void Save()
    {
        try
        {
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch { }
    }

    public Settings Clone() => (Settings)MemberwiseClone();

    // ---- Iniciar com o Windows (HKCU\...\Run) ----
    const string RunKey = @"Software\Microsoft\Windows\CurrentVersion\Run";
    const string RunName = "NTGrego";

    public static bool StartWithWindows
    {
        get
        {
            using var k = Registry.CurrentUser.OpenSubKey(RunKey);
            return k?.GetValue(RunName) is string;
        }
        set
        {
            using var k = Registry.CurrentUser.CreateSubKey(RunKey);
            if (value) k.SetValue(RunName, $"\"{Environment.ProcessPath}\"");
            else k.DeleteValue(RunName, false);
        }
    }
}
