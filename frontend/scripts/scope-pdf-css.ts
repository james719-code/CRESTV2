import postcss from 'postcss'

/** Keep PDF.js's generic viewer rules and variables inside the reader page. */
export function scopePdfCss(source: string): string {
  const css = postcss.parse(source)
  css.walkRules((rule) => {
    // Nested selectors already inherit their enclosing rule. Keyframe selectors
    // describe animation steps and are never ordinary element selectors.
    let parent = rule.parent
    while (parent && parent.type !== 'root') {
      if (parent.type === 'rule' || (parent.type === 'atrule' && /keyframes$/i.test(parent.name)))
        return
      parent = parent.parent
    }
    // PDF.js writes this measurement to document.documentElement. Its scoped
    // default must inherit that live value instead of masking it with zero.
    if (rule.selectors.some((selector) => selector.trim() === ':root')) {
      rule.walkDecls('--viewer-container-height', (declaration) => {
        declaration.value = 'inherit'
      })
    }
    rule.selectors = rule.selectors.map((selector) =>
      /:root\b/.test(selector)
        ? selector.replace(/:root\b/g, '.paper-reader')
        : `.paper-reader ${selector}`,
    )
  })
  return css.toString()
}
