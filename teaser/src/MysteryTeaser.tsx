import {AbsoluteFill, Easing, Html5Audio, Img, interpolate, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {loadFont} from '@remotion/fonts';

loadFont({family: 'Inter', url: staticFile('fonts/InterVariable.woff2'), weight: '100 900'});

const ink = '#040912';
const ice = '#EAF0FF';
const blue = '#467DFF';
const clamp = {extrapolateLeft: 'clamp' as const, extrapolateRight: 'clamp' as const};
const ease = Easing.bezier(.16, 1, .3, 1);
const fade = (t: number, start: number, end: number, attack = .35, release = .3) =>
  interpolate(t, [start, start + attack, end - release, end], [0, 1, 1, 0], clamp);

// One continuous product shot. The complete native capture stays fitted inside
// the handset; lighting and the device angle create the reveal, never a crop.
function DeviceReveal({t}: {t: number}) {
  const {width, height} = useVideoConfig();
  const portrait = height > width;
  const visible = fade(t, 3.55, 7.18, .4, .45);
  const turn = interpolate(t, [3.55, 4.8, 6.15, 7.18], [76, 20, -9, -63], {...clamp, easing: Easing.inOut(Easing.cubic)});
  const tilt = interpolate(t, [3.55, 7.18], [-12, 8], clamp);
  const screenLight = interpolate(t, [3.55, 4.3, 4.85, 5.85, 6.45, 7.18], [0, 0, .82, .82, .35, 0], {...clamp, easing: Easing.inOut(Easing.quad)});
  const shine = interpolate(t, [3.7, 6.9], [-100, 170], clamp);
  const phoneHeight = portrait ? 1180 : 790;
  const phoneWidth = phoneHeight * 1344 / 2992;
  const radius = portrait ? 61 : 44;
  const depth = portrait ? 16 : 12;
  return <AbsoluteFill style={{opacity: visible}}>
    <div style={{position: 'absolute', left: '50%', top: '50%', width: portrait ? 1000 : 1500, height: portrait ? 1450 : 1000, transform: 'translate(-50%, -50%)', background: 'radial-gradient(ellipse, #14295765 0%, #0B17362A 36%, transparent 68%)'}}/>
    <div style={{position: 'absolute', left: '50%', top: `calc(50% + ${phoneHeight * .57}px)`, width: phoneWidth * 1.7, height: 34, borderRadius: '50%', background: '#1D438E', opacity: .28, filter: 'blur(28px)', transform: 'translate(-50%, -50%)'}}/>
    <div style={{position: 'absolute', left: '50%', top: '50%', width: phoneWidth + 22, height: phoneHeight + 22, transform: `translate(-50%, -50%) perspective(2400px) rotateX(7deg) rotateY(${turn}deg) rotateZ(${tilt}deg)`, transformStyle: 'preserve-3d'}}>
      <div style={{position: 'absolute', inset: 0, borderRadius: radius, background: '#080E19', border: '1px solid #364868', transform: `translateZ(-${depth}px)`}}/>
      <div style={{position: 'absolute', left: '100%', top: radius, bottom: radius, width: depth, transformOrigin: 'left center', transform: 'rotateY(90deg)', background: 'linear-gradient(90deg, #7184A0, #111B2B 28%, #080D17)'}}/>
      <div style={{position: 'absolute', right: '100%', top: radius, bottom: radius, width: depth, transformOrigin: 'right center', transform: 'rotateY(-90deg)', background: 'linear-gradient(90deg, #080D17, #203D65, #91ACD2)'}}/>
      <div style={{position: 'absolute', right: -3, top: '22%', width: 4, height: phoneHeight * .105, borderRadius: 4, background: 'linear-gradient(#7184A1, #172338)', transform: 'translateZ(-4px)'}}/>
      <div style={{position: 'absolute', right: -3, top: '36%', width: 4, height: phoneHeight * .065, borderRadius: 4, background: 'linear-gradient(#7184A1, #172338)', transform: 'translateZ(-4px)'}}/>
      <div style={{position: 'absolute', inset: 0, borderRadius: radius, padding: 2, background: 'linear-gradient(145deg, #BED5F3 0%, #33496C 17%, #101826 42%, #223E6B 67%, #8EAED9 91%, #273C5A)', boxShadow: '0 0 3px #789EDB70, 30px 45px 90px #00000088'}}>
        <div style={{position: 'absolute', inset: 2, borderRadius: radius - 2, background: '#02050B', overflow: 'hidden'}}>
          <div style={{position: 'absolute', inset: 9, borderRadius: radius - 10, overflow: 'hidden', background: '#080E19'}}>
            <Img src={staticFile('screens/research-dark.png')} style={{width: '100%', height: '100%', objectFit: 'contain', opacity: screenLight}}/>
            <AbsoluteFill style={{background: `linear-gradient(125deg, #01040CE8 0%, #01040C50 35%, transparent 60%, #02061150 100%)`, opacity: interpolate(t, [4.25, 5.1, 5.8, 6.65], [1, .18, .22, 1], clamp)}}/>
            <div style={{position: 'absolute', width: '85%', height: '160%', top: '-30%', left: `${shine}%`, transform: 'rotate(-22deg)', background: 'linear-gradient(90deg, transparent, #C5DFFF13 44%, #E7F1FF26 50%, #C5DFFF08 56%, transparent)', pointerEvents: 'none'}}/>
          </div>
          <div style={{position: 'absolute', width: portrait ? 14 : 10, height: portrait ? 14 : 10, borderRadius: '50%', left: '50%', top: portrait ? 24 : 19, transform: 'translateX(-50%)', background: '#010205', boxShadow: 'inset 0 0 2px #33415D, 0 0 0 2px #101725'}}/>
        </div>
      </div>
    </div>
  </AbsoluteFill>;
}

function Phrase({t, start, end, children, size}: {t: number; start: number; end: number; children: React.ReactNode; size: number}) {
  const p = interpolate(t, [start, start + .85], [0, 1], {...clamp, easing: ease});
  return <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: fade(t, start, end, .6, .4), transform: `translateY(${(1 - p) * 23}px) scale(${.985 + p * .015})`, filter: `blur(${(1 - p) * 9}px)`}}>
    <div style={{textAlign: 'center', color: ice, fontSize: size, fontWeight: 500, lineHeight: 1.13, letterSpacing: '-.055em'}}>{children}</div>
  </AbsoluteFill>;
}

