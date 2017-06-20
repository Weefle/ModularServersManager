package net.kaikk.msm.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BufferedLineReaderThread extends Thread implements Closeable {
	protected final BufferedReader reader;
	protected final Queue<String> lines;
	protected final AtomicInteger skippedLines = new AtomicInteger();

	public BufferedLineReaderThread(BufferedReader reader) {
		this.reader = reader;
		this.lines = new LinkedBlockingQueue<>();
	}
	
	public BufferedLineReaderThread(BufferedReader reader, String threadName) {
		super(threadName);
		this.reader = reader;
		this.lines = new LinkedBlockingQueue<>();
	}
	
	public BufferedLineReaderThread(BufferedReader reader, String threadName, int capacity) {
		super(threadName);
		this.reader = reader;
		this.lines = new LinkedBlockingQueue<>(capacity);
	}

	@Override
	public void run() {
		String line;
		try {
			while((line = reader.readLine()) != null) {
				if (!lines.offer(line)) {
					this.skippedLines.incrementAndGet();
				}
			}
		} catch (IOException e) {
			
		}
		try {
			reader.close();
		} catch (IOException e1) {
			
		}
	}

	public Queue<String> lines() {
		return lines;
	}
	
	public int skippedLines() {
		return this.skippedLines.getAndSet(0);
	}

	public BufferedReader getReader() {
		return reader;
	}

	@Override
	public void close() throws IOException {
		this.reader.close();
	}
}