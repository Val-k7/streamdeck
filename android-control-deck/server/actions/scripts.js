import { exec } from 'child_process'

export async function handleScript(command) {
  return new Promise((resolve, reject) => {
    exec(command, (error, stdout, stderr) => {
      if (error) {
        console.error('[script] error', error)
        reject(error)
        return
      }
      if (stderr) console.warn('[script] stderr', stderr)
      console.log('[script] stdout', stdout)
      resolve(stdout)
    })
  })
}
