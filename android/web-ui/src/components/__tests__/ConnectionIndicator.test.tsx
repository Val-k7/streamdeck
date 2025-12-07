import { describe, it, expect } from 'vitest'
import { render, screen } from '../../test/utils/test-utils'
import { ConnectionIndicator } from '../ConnectionIndicator'

describe('ConnectionIndicator', () => {
  it('should render connection indicator', () => {
    render(<ConnectionIndicator status="online" />)
    expect(screen.getByText('online')).toBeInTheDocument()
  })

  it('should show online state', () => {
    render(<ConnectionIndicator status="online" />)
    expect(screen.getByText('online')).toBeInTheDocument()
  })

  it('should show offline state', () => {
    render(<ConnectionIndicator status="offline" />)
    expect(screen.getByText('offline')).toBeInTheDocument()
  })

  it('should show connecting state', () => {
    render(<ConnectionIndicator status="connecting" />)
    expect(screen.getByText('connecting')).toBeInTheDocument()
  })
})

