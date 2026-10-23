// Copyright (c) Pilots r kwl. BSD-2-Clause; see the repository LICENSE.
// Distribution-only Windows GUI wrapper. Never compiled into the RuneLite plugin.
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Web.Script.Serialization;
using System.Windows.Forms;

[assembly: AssemblyTitle("From Scratch Beta")]
[assembly: AssemblyProduct("From Scratch Beta")]
[assembly: AssemblyCompany("Pilots r kwl")]

internal static class Launcher
{
    private const string MainClass = "com.fromscratch.beta.BetaLauncher";
    private static readonly object LogLock = new object();
    private static string log;
    private static long loggedBytes;

    [STAThread]
    private static int Main(string[] args)
    {
        bool verify = args.Length == 1 && args[0] == "--verify-only";
        try
        {
            string local = verify ? Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "verification-data") : Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string logs = Path.Combine(local, "FromScratchBeta", "logs");
            Directory.CreateDirectory(logs);
            var old = Array.FindAll(new DirectoryInfo(logs).GetFiles("launcher-*.log"), delegate(FileInfo f) { return System.Text.RegularExpressions.Regex.IsMatch(f.Name, "^launcher-[0-9]{8}-[0-9]{6}-[0-9]{3}-[0-9]+[.]log$"); });
            Array.Sort(old, delegate(FileInfo a, FileInfo b) { return b.LastWriteTimeUtc.CompareTo(a.LastWriteTimeUtc); });
            for (int i = 4; i < old.Length; i++) old[i].Delete();
            log = Path.Combine(logs, "launcher-" + DateTime.UtcNow.ToString("yyyyMMdd-HHmmss-fff") + "-" + Process.GetCurrentProcess().Id + ".log");
            WriteLog("From Scratch Beta launcher. Logs are private; review/redact before sharing.");
            if (args.Length != 0 && !verify) throw new InvalidOperationException("Open From Scratch Beta from its shortcut without extra arguments.");
            using (var mutex = new Mutex(false, verify ? "FromScratchBeta.NativeLauncher.Verify." + Process.GetCurrentProcess().Id : "FromScratchBeta.NativeLauncher"))
            {
                bool owned;
                try { owned = mutex.WaitOne(0); } catch (AbandonedMutexException) { owned = true; }
                if (!owned) throw new InvalidOperationException("From Scratch Beta is already running. Close its existing window before opening another copy.");
                try { return Run(verify, logs); } finally { mutex.ReleaseMutex(); }
            }
        }
        catch (Exception error)
        {
            WriteLog(error.ToString());
            if (!verify)
            {
                Application.EnableVisualStyles();
                MessageBox.Show("From Scratch Beta could not start.\n\n" + error.Message + "\n\n" +
                    (log == null || !File.Exists(log) ? "A diagnostic log could not be created. Check free disk space and folder permissions." : "Diagnostic log:\n" + log) +
                    "\n\nReinstall a trusted beta if application files are missing. Your challenge saves are kept separately.",
                    "From Scratch Beta could not start", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            return 2;
        }
    }

    private static int Run(bool verify, string logs)
    {
        string root = Path.GetFullPath(AppDomain.CurrentDomain.BaseDirectory);
        var release = ReadJson(Path.Combine(root, "release.json"));
        string relative = Text(release, "payload");
        if (!System.Text.RegularExpressions.Regex.IsMatch(relative, "^versions/[A-Za-z0-9._-]+$")) throw new InvalidOperationException("The application version information is invalid.");
        string payload = SafePath(root, relative);
        string manifest = SafePath(payload, "payload.json");
        if (Hash(manifest) != Text(release, "manifestSha256")) throw new InvalidOperationException("The application file list is damaged. Reinstall this beta.");
        var data = ReadJson(manifest);
        var files = data["files"] as System.Collections.IList;
        if (files == null || files.Count < 10 || files.Count > 3000) throw new InvalidOperationException("The application file list is incomplete.");
        var known = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        foreach (object value in files)
        {
            var file = value as Dictionary<string, object>;
            if (file == null) throw new InvalidOperationException("Invalid application file entry.");
            string name = Text(file, "path");
            if (!known.Add(name)) throw new InvalidOperationException("Duplicate application file entry.");
            if (Hash(SafePath(payload, name)) != Text(file, "sha256")) throw new InvalidOperationException("An application file is missing or damaged: " + name);
        }
        foreach (string jar in Directory.GetFiles(SafePath(payload, "lib"), "*.jar"))
            if (!known.Contains("lib/" + Path.GetFileName(jar))) throw new InvalidOperationException("An unexpected library is present. Reinstall a trusted beta.");
        string java = SafePath(payload, verify ? "runtime/bin/java.exe" : "runtime/bin/javaw.exe");
        string classpath = Path.Combine(payload, "lib", "*");
        var start = new ProcessStartInfo(java);
        start.WorkingDirectory = payload;
        start.UseShellExecute = false;
        start.CreateNoWindow = true;
        start.WindowStyle = ProcessWindowStyle.Hidden;
        start.RedirectStandardOutput = true;
        start.RedirectStandardError = true;
        // Only this child is affected. Never inspect credentials or change system/user settings.
        start.EnvironmentVariables.Remove("JAVA_TOOL_OPTIONS");
        start.EnvironmentVariables.Remove("JDK_JAVA_OPTIONS");
        start.EnvironmentVariables.Remove("_JAVA_OPTIONS");
        start.Arguments = "-ea " + (verify ? "-Duser.home=\"" + Path.Combine(logs, "verify-home") + "\" " : "") +
            "-cp \"" + classpath + "\" " + MainClass + (verify ? " --help" : "");
        WriteLog("Release: " + Text(data, "releaseVersion") + "; private runtime: " + java + "; verification: " + verify);
        using (var process = new Process())
        {
            process.StartInfo = start;
            process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs e) { WriteLog(e.Data); };
            process.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs e) { WriteLog(e.Data); };
            if (!process.Start()) throw new InvalidOperationException("The bundled Java runtime could not start.");
            process.BeginOutputReadLine(); process.BeginErrorReadLine();
            if (verify)
            {
                if (!process.WaitForExit(30000)) { process.Kill(); throw new InvalidOperationException("The application verification timed out."); }
            }
            else { while (!process.WaitForExit(60000)) { } }
            if (process.ExitCode != 0) throw new InvalidOperationException("RuneLite stopped with error code " + process.ExitCode + ". Check the diagnostic log.");
        }
        WriteLog(verify ? "Verification passed. No game client or account was opened." : "Application closed normally.");
        return 0;
    }

    private static Dictionary<string, object> ReadJson(string path)
    {
        if (!File.Exists(path) || new FileInfo(path).Length > 2 * 1024 * 1024) throw new InvalidOperationException("Missing or invalid application metadata: " + Path.GetFileName(path));
        return new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(File.ReadAllText(path));
    }
    private static string Text(Dictionary<string, object> value, string key)
    {
        object found;
        if (!value.TryGetValue(key, out found) || !(found is string)) throw new InvalidOperationException("Missing application information: " + key);
        return (string) found;
    }
    private static string SafePath(string root, string relative)
    {
        if (String.IsNullOrWhiteSpace(relative) || relative.IndexOfAny(new char[] { ':', '\\' }) >= 0 || Path.IsPathRooted(relative)) throw new InvalidOperationException("Unsafe application path.");
        string prefix = Path.GetFullPath(root).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
        string path = Path.GetFullPath(Path.Combine(prefix, relative));
        if (!path.StartsWith(prefix, StringComparison.OrdinalIgnoreCase)) throw new InvalidOperationException("Application path leaves its folder.");
        string walk = prefix.TrimEnd(Path.DirectorySeparatorChar);
        foreach (string part in relative.Split('/'))
        {
            if (part.Length == 0 || part == "." || part == "..") throw new InvalidOperationException("Unsafe application path segment.");
            walk = Path.Combine(walk, part);
            if ((File.Exists(walk) || Directory.Exists(walk)) && (File.GetAttributes(walk) & FileAttributes.ReparsePoint) != 0) throw new InvalidOperationException("Linked application folders are not supported.");
        }
        return path;
    }
    private static string Hash(string path)
    {
        if (!File.Exists(path)) throw new InvalidOperationException("A required application file is missing: " + Path.GetFileName(path));
        using (var sha = SHA256.Create()) using (var stream = File.OpenRead(path)) return BitConverter.ToString(sha.ComputeHash(stream)).Replace("-", "").ToLowerInvariant();
    }
    private static void WriteLog(string text)
    {
        if (log == null || text == null) return;
        lock (LogLock)
        {
            if (loggedBytes > 2 * 1024 * 1024) return;
            try { string line = DateTime.UtcNow.ToString("o") + " " + text + Environment.NewLine; File.AppendAllText(log, line, Encoding.UTF8); loggedBytes += Encoding.UTF8.GetByteCount(line); }
            catch (IOException) { } catch (UnauthorizedAccessException) { }
        }
    }
}
