package com.fromscratch.persistence;

import com.fromscratch.playtime.Playtime;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import javax.inject.*;
import net.runelite.client.util.Filepath;

/** Small run-scoped forced checkpoint. Does not migrate or rewrite any bank save or journal. Worker only. */
@Singleton
public final class PlaytimeRepository
{
	private final BankRunCodec codec;
	@Inject public PlaytimeRepository(BankRunCodec codec) { this.codec=codec; }
	public Playtime load(Filepath root,String character,UUID run) throws IOException
	{
		return locked(root,character,run,d ->
		{
			Filepath current=d.joinSegment(file(run));
			if(current.exists()) { return read(current,character,run); }
			try(java.util.stream.Stream<Filepath> files=d.walk(1))
			{
				if(files.anyMatch(f->f.getFileName().startsWith("playtime-"+run+"-")))
				{ throw new SaveException("Playtime checkpoint is missing. Recovery files were preserved; restore the playtime backup before retrying."); }
			}
			Playtime initial=new Playtime(character,run,UUID.randomUUID(),Instant.now(),0);
			write(d,initial);return initial;
		});
	}
	/** A conflicting client wins: return its counter, discarding this overlapping interval rather than counting twice. */
	public Playtime add(Filepath root,Playtime expected,long ticks,UUID batch) throws IOException
	{
		if(ticks<0) { throw new IllegalArgumentException("Negative playtime interval"); }
		return locked(root,expected.getCharacter(),expected.getRun(),d ->
		{
			Playtime current=read(d.joinSegment(file(expected.getRun())),expected.getCharacter(),expected.getRun());
			if(current.getRevision().equals(batch)||!current.getRevision().equals(expected.getRevision())) { return current; }
			Playtime next=new Playtime(current.getCharacter(),current.getRun(),batch,current.getMeasuredFrom(),Math.addExact(current.getTicks(),ticks));
			write(d,next);return next;
		});
	}
	Playtime read(Filepath file,String character,UUID run) throws IOException
	{
		try
		{
			JsonObject p=codec.payload(BankStorageFiles.readText(file),1);
			BankRunCodec.keys(p,"character","run","revision","from","ticks");
			Playtime state=new Playtime(BankRunCodec.str(p,"character"),UUID.fromString(BankRunCodec.str(p,"run")),UUID.fromString(BankRunCodec.str(p,"revision")),Instant.parse(BankRunCodec.str(p,"from")),BankRunCodec.number(p.get("ticks")));
			if(!character.equals(state.getCharacter())||!run.equals(state.getRun())) { throw new IllegalArgumentException("Identity mismatch"); }
			return state;
		}
		catch(RuntimeException ex) { throw new SaveException("Playtime could not be verified. Its checkpoint and backup were preserved."); }
	}
	private void write(Filepath d,Playtime state) throws IOException
	{
		JsonObject p=new JsonObject();p.addProperty("character",state.getCharacter());p.addProperty("run",state.getRun().toString());
		p.addProperty("revision",state.getRevision().toString());p.addProperty("from",state.getMeasuredFrom().toString());p.addProperty("ticks",state.getTicks());
		Filepath current=d.joinSegment(file(state.getRun()));
		Filepath pending=d.joinSegment("playtime-"+state.getRun()+"-pending-"+state.getRevision()+".json");
		BankStorageFiles.writeNew(pending,codec.envelope(p,1));
		// Keep exact previous bytes. Atomic replacement failure leaves both the original and forced candidate.
		if(current.exists())
		{
			Filepath backup=d.joinSegment("playtime-"+state.getRun()+"-backup.json");
			Filepath backupPending=d.joinSegment("playtime-"+state.getRun()+"-backup-"+state.getRevision()+".json");
			BankStorageFiles.writeNew(backupPending,BankStorageFiles.readText(current));
			backupPending.moveTo(backup,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
		}
		pending.moveTo(current,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
		if(!read(current,state.getCharacter(),state.getRun()).equals(state)) { throw new SaveException("Playtime verification failed; recovery files were retained."); }
	}
	private static String file(UUID run) { return "playtime-"+run+".json"; }
	private <T>T locked(Filepath root,String character,UUID run,Work<T> work) throws IOException
	{
		if(character==null||!character.matches("[a-f0-9]{64}")||run==null) { throw new SaveException("Unverified playtime identity"); }
		// Separate directory: economic migration/reset archives cannot race this independent counter.
		// Run-specific old counters remain recoverable after reset and cannot attach to the new run.
		Filepath base=root.join("characters",character);Filepath locks=base.joinSegment("playtime");locks.createDirectories();
		try(FileChannel channel=locks.joinSegment("playtime.lock").openFileChannel(StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=channel.tryLock())
		{
			if(lock==null) { throw new SaveException("Another client is saving playtime. No interval was added."); }
			Filepath d=ProgressBackup.directory(base,codec).joinSegment("playtime");d.createDirectories();
			return work.run(d);
		}
		catch(OverlappingFileLockException ex) { throw new SaveException("Another client is saving playtime. No interval was added."); }
	}
	@FunctionalInterface private interface Work<T> { T run(Filepath directory) throws IOException; }
}
