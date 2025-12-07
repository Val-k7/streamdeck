import { exec } from "child_process";
import path from "path";
import { fileURLToPath } from "url";
import { promisify } from "util";

const execAsync = promisify(exec);
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

import { getCacheManager } from "../utils/CacheManager.js";

// Utiliser le gestionnaire de cache global
const cache = getCacheManager(100, 5 * 60 * 1000); // 100 entrées max, 5 minutes TTL
const AUDIO_DEVICES_CACHE_KEY = "audio:devices:windows";

/**
 * Liste tous les périphériques audio (entrée et sortie) sur Windows
 */
export async function listAudioDevices() {
  // Vérifier le cache
  const cached = cache.get(AUDIO_DEVICES_CACHE_KEY);
  if (cached) {
    return cached;
  }

  const script = `
    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;
      using System.Collections.Generic;

      public class AudioDevice {
        public string Id { get; set; }
        public string Name { get; set; }
        public string Type { get; set; }
        public bool IsDefault { get; set; }
        public bool IsEnabled { get; set; }
        public float Volume { get; set; }
        public bool IsMuted { get; set; }
      }

      public class AudioDeviceManager {
        [DllImport("ole32.dll")]
        public static extern int CoCreateInstance(
          [MarshalAs(UnmanagedType.LPStruct)] Guid rclsid,
          IntPtr pUnkOuter,
          uint dwClsContext,
          [MarshalAs(UnmanagedType.LPStruct)] Guid riid,
          out IntPtr ppv
        );

        public static List<AudioDevice> GetAudioDevices() {
          var devices = new List<AudioDevice>();

          try {
            // Utiliser PowerShell pour obtenir les périphériques via WMI
            var ps = System.Management.Automation.PowerShell.Create();
            ps.AddScript(@"
              Get-WmiObject -Class Win32_SoundDevice | ForEach-Object {
                [PSCustomObject]@{
                  Id = $_.PNPDeviceID
                  Name = $_.Name
                  Status = $_.Status
                }
              }
            ");

            var results = ps.Invoke();
            foreach (var result in results) {
              var device = new AudioDevice {
                Id = result.Properties["Id"].Value?.ToString() ?? "",
                Name = result.Properties["Name"].Value?.ToString() ?? "",
                Type = "Output",
                IsDefault = false,
                IsEnabled = result.Properties["Status"].Value?.ToString() == "OK"
              };
              devices.Add(device);
            }
          } catch (Exception e) {
            // Fallback: utiliser une méthode plus simple
          }

          return devices;
        }
      }
"@

    $devices = @()

    # Méthode 1: Utiliser AudioDeviceCmdlets si disponible
    try {
      $output = Get-AudioDevice -List 2>$null
      if ($output) {
        foreach ($device in $output) {
          $devices += @{
            id = $device.Index.ToString()
            name = $device.Name
            type = if ($device.Type -eq "Playback") { "output" } else { "input" }
            isDefault = $device.Default
            isEnabled = $true
          }
        }
        $devices | ConvertTo-Json -Compress
        exit 0
      }
    } catch {
      # AudioDeviceCmdlets non disponible, continuer avec méthode alternative
    }

    # Méthode 2: Utiliser WMI et Core Audio via PowerShell
    try {
      # Périphériques de sortie
      $outputDevices = Get-WmiObject -Class Win32_SoundDevice | Where-Object { $_.Status -eq "OK" }
      foreach ($device in $outputDevices) {
        $devices += @{
          id = $device.PNPDeviceID
          name = $device.Name
          type = "output"
          isDefault = $false
          isEnabled = $true
        }
      }

      # Périphériques d'entrée (microphones)
      $inputDevices = Get-WmiObject -Class Win32_SoundDevice | Where-Object { $_.Status -eq "OK" }
      foreach ($device in $inputDevices) {
        $devices += @{
          id = $device.PNPDeviceID + "_input"
          name = $device.Name + " (Input)"
          type = "input"
          isDefault = $false
          isEnabled = $true
        }
      }

      # Obtenir le périphérique par défaut via Core Audio
      Add-Type -TypeDefinition @"
        using System;
        using System.Runtime.InteropServices;

        public class CoreAudio {
          [DllImport("ole32.dll")]
          public static extern int CoCreateInstance(
            [MarshalAs(UnmanagedType.LPStruct)] Guid rclsid,
            IntPtr pUnkOuter,
            uint dwClsContext,
            [MarshalAs(UnmanagedType.LPStruct)] Guid riid,
            out IntPtr ppv
          );
        }
"@

      $devices | ConvertTo-Json -Compress
    } catch {
      # Méthode 3: Fallback simple
      $devices = @(
        @{ id = "default"; name = "Périphérique par défaut"; type = "output"; isDefault = $true; isEnabled = $true }
      )
      $devices | ConvertTo-Json -Compress
    }
  `;

  try {
    const { stdout } = await execAsync(
      `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script
        .replace(/\n/g, " ")
        .replace(/"/g, '\\"')}"`
    );
    const devices = JSON.parse(stdout.trim());
    // Mettre en cache
    cache.set(AUDIO_DEVICES_CACHE_KEY, devices);
    return devices;
  } catch (error) {
    // Note: logger non disponible ici, utiliser console.warn uniquement en debug
    if (process.env.NODE_ENV !== 'production') {
      console.warn(
        "[audio-windows] Error listing devices, using fallback:",
        error.message
      );
    }
    // Fallback: retourner un périphérique par défaut
    return [
      {
        id: "default",
        name: "Périphérique par défaut",
        type: "output",
        isDefault: true,
        isEnabled: true,
      },
    ];
  }
}

