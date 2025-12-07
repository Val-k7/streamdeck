import { exec } from "child_process";
import os from "os";
import { promisify } from "util";
import { getLogger } from '../utils/Logger.js';

const logger = getLogger();

const execAsync = promisify(exec);
const platform = os.platform();

// Import des fonctions Windows avancées si disponible
let audioWindows = null;
if (platform === "win32") {
  try {
    const audioWindowsModule = await import("./audio-windows.js");
    audioWindows = audioWindowsModule;
  } catch (e) {
    logger.warn(
      "[audio] Windows audio module not available, using fallback",
      { error: e.message }
    );
  }
}

async function setVolumeWindows(device, volumePercent) {
  const volume = Math.round(volumePercent * 655.35);
  const script = `
    $device = "${device || "default"}"
    $volume = ${volume}
    $audio = New-Object -ComObject Shell.Application
    $audio.ShellExecute("sndvol.exe", "-f $volume", "", "open", 0)
  `;
  await execAsync(`powershell -Command "${script}"`);
}

async function setVolumeMacOS(device, volumePercent) {
  const volume = Math.round(volumePercent);
  const deviceArg = device ? `-d "${device}"` : "";
  await execAsync(
    `osascript -e "set volume output volume ${volume} ${deviceArg}"`
  );
}

async function setVolumeLinux(device, volumePercent) {
  const volume = Math.round(volumePercent);
  const deviceArg = device ? `-D ${device}` : "";
  await execAsync(`pactl set-sink-volume ${deviceArg} ${volume}%`);
}

async function muteWindows(device, mute) {
  const muteValue = mute ? 1 : 0;
  const script = `
    $device = "${device || "default"}"
    $mute = ${muteValue}
    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;
      public class Audio {
        [DllImport("user32.dll")]
        public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);
      }
"@
    [Audio]::keybd_event(0xAD, 0, 0, 0)
  `;
  await execAsync(`powershell -Command "${script}"`);
}

async function muteMacOS(device, mute) {
  const muteValue = mute ? 1 : 0;
  const deviceArg = device ? `-d "${device}"` : "";
  await execAsync(
    `osascript -e "set volume output muted ${muteValue} ${deviceArg}"`
  );
}

async function muteLinux(device, mute) {
  const muteValue = mute ? 1 : 0;
  const deviceArg = device ? `-D ${device}` : "";
  await execAsync(`pactl set-sink-mute ${deviceArg} ${muteValue}`);
}

