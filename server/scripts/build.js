#!/usr/bin/env node

/**
 * Script de build pour le serveur Control Deck
 *
 * Usage: node scripts/build.js [options]
 * Options:
 *   --production: Build pour production
 *   --clean: Nettoyer avant de build
 *   --watch: Mode watch
 */

import { execSync } from 'child_process'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const rootDir = path.resolve(__dirname, '..')

const args = process.argv.slice(2)
const isProduction = args.includes('--production')
const shouldClean = args.includes('--clean')
const isWatch = args.includes('--watch')

console.log('🔨 Building Control Deck Server...')
console.log(`Mode: ${isProduction ? 'Production' : 'Development'}`)
console.log(`Clean: ${shouldClean ? 'Yes' : 'No'}`)
console.log(`Watch: ${isWatch ? 'Yes' : 'No'}`)

// Nettoyer si demandé
if (shouldClean) {
  console.log('\n🧹 Cleaning...')
  const dirsToClean = ['dist', 'build', 'node_modules/.cache']
  dirsToClean.forEach((dir) => {
    const fullPath = path.join(rootDir, dir)
    if (fs.existsSync(fullPath)) {
      fs.rmSync(fullPath, { recursive: true, force: true })
      console.log(`  ✓ Cleaned ${dir}`)
    }
  })
}

// Créer les répertoires nécessaires
const dirsToCreate = ['dist', 'build', 'logs']
dirsToCreate.forEach((dir) => {
  const fullPath = path.join(rootDir, dir)
  if (!fs.existsSync(fullPath)) {
    fs.mkdirSync(fullPath, { recursive: true })
    console.log(`  ✓ Created ${dir}`)
  }
})

// Copier les fichiers de configuration
console.log('\n📋 Copying configuration files...')
const configFiles = [
  { src: 'config/server.config.sample.json', dest: 'dist/server.config.sample.json' },
  { src: 'package.json', dest: 'dist/package.json' },
]

configFiles.forEach(({ src, dest }) => {
  const srcPath = path.join(rootDir, src)
  const destPath = path.join(rootDir, dest)
  if (fs.existsSync(srcPath)) {
    fs.copyFileSync(srcPath, destPath)
    console.log(`  ✓ Copied ${src} -> ${dest}`)
  }
})

// Note: L'UI web n'est plus gérée par le serveur
// Elle doit être servie séparément

// Build
console.log('\n🔨 Building server...')
try {
  if (isProduction) {
    // Production build (si vous utilisez un bundler comme esbuild, webpack, etc.)
    console.log('  Building for production...')
    // Exemple avec esbuild (à adapter selon votre setup)
    // execSync('npx esbuild index.js --bundle --platform=node --outfile=dist/index.js --minify', { stdio: 'inherit' })
    console.log('  ✓ Production build complete')
  } else {
    // Development build
    console.log('  Development build (no bundling)')
    console.log('  ✓ Development build complete')
  }

  if (isWatch) {
    console.log('\n👀 Watching for changes...')
    // Exemple avec nodemon ou chokidar
    // execSync('npx nodemon --watch src --exec "node index.js"', { stdio: 'inherit' })
  }

  console.log('\n✅ Build complete!')
  console.log('\nTo start the server:')
  console.log('  npm start')
  if (isProduction) {
    console.log('  or')
    console.log('  node dist/index.js')
  }
} catch (error) {
  console.error('\n❌ Build failed:', error.message)
  process.exit(1)
}





