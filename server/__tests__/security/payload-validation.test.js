import { describe, it, expect } from '@jest/globals'
import Ajv from 'ajv'

describe('Payload Validation Security Tests', () => {
  const ajv = new Ajv({ allErrors: true })

  const controlActionSchema = {
    type: 'object',
    required: ['kind', 'controlId', 'value', 'messageId', 'sentAt'],
    properties: {
      kind: { type: 'string', const: 'control' },
      controlId: { type: 'string', minLength: 1, maxLength: 100 },
      value: { type: 'number', minimum: 0, maximum: 1 },
      messageId: { type: 'string', pattern: '^[a-zA-Z0-9-_]+$' },
      sentAt: { type: 'number', minimum: 0 },
    },
    additionalProperties: false,
  }

  const validateControlAction = ajv.compile(controlActionSchema)

  describe('SQL Injection Prevention', () => {
    it('should reject SQL injection attempts in controlId', () => {
      const maliciousPayload = {
        kind: 'control',
        controlId: "'; DROP TABLE profiles; --",
        value: 1,
        messageId: 'test-msg',
        sentAt: Date.now(),
      }

      const isValid = validateControlAction(maliciousPayload)
      // Should be rejected by schema validation (pattern mismatch)
      expect(isValid).toBe(false)
    })

    it('should reject SQL injection attempts in messageId', () => {
      const maliciousPayload = {
        kind: 'control',
        controlId: 'test-control',
        value: 1,
        messageId: "'; DROP TABLE profiles; --",
        sentAt: Date.now(),
      }

      const isValid = validateControlAction(maliciousPayload)
      expect(isValid).toBe(false)
    })
  })

  describe('XSS Prevention', () => {
    it('should reject XSS attempts in controlId', () => {
      const maliciousPayload = {
        kind: 'control',
        controlId: '<script>alert("xss")</script>',
        value: 1,
        messageId: 'test-msg',
        sentAt: Date.now(),
      }

      const isValid = validateControlAction(maliciousPayload)
      expect(isValid).toBe(false)
    })
  })

  describe('Type Confusion', () => {
    it('should reject wrong types', () => {
      const invalidPayload = {
        kind: 'control',
        controlId: 123, // Should be string
        value: 'invalid', // Should be number
        messageId: 'test-msg',
        sentAt: Date.now(),
      }

      const isValid = validateControlAction(invalidPayload)
      expect(isValid).toBe(false)
    })

    it('should reject missing required fields', () => {
      const invalidPayload = {
        kind: 'control',
        // Missing controlId, value, messageId, sentAt
      }

      const isValid = validateControlAction(invalidPayload)
      expect(isValid).toBe(false)
    })
  })

  describe('Value Range Validation', () => {
    it('should reject out-of-range values', () => {
      const invalidPayload = {
        kind: 'control',
        controlId: 'test-control',
        value: 2.0, // Should be 0-1
        messageId: 'test-msg',
        sentAt: Date.now(),
      }

      const isValid = validateControlAction(invalidPayload)
      expect(isValid).toBe(false)
    })

    it('should reject negative values', () => {
      const invalidPayload = {
        kind: 'control',
        controlId: 'test-control',
        value: -1,
        messageId: 'test-msg',
        sentAt: Date.now(),
      }

      const isValid = validateControlAction(invalidPayload)
      expect(isValid).toBe(false)
    })
  })

  describe('Additional Properties', () => {
    it('should reject additional properties', () => {
      const invalidPayload = {
        kind: 'control',
        controlId: 'test-control',
        value: 1,
        messageId: 'test-msg',
        sentAt: Date.now(),
        maliciousProperty: 'hack',
      }

      const isValid = validateControlAction(invalidPayload)
      expect(isValid).toBe(false)
    })
  })

  describe('String Length Validation', () => {
    it('should reject overly long controlId', () => {
      const invalidPayload = {
        kind: 'control',
        controlId: 'a'.repeat(101), // Max 100 chars
        value: 1,
        messageId: 'test-msg',
        sentAt: Date.now(),
      }

      const isValid = validateControlAction(invalidPayload)
      expect(isValid).toBe(false)
    })
  })
})


