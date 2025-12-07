import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'
import fs from 'fs'
import path from 'path'

const execAsync = promisify(exec)

/**
 * Opérations sur fichiers Windows
 */

/**
 * Ouvre un fichier avec l'application par défaut
 */
export async function openFileWindows(filePath) {
  if (os.platform() !== 'win32') {
    throw new Error('openFileWindows is only supported on Windows')
  }

  // Normaliser le chemin
  const normalizedPath = path.resolve(filePath)

  if (!fs.existsSync(normalizedPath)) {
    throw new Error(`File not found: ${normalizedPath}`)
  }

  const script = `
    $filePath = "${normalizedPath.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"
    Start-Process $filePath
  `

  try {
    await execAsync(`powershell -Command "${script.replace(/"/g, '`"')}"`)
    return { success: true, filePath: normalizedPath }
  } catch (error) {
    throw new Error(`Failed to open file: ${error.message}`)
  }
}

/**
 * Crée un nouveau fichier
 */
export async function createFileWindows(filePath, content = '') {
  if (os.platform() !== 'win32') {
    throw new Error('createFileWindows is only supported on Windows')
  }

  const normalizedPath = path.resolve(filePath)
  const dir = path.dirname(normalizedPath)

  // Créer le répertoire si nécessaire
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true })
  }

  try {
    fs.writeFileSync(normalizedPath, content, 'utf-8')
    return { success: true, filePath: normalizedPath }
  } catch (error) {
    throw new Error(`Failed to create file: ${error.message}`)
  }
}

/**
 * Supprime un fichier
 */
export async function deleteFileWindows(filePath) {
  if (os.platform() !== 'win32') {
    throw new Error('deleteFileWindows is only supported on Windows')
  }

  const normalizedPath = path.resolve(filePath)

  if (!fs.existsSync(normalizedPath)) {
    throw new Error(`File not found: ${normalizedPath}`)
  }

  try {
    fs.unlinkSync(normalizedPath)
    return { success: true, filePath: normalizedPath }
  } catch (error) {
    throw new Error(`Failed to delete file: ${error.message}`)
  }
}

/**
 * Liste les fichiers d'un répertoire
 */
export async function listFilesWindows(directoryPath, options = {}) {
  if (os.platform() !== 'win32') {
    throw new Error('listFilesWindows is only supported on Windows')
  }

  const normalizedPath = path.resolve(directoryPath)
  const { recursive = false, pattern = '*', includeDirectories = true } = options

  if (!fs.existsSync(normalizedPath)) {
    throw new Error(`Directory not found: ${normalizedPath}`)
  }

  if (!fs.statSync(normalizedPath).isDirectory()) {
    throw new Error(`Path is not a directory: ${normalizedPath}`)
  }

  const script = `
    $dir = "${normalizedPath.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"
    $recursive = ${recursive}
    $pattern = "${pattern}"
    $includeDirs = ${includeDirectories}

    $items = if ($recursive) {
      Get-ChildItem -Path $dir -Recurse -Filter $pattern
    } else {
      Get-ChildItem -Path $dir -Filter $pattern
    }

    $result = $items | Where-Object {
      if ($includeDirs) { $true }
      else { -not $_.PSIsContainer }
    } | ForEach-Object {
      @{
        name = $_.Name
        path = $_.FullName
        isDirectory = $_.PSIsContainer
        size = if ($_.PSIsContainer) { 0 } else { $_.Length }
        lastModified = $_.LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss")
        extension = if ($_.PSIsContainer) { "" } else { $_.Extension }
      }
    }

    $result | ConvertTo-Json -Compress
  `

  try {
    const { stdout } = await execAsync(`powershell -Command "${script.replace(/"/g, '`"')}"`)
    return JSON.parse(stdout.trim())
  } catch (error) {
    throw new Error(`Failed to list files: ${error.message}`)
  }
}

/**
 * Lit le contenu d'un fichier
 */
export async function readFileWindows(filePath, encoding = 'utf-8') {
  if (os.platform() !== 'win32') {
    throw new Error('readFileWindows is only supported on Windows')
  }

  const normalizedPath = path.resolve(filePath)

  if (!fs.existsSync(normalizedPath)) {
    throw new Error(`File not found: ${normalizedPath}`)
  }

  try {
    const content = fs.readFileSync(normalizedPath, encoding)
    return { success: true, content, filePath: normalizedPath }
  } catch (error) {
    throw new Error(`Failed to read file: ${error.message}`)
  }
}

/**
 * Écrit du contenu dans un fichier
 */
export async function writeFileWindows(filePath, content, encoding = 'utf-8') {
  if (os.platform() !== 'win32') {
    throw new Error('writeFileWindows is only supported on Windows')
  }

  const normalizedPath = path.resolve(filePath)
  const dir = path.dirname(normalizedPath)

  // Créer le répertoire si nécessaire
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true })
  }

  try {
    fs.writeFileSync(normalizedPath, content, encoding)
    return { success: true, filePath: normalizedPath }
  } catch (error) {
    throw new Error(`Failed to write file: ${error.message}`)
  }
}

/**
 * Vérifie si un fichier existe
 */
export async function fileExistsWindows(filePath) {
  if (os.platform() !== 'win32') {
    throw new Error('fileExistsWindows is only supported on Windows')
  }

  const normalizedPath = path.resolve(filePath)
  return { exists: fs.existsSync(normalizedPath), filePath: normalizedPath }
}

/**
 * Obtient les informations sur un fichier
 */
export async function getFileInfoWindows(filePath) {
  if (os.platform() !== 'win32') {
    throw new Error('getFileInfoWindows is only supported on Windows')
  }

  const normalizedPath = path.resolve(filePath)

  if (!fs.existsSync(normalizedPath)) {
    throw new Error(`File not found: ${normalizedPath}`)
  }

  try {
    const stats = fs.statSync(normalizedPath)
    return {
      name: path.basename(normalizedPath),
      path: normalizedPath,
      size: stats.size,
      isDirectory: stats.isDirectory(),
      isFile: stats.isFile(),
      created: stats.birthtime.toISOString(),
      modified: stats.mtime.toISOString(),
      accessed: stats.atime.toISOString(),
      extension: stats.isFile() ? path.extname(normalizedPath) : '',
    }
  } catch (error) {
    throw new Error(`Failed to get file info: ${error.message}`)
  }
}

/**
 * Crée un répertoire
 */
export async function createDirectoryWindows(directoryPath) {
  if (os.platform() !== 'win32') {
    throw new Error('createDirectoryWindows is only supported on Windows')
  }

  const normalizedPath = path.resolve(directoryPath)

  try {
    if (!fs.existsSync(normalizedPath)) {
      fs.mkdirSync(normalizedPath, { recursive: true })
    }
    return { success: true, directoryPath: normalizedPath }
  } catch (error) {
    throw new Error(`Failed to create directory: ${error.message}`)
  }
}

/**
 * Supprime un répertoire
 */
export async function deleteDirectoryWindows(directoryPath, recursive = false) {
  if (os.platform() !== 'win32') {
    throw new Error('deleteDirectoryWindows is only supported on Windows')
  }

  const normalizedPath = path.resolve(directoryPath)

  if (!fs.existsSync(normalizedPath)) {
    throw new Error(`Directory not found: ${normalizedPath}`)
  }

  try {
    if (recursive) {
      fs.rmSync(normalizedPath, { recursive: true, force: true })
    } else {
      fs.rmdirSync(normalizedPath)
    }
    return { success: true, directoryPath: normalizedPath }
  } catch (error) {
    throw new Error(`Failed to delete directory: ${error.message}`)
  }
}





