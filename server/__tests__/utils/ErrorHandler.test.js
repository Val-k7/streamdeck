import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import { ErrorHandler } from '../../utils/ErrorHandler.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

describe('ErrorHandler', () => {
  let errorHandler

  beforeEach(() => {
    vi.clearAllMocks()
    errorHandler = new ErrorHandler(mockLogger)
  })

  describe('handleError', () => {
    it('should handle generic errors', () => {
      const error = new Error('Test error')

      const response = errorHandler.handleError(error, { action: 'test' })

      expect(response).toHaveProperty('error')
      expect(response).toHaveProperty('errorId')
      expect(mockLogger.error).toHaveBeenCalled()
    })

    it('should handle network errors', () => {
      const error = new Error('Network error')
      error.code = 'ECONNREFUSED'

      const response = errorHandler.handleError(error, { action: 'network' })

      expect(response).toHaveProperty('error')
      expect(mockLogger.error).toHaveBeenCalled()
    })

    it('should handle validation errors', () => {
      const error = new Error('Validation failed')
      error.name = 'ValidationError'

      const response = errorHandler.handleError(error, { action: 'validation' })

      expect(response).toHaveProperty('error')
      expect(mockLogger.warn).toHaveBeenCalled()
    })

    it('should track error counts', () => {
      const error = new Error('Test error')

      errorHandler.handleError(error, { action: 'test' })
      errorHandler.handleError(error, { action: 'test' })

      expect(errorHandler.errorCounts.size).toBeGreaterThan(0)
    })
  })

  describe('getUserFriendlyMessage', () => {
    it('should return user-friendly message for network errors', () => {
      const error = new Error('ECONNREFUSED')
      error.code = 'ECONNREFUSED'

      const message = errorHandler.getUserFriendlyMessage(error)

      expect(message).toBeDefined()
      expect(typeof message).toBe('string')
    })

    it('should return user-friendly message for timeout errors', () => {
      const error = new Error('ETIMEDOUT')
      error.code = 'ETIMEDOUT'

      const message = errorHandler.getUserFriendlyMessage(error)

      expect(message).toBeDefined()
    })

    it('should return generic message for unknown errors', () => {
      const error = new Error('Unknown error')

      const message = errorHandler.getUserFriendlyMessage(error)

      expect(message).toBeDefined()
      expect(message.length).toBeGreaterThan(0)
    })
  })

  describe('isRetryable', () => {
    it('should identify retryable errors', () => {
      const networkError = new Error('Network error')
      networkError.code = 'ECONNREFUSED'

      expect(errorHandler.isRetryable(networkError)).toBe(true)
    })

    it('should identify non-retryable errors', () => {
      const validationError = new Error('Validation failed')
      validationError.name = 'ValidationError'

      expect(errorHandler.isRetryable(validationError)).toBe(false)
    })
  })
})

