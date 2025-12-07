import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '../../test/utils/test-utils'
import DeckGrid from '../DeckGrid'

const mockPads = [
  {
    id: 'pad-1',
    type: 'button' as const,
    size: '1x1' as const,
    label: 'Button 1',
    iconName: 'Play',
    color: 'primary' as const,
  },
  {
    id: 'pad-2',
    type: 'button' as const,
    size: '1x1' as const,
    label: 'Button 2',
    iconName: 'Square',
    color: 'accent' as const,
  },
]

// Mock ControlPad
vi.mock('../ControlPad', () => ({
  ControlPad: ({ label, onPress }: any) => (
    <button onClick={onPress} data-testid={`pad-${label}`}>
      {label}
    </button>
  ),
}))

// Mock PadEditor
vi.mock('../PadEditor', () => ({
  PadEditor: () => <div>Pad Editor</div>,
  getIconByName: () => null,
}))

describe('DeckGrid', () => {
  it('should render grid with pads', () => {
    render(
      <DeckGrid
        profileName="test"
        pads={mockPads}
        onPadPress={() => {}}
        onPadLongPress={() => {}}
        onPadValueChange={() => {}}
        onPadEdit={() => {}}
      />
    )

    expect(screen.getByTestId('pad-Button 1')).toBeInTheDocument()
    expect(screen.getByTestId('pad-Button 2')).toBeInTheDocument()
  })

  it('should render empty grid when no pads', () => {
    render(
      <DeckGrid
        profileName="test"
        pads={[]}
        onPadPress={() => {}}
        onPadLongPress={() => {}}
        onPadValueChange={() => {}}
        onPadEdit={() => {}}
      />
    )

    // Grid should still render
    expect(screen.queryByTestId('pad-Button 1')).not.toBeInTheDocument()
  })

  it('should call onPadPress when pad is clicked', () => {
    const onPadPress = vi.fn()
    render(
      <DeckGrid
        profileName="test"
        pads={mockPads}
        onPadPress={onPadPress}
        onPadLongPress={() => {}}
        onPadValueChange={() => {}}
        onPadEdit={() => {}}
      />
    )

    const button = screen.getByTestId('pad-Button 1')
    button.click()

    expect(onPadPress).toHaveBeenCalledWith(mockPads[0].id)
  })
})

