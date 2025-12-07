import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '../../test/utils/test-utils'
import ProfileTabs from '../ProfileTabs'

// Mock useProfiles
const mockProfiles = [
  { id: 'profile-1', name: 'Profile 1' },
  { id: 'profile-2', name: 'Profile 2' },
]

vi.mock('../../hooks/useProfiles', () => ({
  default: () => ({
    profiles: mockProfiles,
    activeProfile: mockProfiles[0],
    selectProfile: vi.fn(),
    isLoading: false,
  }),
}))

// Mock useWebSocket
vi.mock('../../hooks/useWebSocket', () => ({
  default: () => ({
    status: 'online',
    selectProfile: vi.fn(),
  }),
}))

describe('ProfileTabs', () => {
  it('should render profile tabs', () => {
    render(<ProfileTabs />)
    expect(screen.getByText('Profile 1')).toBeInTheDocument()
    expect(screen.getByText('Profile 2')).toBeInTheDocument()
  })

  it('should allow profile selection', () => {
    const onProfileSelect = vi.fn()
    render(<ProfileTabs onProfileSelect={onProfileSelect} />)

    const profile2 = screen.getByText('Profile 2')
    fireEvent.click(profile2)

    expect(onProfileSelect).toHaveBeenCalled()
  })
})