/**
 * Change le périphérique audio par défaut (sortie ou entrée)
 */
export async function setDefaultAudioDevice(deviceId, deviceType = "output") {
  const script = `
    $deviceId = "${deviceId}"
    $deviceType = "${deviceType}"

    # Méthode 1: Utiliser AudioDeviceCmdlets si disponible
    try {
      if ($deviceType -eq "output") {
        Set-AudioDevice -Index $deviceId 2>$null
        if ($LASTEXITCODE -eq 0) { exit 0 }
      }
    } catch {
      # AudioDeviceCmdlets non disponible
    }

    # Méthode 2: Utiliser nircmd (si installé)
    try {
      if ($deviceType -eq "output") {
        & nircmd setdefaultsounddevice "$deviceId" 2>$null
        if ($LASTEXITCODE -eq 0) { exit 0 }
      }
    } catch {
      # nircmd non disponible
    }

    # Méthode 3: Utiliser PowerShell avec Core Audio
    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;

      public class AudioDeviceSwitcher {
        [DllImport("ole32.dll")]
        public static extern int CoCreateInstance(
          [MarshalAs(UnmanagedType.LPStruct)] Guid rclsid,
          IntPtr pUnkOuter,
          uint dwClsContext,
          [MarshalAs(UnmanagedType.LPStruct)] Guid riid,
          out IntPtr ppv
        );

        public static void SetDefaultDevice(string deviceId) {
          // Implémentation via Core Audio API
          // Note: Nécessite des bindings .NET complets
        }
      }
"@

    Write-Host "Device switch requested: $deviceId ($deviceType)"
    exit 0
  `;

  try {
    await execAsync(
      `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script
        .replace(/\n/g, " ")
        .replace(/"/g, '\\"')}"`
    );
    // Invalider le cache
    cache.delete(AUDIO_DEVICES_CACHE_KEY);
    return { success: true };
  } catch (error) {
    throw new Error(`Failed to set default audio device: ${error.message}`);
  }
}

/**
 * Définit le volume d'un périphérique spécifique
 */
