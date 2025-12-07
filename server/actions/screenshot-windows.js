import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const execAsync = promisify(exec)
const platform = os.platform()
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * Capture d'écran Windows
 */

/**
 * Capture l'écran complet
 */
export async function captureFullScreen(outputPath) {
  if (platform !== 'win32') {
    throw new Error('Screenshot is only supported on Windows')
  }

  try {
    const defaultPath = path.join(os.tmpdir(), `screenshot_${Date.now()}.png`)
    const finalPath = outputPath || defaultPath
    const finalPathEscaped = finalPath.replace(/\\/g, '/')

    // Utiliser PowerShell avec .NET pour capturer l'écran
    // Séparer les commandes avec point-virgule pour éviter les problèmes de newline
    const script = `Add-Type -AssemblyName System.Drawing; Add-Type -AssemblyName System.Windows.Forms; $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds; $bitmap = New-Object System.Drawing.Bitmap($bounds.Width, $bounds.Height); $graphics = [System.Drawing.Graphics]::FromImage($bitmap); $graphics.CopyFromScreen($bounds.Location, (New-Object System.Drawing.Point(0,0)), $bounds.Size); $bitmap.Save('${finalPathEscaped}', [System.Drawing.Imaging.ImageFormat]::Png); $graphics.Dispose(); $bitmap.Dispose(); Write-Output '${finalPath}'`

    const { stdout } = await execAsync(`powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/"/g, '\\"')}"`)
    return { success: true, path: stdout.trim() || finalPath }
  } catch (error) {
    throw new Error(`Failed to capture screen: ${error.message}`)
  }
}

/**
 * Capture une fenêtre spécifique
 */
export async function captureWindow(windowTitle, outputPath) {
  if (platform !== 'win32') {
    throw new Error('Window screenshot is only supported on Windows')
  }

  try {
    const defaultPath = path.join(os.tmpdir(), `screenshot_window_${Date.now()}.png`)
    const finalPath = outputPath || defaultPath
    const finalPathEscaped = finalPath.replace(/\\/g, '/')

    // Créer le code C# dans un fichier temporaire
    const csharpCode = `
using System;
using System.Runtime.InteropServices;
using System.Drawing;
using System.Drawing.Imaging;
using System.Diagnostics;

public class WindowCapture {
  [DllImport("user32.dll")]
  public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

  [DllImport("user32.dll")]
  public static extern bool IsWindowVisible(IntPtr hWnd);

  [StructLayout(LayoutKind.Sequential)]
  public struct RECT {
    public int Left;
    public int Top;
    public int Right;
    public int Bottom;
  }

  public static Bitmap CaptureByTitle(string windowTitle) {
    IntPtr hWnd = IntPtr.Zero;
    Process[] processes = Process.GetProcesses();
    foreach (var proc in processes) {
      if (proc.MainWindowTitle.Contains(windowTitle) && IsWindowVisible(proc.MainWindowHandle)) {
        hWnd = proc.MainWindowHandle;
        break;
      }
    }
    if (hWnd == IntPtr.Zero) {
      throw new Exception("Window not found: " + windowTitle);
    }
    RECT rect;
    GetWindowRect(hWnd, out rect);
    int width = rect.Right - rect.Left;
    int height = rect.Bottom - rect.Top;
    Bitmap bitmap = new Bitmap(width, height);
    Graphics graphics = Graphics.FromImage(bitmap);
    graphics.CopyFromScreen(rect.Left, rect.Top, 0, 0, new Size(width, height));
    graphics.Dispose();
    return bitmap;
  }
}
`.trim()

    const tempFile = path.join(os.tmpdir(), `wincapture_${Date.now()}.cs`)
    fs.writeFileSync(tempFile, csharpCode)

    const windowTitleEscaped = windowTitle.replace(/'/g, "''")
    const script = `
Add-Type -AssemblyName System.Drawing;
Add-Type -AssemblyName System.Windows.Forms;
$code = Get-Content -Path '${tempFile.replace(/\\/g, '\\\\')}' -Raw;
Add-Type -TypeDefinition $code -ReferencedAssemblies System.Drawing,System.Windows.Forms -Language CSharp;
try {
  $bitmap = [WindowCapture]::CaptureByTitle('${windowTitleEscaped}');
  $bitmap.Save('${finalPathEscaped}', [System.Drawing.Imaging.ImageFormat]::Png);
  $bitmap.Dispose();
  Write-Output '${finalPath}';
} catch {
  $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds;
  $bitmap = New-Object System.Drawing.Bitmap($bounds.Width, $bounds.Height);
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap);
  $graphics.CopyFromScreen($bounds.Location, (New-Object System.Drawing.Point(0,0)), $bounds.Size);
  $bitmap.Save('${finalPathEscaped}', [System.Drawing.Imaging.ImageFormat]::Png);
  $graphics.Dispose();
  $bitmap.Dispose();
  Write-Output '${finalPath}';
}
`.replace(/\n/g, ' ')

    const { stdout } = await execAsync(`powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/"/g, '\\"')}"`)
    
    try { fs.unlinkSync(tempFile) } catch {}
    
    return { success: true, path: stdout.trim() || finalPath, windowTitle }
  } catch (error) {
    throw new Error(`Failed to capture window: ${error.message}`)
  }
}



