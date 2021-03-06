package mc.alk.arena.controllers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.competition.events.Event;
import mc.alk.arena.events.events.EventFinishedEvent;
import mc.alk.arena.executors.EventExecutor;
import mc.alk.arena.executors.ReservedArenaEventExecutor;
import mc.alk.arena.executors.TournamentExecutor;
import mc.alk.arena.listeners.ArenaListener;
import mc.alk.arena.objects.EventParams;
import mc.alk.arena.objects.events.MatchEventHandler;
import mc.alk.arena.objects.exceptions.InvalidEventException;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.pairs.EventPair;
import mc.alk.arena.util.Log;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class EventScheduler implements Runnable, ArenaListener{

	int curEvent = 0;
	Long delay = 5L;
	boolean continuous= false;
	boolean running = false;
	boolean stop = false;
	Integer currentTimer = null;

	final CopyOnWriteArrayList<EventPair> events = new CopyOnWriteArrayList<EventPair>();

	public boolean scheduleEvent(EventParams eventParams, String[] args) {
		events.add(new EventPair(eventParams,args)); /// TODO verify these arguments here instead of waiting until running them
		return true;
	}

	@Override
	public void run() {
		if (events.isEmpty() || stop)
			return;
		running = true;
		int index = curEvent % events.size();
		curEvent++;
		currentTimer = Bukkit.getScheduler().scheduleSyncDelayedTask(BattleArena.getSelf(), new RunEvent(this, events.get(index)));
	}

	public class RunEvent implements Runnable{
		final EventPair eventPair;
		final EventScheduler scheduler;
		public RunEvent(EventScheduler scheduler, EventPair eventPair) {
			this.eventPair = eventPair;
			this.scheduler = scheduler;
		}
		@Override
		public void run() {
			if (stop)
				return;
			EventExecutor ee = EventController.getEventExecutor(eventPair.getEventParams().getName());
			if (ee == null){
				Log.err("executor for " + eventPair.getEventParams() +" was not found");
				return;
			}
			CommandSender sender = Bukkit.getConsoleSender();
//			CommandSender sender = ColouredConsoleSender.getInstance();
			EventParams eventParams = eventPair.getEventParams();
			//			event.addTransitionListener(scheduler);
			String args[] = eventPair.getArgs();
			//			boolean success = false;
			Event event = null;
			try {
				if (ee instanceof ReservedArenaEventExecutor){
					ReservedArenaEventExecutor exe = (ReservedArenaEventExecutor) ee;
					event = exe.openIt(eventParams, args);
				} else if (ee instanceof TournamentExecutor){
					TournamentExecutor exe = (TournamentExecutor) ee;
					event = exe.openIt(sender, eventParams, args);
				}
			} catch (InvalidEventException e) {
				/** do nothing */
			} catch (InvalidOptionException e) {
				/** do nothing */
			} catch (Exception e){
				e.printStackTrace();
			}
			if (event != null){
				event.addArenaListener(scheduler);
			} else {  /// wait then start up the scheduler again in x seconds
				currentTimer = Bukkit.getScheduler().scheduleAsyncDelayedTask(BattleArena.getSelf(),
						scheduler, 20L*Defaults.TIME_BETWEEN_SCHEDULED_EVENTS);
			}
		}
	}

	@MatchEventHandler
	public void onEventFinished(EventFinishedEvent event){
		Event e = event.getEvent();
		e.removeArenaListener(this);
		if (continuous){
			/// Wait x sec then start the next event
			Bukkit.getScheduler().scheduleAsyncDelayedTask(BattleArena.getSelf(), this, (long) (20L*Defaults.TIME_BETWEEN_SCHEDULED_EVENTS*Defaults.TICK_MULT));
			if (Defaults.SCHEDULER_ANNOUNCE_TIMETILLNEXT){
				Bukkit.getServer().broadcastMessage(ChatColor.GOLD+"Next event will start in "+Defaults.TIME_BETWEEN_SCHEDULED_EVENTS+" seconds");}
		} else {
			running = false;
		}
	}

	public boolean isRunning() {
		return running;
	}

	public void stop() {
		stop = true;
		running = false;
		continuous = false;
	}

	public List<EventPair> getEvents() {
		return events;
	}

	public void start() {
		continuous = true;
		stop = false;
		new Thread(this).start();
	}

	public void startNext() {
		continuous = false;
		if (currentTimer != null)
			Bukkit.getScheduler().cancelTask(currentTimer);
		stop = false;
		new Thread(this).start();
	}

	public void deleteEvent(int i) {
		events.remove(i);
	}

}