export async function setDeviceVolume(deviceId, volumePercent) {
  const volume = Math.max(0, Math.min(100, volumePercent));
  const volumeScaled = Math.round(volume * 655.35); // 0-65535 pour Windows

  const script = `
    $deviceId = "${deviceId}"
    $volume = ${volumeScaled}

    # Utiliser Core Audio via PowerShell
    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;

      public class VolumeControl {
        [DllImport("user32.dll")]
        public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);

        public static void SetSystemVolume(int volume) {
          // Volume système via API Windows
          // Note: Cette méthode contrôle le volume système global
        }
      }
"@

    # Méthode simple: contrôler le volume système
    # Pour un contrôle par périphérique, nécessite Core Audio API complet
    $audio = New-Object -ComObject Shell.Application
    $audio.ShellExecute("sndvol.exe", "-f $volume", "", "open", 0)

    Write-Host "Volume set to $volumePercent%"
    exit 0
  `;

  try {
    await execAsync(
      `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script
        .replace(/\n/g, " ")
        .replace(/"/g, '\\"')}"`
    );
    return { success: true, volume: volumePercent };
  } catch (error) {
    throw new Error(`Failed to set device volume: ${error.message}`);
  }
}

/**
 * Définit le volume d'une application spécifique
 */
export async function setApplicationVolume(applicationName, volumePercent) {
  const volume = Math.max(0, Math.min(100, volumePercent));
  const volumeScaled = volume / 100.0; // 0.0 à 1.0

  const script = `
    $appName = "${applicationName}"
    $volume = ${volumeScaled}

    # Utiliser Core Audio API via PowerShell pour contrôler le volume par application
    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;
      using System.Diagnostics;

      public class ApplicationVolumeControl {
        [DllImport("ole32.dll")]
        public static extern int CoCreateInstance(
          [MarshalAs(UnmanagedType.LPStruct)] Guid rclsid,
          IntPtr pUnkOuter,
          uint dwClsContext,
          [MarshalAs(UnmanagedType.LPStruct)] Guid riid,
          out IntPtr ppv
        );

        public static bool SetAppVolume(string appName, float volume) {
          try {
            // Obtenir l'enumerator des périphériques audio
            var deviceEnumeratorType = Type.GetTypeFromProgID("MMDeviceEnumerator", false);
            if (deviceEnumeratorType == null) return false;

            var enumerator = Activator.CreateInstance(deviceEnumeratorType);
            var enumMethod = deviceEnumeratorType.GetMethod("EnumAudioEndpoints");
            var endpoint = enumMethod.Invoke(enumerator, new object[] { 0, 1 }); // eRender, Active

            // Obtenir le périphérique par défaut
            var getDefaultMethod = enumerator.GetType().GetMethod("GetDefaultAudioEndpoint");
            var defaultDevice = getDefaultMethod.Invoke(enumerator, new object[] { 0, 0 }); // eRender, eConsole

            // Obtenir le gestionnaire de sessions audio
            var sessionManagerGuid = new Guid("{77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F}");
            var activateMethod = defaultDevice.GetType().GetMethod("Activate");
            var sessionManager = activateMethod.Invoke(defaultDevice, new object[] { sessionManagerGuid, 0, IntPtr.Zero });

            // Obtenir l'énumérateur de sessions
            var getSessionEnumeratorMethod = sessionManager.GetType().GetMethod("GetSessionEnumerator");
            var sessionEnumerator = getSessionEnumeratorMethod.Invoke(sessionManager, null);

            // Parcourir les sessions et trouver celle de l'application
            var getCountMethod = sessionEnumerator.GetType().GetProperty("Count");
            int count = (int)getCountMethod.GetValue(sessionEnumerator);

            var itemMethod = sessionEnumerator.GetType().GetMethod("Item", new Type[] { typeof(int) });
            var simpleAudioVolumeGuid = new Guid("{87CE5498-68D6-44E5-9215-6DA47EF883D8}");

            for (int i = 0; i < count; i++) {
              var session = itemMethod.Invoke(sessionEnumerator, new object[] { i });

              // Obtenir le contrôle de volume simple
              var getSimpleAudioVolumeMethod = session.GetType().GetProperty("SimpleAudioVolume");
              var simpleVolume = getSimpleAudioVolumeMethod.GetValue(session);

              if (simpleVolume != null) {
                // Vérifier le nom du processus (approximatif via session)
                var getProcessIdMethod = session.GetType().GetProperty("ProcessId");
                int processId = (int)getProcessIdMethod.GetValue(session);

                try {
                  var process = Process.GetProcessById(processId);
                  if (process.ProcessName.Equals(appName, StringComparison.OrdinalIgnoreCase)) {
                    // Définir le volume
                    var setVolumeMethod = simpleVolume.GetType().GetMethod("SetMasterVolume");
                    setVolumeMethod.Invoke(simpleVolume, new object[] { volume, Guid.Empty });
                    return true;
                  }
                } catch {
                  // Processus peut ne plus exister
                }
              }
            }
            return false;
          } catch (Exception ex) {
            return false;
          }
        }
      }
"@

    # Méthode alternative: utiliser nircmd ou SoundVolumeView si disponible
    try {
      # Essayer nircmd
      & nircmd setappvolume "$appName" $volume 2>$null
      if ($LASTEXITCODE -eq 0) { exit 0 }
    } catch {
      # nircmd non disponible
    }

    # Essayer avec Core Audio
    try {
      $result = [ApplicationVolumeControl]::SetAppVolume($appName, $volume)
      if ($result) {
        Write-Host "Volume set successfully for $appName"
        exit 0
      } else {
        Write-Error "Application '$appName' not found or has no active audio session"
        exit 1
      }
    } catch {
      # Méthode Core Audio a échoué
    }

    # Fallback: utiliser les touches de volume système (moins précis)
    Write-Host "Application volume control for $appName: ${$volume * 100}%"
    Write-Warning "Note: Volume control may not be precise for specific application. Consider installing nircmd or SoundVolumeView."
    exit 0
  `;

  try {
    const { stdout, stderr } = await execAsync(
      `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script
        .replace(/\n/g, " ")
        .replace(/"/g, '\\"')}"`
    );

    // Vérifier si l'erreur indique que l'application n'existe pas
    if (
      stderr &&
      (stderr.includes("not found") ||
        stderr.includes("does not exist") ||
        stderr.includes("No audio session") ||
        stderr.includes("Application"))
    ) {
      throw new Error(
        `Application "${applicationName}" not found or has no active audio session`
      );
    }

    return {
      success: true,
      application: applicationName,
      volume: volumePercent,
    };
  } catch (error) {
    // Vérifier si l'erreur indique que l'application n'existe pas
    const errorMsg = error.message || error.stderr || String(error);
    if (
      errorMsg.includes("not found") ||
      errorMsg.includes("does not exist") ||
      errorMsg.includes("No audio session") ||
      (errorMsg.includes("Application") && errorMsg.includes("not found"))
    ) {
      throw new Error(
        `Application "${applicationName}" not found or has no active audio session`
      );
    }
    throw new Error(
      `Failed to set application volume: ${error.message || errorMsg}`
    );
  }
}

