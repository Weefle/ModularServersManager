package net.kaikk.msm.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.nio.BufferOverflowException;
import java.util.Iterator;

/**
 * A simple non-blocking String line buffered reader.
 * Useful for reading lines from a process console.
 * 
 * @author Kai
 *
 */
public class BufferedLineReader implements Closeable, AutoCloseable, Iterable<String>, Iterator<String> {
	protected final Reader reader;
	protected final char[] buffer;
	protected int i = 0;
	protected String bufferedLine;
	
	/**
	 * Creates a buffering character-input stream that uses an input buffer of
     * 32384 bytes.
	 * 
	 * @param reader a reader
	 */
	public BufferedLineReader(final Reader reader) {
		this(reader, 32384);
	}
	
	/**
	 * Creates a buffering character-input stream that uses an input buffer of
     * the specified size.
     * 
	 * @param reader a reader 
	 * @param bufferSize the buffer size, in bytes
	 */
	public BufferedLineReader(final Reader reader, final int bufferSize) {
		this.reader = reader;
		this.buffer = new char[bufferSize];
	}
	
	/**
	 * Reads the next line from the reader. This operation is supposed to be non-blocking.<br>
	 * The returned string will not containing any \r or \n character.<br><br>
	 * 
	 * Do not use this after a call to 
	 * 
	 * @return a String from the reader, null if no line is present.
	 * @throws IOException thrown by the reader's {@link Reader#ready()} and {@link Reader#read()} methods.
	 * @throws BufferOverflowException if the buffer is not big enough to read the entire line from the reader
	 */
	public String nextLine() throws IOException, BufferOverflowException {
		if (this.isBufferFull()) {
			throw new BufferOverflowException();
		}
		
		while(reader.ready()) {
			if (i >= buffer.length) {
				i--;
				throw new BufferOverflowException();
			}
			
			final char c = (char) reader.read();
			if (c == '\n') {
				return this.getBufferAndClear();
			} else if (c != '\r') {
				buffer[i++] = c;
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the current buffer content without clearing it. This may contain an incomplete line.
	 * 
	 * @return the buffer current content
	 */
	public String getBuffer() {
		return new String(buffer, 0, i);
	}

	/**
	 * Gets the current buffer content and clear it by resetting the internal cursor. This may contain an incomplete line.
	 * 
	 * @return the buffer current content
	 */
	public String getBufferAndClear() {
		int cp = i;
		i = 0;
		return new String(buffer, 0, cp);
	}
	
	/**
	 * Clears the buffer content by resetting the internal cursor
	 * */
	public void clear() {
		i = 0;
	}
	
	public boolean isBufferEmpty() {
		return i == 0;
	}
	
	public boolean isBufferFull() {
		return i + 1 >= buffer.length;
	}

	@Override
	public void close() throws IOException {
		this.reader.close();
	}

	@Override
	public Iterator<String> iterator() {
		return this;
	}
	
	/**
	 * Returns true if there is a new line, false otherwise.
	 * 
	 * @return whether there is a new line
	 * */
	@Override
	public boolean hasNext() {
		if (bufferedLine != null) {
			return true;
		}
		
		try {
			bufferedLine = nextLine();
		} catch (BufferOverflowException | IOException e) {
			throw new Error(e);
		}
		
		return bufferedLine != null;
	}

	@Override
	public String next() {
		if (bufferedLine != null) {
			return this.bufferedLine();
		}
		
		try {
			bufferedLine = nextLine();
		} catch (BufferOverflowException | IOException e) {
			throw new Error(e);
		}
		
		return this.bufferedLine();
	}
	
	private String bufferedLine() {
		final String tmp = bufferedLine;
		bufferedLine = null;
		return tmp;
	}
}
