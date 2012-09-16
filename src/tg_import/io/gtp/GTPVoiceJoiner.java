package tg_import.io.gtp;

import tg_import.song.factory.TGFactory;
import tg_import.song.models.TGBeat;
import tg_import.song.models.TGDuration;
import tg_import.song.models.TGMeasure;
import tg_import.song.models.TGNote;
import tg_import.song.models.TGVoice;

public class GTPVoiceJoiner {
	private TGFactory factory;
	private TGMeasure measure;
	
	public GTPVoiceJoiner(TGFactory factory,TGMeasure measure){
		this.factory = factory;
		this.measure = measure.clone(factory, measure.getHeader());
		this.measure.setTrack( measure.getTrack() );
	}
	
	public TGMeasure process(){
		this.orderBeats();
		this.joinBeats();
		return this.measure;
	}
	
	public void joinBeats(){
		TGBeat previous = null;
		boolean finish = true;
		
		long measureStart = this.measure.getStart();
		long measureEnd = (measureStart + this.measure.getLength());
		for(int i = 0;i < this.measure.countBeats();i++){
			TGBeat beat = this.measure.getBeat( i );
			TGVoice voice = beat.getVoice(0);
			for(int v = 1; v < beat.countVoices(); v++ ){
				TGVoice currentVoice = beat.getVoice(v);
				if(!currentVoice.isEmpty()){
					for(int n = 0 ; n < currentVoice.countNotes() ; n++ ){
						TGNote note = currentVoice.getNote( n );
						voice.addNote( note );
					}
				}
			}
			if( voice.isEmpty() ){
				this.measure.removeBeat(beat);
				finish = false;
				break;
			}
			
			long beatStart = beat.getStart();
			if(previous != null){
				long previousStart = previous.getStart();
				
				TGDuration previousBestDuration = null;
				for(int v = /*1*/0; v < previous.countVoices(); v++ ){
					TGVoice previousVoice = previous.getVoice(v);
					if(!previousVoice.isEmpty()){
						long length = previousVoice.getDuration().getTime();
						if( (previousStart + length) <= beatStart){
							if( previousBestDuration == null || length > previousBestDuration.getTime() ){
								previousBestDuration = previousVoice.getDuration();
							}
						}
					}
				}
				
				if(previousBestDuration != null){
					previousBestDuration.copy( previous.getVoice(0).getDuration() );
				}else{
					if(voice.isRestVoice()){
						this.measure.removeBeat(beat);
						finish = false;
						break;
					}
					TGDuration duration = TGDuration.fromTime(this.factory, (beatStart - previousStart) );
					duration.copy( previous.getVoice(0).getDuration() );
				}
			}
			
			TGDuration beatBestDuration = null;
			for(int v = /*1*/0; v < beat.countVoices(); v++ ){
				TGVoice currentVoice = beat.getVoice(v);
				if(!currentVoice.isEmpty()){
					long length = currentVoice.getDuration().getTime();
					if( (beatStart + length) <= measureEnd ){
						if( beatBestDuration == null || length > beatBestDuration.getTime() ){
							beatBestDuration = currentVoice.getDuration();
						}
					}
				}
			}
			
			if(beatBestDuration == null){
				if(voice.isRestVoice()){
					this.measure.removeBeat(beat);
					finish = false;
					break;
				}
				TGDuration duration = TGDuration.fromTime(this.factory, (measureEnd - beatStart) );
				duration.copy( voice.getDuration() );
			}
			previous = beat;
		}
		if(!finish){
			joinBeats();
		}
	}
	
	public void orderBeats(){
		for(int i = 0;i < this.measure.countBeats();i++){
			TGBeat minBeat = null;
			for(int j = i;j < this.measure.countBeats();j++){
				TGBeat beat = this.measure.getBeat(j);
				if(minBeat == null || beat.getStart() < minBeat.getStart()){
					minBeat = beat;
				}
			}
			this.measure.moveBeat(i, minBeat);
		}
	}
}