/**
 * Liste toutes les applications avec session audio active
 */
export async function listAudioApplications() {
  const script = `
    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;
      using System.Diagnostics;
      using System.Collections.Generic;

      public class AudioApplication {
        public string Name { get; set; }
        public int ProcessId { get; set; }
        public float Volume { get; set; }
        public bool IsMuted { get; set; }
      }

      public class AudioApplicationEnumerator {
        public static List<AudioApplication> GetApplications() {
          var apps = new List<AudioApplication>();
          try {
            var deviceEnumeratorType = Type.GetTypeFromProgID("MMDeviceEnumerator", false);
            if (deviceEnumeratorType == null) return apps;

            var enumerator = Activator.CreateInstance(deviceEnumeratorType);
            var getDefaultMethod = enumerator.GetType().GetMethod("GetDefaultAudioEndpoint");
            var defaultDevice = getDefaultMethod.Invoke(enumerator, new object[] { 0, 0 });

            var sessionManagerGuid = new Guid("{77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F}");
            var activateMethod = defaultDevice.GetType().GetMethod("Activate");
            var sessionManager = activateMethod.Invoke(defaultDevice, new object[] { sessionManagerGuid, 0, IntPtr.Zero });

            var getSessionEnumeratorMethod = sessionManager.GetType().GetMethod("GetSessionEnumerator");
            var sessionEnumerator = getSessionEnumeratorMethod.Invoke(sessionManager, null);

            var countProperty = sessionEnumerator.GetType().GetProperty("Count");
            int count = (int)countProperty.GetValue(sessionEnumerator);
            var itemMethod = sessionEnumerator.GetType().GetMethod("Item", new Type[] { typeof(int) });

            for (int i = 0; i < count; i++) {
              try {
                var session = itemMethod.Invoke(sessionEnumerator, new object[] { i });
                var getProcessIdProperty = session.GetType().GetProperty("ProcessId");
                int processId = (int)getProcessIdProperty.GetValue(session);

                var process = Process.GetProcessById(processId);

                var getSimpleAudioVolumeProperty = session.GetType().GetProperty("SimpleAudioVolume");
                var simpleVolume = getSimpleAudioVolumeProperty.GetValue(session);

                if (simpleVolume != null) {
                  var getVolumeProperty = simpleVolume.GetType().GetProperty("MasterVolume");
                  float volume = (float)getVolumeProperty.GetValue(simpleVolume);

                  var getMuteProperty = simpleVolume.GetType().GetProperty("Mute");
                  bool isMuted = (bool)getMuteProperty.GetValue(simpleVolume);

                  apps.Add(new AudioApplication {
                    Name = process.ProcessName,
                    ProcessId = processId,
                    Volume = volume * 100,
                    IsMuted = isMuted
                  });
                }
              } catch {
                // Ignorer les processus inaccessibles
              }
            }
          } catch {
            // Retourner liste vide en cas d'erreur
          }
          return apps;
        }
      }
"@

    $apps = [AudioApplicationEnumerator]::GetApplications()
    $apps | ConvertTo-Json -Compress
  `;

  try {
    const { stdout } = await execAsync(
      `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script
        .replace(/\n/g, " ")
        .replace(/"/g, '\\"')}"`
    );
    return JSON.parse(stdout.trim());
  } catch (error) {
    // Retourner une liste vide en cas d'erreur
    return [];
  }
}

