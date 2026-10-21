package com.fromscratch.persistence;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import net.runelite.client.util.Filepath;

/** Bounded, forced Filepath IO. Background worker only. */
final class BankStorageFiles
{
	private static final long MAX_SAVE_BYTES = 4 * 1024 * 1024;
	private BankStorageFiles() { }

	static String readText(Filepath file) throws IOException
	{
		if (file.size() > MAX_SAVE_BYTES) { throw new SaveException("Save exceeds the supported size. It was not modified."); }
		StringBuilder data = new StringBuilder();
		try (Reader reader = file.openReader())
		{
			char[] buffer = new char[4096];
			int length;
			while ((length = reader.read(buffer)) != -1)
			{
				if (data.length() + length > MAX_SAVE_BYTES) { throw new SaveException("Save exceeds the supported size."); }
				data.append(buffer, 0, length);
			}
		}
		return data.toString();
	}

	static void writeNew(Filepath file, String data) throws IOException
	{
		if (Thread.currentThread().isInterrupted()) { throw new SaveException("Save interrupted. Existing recovery files were retained."); }
		byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
		if (bytes.length > MAX_SAVE_BYTES) { throw new SaveException("Snapshot exceeds the supported save size."); }
		try (FileChannel channel = file.openFileChannel(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))
		{
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			while (buffer.hasRemaining()) { channel.write(buffer); }
			channel.force(true);
		}
	}
}
