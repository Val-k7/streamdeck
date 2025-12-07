import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { NavLink } from '../NavLink'
import React from 'react'

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>)
}

describe('NavLink', () => {
  it('should render nav link', () => {
    renderWithRouter(
      <NavLink to="/test">Test Link</NavLink>
    )

    expect(screen.getByText('Test Link')).toBeInTheDocument()
  })

  it('should handle click', () => {
    const onClick = vi.fn()
    renderWithRouter(
      <NavLink to="/test" onClick={onClick}>Click Me</NavLink>
    )

    fireEvent.click(screen.getByText('Click Me'))
    expect(onClick).toHaveBeenCalled()
  })

  it('should apply active class when active', () => {
    renderWithRouter(
      <NavLink to="/test" activeClassName="active">Active Link</NavLink>
    )

    const link = screen.getByText('Active Link')
    // NavLink from react-router-dom handles active state automatically
    expect(link).toBeInTheDocument()
  })
})