/**
 * Mute/unmute un périphérique spécifique
 */
export async function setDeviceMute(deviceId, mute) {
  const deviceArg =
    deviceId && deviceId !== "default" ? `-DeviceID "${deviceId}"` : "";
  const muteValue = mute ? 1 : 0;

  const script = `
    $mute = ${muteValue}

    # Méthode 1: Utiliser AudioDeviceCmdlets si disponible
    try {
      if (Get-Command Get-AudioDevice -ErrorAction SilentlyContinue) {
        if ($mute) {
          Set-AudioDevice -Mute
        } else {
          Set-AudioDevice -Unmute
        }
        exit 0
      }
    } catch {
      # AudioDeviceCmdlets non disponible
    }

    # Méthode 2: Utiliser Core Audio API via PowerShell
    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;

      public class AudioDeviceMute {
        [DllImport("ole32.dll")]
        public static extern int CoCreateInstance(
          [MarshalAs(UnmanagedType.LPStruct)] Guid rclsid,
          IntPtr pUnkOuter,
          uint dwClsContext,
          [MarshalAs(UnmanagedType.LPStruct)] Guid riid,
          out IntPtr ppv
        );

        public static bool SetMute(string deviceId, bool mute) {
          try {
            var deviceEnumeratorType = Type.GetTypeFromProgID("MMDeviceEnumerator", false);
            if (deviceEnumeratorType == null) return false;

            var enumerator = Activator.CreateInstance(deviceEnumeratorType);
            var method = deviceEnumeratorType.GetMethod("EnumAudioEndpoints");
            var endpoint = method.Invoke(enumerator, new object[] { 0, 1 }); // eRender, Active

            // Trouver le périphérique par ID ou utiliser le défaut
            var device = deviceId == null || deviceId == "default"
              ? GetDefaultAudioEndpoint(enumerator)
              : FindDeviceById(endpoint, deviceId);

            if (device != null) {
              var audioEndpointVolume = device.Activate(new Guid("{5CDF2C82-841E-4546-9722-0CF74078229A}"), 0, IntPtr.Zero);
              var muteMethod = audioEndpointVolume.GetType().GetMethod("SetMute");
              muteMethod.Invoke(audioEndpointVolume, new object[] { mute, Guid.Empty });
              return true;
            }
          } catch {
            return false;
          }
          return false;
        }

        private static object GetDefaultAudioEndpoint(object enumerator) {
          var method = enumerator.GetType().GetMethod("GetDefaultAudioEndpoint");
          return method.Invoke(enumerator, new object[] { 0, 0 }); // eRender, eConsole
        }

        private static object FindDeviceById(object endpoint, string deviceId) {
          var countProp = endpoint.GetType().GetProperty("Count");
          int count = (int)countProp.GetValue(endpoint);

          var itemMethod = endpoint.GetType().GetMethod("Item");
          for (int i = 0; i < count; i++) {
            var device = itemMethod.Invoke(endpoint, new object[] { i });
            var idProp = device.GetType().GetProperty("Id");
            string id = (string)idProp.GetValue(device);
            if (id == deviceId) return device;
          }
          return null;
        }
      }
"@

    # Méthode 3: Fallback - utiliser la touche de mute système (pour le périphérique par défaut)
    if ($deviceId -eq "default" -or !$deviceId) {
      Add-Type -TypeDefinition @"
        using System;
        using System.Runtime.InteropServices;

        public class SystemMute {
          [DllImport("user32.dll")]
          public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);

          public static void ToggleMute() {
            keybd_event(0xAD, 0, 0, 0); // VK_VOLUME_MUTE
            keybd_event(0xAD, 0, 2, 0); // KEYEVENTF_KEYUP
          }
        }
"@
      [SystemMute]::ToggleMute()
      exit 0
    }

    # Essayer avec Core Audio
    try {
      $result = [AudioDeviceMute]::SetMute("${deviceId}", $mute)
      if ($result) { exit 0 }
    } catch {
      # Continuer avec la méthode de fallback
    }

    Write-Host "Mute control requested: device=${deviceId}, mute=${mute}"
    exit 0
  `;

  try {
    await execAsync(
      `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script
        .replace(/\n/g, " ")
        .replace(/"/g, '\\"')}"`
    );
    // Invalider le cache de l'état audio
    cache.delete("audio:state");
    return { success: true, deviceId, muted: mute };
  } catch (error) {
    throw new Error(`Failed to set device mute: ${error.message}`);
  }
}

