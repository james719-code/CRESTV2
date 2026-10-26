import type { Workspace, ResearchGroup, Student } from '../domain'

const groupDetails = [
  [
    'g1',
    'The Green Collective',
    'STEM',
    'Turning coconut husks into a sustainable water filter',
    'quantitative',
    'pending',
  ],
  [
    'g2',
    'Beyond the Numbers',
    'ABM',
    'Digital payment habits of small business owners in Pili',
    'quantitative',
    'pending',
  ],
  [
    'g3',
    'The Story Seekers',
    'HUMSS',
    'The lived experiences of first-generation senior high school students',
    'qualitative',
    'pending',
  ],
  [
    'g4',
    'Project SIKAP',
    'STEM',
    'Banana peel extract as an alternative organic fertilizer',
    'quantitative',
    'changes',
  ],
  [
    'g5',
    'Local Perspectives',
    'HUMSS',
    'Preserving Bicolano traditions through community storytelling',
    'qualitative',
    'changes',
  ],
  [
    'g6',
    'Tomorrow Lab',
    'STEM',
    'A low-cost rainwater collection system for school gardens',
    'quantitative',
    'approved',
  ],
  [
    'g7',
    'Market Minds',
    'ABM',
    'Student saving habits and financial literacy',
    'quantitative',
    'approved',
  ],
  [
    'g8',
    'Craft & Community',
    'TVL',
    'Sustainable packaging practices of local food vendors',
    'qualitative',
    'approved',
  ],
  ['g9', 'New Horizons', 'TVL', '', '', 'preparing'],
] as const

const names = [
  'Althea Mendoza',
  'Gabriel Ramos',
  'Ysabel Villanueva',
  'Rafael Soriano',
  'Bea Dela Cruz',
  'Miguel Santos',
  'Andrea Bautista',
  'Paolo Navarro',
  'Sofia Alarcon',
  'Joaquin Reyes',
  'Camille Ferrer',
  'Luis Manalo',
  'Cheska Aquino',
  'Enzo Lim',
  'Nica Salcedo',
  'Daniel Abella',
  'Katrina Ocampo',
  'Marco Padilla',
  'Trisha Vergara',
  'Adrian Cortez',
  'Hannah Agustin',
  'Theo Mercado',
  'Alyssa Robles',
  'Carlo Dizon',
  'Mikaela Panganiban',
  'Franco Espino',
  'Julia Sarmiento',
  'Aaron Tolentino',
  'Elise Fajardo',
  'Nico Cabrera',
  'Bianca Tuazon',
  'Andre Valencia',
  'Janelle Peralta',
  'Liam Pascual',
  'Ria Montemayor',
  'Joshua Escobar',
]

export function createDemoWorkspace(): Workspace {
  const students: Student[] = names.map((name, index) => ({
    id: `s${index + 1}`,
    name,
    strand: groupDetails[Math.floor(index / 4)][2],
    groupId: groupDetails[Math.floor(index / 4)][0],
    accepted: true,
  }))
  students.push(
    { id: 's37', name: 'Maia Del Rosario', strand: 'STEM', groupId: '', accepted: false },
    { id: 's38', name: 'Elijah Buenaventura', strand: 'HUMSS', groupId: '', accepted: false },
    { id: 's39', name: 'Isabelle Gonzales', strand: 'ABM', groupId: '', accepted: false },
  )
  const groups: ResearchGroup[] = groupDetails.map(
    ([id, name, strand, title, type, status], index) => ({
      id,
      name,
      strand,
      title,
      type,
      memberIds: students.slice(index * 4, index * 4 + 4).map((student) => student.id),
      uploaded: status === 'pending',
      accepted: status === 'approved',
      fileId: status === 'preparing' ? '' : `sample-${id}`,
      comments:
        status === 'changes'
          ? [
              'Please clarify your sampling method and make sure each source in the literature review appears in your references.',
            ]
          : [],
    }),
  )
  return {
    students,
    teachers: [{ id: 'demo-teacher', name: 'Alex Rivera', email: 'Teacher demo', access: true }],
    groups,
    research: groups
      .filter((group) => group.accepted)
      .map((group, index) => ({
        id: `r${index}`,
        title: group.title,
        strand: group.strand,
        type: group.type || 'qualitative',
        fileId: group.fileId,
        members: students
          .filter((student) => group.memberIds.includes(student.id))
          .map((student) => student.name),
        createdAt: Date.UTC(2026, 9, 23 - index * 2),
        downloads: [12, 8, 5][index],
        unfinished: false,
      })),
    documents: [
      {
        id: 'd1',
        name: 'Research manuscript template',
        description: 'A starting point for your title page, chapters, and references.',
        fileId: 'sample-template',
        createdAt: Date.UTC(2026, 9, 12),
      },
      {
        id: 'd2',
        name: 'Research evaluation rubric',
        description: 'Review the criteria for content, methodology, and presentation.',
        fileId: 'sample-rubric',
        createdAt: Date.UTC(2026, 9, 10),
      },
      {
        id: 'd3',
        name: 'A guide to APA references',
        description: 'Examples to help students cite their sources consistently.',
        fileId: 'sample-references',
        createdAt: Date.UTC(2026, 9, 8),
      },
    ],
  }
}
