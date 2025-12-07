import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'

const execAsync = promisify(exec)

/**
 * Gestion des processus Windows
 */

/**
 * Liste tous les processus en cours d'exécution
 */
export async function listProcessesWindows() {
  if (os.platform() !== 'win32') {
    throw new Error('listProcessesWindows is only supported on Windows')
  }

  const script = `
    Get-Process | Select-Object Id, ProcessName, CPU, WorkingSet, Path |
    ConvertTo-Json -Compress
  `
  const command = `powershell -Command "${script.replace(/"/g, '`"')}"`
  const { stdout } = await execAsync(command)
  return JSON.parse(stdout)
}

/**
 * Lance un processus Windows
 */
export async function startProcessWindows(command, args = [], options = {}) {
  if (os.platform() !== 'win32') {
    throw new Error('startProcessWindows is only supported on Windows')
  }

  const { workingDirectory, wait = false, windowStyle = 'Normal' } = options

  const escapedCommand = command.replace(/"/g, '`"')
  const escapedArgs = args.map(arg => `"${arg.replace(/"/g, '`"')}"`).join(' ')
  const cdCommand = workingDirectory ? `Set-Location "${workingDirectory}"; ` : ''

  const script = `
    ${cdCommand}
    $processInfo = New-Object System.Diagnostics.ProcessStartInfo
    $processInfo.FileName = "${escapedCommand}"
    $processInfo.Arguments = "${escapedArgs}"
    $processInfo.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::${windowStyle}
    $processInfo.UseShellExecute = $true
    $process = [System.Diagnostics.Process]::Start($processInfo)
    if (${wait}) {
      $process.WaitForExit()
      @{ Id = $process.Id; ExitCode = $process.ExitCode } | ConvertTo-Json -Compress
    } else {
      @{ Id = $process.Id; Status = "Started" } | ConvertTo-Json -Compress
    }
  `

  const powerShellCommand = `powershell -Command "${script.replace(/"/g, '`"')}"`
  const { stdout } = await execAsync(powerShellCommand)
  return JSON.parse(stdout)
}

/**
 * Arrête un processus Windows par ID
 */
export async function stopProcessWindows(processId) {
  if (os.platform() !== 'win32') {
    throw new Error('stopProcessWindows is only supported on Windows')
  }

  const script = `
    $process = Get-Process -Id ${processId} -ErrorAction SilentlyContinue
    if ($process) {
      $process.Kill()
      @{ Success = $true; Message = "Process ${processId} terminated" } | ConvertTo-Json -Compress
    } else {
      @{ Success = $false; Message = "Process ${processId} not found" } | ConvertTo-Json -Compress
    }
  `

  const command = `powershell -Command "${script.replace(/"/g, '`"')}"`
  const { stdout } = await execAsync(command)
  return JSON.parse(stdout)
}

/**
 * Arrête un processus Windows par nom
 */
export async function stopProcessByNameWindows(processName, force = false) {
  if (os.platform() !== 'win32') {
    throw new Error('stopProcessByNameWindows is only supported on Windows')
  }

  const script = `
    $processes = Get-Process -Name "${processName}" -ErrorAction SilentlyContinue
    if ($processes) {
      $processes | ForEach-Object {
        if (${force}) {
          $_.Kill()
        } else {
          $_.CloseMainWindow()
        }
      }
      Start-Sleep -Seconds 2
      $remaining = Get-Process -Name "${processName}" -ErrorAction SilentlyContinue
      if ($remaining -and ${force}) {
        $remaining | ForEach-Object { $_.Kill() }
      }
      @{ Success = $true; Message = "Processes ${processName} terminated" } | ConvertTo-Json -Compress
    } else {
      @{ Success = $false; Message = "Process ${processName} not found" } | ConvertTo-Json -Compress
    }
  `

  const command = `powershell -Command "${script.replace(/"/g, '`"')}"`
  const { stdout } = await execAsync(command)
  return JSON.parse(stdout)
}

/**
 * Obtient des informations sur un processus
 */
export async function getProcessInfoWindows(processId) {
  if (os.platform() !== 'win32') {
    throw new Error('getProcessInfoWindows is only supported on Windows')
  }

  const script = `
    $process = Get-Process -Id ${processId} -ErrorAction SilentlyContinue
    if ($process) {
      @{
        Id = $process.Id
        Name = $process.ProcessName
        CPU = $process.CPU
        Memory = $process.WorkingSet64
        Path = $process.Path
        StartTime = $process.StartTime.ToString()
        Threads = $process.Threads.Count
        Handles = $process.HandleCount
      } | ConvertTo-Json -Compress
    } else {
      @{ Error = "Process ${processId} not found" } | ConvertTo-Json -Compress
    }
  `

  const command = `powershell -Command "${script.replace(/"/g, '`"')}"`
  const { stdout } = await execAsync(command)
  return JSON.parse(stdout)
}

/**
 * Vérifie si un processus est en cours d'exécution
 */
export async function isProcessRunningWindows(processName) {
  if (os.platform() !== 'win32') {
    throw new Error('isProcessRunningWindows is only supported on Windows')
  }

  const script = `
    $process = Get-Process -Name "${processName}" -ErrorAction SilentlyContinue
    @{ Running = ($process -ne $null) } | ConvertTo-Json -Compress
  `

  const command = `powershell -Command "${script.replace(/"/g, '`"')}"`
  const { stdout } = await execAsync(command)
  return JSON.parse(stdout)
}





