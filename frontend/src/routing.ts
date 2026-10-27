import type { Page } from './domain'

export type GroupRoute = `groups/${string}`
export type Route = Page | GroupRoute | 'home' | 'signin'

export function groupRoute(id: string): GroupRoute {
  return `groups/${encodeURIComponent(id)}`
}

export function routeGroupId(route: Route): string | null {
  return route.startsWith('groups/') ? decodeURIComponent(route.slice(7)) : null
}

export function parseRoute(hash: string): Route {
  const route = hash.replace(/^#/, '')
  if (route.startsWith('groups/')) {
    try {
      const id = decodeURIComponent(route.slice(7))
      if (id && !id.includes('/')) return groupRoute(id)
    } catch {
      /* Invalid links return to the public page. */
    }
    return 'home'
  }
  switch (route) {
    case 'overview':
    case 'groups':
    case 'reviews':
    case 'students':
    case 'research':
    case 'documents':
    case 'accounts':
    case 'settings':
    case 'about':
    case 'signin':
      return route
    default:
      return 'home'
  }
}

export function isWorkspaceRoute(route: Route): route is Page | GroupRoute {
  return route !== 'home' && route !== 'signin'
}
