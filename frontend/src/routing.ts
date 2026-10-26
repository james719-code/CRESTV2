import type { Page } from './domain'

export type Route = Page | 'home' | 'signin'

export function parseRoute(hash: string): Route {
  const route = hash.replace(/^#/, '')
  switch (route) {
    case 'overview':
    case 'groups':
    case 'reviews':
    case 'students':
    case 'research':
    case 'documents':
    case 'accounts':
    case 'signin':
      return route
    default:
      return 'home'
  }
}

export function isWorkspaceRoute(route: Route): route is Page {
  return route !== 'home' && route !== 'signin'
}
