import {type ReactNode} from 'react';
import {AbsoluteFill, Easing, Html5Audio, Img, Sequence, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {loadFont} from '@remotion/fonts';

loadFont({family: 'Inter', url: staticFile('fonts/InterVariable.woff2'), weight: '100 900'});

export const announcementFrames = 720;

const ink = '#101D38';
const blue = '#2457D6';
const paper = '#F5F7FF';
const mint = '#A4F2DC';
const yellow = '#FFE382';
const clamp = {extrapolateLeft: 'clamp' as const, extrapolateRight: 'clamp' as const};

type Gallery = {
  front: string;
  back: string;
  color: string;
  detail: string;
  reverse?: boolean;
};

const introGallery: Gallery = {front: 'v2-student-home', back: 'v2-research-dark', color: '#1746BA', detail: mint};

const features = [
  {
    from: 90, label: 'Research library', lines: ['Find your', 'next idea.'],
    description: 'Search, read, and save school research papers.',
    background: paper, foreground: ink, accent: blue,
    gallery: {front: 'v2-research', back: 'v2-research-dark', color: '#C6D6FF', detail: blue},
  },
  {
    from: 210, label: 'Group workspace', lines: ['Submit as', 'a team.'],
    description: 'Work in your research group and submit a paper together.',
    background: '#0E2543', foreground: paper, accent: mint,
    gallery: {front: 'v2-student-home', back: 'groups', color: '#145F65', detail: mint, reverse: true},
  },
  {
    from: 330, label: 'Deadlines & milestones', lines: ['Stay on', 'track.'],
    description: 'See what’s due, from the proposal to the final manuscript.',
    background: mint, foreground: ink, accent: '#10636A',
    gallery: {front: 'milestones', back: 'inbox', color: '#D9FFF1', detail: '#10636A'},
  },
  {
    from: 450, label: 'Teacher workspace', lines: ['Review.', 'Give feedback.'],
    description: 'Review submissions and guide students through revisions.',
    background: '#FFECAB', foreground: ink, accent: '#6B4C00',
    gallery: {front: 'v2-teacher-home', back: 'groups', color: '#FFF6D9', detail: '#A06812', reverse: true},
  },
];

function Reveal({children, delay = 0, distance = 90}: {children: ReactNode; delay?: number; distance?: number}) {
  const frame = useCurrentFrame() - delay;
  const {fps} = useVideoConfig();
  const position = spring({frame, fps, config: {damping: 22, stiffness: 166, mass: .86}});
  return <div style={{overflow: 'hidden', paddingBottom: 6}}>
    <div style={{transform: `translateY(${(1 - position) * distance}px)`, opacity: interpolate(frame, [0, 10], [0, 1], clamp)}}>{children}</div>
  </div>;
}

function Brand({size = 60}: {size?: number}) {
  return <div style={{display: 'inline-flex', alignItems: 'center', gap: size * .25}}>
    <Img src={staticFile('crest-logo.svg')} style={{width: size, height: size, objectFit: 'contain'}}/>
    <span style={{fontSize: size * .63, fontWeight: 820, letterSpacing: '.035em'}}>CREST</span>
  </div>;
}

function BeatBars({color}: {color: string}) {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const beats = frame / fps * 2;
  const kick = Math.exp(-(beats % 1) * 6);
  const snare = Math.exp(-((beats + 1) % 2) * 7);
  const hats = Math.exp(-((beats + .5) % 1) * 6);
  const hits = [hats * .56, snare * .8, kick, hats * .72, kick * .48];

  return <div style={{position: 'absolute', left: 48, bottom: 46, height: 55, display: 'flex', alignItems: 'center', gap: 6, color}}>
    {hits.map((hit, index) => <span key={index} style={{width: 5, height: 8 + 39 * hit, borderRadius: 4, background: 'currentColor', opacity: .62 + hit * .38}}/>)}
  </div>;
}

function Phone({screen, width, x, y, tilt, delay, front}: {
  screen: string; width: number; x: number; y: number; tilt: number; delay: number; front?: boolean;
}) {
  const frame = useCurrentFrame() - delay;
  const {fps} = useVideoConfig();
  const lift = spring({frame, fps, config: {damping: 18, stiffness: 114, mass: .9}});
  const drift = Math.sin(frame / 34) * (front ? 8 : -7);
  return <div style={{
    position: 'absolute', left: x, top: y, width, padding: 8, boxSizing: 'content-box',
    background: '#0A1325', border: '2px solid #586982', borderRadius: 37,
    boxShadow: '0 22px 30px #09172E45, 0 5px 8px #09172E60',
    opacity: interpolate(frame, [0, 12], [0, 1], clamp),
    transform: `translateY(${(1 - lift) * 200 + drift}px) rotate(${tilt + (1 - lift) * -12}deg) scale(${.91 + lift * .09})`,
  }}>
    <Img src={staticFile(`screens/${screen}.png`)} style={{display: 'block', width: '100%', borderRadius: 27, aspectRatio: '1344 / 2992', objectFit: 'cover'}}/>
  </div>;
}

function ProductGallery({gallery, portrait}: {gallery: Gallery; portrait: boolean}) {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const open = spring({frame, fps, config: {damping: 24, stiffness: 125}});
  const frontX = gallery.reverse ? (portrait ? 185 : 135) : (portrait ? 394 : 392);
  const backX = gallery.reverse ? (portrait ? 512 : 471) : (portrait ? 134 : 106);

  return <div style={{
    position: 'absolute', left: portrait ? 82 : 975, top: portrait ? 735 : 84,
    width: portrait ? 916 : 862, height: portrait ? 980 : 912,
    borderRadius: portrait ? 56 : 49, overflow: 'hidden', background: gallery.color,
    transform: `translateX(${portrait ? 0 : (1 - open) * 94}px) translateY(${portrait ? (1 - open) * 102 : 0}px)`,
    opacity: interpolate(frame, [0, 11], [0, 1], clamp),
  }}>
    <AbsoluteFill style={{
      backgroundImage: `radial-gradient(${gallery.detail} 1.3px, transparent 1.65px)`,
      backgroundSize: '21px 21px', opacity: .23,
      maskImage: 'linear-gradient(135deg, #000 8%, #0000 65%, #000 100%)',
    }}/>
    <div style={{position: 'absolute', width: 1040, height: 410, top: 235, left: -93, transform: `rotate(${-31 + Math.sin(frame / 55) * 4}deg)`, color: gallery.detail}}>
      {[0, 33, 66].map((inset) => <div key={inset} style={{position: 'absolute', inset, border: '2px solid currentColor', borderRadius: '50%', opacity: .38}}/>)}
    </div>
    <div style={{position: 'absolute', right: 42, top: 38, color: gallery.detail, fontSize: 52, lineHeight: 1, fontWeight: 450}}>✦</div>
    <BeatBars color={gallery.detail}/>
    <Phone screen={gallery.back} width={portrait ? 288 : 268} x={backX} y={portrait ? 164 : 132} tilt={gallery.reverse ? 13 : -13} delay={7}/>
    <Phone screen={gallery.front} width={portrait ? 361 : 332} x={frontX} y={portrait ? 76 : 65} tilt={gallery.reverse ? -7 : 7} delay={12} front/>
  </div>;
}

function Intro() {
  const {width, height, fps} = useVideoConfig();
  const portrait = height > width;
  const frame = useCurrentFrame();
  const tag = spring({frame: frame - 13, fps, config: {damping: 12, stiffness: 190}});
  const bounce = Math.exp(-((frame / fps * 2) % 1) * 7) * .055;
  return <AbsoluteFill style={{background: blue, color: paper}}>
    <ProductGallery gallery={introGallery} portrait={portrait}/>
    <div style={{position: 'absolute', left: portrait ? 102 : 116, top: portrait ? 217 : 250}}>
      <div style={{display: 'flex', alignItems: 'center', gap: 30}}>
        <Reveal distance={128}><span style={{fontSize: portrait ? 158 : 192, lineHeight: .98, fontWeight: 900, letterSpacing: '-.057em'}}>CREST</span></Reveal>
        <div style={{
          fontSize: portrait ? 56 : 65, fontWeight: 880, letterSpacing: '-.06em', color: ink, background: mint,
          padding: '11px 24px', borderRadius: 17, transform: `rotate(-9deg) scale(${Math.max(.001, tag) * (1 + bounce)})`,
        }}>V2</div>
      </div>
      <div style={{fontSize: portrait ? 79 : 84, fontWeight: 730, lineHeight: 1, letterSpacing: '-.047em', wordSpacing: '.032em', marginTop: 27}}>
        <Reveal delay={12}>School research,</Reveal>
        <Reveal delay={19}><span style={{color: mint}}>reimagined.</span></Reveal>
      </div>
    </div>
  </AbsoluteFill>;
}

function Feature({feature}: {feature: typeof features[number]}) {
  const {width, height} = useVideoConfig();
  const portrait = height > width;
  return <AbsoluteFill style={{background: feature.background, color: feature.foreground}}>
    <ProductGallery gallery={feature.gallery} portrait={portrait}/>
    <div style={{position: 'absolute', left: portrait ? 104 : 116, top: portrait ? 203 : 265, width: portrait ? 860 : 839}}>
      <Reveal delay={3} distance={40}>
        <div style={{display: 'flex', alignItems: 'center', gap: 15, fontSize: portrait ? 27 : 23, letterSpacing: '.105em', textTransform: 'uppercase', fontWeight: 810, color: feature.accent}}>
          <span style={{width: 26, height: 4, borderRadius: 8, background: 'currentColor'}}/>{feature.label}
        </div>
      </Reveal>
      <div style={{fontSize: portrait ? 122 : 126, lineHeight: 1, letterSpacing: '-.054em', wordSpacing: '.035em', fontWeight: 860, marginTop: 27}}>
        <Reveal delay={7} distance={120}>{feature.lines[0]}</Reveal>
        <Reveal delay={13} distance={120}><span style={{color: feature.accent}}>{feature.lines[1]}</span></Reveal>
      </div>
      <div style={{fontSize: portrait ? 39 : 35, fontWeight: 520, lineHeight: 1.34, marginTop: 27, maxWidth: portrait ? 760 : 680}}>
        <Reveal delay={23} distance={45}>{feature.description}</Reveal>
      </div>
    </div>
  </AbsoluteFill>;
}

function Finale() {
  const frame = useCurrentFrame();
  const {width, height, fps} = useVideoConfig();
  const portrait = height > width;
  const burst = spring({frame: frame - 5, fps, config: {damping: 20, stiffness: 65, mass: 1.35}});
  const rings = portrait ? 1140 : 1100;
  return <AbsoluteFill style={{background: '#0B2260', color: paper, justifyContent: 'center', alignItems: 'center'}}>
    <div style={{position: 'absolute', width: rings, height: rings, left: (width - rings) / 2, top: (height - rings) / 2, transform: `rotate(-24deg) scale(${.65 + .35 * burst})`, opacity: .82}}>
      {[0, 51, 102, 153].map((inset, index) => <div key={inset} style={{position: 'absolute', inset, border: `2px solid ${index % 2 ? '#3965D8' : '#5D89FA'}`, borderRadius: '50%', transform: 'scaleX(1.48)'}}/>)}
      <div style={{position: 'absolute', left: portrait ? 155 : 0, top: 159, color: mint, fontSize: 68}}>✦</div>
      <div style={{position: 'absolute', right: portrait ? 142 : -74, bottom: 244, color: yellow, fontSize: 48}}>✦</div>
    </div>
    <div style={{position: 'relative', width: portrait ? 900 : 1448, textAlign: 'center', padding: portrait ? '54px 30px 62px' : '42px 50px 50px', border: '2px solid #5D89FA', borderRadius: 54, background: '#102B70', boxSizing: 'border-box'}}>
      <Reveal delay={7}><Brand size={portrait ? 74 : 67}/></Reveal>
      <div style={{fontSize: portrait ? 145 : 204, fontWeight: 900, letterSpacing: '-.062em', wordSpacing: '.04em', lineHeight: 1.04, margin: portrait ? '25px 0 18px' : '15px 0 10px'}}>
        <Reveal delay={12} distance={150}><span style={{color: mint}}>V2</span> is here.</Reveal>
      </div>
      <div style={{fontSize: portrait ? 43 : 47, fontWeight: 690, letterSpacing: '-.035em'}}><Reveal delay={24}>School research, shared.</Reveal></div>
      <div style={{marginTop: portrait ? 24 : 20, fontSize: portrait ? 26 : 27, fontWeight: 460, color: '#C7D7FF'}}><Reveal delay={33}>Bonifacio D. Borebor Sr. High School</Reveal></div>
    </div>
  </AbsoluteFill>;
}

function Wipe({color, reverse}: {color: string; reverse: boolean}) {
  const frame = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const overhang = Math.ceil(height * .14);
  const distance = width + overhang * 2;
  const travel = interpolate(frame, [0, 8, 17], [-distance, 0, distance], {...clamp, easing: Easing.inOut(Easing.quad)});
  return <div style={{position: 'absolute', inset: `0 -${overhang}px`, transform: `translateX(${reverse ? -travel : travel}px) skewX(-12deg)`, background: color, borderInline: '32px solid #F5F7FF', boxSizing: 'border-box'}}/>;
}

export function Announcement() {
  const cuts = [90, 210, 330, 450, 570];
  return <AbsoluteFill style={{fontFamily: 'Inter, sans-serif', overflow: 'hidden', background: blue}}>
    <Html5Audio src={staticFile('audio/crest-announcement.mp3')}/>
    <Sequence durationInFrames={90} name="CREST V2"><Intro/></Sequence>
    {features.map((feature) => <Sequence key={feature.from} from={feature.from} durationInFrames={120} name={feature.label} premountFor={16}><Feature feature={feature}/></Sequence>)}
    <Sequence from={570} durationInFrames={150} name="V2 is here" premountFor={16}><Finale/></Sequence>
    {cuts.map((cut, i) => <Sequence key={cut} from={cut - 8} durationInFrames={18} name="Wipe"><Wipe color={i % 2 ? mint : blue} reverse={i % 2 === 1}/></Sequence>)}
  </AbsoluteFill>;
}
