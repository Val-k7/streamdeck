import { exec } from 'child_process'
import { getLogger } from '../utils/Logger.js'

const logger = getLogger()

export async function handleScript(command) {
  return new Promise((resolve, reject) => {
    exec(command, (error, stdout, stderr) => {
      if (error) {
        logger.error('[script] error', { error: error.message, stack: error.stack, command })
        reject(error)
        return
      }
      if (stderr) logger.warn('[script] stderr', { stderr, command })
      logger.debug('[script] stdout', { stdout, command })
      resolve(stdout)
    })
  })
}
