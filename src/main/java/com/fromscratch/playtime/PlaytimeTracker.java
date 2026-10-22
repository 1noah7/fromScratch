package com.fromscratch.playtime;

import com.fromscratch.bank.*;
import com.fromscratch.persistence.PlaytimeRepository;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javax.inject.*;
import net.runelite.client.callback.ClientThread;

/** Counts actual logged-in GameTick callbacks, never intervals between timestamps. IO uses a dedicated FIFO. */
@Singleton
public final class PlaytimeTracker
{
	private final PlaytimeRepository repository;
	private final Consumer<Runnable> dispatch;
	private final Consumer<View> display;
	private ExecutorService worker;
	private BankController.DirectoryProvider directory;
	private Context current;
	private long lastTick=Long.MIN_VALUE;
	private boolean enabled;
	private boolean suspended;
	@Inject public PlaytimeTracker(PlaytimeRepository repository,ClientThread thread,BankSidebar sidebar)
	{ this(repository,thread::invokeLater,sidebar::playtime); }
	public PlaytimeTracker(PlaytimeRepository repository,Consumer<Runnable> dispatch,Consumer<View> display)
	{ this.repository=repository;this.dispatch=dispatch;this.display=display; }
	public void start(BankController.DirectoryProvider directory)
	{ start(directory,Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"fromscratch-playtime");t.setDaemon(true);return t;})); }
	public void start(BankController.DirectoryProvider directory,ExecutorService worker)
	{ this.directory=directory;this.worker=worker;enabled=true;suspended=false;current=null;lastTick=Long.MIN_VALUE; }
	public void tick(BankRun run,boolean loggedIn,long tick)
	{
		if(!enabled||suspended) { return; }
		select(run);
		if(!loggedIn||current==null||!current.loaded||current.error!=null||lastTick==tick) { return; }
		lastTick=tick;current.pending=Math.incrementExact(current.pending);current.measured=Math.incrementExact(current.measured);
		if(current.pending>=100) { flush(current); }
		if(current.measured%10==0) { publish(current); }
	}
	public void interrupted()
	{ if(current!=null) { flush(current); } }
	/** Drain acknowledged playtime before a user-confirmed restore; no thread waits or per-tick disk work. */
	public void beforeRestore(Runnable ready)
	{
		suspended=true;checkpoint(ready);
	}
	public void checkpoint(Runnable ready)
	{
		interrupted();
		if(worker==null||!enabled){ready.run();return;}
		worker.execute(()->dispatch.accept(ready));
	}
	public void afterRestore() { current=null;lastTick=Long.MIN_VALUE;suspended=false; }
	private void select(BankRun run)
	{
		if(current!=null&&run!=null&&current.run.equals(run.getRunId())&&current.character.equals(run.getCharacter())) { return; }
		if(current!=null) { flush(current); }
		current=null;lastTick=Long.MIN_VALUE;
		if(run==null) { return; }
		Context c=new Context(run.getCharacter(),run.getRunId(),directory);current=c;
		worker.execute(()->
		{
			try
			{
				c.stored=repository.load(c.directory.get(),c.character,c.run);
				dispatch.accept(()->{if(enabled&&current==c) { c.loaded=true;c.measured=c.stored.getTicks();publish(c); }});
			}
			catch(IOException|RuntimeException ex) { fail(c); }
		});
	}
	private void flush(Context c)
	{
		if(!c.loaded||c.error!=null||c.pending==0) { return; }
		long ticks=c.pending,generation=c.generation;c.pending=0;UUID batch=UUID.randomUUID();
		worker.execute(()->
		{
			if(generation!=c.generation) { return; }
			try
			{
				Playtime saved=repository.add(c.directory.get(),c.stored,ticks,batch);
				boolean conflict=!saved.getRevision().equals(batch);c.stored=saved;
				if(conflict) { c.generation++; }
				dispatch.accept(()->
				{
					if(enabled&&current==c)
					{
						if(conflict) { c.pending=0;c.measured=saved.getTicks(); }
						publish(c);
					}
				});
			}
			catch(IOException|RuntimeException ex) { fail(c); }
		});
	}
	private void fail(Context c)
	{
		dispatch.accept(()->{if(enabled&&current==c) { c.error="Time played could not be saved. Recovery files were preserved; restart after checking local storage.";publish(c); }});
	}
	private void publish(Context c) { display.accept(new View(c.character,c.run,c.measured,c.error)); }
	/** Queue the final partial minute, then drain without blocking startup/shutdown or any game thread. */
	public void stop()
	{
		if(current!=null) { flush(current); }
		enabled=false;current=null;
		// Terminate from the tail of the FIFO: the final forced checkpoint finishes without
		// waiting on the client/EDT. No scheduled jobs or per-tick tasks survive shutdown.
		if(worker!=null) { ExecutorService ending=worker;ending.execute(ending::shutdownNow); }
	}
	@lombok.Value public static class View { String character;UUID run;long ticks;String error; }
	private static final class Context
	{
		final String character;final UUID run;final BankController.DirectoryProvider directory;
		volatile Playtime stored;volatile long generation;
		boolean loaded;long measured,pending;String error;
		Context(String character,UUID run,BankController.DirectoryProvider directory) { this.character=character;this.run=run;this.directory=directory; }
	}
}
