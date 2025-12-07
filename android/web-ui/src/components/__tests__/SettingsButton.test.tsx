import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '../../test/utils/test-utils'
import { SettingsButton } from '../SettingsButton'

describe('SettingsButton', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render settings button', () => {
    render(<SettingsButton onClick={vi.fn()} />)

    const button = screen.getByRole('button')
    expect(button).toBeInTheDocument()
  })

  it('should call onClick when clicked', () => {
    const onClick = vi.fn()
    render(<SettingsButton onClick={onClick} />)

    const button = screen.getByRole('button')
    fireEvent.click(button)

    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('should be accessible', () => {
    render(<SettingsButton onClick={vi.fn()} />)

    const button = screen.getByRole('button')
    expect(button).toHaveAttribute('aria-label', expect.stringContaining('settings'))
  })
})