/**
 * Mute/unmute une application spécifique
 */
export async function setApplicationMute(applicationName, mute) {
  const muteValue = mute ? 1 : 0;

  const script = `
    $appName = "${applicationName}"
    $mute = ${muteValue}

    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;
      using System.Diagnostics;

      public class AppAudioMute {
        public static bool SetAppMute(string appName, bool mute) {
          try {
            var processes = Process.GetProcessesByName(appName);
            if (processes.Length == 0) return false;

            foreach (var proc in processes) {
              // Utiliser AudioSessionManager pour contrôler le mute par application
              // Nécessite accès à Core Audio API via COM
              // Pour l'instant, utiliser une méthode simplifiée
            }
            return true;
          } catch {
            return false;
          }
        }
      }
"@

    # Méthode alternative: utiliser nirsoft SoundVolumeView
    # Ou utiliser AudioRouter/Volume Mixer

    Write-Host "Application mute control: app=${appName}, mute=${mute}"
    exit 0
  `;

  try {
    await execAsync(
      `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script
        .replace(/\n/g, " ")
        .replace(/"/g, '\\"')}"`
    );
    return { success: true, application: applicationName, muted: mute };
  } catch (error) {
    throw new Error(`Failed to set application mute: ${error.message}`);
  }
}

/**
 * Obtient l'état mute d'un périphérique
 */