export async function handleAudio(payload) {
  if (typeof payload !== "string" && typeof payload !== "object") {
    throw new Error(
      "Audio payload must be a string (device name) or object (action with params)"
    );
  }

  let action = "SET_VOLUME";
  let device = null;
  let deviceType = "output";
  let volume = null;
  let mute = null;
  let application = null;
  let balance = null;

  if (typeof payload === "string") {
    device = payload;
  } else {
    action = payload.action || payload.type || "SET_VOLUME";
    device = payload.device || payload.deviceName || null;
    deviceType = payload.deviceType || "output";
    volume =
      payload.volume !== undefined ? payload.volume : payload.volumePercent;
    mute = payload.mute !== undefined ? payload.mute : null;
    application = payload.application || payload.appName || null;
    balance = payload.balance !== undefined ? payload.balance : null;
  }

  try {
    // Actions Windows avancées
    if (platform === "win32" && audioWindows) {
      switch (action.toUpperCase()) {
        case "LIST_DEVICES":
          return await audioWindows.listAudioDevices();

        case "SET_DEFAULT_DEVICE":
          if (!device)
            throw new Error("Device ID required for SET_DEFAULT_DEVICE");
          return await audioWindows.setDefaultAudioDevice(device, deviceType);

        case "SET_DEVICE_VOLUME":
          if (volume === null || volume === undefined) {
            throw new Error("Volume required for SET_DEVICE_VOLUME");
          }
          const volumePercent =
            typeof volume === "number" ? volume : parseFloat(volume);
          if (
            isNaN(volumePercent) ||
            volumePercent < 0 ||
            volumePercent > 100
          ) {
            throw new Error("Volume must be a number between 0 and 100");
          }
          try {
            return await audioWindows.setDeviceVolume(
              device || "default",
              volumePercent
            );
          } catch (error) {
            // Vérifier si l'erreur indique que le périphérique n'existe pas
            if (error.message && (
              error.message.includes("not found") ||
              error.message.includes("does not exist") ||
              error.message.includes("Device not found")
            )) {
              throw new Error(`Audio device "${device || "default"}" not found`);
            }
            throw error;
          }

        case "SET_APPLICATION_VOLUME":
          if (!application) throw new Error("Application name required");
          if (volume === null || volume === undefined) {
            throw new Error("Volume required for SET_APPLICATION_VOLUME");
          }
          const appVolume =
            typeof volume === "number" ? volume : parseFloat(volume);
          if (isNaN(appVolume) || appVolume < 0 || appVolume > 100) {
            throw new Error("Volume must be a number between 0 and 100");
          }
          try {
            return await audioWindows.setApplicationVolume(
              application,
              appVolume
            );
          } catch (error) {
            // Vérifier si l'erreur indique que l'application n'existe pas
            if (error.message && (
              error.message.includes("not found") ||
              error.message.includes("does not exist") ||
              error.message.includes("No audio session")
            )) {
              throw new Error(`Application "${application}" not found or has no active audio session`);
            }
            throw error;
          }

        case "SET_DEVICE_MUTE":
          if (mute === null || mute === undefined) {
            throw new Error("Mute value required (true/false)");
          }
          return await audioWindows.setDeviceMute(
            device || "default",
            mute === true || mute === "true"
          );

        case "SET_APPLICATION_MUTE":
          if (!application) throw new Error("Application name required");
          if (mute === null || mute === undefined) {
            throw new Error("Mute value required (true/false)");
          }
          return await audioWindows.setApplicationMute(
            application,
            mute === true || mute === "true"
          );

        case "GET_DEVICE_MUTE":
          return await audioWindows.getDeviceMute(device || "default");

        case "GET_AUDIO_STATE":
          return await audioWindows.getAudioState();

        case "LIST_APPLICATIONS":
        case "LIST_AUDIO_APPLICATIONS":
          return await audioWindows.listAudioApplications();

        case "SET_BALANCE":
          if (!audioWindows) {
            throw new Error("Audio balance is only supported on Windows");
          }
          const balance = payload?.balance ?? 0;
          if (isNaN(balance) || balance < -100 || balance > 100) {
            throw new Error("Balance must be between -100 and 100");
          }
          return await audioWindows.setAudioBalance(
            device || "default",
            balance
          );
      }
    }

    // Actions de base (compatibilité avec l'ancien code)
    if (
      action.toUpperCase() === "SET_VOLUME" &&
      volume !== null &&
      volume !== undefined
    ) {
      const volumePercent =
        typeof volume === "number" ? volume : parseFloat(volume);
      if (isNaN(volumePercent) || volumePercent < 0 || volumePercent > 100) {
        throw new Error("Volume must be a number between 0 and 100");
      }

      if (platform === "win32") {
        // Utiliser la nouvelle fonction si disponible
        if (audioWindows && device) {
          return await audioWindows.setDeviceVolume(device, volumePercent);
        }
        await setVolumeWindows(device, volumePercent);
      } else if (platform === "darwin") {
        await setVolumeMacOS(device, volumePercent);
      } else if (platform === "linux") {
        await setVolumeLinux(device, volumePercent);
      } else {
        throw new Error(`Unsupported platform: ${platform}`);
      }
    } else if (action.toUpperCase() === "MUTE" || mute === true) {
      if (platform === "win32") {
        if (audioWindows && device) {
          return await audioWindows.setDeviceMute(device, true);
        }
        await muteWindows(device, true);
      } else if (platform === "darwin") {
        await muteMacOS(device, true);
      } else if (platform === "linux") {
        await muteLinux(device, true);
      } else {
        throw new Error(`Unsupported platform: ${platform}`);
      }
    } else if (action.toUpperCase() === "UNMUTE" || mute === false) {
      if (platform === "win32") {
        if (audioWindows && device) {
          return await audioWindows.setDeviceMute(device, false);
        }
        await muteWindows(device, false);
      } else if (platform === "darwin") {
        await muteMacOS(device, false);
      } else if (platform === "linux") {
        await muteLinux(device, false);
      } else {
        throw new Error(`Unsupported platform: ${platform}`);
      }
    } else {
      throw new Error(`Unknown audio action: ${action}`);
    }

    return { success: true };
  } catch (error) {
    logger.error("[audio] error", { error: error.message, stack: error.stack });
    throw error;
  }
}
