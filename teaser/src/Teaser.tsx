import React, {type CSSProperties} from 'react';
import {AbsoluteFill, Easing, Html5Audio, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {loadFont} from '@remotion/fonts';
import {TransitionSeries, linearTiming} from '@remotion/transitions';
import {fade} from '@remotion/transitions/fade';

loadFont({family: 'Inter', url: staticFile('fonts/InterVariable.woff2'), weight: '100 900'});

const C = {bg: '#101C30', white: '#F5F7FB', muted: '#A8B5CA', blue: '#2457D6', pale: '#AEC5FF', teal: '#86D5D1', line: '#344259'};
const clamp = {extrapolateLeft: 'clamp' as const, extrapolateRight: 'clamp' as const};
const starts = [0, 90, 180, 330, 480, 630, 750];
const lengths = [102, 102, 162, 162, 162, 132, 150];
const labels = ['A NEW CHAPTER', 'MEET V2', 'DISCOVER', 'TOGETHER', 'STAY ON TRACK', 'YOUR RESEARCH', 'COMING SOON'];
const enter = (frame: number, fps: number, delay = 0) => spring({frame: frame - delay, fps, config: {damping: 24, stiffness: 150, mass: .8}});

function Reveal({children, delay = 0, style}: {children: React.ReactNode; delay?: number; style?: CSSProperties}) {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const p = enter(frame, fps, delay);
  return <div style={{opacity: interpolate(frame - delay, [0, 12], [0, 1], clamp), transform: `translateY(${(1 - p) * 44}px)`, ...style}}>{children}</div>;
}

function Icon({name, size = 28, color = C.teal}: {name: 'search' | 'book' | 'group' | 'check' | 'bell' | 'calendar' | 'arrow' | 'heart'; size?: number; color?: string}) {
  const paths: Record<string, React.ReactNode> = {
    search: <><circle cx="10" cy="10" r="6.5"/><path d="m15 15 6 6"/></>,
    book: <><path d="M3 4h7l2 2 2-2h7v16h-7l-2 1-2-1H3zM12 6v15"/><path d="M6 9h3m-3 4h3m6-4h3m-3 4h3"/></>,
    group: <><circle cx="12" cy="7" r="3"/><path d="M6 21v-3a6 6 0 0 1 12 0v3M3 9a3 3 0 0 1 3-3m15 3a3 3 0 0 0-3-3M2 20v-3a4 4 0 0 1 2-3m18 6v-3a4 4 0 0 0-2-3"/></>,
    check: <path d="m4 12 5 5L20 6"/>,
    bell: <><path d="M5 17h14l-2-3V9a5 5 0 0 0-10 0v5zM10 21h4M12 2v2"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M7 2v6m10-6v6M3 11h18m-13 4h2m4 0h2"/></>,
    arrow: <path d="M3 12h17m-6-6 6 6-6 6"/>,
    heart: <path d="M12 21 3 12C-3 4 6-1 12 6c6-7 15-2 9 6z"/>,
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>;
}

function Eyebrow({children, delay = 0}: {children: React.ReactNode; delay?: number}) {
  return <Reveal delay={delay} style={{display: 'flex', gap: 18, alignItems: 'center', color: C.teal, fontSize: 21, letterSpacing: 3, fontWeight: 600}}><span style={{height: 2, width: 44, background: C.teal}}/>{children}</Reveal>;
}

function Heading({lines, size, delay = 5}: {lines: React.ReactNode[]; size: number; delay?: number}) {
  return <div style={{fontSize: size, fontWeight: 700, letterSpacing: '-.05em', lineHeight: 1.08}}>{lines.map((line, i) => <Reveal key={i} delay={delay + i * 7}>{line}</Reveal>)}</div>;
}

function Features({items, delay = 35}: {items: [React.ComponentProps<typeof Icon>['name'], string][]; delay?: number}) {
  return <div style={{display: 'flex', gap: 28, flexWrap: 'wrap'}}>{items.map(([icon, text], i) => <Reveal key={text} delay={delay + i * 6} style={{display: 'flex', gap: 10, alignItems: 'center', color: C.muted, fontSize: 23}}><Icon name={icon} size={25}/>{text}</Reveal>)}</div>;
}

function Phone({screen, x, y, height, angle = 0, delay = 8, opacity = 1}: {screen: string; x: number; y: number; height: number; angle?: number; delay?: number; opacity?: number}) {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const p = enter(frame, fps, delay);
  const width = height * 1344 / 2992;
  const lift = Math.sin(frame / fps * .85) * 3;
  return <div style={{position: 'absolute', left: x, top: y, width: width + 20, height: height + 20, opacity: interpolate(frame - delay, [0, 14], [0, opacity], clamp), transform: `perspective(2000px) translateY(${(1 - p) * 150 + lift}px) rotate(${angle}deg) scale(${.94 + p * .06})`, borderRadius: 43, padding: 8, background: '#07101D', border: '2px solid #617088', boxShadow: '0 28px 65px #00000045', boxSizing: 'content-box'}}>
    <div style={{width, height, borderRadius: 33, overflow: 'hidden', background: '#F5F7FB'}}><Img src={staticFile(`screens/${screen}.png`)} style={{width: '100%', height: '100%', objectFit: 'fill'}}/></div>
  </div>;
}

function Tag({icon, title, text, x, y, delay = 20, width = 350}: {icon: React.ComponentProps<typeof Icon>['name']; title: string; text: string; x: number; y: number; delay?: number; width?: number}) {
  return <Reveal delay={delay} style={{position: 'absolute', left: x, top: y, width, background: '#17273F', border: `1px solid ${C.line}`, borderLeft: `3px solid ${C.teal}`, borderRadius: 16, padding: '23px 26px', boxShadow: '0 15px 35px #00000026'}}>
    <div style={{display: 'flex', alignItems: 'center', gap: 15}}><Icon name={icon} size={28}/><span style={{fontSize: 24, fontWeight: 600}}>{title}</span></div>
    <div style={{color: C.muted, fontSize: 20, lineHeight: 1.5, marginTop: 11}}>{text}</div>
  </Reveal>;
}

function Background({index}: {index: number}) {
  const {width, height} = useVideoConfig();
  const frame = useCurrentFrame();
  const portrait = height > width;
  return <AbsoluteFill style={{background: C.bg, overflow: 'hidden'}}>
    <div style={{position: 'absolute', right: -20, top: portrait ? 560 : 60, fontSize: portrait ? 750 : 950, fontWeight: 750, color: '#FFFFFF04', letterSpacing: '-.09em', lineHeight: 1}}>02</div>
    <svg width={width} height={height} style={{position: 'absolute', opacity: .48}}>
      <path d={`M${width * .59} 140 H${width - 75} V${height - 140} H${width * .59}`} stroke="#35445B" strokeWidth="1" fill="none"/>
      <path d={`M${width * .59 + 16} 156 H${width - 91} V${height - 156}`} stroke="#25364E" strokeWidth="1" fill="none"/>
      {[.25, .5, .75].map((v) => <path key={v} d={`M${width - 83} ${height * v}h16m-8-8v16`} stroke="#6D849F" strokeWidth="1"/>)}
    </svg>
    <div style={{position: 'absolute', left: portrait ? 72 : 92, right: portrait ? 72 : 92, top: portrait ? 76 : 54, display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
      <div style={{display: 'flex', alignItems: 'center', gap: 14}}><Img src={staticFile('crest-logo.svg')} style={{width: 42, height: 42}}/><span style={{fontWeight: 700, fontSize: 26, letterSpacing: 3}}>CREST</span></div>
      <div style={{fontSize: 16, letterSpacing: 3, color: C.muted}}>V2 / FIRST LOOK</div>
    </div>
    <div style={{position: 'absolute', left: portrait ? 72 : 92, right: portrait ? 72 : 92, bottom: portrait ? 72 : 47}}>
      <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 21, color: C.muted, fontSize: 14, letterSpacing: 2.2}}><span>{labels[index]}</span><span>{String(index + 1).padStart(2, '0')} / 07</span></div>
      <div style={{display: 'flex', gap: 9}}>{starts.map((from, i) => <div key={from} style={{flex: 1, height: 2, background: '#354157'}}><div style={{height: '100%', width: `${i < index ? 100 : i > index ? 0 : interpolate(frame, [0, lengths[index]], [0, 100], clamp)}%`, background: i === index ? C.teal : C.pale}}/></div>)}</div>
    </div>
  </AbsoluteFill>;
}

function Intro() {
  const {width, height, fps} = useVideoConfig();
  const frame = useCurrentFrame();
  const portrait = height > width;
  return <AbsoluteFill><Background index={0}/>
    <div style={{position: 'absolute', left: portrait ? 80 : 180, top: portrait ? 490 : 250}}>
      <Eyebrow>A NEW CHAPTER IN SCHOOL RESEARCH</Eyebrow>
      <div style={{marginTop: 45}}><Heading size={portrait ? 112 : 126} lines={['Every idea.', <span style={{color: C.pale}}>A new chapter.</span>]}/></div>
      <Reveal delay={32} style={{fontSize: portrait ? 30 : 29, color: C.muted, marginTop: 38}}>Built for students. Made for teachers.</Reveal>
      <div style={{height: 6, marginTop: 50, background: C.blue, width: interpolate(frame, [22, 2.2 * fps], [0, portrait ? 350 : 490], {...clamp, easing: Easing.out(Easing.cubic)})}}/>
    </div>
    <Reveal delay={22} style={{position: 'absolute', right: portrait ? 95 : 160, top: portrait ? 1050 : 290, fontSize: portrait ? 280 : 250, fontWeight: 700, letterSpacing: '-.09em', lineHeight: 1, WebkitTextStroke: '1.5px #6B86B2', color: 'transparent'}}>02</Reveal>
  </AbsoluteFill>;
}

function Meet() {
  const {width, height} = useVideoConfig();
  const p = height > width;
  return <AbsoluteFill><Background index={1}/>
    <div style={{position: 'absolute', left: p ? 80 : 130, top: p ? 265 : 280}}>
      <Eyebrow>INTRODUCING</Eyebrow>
      <div style={{marginTop: 26}}><Heading size={p ? 138 : 151} lines={['CREST', <span style={{color: C.pale}}>V2.</span>]}/></div>
      <Reveal delay={26} style={{fontSize: 30, color: C.muted, marginTop: 24}}>Your research. More connected.</Reveal>
    </div>
    <Phone screen="research-dark" x={p ? 140 : 1040} y={p ? 900 : 150} height={p ? 650 : 690} angle={-9} delay={5}/>
    <Phone screen="student-home" x={p ? 500 : 1340} y={p ? 785 : 195} height={p ? 790 : 760} angle={5} delay={13}/>
    <Reveal delay={35} style={{position: 'absolute', left: p ? 100 : 130, top: p ? 1640 : 855, display: 'flex', alignItems: 'center', gap: 18, color: C.teal, fontSize: 20, letterSpacing: 2}}><span style={{width: 8, height: 8, background: C.teal, borderRadius: '50%'}}/>A FIRST LOOK AT WHAT’S NEXT</Reveal>
  </AbsoluteFill>;
}

function Discover() {
  const {width, height} = useVideoConfig();
  const p = height > width;
  return <AbsoluteFill><Background index={2}/>
    <div style={{position: 'absolute', left: p ? 80 : 115, top: p ? 240 : 265, width: p ? 900 : 780}}>
      <Eyebrow>01 / THE RESEARCH LIBRARY</Eyebrow>
      <div style={{marginTop: 33}}><Heading size={p ? 102 : 100} lines={['Find your', <span style={{color: C.pale}}>next insight.</span>]}/></div>
      <Reveal delay={25} style={{fontSize: 29, lineHeight: 1.55, color: C.muted, maxWidth: p ? 820 : 650, marginTop: 34}}>Explore papers from your school.<br/>Read, save, and build on what came before.</Reveal>
      <div style={{marginTop: 35}}><Features items={[["search", 'Search'], ['book', 'Read papers'], ['heart', 'Favorites']]}/></div>
    </div>
    <Phone screen="paper-details" x={p ? 140 : 1000} y={p ? 925 : 265} height={p ? 690 : 645} angle={-7} delay={17} opacity={.83}/>
    <Phone screen="research" x={p ? 465 : 1330} y={p ? 825 : 140} height={p ? 815 : 790} angle={3} delay={7}/>
  </AbsoluteFill>;
}

function Together() {
  const {width, height} = useVideoConfig();
  const p = height > width;
  return <AbsoluteFill><Background index={3}/>
    <div style={{position: 'absolute', left: p ? 80 : 115, top: p ? 240 : 245, width: p ? 900 : 780}}>
      <Eyebrow>02 / BETTER, TOGETHER</Eyebrow>
      <div style={{marginTop: 33}}><Heading size={p ? 102 : 100} lines={['Your group.', <span style={{color: C.pale}}>Your next step.</span>]}/></div>
      <Reveal delay={25} style={{fontSize: 29, lineHeight: 1.55, color: C.muted, maxWidth: 720, marginTop: 34}}>Submit as a team.<br/>Move forward with teacher feedback.</Reveal>
      <div style={{marginTop: 35}}><Features items={[["group", 'Group workspace'], ['check', 'Teacher reviews']]}/></div>
    </div>
    <Phone screen="teacher-home" x={p ? 130 : 1000} y={p ? 940 : 255} height={p ? 680 : 660} angle={-7} delay={16} opacity={.86}/>
    <Phone screen="student-home" x={p ? 465 : 1335} y={p ? 835 : 155} height={p ? 810 : 780} angle={3} delay={7}/>
    <Tag icon="group" title="One shared journey" text="Group  →  Submit  →  Review" x={p ? 88 : 835} y={p ? 1515 : 725} width={p ? 390 : 365} delay={45}/>
  </AbsoluteFill>;
}

function Planning() {
  const {width, height} = useVideoConfig();
  const frame = useCurrentFrame();
  const p = height > width;
  const inbox = frame >= 85;
  return <AbsoluteFill><Background index={4}/>
    <div style={{position: 'absolute', left: p ? 80 : 115, top: p ? 240 : 250, width: p ? 900 : 800}}>
      <Eyebrow>03 / KEEP MOVING FORWARD</Eyebrow>
      <div style={{marginTop: 33}}><Heading size={p ? 102 : 100} lines={['Big goals.', <span style={{color: C.pale}}>Clear next steps.</span>]}/></div>
      <Reveal delay={25} style={{fontSize: 29, lineHeight: 1.55, color: C.muted, maxWidth: 690, marginTop: 34}}>Track research milestones.<br/>Keep submissions and feedback in view.</Reveal>
      <div style={{marginTop: 35}}><Features items={[["calendar", 'Milestones'], ['bell', 'Notification inbox']]}/></div>
    </div>
    <Phone screen="milestones" x={p ? 130 : 1005} y={p ? 940 : 255} height={p ? 690 : 660} angle={-7} delay={15} opacity={.86}/>
    <Phone screen="inbox" x={p ? 470 : 1335} y={p ? 835 : 150} height={p ? 815 : 790} angle={3} delay={7}/>
    <Tag key={String(inbox)} icon={inbox ? 'bell' : 'calendar'} title={inbox ? 'Feedback, in one place' : 'A plan for every stage'} text={inbox ? 'Submissions · Revisions · Approvals' : 'Proposal · Chapters · Defense'} x={p ? 82 : 835} y={p ? 1530 : 730} width={p ? 420 : 390} delay={inbox ? 0 : 35}/>
  </AbsoluteFill>;
}

function Journey() {
  const {width, height, fps} = useVideoConfig();
  const frame = useCurrentFrame();
  const p = height > width;
  const progress = interpolate(frame, [.8 * fps, 3.5 * fps], [0, 1], clamp);
  const steps = ['Discover', 'Collaborate', 'Submit', 'Grow'];
  return <AbsoluteFill><Background index={5}/>
    <div style={{position: 'absolute', left: p ? 80 : 145, top: p ? 420 : 230, right: p ? 80 : 100}}>
      <Eyebrow>EVERY STAGE. ONE PLACE.</Eyebrow>
      <div style={{marginTop: 38}}><Heading size={p ? 102 : 110} lines={['From first question.', <span style={{color: C.pale}}>To final manuscript.</span>]}/></div>
      <Reveal delay={30} style={{fontSize: 29, lineHeight: 1.6, marginTop: 36, color: C.muted}}>For the ideas that deserve to go further.</Reveal>
    </div>
    <div style={{position: 'absolute', left: p ? 100 : 150, right: p ? 100 : 150, top: p ? 1120 : 740, display: 'flex', flexDirection: p ? 'column' : 'row', gap: p ? 52 : 0, justifyContent: 'space-between'}}>
      <div style={{position: 'absolute', left: p ? 23 : 0, right: p ? undefined : 0, top: p ? 18 : 25, width: p ? 2 : undefined, height: p ? 330 : 2, background: C.line}}/>
      <div style={{position: 'absolute', left: p ? 23 : 0, top: p ? 18 : 25, width: p ? 2 : `${progress * 100}%`, height: p ? 330 * progress : 2, background: C.teal}}/>
      {steps.map((step, i) => <Reveal key={step} delay={24 + i * 13} style={{position: 'relative', display: 'flex', flexDirection: p ? 'row' : 'column', gap: 22, alignItems: p ? 'center' : i === 0 ? 'flex-start' : i === 3 ? 'flex-end' : 'center'}}><div style={{width: 48, height: 48, borderRadius: 14, background: progress >= i / 3 ? C.teal : C.bg, border: `1px solid ${progress >= i / 3 ? C.teal : C.line}`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 19, color: progress >= i / 3 ? C.bg : C.muted, fontWeight: 600}}>{String(i + 1).padStart(2, '0')}</div><span style={{fontSize: 27, color: C.white}}>{step}</span></Reveal>)}
    </div>
  </AbsoluteFill>;
}

function Outro() {
  const {width, height} = useVideoConfig();
  const p = height > width;
  return <AbsoluteFill><Background index={6}/>
    <div style={{position: 'absolute', left: p ? 80 : 130, right: p ? 80 : 130, top: p ? 390 : 200, textAlign: 'center'}}>
      <Reveal style={{display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 30}}><Img src={staticFile('crest-logo.svg')} style={{width: p ? 100 : 94, height: p ? 100 : 94}}/><span style={{fontSize: p ? 76 : 75, fontWeight: 700, letterSpacing: 7}}>CREST</span><span style={{fontSize: 24, padding: '10px 17px', borderRadius: 10, background: C.blue, letterSpacing: 1}}>V2</span></Reveal>
      <div style={{marginTop: p ? 70 : 42}}><Heading size={p ? 116 : 105} delay={10} lines={['School research,', <span style={{color: C.pale}}>shared.</span>]}/></div>
      <Reveal delay={36} style={{marginTop: p ? 68 : 38, display: 'flex', justifyContent: 'center'}}><div style={{display: 'flex', gap: 18, alignItems: 'center', border: '1px solid #738CB5', borderRadius: 12, padding: '18px 30px', fontSize: 22, color: C.white, letterSpacing: 4}}>COMING SOON<Icon name="arrow" size={27}/></div></Reveal>
      <Reveal delay={45} style={{marginTop: p ? 65 : 36, fontSize: p ? 22 : 21, lineHeight: 1.8, color: C.muted}}>Bonifacio D. Borebor Sr. High School</Reveal>
      <Reveal delay={52} style={{marginTop: 8, fontSize: 17, color: '#8392AA'}}>Created by James Ryan S. Gallego</Reveal>
    </div>
  </AbsoluteFill>;
}

export function Teaser() {
  const scenes = [Intro, Meet, Discover, Together, Planning, Journey, Outro];
  return <AbsoluteFill style={{fontFamily: 'Inter, sans-serif', color: C.white, background: C.bg}}>
    <Html5Audio src={staticFile('audio/crest-original.wav')} volume={.85}/>
    <TransitionSeries>{scenes.map((Scene, i) => <React.Fragment key={labels[i]}>
      {i > 0 && <TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: 12})}/>}
      <TransitionSeries.Sequence durationInFrames={lengths[i]} premountFor={30}><Scene/></TransitionSeries.Sequence>
    </React.Fragment>)}</TransitionSeries>
  </AbsoluteFill>;
}
