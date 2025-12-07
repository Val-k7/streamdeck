import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '../../test/utils/test-utils'
import { PadEditor } from '../PadEditor'

// Mock icons
vi.mock('lucide-react', () => ({
  Play: () => <div>PlayIcon</div>,
  Square: () => <div>SquareIcon</div>,
  Mic: () => <div>MicIcon</div>,
}))

describe('PadEditor', () => {
  const mockPad = {
    id: 'pad-1',
    type: 'button' as const,
    size: '1x1' as const,
    label: 'Test Pad',
    color: 'primary' as const,
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render pad editor when open', () => {
    render(
      <PadEditor
        pad={mockPad}
        open={true}
        onClose={vi.fn()}
        onSave={vi.fn()}
      />
    )

    expect(screen.getByText(/edit|pad/i)).toBeInTheDocument()
  })

  it('should not render when closed', () => {
    render(
      <PadEditor
        pad={mockPad}
        open={false}
        onClose={vi.fn()}
        onSave={vi.fn()}
      />
    )

    expect(screen.queryByText(/edit|pad/i)).not.toBeInTheDocument()
  })

  it('should allow editing pad label', () => {
    const onSave = vi.fn()
    render(
      <PadEditor
        pad={mockPad}
        open={true}
        onClose={vi.fn()}
        onSave={onSave}
      />
    )

    const labelInput = screen.getByLabelText(/label/i)
    if (labelInput) {
      fireEvent.change(labelInput, { target: { value: 'New Label' } })
      expect(labelInput).toHaveValue('New Label')
    }
  })

  it('should call onSave when saving', () => {
    const onSave = vi.fn()
    render(
      <PadEditor
        pad={mockPad}
        open={true}
        onClose={vi.fn()}
        onSave={onSave}
      />
    )

    const saveButton = screen.getByText(/save/i)
    if (saveButton) {
      fireEvent.click(saveButton)
      expect(onSave).toHaveBeenCalled()
    }
  })

  it('should call onClose when closing', () => {
    const onClose = vi.fn()
    render(
      <PadEditor
        pad={mockPad}
        open={true}
        onClose={onClose}
        onSave={vi.fn()}
      />
    )

    const closeButton = screen.getByText(/close|cancel/i)
    if (closeButton) {
      fireEvent.click(closeButton)
      expect(onClose).toHaveBeenCalled()
    }
  })
})


