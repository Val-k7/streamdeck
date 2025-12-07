import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '../../test/utils/test-utils'
import SettingsOverlay from '../SettingsOverlay'

// Mock useConnectionSettings
vi.mock('../../hooks/useConnectionSettings', () => ({
  default: () => ({
    serverIp: 'localhost',
    serverPort: 8080,
    useTls: false,
    setServerIp: vi.fn(),
    setServerPort: vi.fn(),
    setUseTls: vi.fn(),
    connect: vi.fn(),
    disconnect: vi.fn(),
  }),
}))

// Mock useWebSocket
vi.mock('../../hooks/useWebSocket', () => ({
  default: () => ({
    status: 'offline',
    connect: vi.fn(),
    disconnect: vi.fn(),
  }),
}))

describe('SettingsOverlay', () => {
  it('should render settings overlay when open', () => {
    render(<SettingsOverlay open={true} onClose={vi.fn()} />)
    expect(screen.getByText(/server/i)).toBeInTheDocument()
  })

  it('should not render when closed', () => {
    render(<SettingsOverlay open={false} onClose={vi.fn()} />)
    expect(screen.queryByText(/server/i)).not.toBeInTheDocument()
  })

  it('should allow editing server IP', () => {
    render(<SettingsOverlay open={true} onClose={vi.fn()} />)

    const ipInput = screen.getByLabelText(/server ip/i)
    fireEvent.change(ipInput, { target: { value: '192.168.1.100' } })

    expect(ipInput).toHaveValue('192.168.1.100')
  })

  it('should allow editing server port', () => {
    render(<SettingsOverlay open={true} onClose={vi.fn()} />)

    const portInput = screen.getByLabelText(/port/i)
    fireEvent.change(portInput, { target: { value: '4455' } })

    expect(portInput).toHaveValue('4455')
  })

  it('should allow toggling TLS', () => {
    render(<SettingsOverlay open={true} onClose={vi.fn()} />)

    const tlsToggle = screen.getByLabelText(/use tls/i)
    fireEvent.click(tlsToggle)

    expect(tlsToggle).toBeChecked()
  })
})


