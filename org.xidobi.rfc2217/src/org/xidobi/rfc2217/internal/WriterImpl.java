/*
 * Copyright Gemtec GmbH 2009-2013
 *
 * Erstellt am: 19.08.2013 14:03:30
 * Erstellt von: Christian Schwarz 
 */
package org.xidobi.rfc2217.internal;

import static org.xidobi.spi.Preconditions.checkArgumentNotNull;

import java.io.IOException;
import java.io.OutputStream;

import org.xidobi.spi.Writer;

/**
 * @author Christian Schwarz
 */
@SuppressWarnings("restriction")
final class WriterImpl implements Writer {

	private OutputStream outputStream;

	/**
	 * @param outputStream
	 */
	public WriterImpl(OutputStream outputStream) {
		this.outputStream = checkArgumentNotNull(outputStream, "outputStream");

	}

	public void close() throws IOException {
	}

	public void dispose() {
	}

	public void write(byte[] data) throws IOException {
		outputStream.write(data);
	}
}