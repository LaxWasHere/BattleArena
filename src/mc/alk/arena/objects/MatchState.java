package mc.alk.arena.objects;



/**
 * @author alkarin
 *
 * Enum of MatchTransitions, and MatchStates
 */
public enum MatchState implements CompetitionState{
	NONE("None"), DEFAULTS("defaults"),
	PREREQS ("preReqs"), ONOPEN("onOpen"), ONJOIN ("onJoin"),
	ONBEGIN("onBegin"), ONPRESTART ("onPrestart"), ONSTART ("onStart"), ONVICTORY ("onVictory"),
	ONCOMPLETE ("onComplete"), ONCANCEL ("onCancel"), ONFINISH("onFinish"),
	ONDEATH ("onDeath"), ONSPAWN ("onSpawn"),
	WINNER ("winner"),LOSERS ("losers"),
	FIRSTPLACE ("firstPlace"), PARTICIPANTS("participants"),
	ONENTER("onEnter"), ONLEAVE("onLeave"), ONENTERWAITROOM("onEnterWaitRoom"),
	ONMATCHINTERVAL("onMatchInterval"), ONMATCHTIMEEXPIRED("onMatchTimeExpired"),
	ONCOUNTDOWNTOEVENT("onCountdownToEvent"),
	ONENTERQUEUE("onEnterQueue")
	;

	String name;
	MatchState(String name){
		this.name = name;
	}
	@Override
	public String toString(){
		return name;
	}
	public static MatchState fromName(String str){
		str = str.toUpperCase();
		return MatchState.valueOf(str);
	}
	public boolean isRunning() {
		return this == MatchState.ONSTART;
	}
}
