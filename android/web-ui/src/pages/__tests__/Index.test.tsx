import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '../../test/utils/test-utils'
import Index from '../Index'

// Mock useWebSocket
vi.mock('../../hooks/useWebSocket', () => ({
  default: () => ({
    status: 'offline',
    connect: vi.fn(),
    disconnect: vi.fn(),
    send: vi.fn(),
  }),
}))

// Mock useDeckStorage
vi.mock('../../hooks/useDeckStorage', () => ({
  default: () => ({
    pads: [],
    profiles: [],
    currentProfile: null,
    loadDeck: vi.fn(),
    saveDeck: vi.fn(),
    updatePad: vi.fn(),
    addPad: vi.fn(),
    removePad: vi.fn(),
  }),
}))

// Mock useProfiles
vi.mock('../../hooks/useProfiles', () => ({
  default: () => ({
    profiles: [],
    currentProfile: null,
    selectProfile: vi.fn(),
    createProfile: vi.fn(),
    updateProfile: vi.fn(),
    deleteProfile: vi.fn(),
  }),
}))

describe('Index Page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render the main page', () => {
    render(<Index />)

    // Should render the main deck interface
    expect(screen.getByText(/control deck|deck/i)).toBeInTheDocument()
  })

  it('should render deck grid', () => {
    render(<Index />)

    // Should render the deck grid component
    const deckGrid = screen.queryByTestId('deck-grid') || screen.queryByRole('grid')
    expect(deckGrid || screen.getByText(/pad|button/i)).toBeDefined()
  })

  it('should render profile tabs', () => {
    render(<Index />)

    // Should render profile selection
    const profileTabs = screen.queryByTestId('profile-tabs') || screen.queryByRole('tablist')
    expect(profileTabs || screen.getByText(/profile/i)).toBeDefined()
  })
})