export function MysteryTeaser() {
  const frame = useCurrentFrame();
  const {fps, width, height} = useVideoConfig();
  const t = frame / fps;
  const portrait = height > width;
  const numberIn = interpolate(t, [9.18, 10.4], [0, 1], {...clamp, easing: Easing.inOut(Easing.cubic)});
  const reveal = interpolate(t, [11.1, 11.9], [0, 1], {...clamp, easing: ease});
  const endFade = interpolate(t, [15.55, 16], [1, 0], clamp);
  return <AbsoluteFill style={{background: ink, fontFamily: 'Inter, sans-serif', overflow: 'hidden'}}>
    <Html5Audio src={staticFile('audio/crest-mystery.wav')}/>

    {/* A quiet sliver of light starts the film. No brand or UI labels yet. */}
    <div style={{position: 'absolute', left: '50%', top: '50%', width: interpolate(t, [0, 1.2, 3.7], [4, portrait ? 520 : 680, portrait ? 740 : 1000], clamp), height: 1, background: blue, opacity: fade(t, 0, 3.9, .5, .6) * .6, transform: `translate(-50%, ${portrait ? 240 : 190}px)`, boxShadow: '0 0 22px 2px #2457D644'}}/>
    <Phrase t={t} start={.65} end={3.65} size={portrait ? 110 : 112}>Something’s<br/>changing.</Phrase>

    <DeviceReveal t={t}/>

    <Phrase t={t} start={7.12} end={9.2} size={portrait ? 116 : 120}>Look closer.</Phrase>

    {/* A single version-number clue; a deliberate black beat precedes the reveal. */}
    <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: fade(t, 9.2, 10.6, .3, .18), transform: `scale(${1.15 - numberIn * .15})`, clipPath: `inset(${(1 - numberIn) * 49}% 0 ${(1 - numberIn) * 49}% 0)`}}>
      <div style={{fontSize: portrait ? 910 : 940, fontWeight: 600, lineHeight: 1, letterSpacing: '-.08em', paddingRight: '.08em', WebkitTextStroke: '2px #467DFF', color: '#0A1428'}}>2</div>
    </AbsoluteFill>

    <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: reveal * endFade, transform: `scale(${1.035 - reveal * .035})`}}>
      <div style={{position: 'absolute', width: portrait ? 750 : 1050, height: portrait ? 750 : 620, background: 'radial-gradient(ellipse, #16347330, transparent 68%)'}}/>
      <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', position: 'relative'}}>
        <Img src={staticFile('crest-logo.svg')} style={{width: portrait ? 100 : 88, height: portrait ? 100 : 88, marginBottom: portrait ? 46 : 34}}/>
        <div style={{display: 'flex', alignItems: 'center', gap: portrait ? 24 : 32}}>
          <span style={{color: ice, fontSize: portrait ? 114 : 128, fontWeight: 700, letterSpacing: '.055em', lineHeight: 1}}>CREST</span>
          <span style={{color: '#8CAFFF', fontSize: portrait ? 77 : 89, fontWeight: 400, letterSpacing: '-.06em', lineHeight: 1}}>V2</span>
        </div>
        <div style={{width: interpolate(t, [11.6, 12.7], [0, portrait ? 570 : 635], {...clamp, easing: ease}), height: 1, background: '#4064A1', marginTop: portrait ? 43 : 38}}/>
        <div style={{opacity: interpolate(t, [12.25, 12.8], [0, 1], clamp), color: '#AEBBD3', marginTop: portrait ? 43 : 37, fontSize: portrait ? 25 : 23, letterSpacing: '.33em', paddingLeft: '.33em'}}>COMING SOON</div>
      </div>
    </AbsoluteFill>
  </AbsoluteFill>;
}
