// Open-source software and fonts that CREST's website is built with. This list drives the
// credits shown on the About page and the generated /licenses/THIRD-PARTY-NOTICES.txt, so
// adding a runtime dependency to package.json also needs an entry here (see tests/credits).
export type CreditGroup = 'Typeface' | 'Interface' | 'Documents and sign-in'
export type CreditLicense = 'MIT' | 'Apache-2.0' | 'SIL OFL 1.1'

export interface Credit {
  name: string
  role: string
  group: CreditGroup
  license: CreditLicense
  url: string
  /** npm packages whose license text is gathered into the notices file. */
  packages: string[]
}

export const credits: Credit[] = [
  {
    name: 'Inter',
    role: 'The typeface used throughout CREST',
    group: 'Typeface',
    license: 'SIL OFL 1.1',
    url: 'https://rsms.me/inter/',
    packages: [],
  },
  {
    name: 'React',
    role: 'Builds every screen you see',
    group: 'Interface',
    license: 'MIT',
    url: 'https://react.dev',
    packages: ['react', 'react-dom'],
  },
  {
    name: 'Radix UI',
    role: 'Accessible dialogs, menus, tabs and tooltips',
    group: 'Interface',
    license: 'MIT',
    url: 'https://www.radix-ui.com',
    packages: ['radix-ui'],
  },
  {
    name: 'shadcn/ui',
    role: 'The foundation for CREST’s buttons, cards and forms',
    group: 'Interface',
    license: 'MIT',
    url: 'https://ui.shadcn.com',
    packages: [],
  },
  {
    name: 'Tailwind CSS',
    role: 'Styling utilities',
    group: 'Interface',
    license: 'MIT',
    url: 'https://tailwindcss.com',
    packages: ['tailwindcss'],
  },
  {
    name: 'Phosphor Icons',
    role: 'Every icon in the workspace',
    group: 'Interface',
    license: 'MIT',
    url: 'https://phosphoricons.com',
    packages: ['@phosphor-icons/react'],
  },
  {
    name: 'Sonner',
    role: 'Small confirmation messages',
    group: 'Interface',
    license: 'MIT',
    url: 'https://sonner.emilkowal.ski',
    packages: ['sonner'],
  },
  {
    name: 'class-variance-authority',
    role: 'Button and badge style variants',
    group: 'Interface',
    license: 'Apache-2.0',
    url: 'https://cva.style',
    packages: ['class-variance-authority'],
  },
  {
    name: 'clsx and tailwind-merge',
    role: 'Combining style classes safely',
    group: 'Interface',
    license: 'MIT',
    url: 'https://github.com/lukeed/clsx',
    packages: ['clsx', 'tailwind-merge'],
  },
  {
    name: 'PDF.js',
    role: 'Opens and displays research papers in your browser',
    group: 'Documents and sign-in',
    license: 'Apache-2.0',
    url: 'https://mozilla.github.io/pdf.js/',
    packages: ['pdfjs-dist'],
  },
  {
    name: 'Firebase SDK',
    role: 'School Google sign-in and shared records',
    group: 'Documents and sign-in',
    license: 'Apache-2.0',
    url: 'https://firebase.google.com',
    packages: ['firebase'],
  },
]

export const noticesPath = '/licenses/THIRD-PARTY-NOTICES.txt'
export const interLicensePath = '/licenses/Inter-OFL.txt'
