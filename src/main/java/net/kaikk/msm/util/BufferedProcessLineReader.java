package net.kaikk.msm.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BufferedProcessLineReader {
	protected final Process process;
	protected final LinkedBlockingQueue<LogLine> lines;
	protected final Thread threadIn;
	protected final Thread threadErr;
	protected final AtomicInteger skippedLines = new AtomicInteger();

	public BufferedProcessLineReader(Process process, String threadName, int capacity) {
		this.process = process;
		this.lines = new LinkedBlockingQueue<>(capacity);
		
		this.threadIn = new Thread(() -> {
			try (final BufferedReader brIn = new BufferedReader(new InputStreamReader(process.getInputStream()), 1048576);) {
				String line;
				while (process.isAlive()) {
					while((line = brIn.readLine()) != null) {
						if (!lines.offer(new LogLine(line, false))) {
							this.skippedLines.incrementAndGet();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, threadName+"_InReader");
		
		this.threadErr = new Thread(() -> {
			try (final BufferedReader brErr = new BufferedReader(new InputStreamReader(process.getErrorStream()), 1048576);) {
				String line;
				while (process.isAlive()) {
					while((line = brErr.readLine()) != null) {
						if (!lines.offer(new LogLine(line, true))) {
							this.skippedLines.incrementAndGet();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, threadName+"_ErrReader");
		
		this.threadIn.start();
		this.threadErr.start();
	}
	
	public LogLine take() throws InterruptedException {
		LogLine logLine = null;
		do {
			logLine = lines.poll(100L, TimeUnit.MILLISECONDS); // TODO improve... although it's just max 100 ms wait after the process ends
		} while (logLine == null && process.isAlive());
		return logLine;
	}
	
	public int skippedLines() {
		return this.skippedLines.getAndSet(0);
	}
	
	public static class LogLine {
		final private String string;
		final private boolean isError;
		
		public LogLine(String string, boolean isError) {
			this.string = string;
			this.isError = isError;
		}

		public String getString() {
			return string;
		}
		public boolean isError() {
			return isError;
		}
	}
}