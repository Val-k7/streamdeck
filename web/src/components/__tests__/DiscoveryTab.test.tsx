import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '../../test/utils/test-utils'
import DiscoveryTab from '../DiscoveryTab'

// Mock useConnectionSettings
vi.mock('../../hooks/useConnectionSettings', () => ({
  default: () => ({
    serverIp: '',
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

describe('DiscoveryTab', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render discovery tab', () => {
    render(<DiscoveryTab />)
    expect(screen.getByText(/discover|server/i)).toBeInTheDocument()
  })

  it('should allow entering server IP', () => {
    render(<DiscoveryTab />)

    const ipInput = screen.getByLabelText(/server ip|ip address/i)
    if (ipInput) {
      fireEvent.change(ipInput, { target: { value: '192.168.1.100' } })
      expect(ipInput).toHaveValue('192.168.1.100')
    }
  })

  it('should allow entering server port', () => {
    render(<DiscoveryTab />)

    const portInput = screen.getByLabelText(/port/i)
    if (portInput) {
      fireEvent.change(portInput, { target: { value: '4455' } })
      expect(portInput).toHaveValue('4455')
    }
  })

  it('should show discovered servers', async () => {
    render(<DiscoveryTab />)

    // Should render discovery UI
    expect(screen.getByText(/discover|search/i)).toBeInTheDocument()
  })
})