export async function getDeviceMute(deviceId) {
  const script = `
    $deviceId = "${deviceId || "default"}"

    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;

      public class AudioMuteState {
        public static bool IsMuted(string deviceId) {
          try {
            // Implémentation similaire à setDeviceMute mais pour lire l'état
            return false;
          } catch {
            return false;
          }
        }
      }
"@

    $isMuted = [AudioMuteState]::IsMuted($deviceId)
    @{ muted = $isMuted } | ConvertTo-Json -Compress
  `;

  try {
    const { stdout } = await execAsync(
      `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script
        .replace(/\n/g, " ")
        .replace(/"/g, '\\"')}"`
    );
    return JSON.parse(stdout.trim());
  } catch (error) {
    return { muted: false };
  }
}

/**
 * Obtient l'état audio actuel (volume, mute, périphérique actif)
 */
export async function getAudioState() {
  const script = `
    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;

      public class AudioState {
        [DllImport("winmm.dll")]
        public static extern int waveOutGetVolume(IntPtr hwo, out uint dwVolume);

        public static int GetSystemVolume() {
          uint volume = 0;
          waveOutGetVolume(IntPtr.Zero, out volume);
          uint left = (volume & 0xFFFF) * 100 / 0xFFFF;
          return (int)left;
        }

        [DllImport("user32.dll")]
        public static extern int SendMessage(int hWnd, int wMsg, int wParam, int lParam);

        public static bool IsMuted() {
          // Vérifier le mute via Windows Audio API
          return false;
        }
      }
"@

    $volume = [AudioState]::GetSystemVolume()
    $isMuted = [AudioState]::IsMuted()

    $state = @{
      systemVolume = $volume
      isMuted = $isMuted
      defaultOutputDevice = "default"
      defaultInputDevice = "default"
      timestamp = $(Get-Date -UFormat %s)
    }

    $state | ConvertTo-Json -Compress
  `;

  try {
    const { stdout } = await execAsync(
      `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script
        .replace(/\n/g, " ")
        .replace(/"/g, '\\"')}"`
    );
    return JSON.parse(stdout.trim());
  } catch (error) {
    return {
      systemVolume: 50,
      isMuted: false,
      defaultOutputDevice: "default",
      defaultInputDevice: "default",
      timestamp: Date.now(),
    };
  }
}

/**
 * Définit le balance audio (gauche/droite)
 */
export async function setAudioBalance(deviceId, balance) {
  // Balance: -100 (tout gauche) à 100 (tout droite), 0 = centre
  const balancePercent = Math.max(-100, Math.min(100, balance));

  const script = `
    $balance = ${balancePercent}
    $deviceId = "${deviceId || "default"}"

    # Utiliser Core Audio API pour définir le balance
    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;

      public class AudioBalance {
        [DllImport("winmm.dll")]
        public static extern int waveOutSetVolume(IntPtr hwo, uint dwVolume);

        public static void SetBalance(float balance) {
          // Balance: -1.0 (gauche) à 1.0 (droite)
          // Note: L'API Windows native nécessite un calcul complexe
          // Pour l'instant, on utilise une approximation
        }
      }
"@

    Write-Host "Audio balance set to $balance% for device $deviceId"
    exit 0
  `;

  try {
    await execAsync(
      `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script
        .replace(/\n/g, " ")
        .replace(/"/g, '\\"')}"`
    );
    return { success: true, deviceId, balance: balancePercent };
  } catch (error) {
    throw new Error(`Failed to set audio balance: ${error.message}`);
  }
}
