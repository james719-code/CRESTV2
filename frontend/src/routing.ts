import type { Page } from './domain'

export type GroupRoute = `groups/${string}`
export type ReaderSource = 'qualitative' | 'quantitative' | 'documents' | 'groups'
export interface ReaderTarget {
  source: ReaderSource
  id: string
}
export type ReaderRoute = `read/${ReaderSource}/${string}`
export type Route = Page | GroupRoute | ReaderRoute | 'home' | 'signin'

export function readerRoute(target: ReaderTarget): ReaderRoute {
  return `read/${target.source}/${encodeURIComponent(target.id)}`
}

export function routeReader(route: string): ReaderTarget | null {
  const parts = route.split('/')
  if (
    parts.length !== 3 ||
    parts[0] !== 'read' ||
    !['qualitative', 'quantitative', 'documents', 'groups'].includes(parts[1])
  )
    return null
  try {
    const id = decodeURIComponent(parts[2])
    return id && !id.includes('/') ? { source: parts[1] as ReaderSource, id } : null
  } catch {
    return null
  }
}

export function groupRoute(id: string): GroupRoute {
  return `groups/${encodeURIComponent(id)}`
}

export function routeGroupId(route: Route): string | null {
  return route.startsWith('groups/') ? decodeURIComponent(route.slice(7)) : null
}

export function parseRoute(hash: string): Route {
  const route = hash.replace(/^#/, '')
  if (route.startsWith('read/')) {
    const target = routeReader(route)
    return target ? readerRoute(target) : 'home'
  }
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
    case 'help':
    case 'signin':
      return route
    default:
      return 'home'
  }
}

export function isWorkspaceRoute(route: Route): route is Page | GroupRoute | ReaderRoute {
  return route !== 'home' && route !== 'signin'
}
