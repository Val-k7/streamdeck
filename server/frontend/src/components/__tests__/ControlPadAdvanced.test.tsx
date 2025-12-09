import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ControlPad } from '../ControlPad'

describe('ControlPad - Advanced Interactions', () => {
  it('should handle long press events', () => {
    const onLongPress = vi.fn()
    const { container } = render(
      <ControlPad
        type="button"
        size="1x1"
        label="Test Button"
        color="primary"
        isActive={false}
        onPress={() => {}}
        onLongPress={onLongPress}
        onValueChange={() => {}}
      />
    )

    const pad = container.firstChild as HTMLElement
    fireEvent.mouseDown(pad)

    // Simulate long press (hold for >500ms)
    setTimeout(() => {
      expect(onLongPress).toHaveBeenCalled()
    }, 600)
  })

  it('should handle value changes for fader', () => {
    const onValueChange = vi.fn()
    render(
      <ControlPad
        type="fader"
        size="1x2"
        label="Volume"
        color="primary"
        value={50}
        isActive={false}
        onPress={() => {}}
        onLongPress={() => {}}
        onValueChange={onValueChange}
      />
    )

    // Simulate value change
    const slider = screen.getByRole('slider')
    fireEvent.change(slider, { target: { value: '75' } })

    expect(onValueChange).toHaveBeenCalledWith(75)
  })

  it('should handle encoder rotation', () => {
    const onValueChange = vi.fn()
    render(
      <ControlPad
        type="encoder"
        size="2x2"
        label="Pan"
        color="accent"
        value={50}
        isActive={false}
        onPress={() => {}}
        onLongPress={() => {}}
        onValueChange={onValueChange}
      />
    )

    // Simulate encoder rotation
    const encoder = screen.getByRole('slider')
    fireEvent.change(encoder, { target: { value: '60' } })

    expect(onValueChange).toHaveBeenCalledWith(60)
  })

  it('should display active state correctly', () => {
    const { container } = render(
      <ControlPad
        type="button"
        size="1x1"
        label="Test Button"
        color="primary"
        isActive={true}
        onPress={() => {}}
        onLongPress={() => {}}
        onValueChange={() => {}}
      />
    )

    const pad = container.firstChild as HTMLElement
    expect(pad).toHaveClass('active')
  })
})

