import {Composition} from 'remotion';
import {Announcement, announcementFrames} from './Announcement';
import {MysteryTeaser} from './MysteryTeaser';

export const Root = () => <>
  <Composition id="CREST-V2-Cinematic" component={MysteryTeaser} durationInFrames={480} fps={30} width={1920} height={1080}/>
  <Composition id="CREST-V2-Cinematic-Vertical" component={MysteryTeaser} durationInFrames={480} fps={30} width={1080} height={1920}/>
  <Composition id="CREST-V2-Announcement" component={Announcement} durationInFrames={announcementFrames} fps={30} width={1920} height={1080}/>
  <Composition id="CREST-V2-Announcement-Vertical" component={Announcement} durationInFrames={announcementFrames} fps={30} width={1080} height={1920}/>
</>;
