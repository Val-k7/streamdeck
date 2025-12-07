import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import Ajv from 'ajv'
import { ConfigValidator } from '../../utils/ConfigValidator.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

describe('ConfigValidator', () => {
  let validator

  beforeEach(() => {
    vi.clearAllMocks()
    validator = new ConfigValidator(mockLogger)
  })

  describe('validateProfile', () => {
    it('should validate a valid profile', () => {
      const validProfile = {
        id: 'test-profile',
        name: 'Test Profile',
        version: 1,
        rows: 3,
        cols: 5,
        controls: [
          {
            id: 'control-1',
            type: 'BUTTON',
            row: 0,
            col: 0,
            label: 'Test Button',
            colorHex: '#FF0000',
            action: {
              type: 'KEYBOARD',
              payload: 'CTRL+S',
            },
          },
        ],
      }

      const result = validator.validateProfile(validProfile)
      expect(result.valid).toBe(true)
      expect(result.errors).toHaveLength(0)
    })

    it('should reject profile with missing required fields', () => {
      const invalidProfile = {
        name: 'Test Profile',
        // Missing id, version, rows, cols
      }

      const result = validator.validateProfile(invalidProfile)
      expect(result.valid).toBe(false)
      expect(result.errors.length).toBeGreaterThan(0)
    })

    it('should reject profile with invalid control type', () => {
      const invalidProfile = {
        id: 'test-profile',
        name: 'Test Profile',
        version: 1,
        rows: 3,
        cols: 5,
        controls: [
          {
            id: 'control-1',
            type: 'INVALID_TYPE',
            row: 0,
            col: 0,
          },
        ],
      }

      const result = validator.validateProfile(invalidProfile)
      expect(result.valid).toBe(false)
    })

    it('should reject profile with invalid action type', () => {
      const invalidProfile = {
        id: 'test-profile',
        name: 'Test Profile',
        version: 1,
        rows: 3,
        cols: 5,
        controls: [
          {
            id: 'control-1',
            type: 'BUTTON',
            row: 0,
            col: 0,
            action: {
              type: 'INVALID_ACTION',
              payload: 'test',
            },
          },
        ],
      }

      const result = validator.validateProfile(invalidProfile)
      expect(result.valid).toBe(false)
    })

    it('should validate profile with all control types', () => {
      const profile = {
        id: 'test-profile',
        name: 'Test Profile',
        version: 1,
        rows: 3,
        cols: 5,
        controls: [
          { id: '1', type: 'BUTTON', row: 0, col: 0, action: { type: 'KEYBOARD', payload: 'A' } },
          { id: '2', type: 'TOGGLE', row: 0, col: 1, action: { type: 'KEYBOARD', payload: 'B' } },
          { id: '3', type: 'FADER', row: 0, col: 2, action: { type: 'AUDIO', payload: 'volume' } },
          { id: '4', type: 'KNOB', row: 0, col: 3, action: { type: 'AUDIO', payload: 'pan' } },
          { id: '5', type: 'PAD', row: 0, col: 4, action: { type: 'OBS', payload: 'scene' } },
        ],
      }

      const result = validator.validateProfile(profile)
      expect(result.valid).toBe(true)
    })
  })

  describe('validateControl', () => {
    it('should validate a valid control', () => {
      const control = {
        id: 'control-1',
        type: 'BUTTON',
        row: 0,
        col: 0,
        label: 'Test',
        action: { type: 'KEYBOARD', payload: 'A' },
      }

      const result = validator.validateControl(control)
      expect(result.valid).toBe(true)
    })

    it('should reject control with missing id', () => {
      const control = {
        type: 'BUTTON',
        row: 0,
        col: 0,
      }

      const result = validator.validateControl(control)
      expect(result.valid).toBe(false)
    })

    it('should reject control with invalid row/col', () => {
      const control = {
        id: 'control-1',
        type: 'BUTTON',
        row: -1,
        col: 0,
        action: { type: 'KEYBOARD', payload: 'A' },
      }

      const result = validator.validateControl(control)
      expect(result.valid).toBe(false)
    })
  })

  describe('validateAction', () => {
    it('should validate valid keyboard action', () => {
      const action = { type: 'KEYBOARD', payload: 'CTRL+S' }
      const result = validator.validateAction(action)
      expect(result.valid).toBe(true)
    })

    it('should validate valid OBS action', () => {
      const action = { type: 'OBS', payload: 'StartStreaming' }
      const result = validator.validateAction(action)
      expect(result.valid).toBe(true)
    })

    it('should reject action with missing type', () => {
      const action = { payload: 'test' }
      const result = validator.validateAction(action)
      expect(result.valid).toBe(false)
    })

    it('should reject action with invalid type', () => {
      const action = { type: 'INVALID', payload: 'test' }
      const result = validator.validateAction(action)
      expect(result.valid).toBe(false)
    })
  })
})


