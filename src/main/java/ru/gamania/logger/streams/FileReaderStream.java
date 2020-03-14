package ru.gamania.logger.streams;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileReaderStream
{
	private static final boolean STRICT_MODE = true;
	private final Path log;

	public FileReaderStream(File file)
	{
		this.log = file.getParentFile().toPath().resolve("logs").resolve("latest.log");
	}

	public final List<String> getLines()
	{
		if (STRICT_MODE)
		{
			try
			{
				return Files.readAllLines(this.log, StandardCharsets.UTF_8);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			return Collections.emptyList();
		}

		List<String> lines = new ArrayList<>();

		try (BufferedReader reader = Files.newBufferedReader(this.log, StandardCharsets.UTF_8))
		{
			while (true)
			{
				String line = reader.readLine();
				if (line == null)
					break;
				lines.add(line);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return lines;
	}
}
