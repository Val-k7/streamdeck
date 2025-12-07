import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ProfileTabs } from '../ProfileTabs'

describe('ProfileTabs - Advanced Interactions', () => {
  const mockProfiles = [
    {
      id: 'profile-1',
      label: 'Streaming',
      color: 'primary',
    },
    {
      id: 'profile-2',
      label: 'Music',
      color: 'accent',
    },
    {
      id: 'profile-3',
      label: 'Gaming',
      color: 'destructive',
    },
  ]

  it('should handle profile selection', () => {
    const onProfileChange = vi.fn()
    render(
      <ProfileTabs
        profiles={mockProfiles}
        activeProfile="profile-1"
        onProfileChange={onProfileChange}
      />
    )

    const musicTab = screen.getByText('Music')
    fireEvent.click(musicTab)

    expect(onProfileChange).toHaveBeenCalledWith('profile-2')
  })

  it('should display active profile correctly', () => {
    render(
      <ProfileTabs
        profiles={mockProfiles}
        activeProfile="profile-2"
        onProfileChange={() => {}}
      />
    )

    const activeTab = screen.getByText('Music').closest('button')
    expect(activeTab).toHaveClass('active')
  })

  it('should handle empty profiles list', () => {
    render(
      <ProfileTabs
        profiles={[]}
        activeProfile=""
        onProfileChange={() => {}}
      />
    )

    // Should render without errors
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('should handle many profiles with scrolling', () => {
    const manyProfiles = Array.from({ length: 20 }, (_, i) => ({
      id: `profile-${i}`,
      label: `Profile ${i}`,
      color: 'primary',
    }))

    render(
      <ProfileTabs
        profiles={manyProfiles}
        activeProfile="profile-0"
        onProfileChange={() => {}}
      />
    )

    // Should render all profiles
    expect(screen.getByText('Profile 0')).toBeInTheDocument()
    expect(screen.getByText('Profile 19')).toBeInTheDocument()
  })
})

