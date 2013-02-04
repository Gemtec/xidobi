/*
 * Copyright 2013 Gemtec GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xidobi;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.xidobi.WinApi.ERROR_INVALID_HANDLE;
import static org.xidobi.WinApi.ERROR_IO_PENDING;
import static org.xidobi.WinApi.INVALID_HANDLE_VALUE;
import static org.xidobi.WinApi.WAIT_ABANDONED;
import static org.xidobi.WinApi.WAIT_FAILED;
import static org.xidobi.WinApi.WAIT_OBJECT_0;
import static org.xidobi.WinApi.WAIT_TIMEOUT;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.xidobi.spi.NativeCodeException;
import org.xidobi.structs.DWORD;
import org.xidobi.structs.INT;
import org.xidobi.structs.NativeByteArray;
import org.xidobi.structs.OVERLAPPED;

/**
 * Tests the class {@link SerialConnectionImpl}
 * 
 * @author Christian Schwarz
 * @author Tobias Bre�ler
 */
@SuppressWarnings("javadoc")
public class TestSerialConnectionImpl {

	private static final int OVERLAPPED_SIZE = 1;
	private static final int DWORD_SIZE = 2;

	private static final int DUMMY_ERROR_CODE = 12345;

	private static final int eventHandle = 1;

	/** a valid HANDLE value used in tests */
	private static final int handle = 2;

	private static final byte[] DATA = new byte[5];

	/** check exceptions */
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	private WinApi win;

	@Mock
	private SerialPort portHandle;

	/** the class under test */
	private SerialConnectionImpl port;

	/** pointer to an {@link OVERLAPPED}-struct */
	private int ptrOverlapped = 1;
	/** pointer to an {@link DWORD} */
	private int ptrBytesTransferred = 2;

	/** pointer to an {@link NativeByteArray} */
	private int ptrNativeByteArray = 3;

	@Before
	public void setUp() {
		initMocks(this);

		port = new SerialConnectionImpl(portHandle, win, handle);

		when(win.sizeOf_OVERLAPPED()).thenReturn(OVERLAPPED_SIZE);
		when(win.sizeOf_DWORD()).thenReturn(DWORD_SIZE);

		when(win.malloc(OVERLAPPED_SIZE)).thenReturn(ptrOverlapped);
		when(win.malloc(DWORD_SIZE)).thenReturn(ptrBytesTransferred);

		when(win.malloc(255)).thenReturn(ptrNativeByteArray);

		when(portHandle.getPortName()).thenReturn("COM1");
		when(win.CloseHandle(anyInt())).thenReturn(true);
	}

	/**
	 * Verifies that an {@link IllegalArgumentException} is throw when the passed {@link WinApi} is
	 * <code>null</code>.
	 */
	@Test
	@SuppressWarnings({ "resource", "unused" })
	public void new_nullOs() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Argument >os< must not be null!");

		new SerialConnectionImpl(portHandle, null, handle);
	}

	/**
	 * Verifies that an {@link IllegalArgumentException} is throw when the passed {@link SerialPort}
	 * is <code>null</code>.
	 */
	@Test
	@SuppressWarnings({ "resource", "unused" })
	public void new_nullPortHandle() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Argument >portHandle< must not be null!");

		new SerialConnectionImpl(null, win, handle);
	}

	/**
	 * Verifies that an {@link IllegalArgumentException} is thrown when the handle is
	 * {@link WinApi#INVALID_HANDLE_VALUE} (-1).
	 * 
	 */
	@Test
	@SuppressWarnings({ "resource", "unused" })
	public void new_negativeHandle() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Argument >handle< is invalid! Invalid handle value");

		new SerialConnectionImpl(portHandle, win, INVALID_HANDLE_VALUE);
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when <code>CreateEventA(...)</code>
	 * fails. In this case the method returns 0.
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_CreateEventAReturns0() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(0);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("CreateEventA illegally returned 0!");

		try {
			port.write(DATA);
		}
		finally {
			verify(win, never()).CloseHandle(0);
			verify(win, times(1)).free(ptrOverlapped);
			verify(win, times(1)).free(ptrBytesTransferred);
		}
	}

	/**
	 * Simulates are write operation that completes immediatly without the need to wait for
	 * completion of the pendig operation..
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_succeedImmediatly() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED())).thenReturn(true);

		port.write(DATA);

		verify(win, times(1)).WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED());
		verify(win, times(1)).CloseHandle(eventHandle);
		verify(win, times(1)).free(ptrOverlapped);
		verify(win, times(1)).free(ptrBytesTransferred);
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when <code>WriteFile(...)</code>
	 * returns <code>false</code> and the last error code is not <code>ERROR_IO_PENDING</code>.
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_WriteFileFailsWithERROR_INVALID_HANDLE() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_INVALID_HANDLE);

		exception.expect(IOException.class);
		exception.expectMessage("Port COM1 is closed! Write operation failed, because the handle is invalid!");

		try {
			port.write(DATA);
		}
		finally {
			verifyResourcesDisposed();
		}
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when <code>WriteFile(...)</code>
	 * returns <code>false</code> and the last error code is not <code>ERROR_IO_PENDING</code>.
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_WriteFileFails() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(DUMMY_ERROR_CODE);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("WriteFile failed unexpected!");

		try {
			port.write(DATA);
		}
		finally {
			verifyResourcesDisposed();
		}
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when
	 * <code>WaitForSingleObject(...)</code> returns an undefined value.
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_WaitForSingleObjectReturnsUndefinedValue() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(DUMMY_ERROR_CODE);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("WaitForSingleObject returned unexpected value! Got: " + DUMMY_ERROR_CODE);

		try {
			port.write(DATA);
		}
		finally {
			verify(win, times(1)).WaitForSingleObject(eventHandle, 2000);
			verifyResourcesDisposed();
		}
	}

	/**
	 * Verifies that {@link SerialConnection#write(byte[])} returns normally, when all bytes are
	 * written.
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_successfull() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(WAIT_OBJECT_0);
		when(win.GetOverlappedResult(eq(handle), anyOVERLAPPED(), anyDWORD(), eq(true))).thenReturn(true);
		when(win.getValue_DWORD(anyDWORD())).thenReturn(DATA.length);

		port.write(DATA);

		verifyResourcesDisposed();
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when GetOverlappedResult(...)
	 * indicates that not all bytes are written.
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_lessBytesWritten() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(WAIT_OBJECT_0);
		when(win.GetOverlappedResult(eq(handle), anyOVERLAPPED(), anyDWORD(), eq(true))).thenReturn(true);
		when(win.getValue_DWORD(anyDWORD())).thenReturn(DATA.length - 1);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("GetOverlappedResult returned an unexpected number of transferred bytes! Transferred: " + (DATA.length - 1) + ", expected: " + DATA.length);

		try {
			port.write(DATA);
		}
		finally {
			verifyResourcesDisposed();
		}
	}

	/**
	 * Verifies that an {@link IOException} is thrown, when the
	 * <code>WaitForSingleObject(...)</code> indicates a time-out.
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_WaitForSingleObjectReturnsWAIT_TIMEOUT() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(WAIT_TIMEOUT);

		exception.expect(IOException.class);
		exception.expectMessage("Write operation timed out after 2000 milliseconds!");

		try {
			port.write(DATA);
		}
		finally {
			verify(win, times(1)).WaitForSingleObject(eventHandle, 2000);
			verifyResourcesDisposed();
		}
	}

	/**
	 * Verifies that an {@link NativeCodeException} is thrown, when the
	 * <code>WaitForSingleObject(...)</code> returns <code>WAIT_FAILED</code>.
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_WaitForSingleObjectReturnsWAIT_FAILED() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(WAIT_FAILED);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("WaitForSingleObject returned an unexpected value: WAIT_FAILED!");

		try {
			port.write(DATA);
		}
		finally {
			verify(win, times(1)).WaitForSingleObject(eventHandle, 2000);
			verifyResourcesDisposed();
		}
	}

	/**
	 * Verifies that an {@link NativeCodeException} is thrown, when the
	 * <code>WaitForSingleObject(...)</code> returns <code>WAIT_ABANDONED</code>.
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_WaitForSingleObjectReturnsWAIT_ABANDONED() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(WAIT_ABANDONED);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("WaitForSingleObject returned an unexpected value: WAIT_ABANDONED!");

		try {
			port.write(DATA);
		}
		finally {
			verify(win, times(1)).WaitForSingleObject(eventHandle, 2000);
			verifyResourcesDisposed();
		}
	}

	/**
	 * Verifies that an {@link NativeCodeException} is thrown, when the
	 * <code>GetOverlappedResult(...)</code> returns <code>false</code>.
	 * 
	 * @throws IOException
	 */
	@Test
	public void write_GetOverlappedResultFails() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WriteFile(eq(handle), eq(DATA), eq(DATA.length), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(WAIT_OBJECT_0);
		when(win.GetOverlappedResult(eq(handle), anyOVERLAPPED(), anyDWORD(), eq(true))).thenReturn(false);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("GetOverlappedResult failed unexpected!");

		try {
			port.write(DATA);
		}
		finally {
			verify(win, times(1)).WaitForSingleObject(eventHandle, 2000);
			verify(win, times(1)).GetOverlappedResult(eq(handle), anyOVERLAPPED(), anyDWORD(), eq(true));
			verifyResourcesDisposed();
		}
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when <code>CreateEventA(...)</code>
	 * fails. In this case the method returns 0.
	 * 
	 * @throws IOException
	 */
	@Test
	public void read_CreateEventAReturns0() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(0);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("CreateEventA illegally returned 0!");

		try {
			port.read();
		}
		finally {
			verify(win, never()).CloseHandle(0);
			verify(win, times(1)).free(ptrOverlapped);
			verify(win, times(1)).free(ptrBytesTransferred);
		}
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when <code>WaitCommEvent(...)</code>
	 * returns <code>false</code> and the last error is not <code>ERROR_IO_PENDING</code>.
	 * 
	 * @throws IOException
	 */
	@Test
	public void read_WaitCommEventFailsWithUnexpectedErrorCode() throws IOException {
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WaitCommEvent(eq(handle), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(DUMMY_ERROR_CODE);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("WaitCommEvent failed unexpected!");

		try {
			port.read();
		}
		finally {
			verify(win, times(1)).ResetEvent(eventHandle);
			verify(win, times(1)).free(ptrOverlapped);
			verify(win, times(2)).free(ptrBytesTransferred);
		}
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when <code>WaitCommEvent(...)</code>
	 * returns <code>false</code>, the last error is <code>ERROR_IO_PENDING</code>,
	 * <code>WaitForSingleObject(...)</code> returns <code>WAIT_OBJECT_0</code> and
	 * <code>GetOverlappedResult(...)</code> returns <code>false</code>.
	 * 
	 * @throws IOException
	 */
	@Test
	public void read_WaitCommEventPendingAndGetOverlappedResultFails() throws IOException {
		//@formatter:off
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WaitCommEvent(eq(handle), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING, 
		                                         DUMMY_ERROR_CODE);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(WAIT_OBJECT_0);
		when(win.GetOverlappedResult(eq(handle), anyOVERLAPPED(), anyDWORD(), eq(true))).thenReturn(false);
		//@formatter:on

		exception.expect(NativeCodeException.class);
		exception.expectMessage("GetOverlappedResult failed unexpected!");

		try {
			port.read();
		}
		finally {
			verify(win, times(1)).ResetEvent(eventHandle);
			verify(win, times(1)).free(ptrOverlapped);
			verify(win, times(2)).free(ptrBytesTransferred);
		}
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when <code>WaitCommEvent(...)</code>
	 * is pending and <code>GetOverlappedResult(...)</code> returnes less bytes read.
	 * 
	 * @throws IOException
	 */
	@Test
	public void read_WaitCommEventUnexpectedEvent() throws IOException {
		final int BYTES_READ = 123;

		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WaitCommEvent(eq(handle), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(WAIT_OBJECT_0);
		when(win.GetOverlappedResult(eq(handle), anyOVERLAPPED(), anyDWORD(), eq(true))).thenReturn(true);
		when(win.getValue_DWORD(anyDWORD())).thenReturn(BYTES_READ);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("What do we expect here??");

		try {
			port.read();
		}
		finally {
			verify(win, times(1)).ResetEvent(eventHandle);
			verify(win, times(1)).free(ptrOverlapped);
			verify(win, times(2)).free(ptrBytesTransferred);
		}
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when <code>WaitCommEvent(...)</code>
	 * is pending and <code>WaitForSingleObject(...)</code> returns <code>WAIT_ABANDONED</code>.
	 * 
	 * @throws IOException
	 */
	@Test
	public void read_WaitForSingleObject_abandoned() throws IOException {
		//@formatter:off
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WaitCommEvent(eq(handle), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING, 
		                                         DUMMY_ERROR_CODE);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(WAIT_ABANDONED);
		//@formatter:on

		exception.expect(NativeCodeException.class);
		exception.expectMessage("WaitForSingleObject returned an unexpected value: WAIT_ABANDONED!");

		try {
			port.read();
		}
		finally {
			verify(win, times(1)).ResetEvent(eventHandle);
			verify(win, times(1)).free(ptrOverlapped);
			verify(win, times(2)).free(ptrBytesTransferred);
		}
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when <code>WaitCommEvent(...)</code>
	 * returns <code>false</code>, the last error is <code>ERROR_IO_PENDING</code> and
	 * <code>WaitForSingleObject(...)</code> returns <code>WAIT_OBJECT_0</code>.
	 * 
	 * @throws IOException
	 */
	@Test
	public void read_WaitCommEventPendingReturnsWAIT_FAILED() throws IOException {
		//@formatter:off
		when(win.CreateEventA(0, true, false, null)).thenReturn(eventHandle);
		when(win.WaitCommEvent(eq(handle), anyDWORD(), anyOVERLAPPED())).thenReturn(false);
		when(win.getPreservedError()).thenReturn(ERROR_IO_PENDING, 
		                                         DUMMY_ERROR_CODE);
		when(win.WaitForSingleObject(eventHandle, 2000)).thenReturn(WAIT_FAILED);
		//@formatter:on

		exception.expect(NativeCodeException.class);
		exception.expectMessage("WaitForSingleObject returned an unexpected value: WAIT_FAILED!");

		try {
			port.read();
		}
		finally {
			verify(win, times(1)).ResetEvent(eventHandle);
			verify(win, times(1)).free(ptrOverlapped);
			verify(win, times(2)).free(ptrBytesTransferred);
		}
	}

	/**
	 * Verifies that a call to {@link SerialConnection#close()} frees the native resources.
	 */
	@Test
	public void close() throws Exception {
		when(win.CloseHandle(handle)).thenReturn(true);

		port.close();

		verify(win).CloseHandle(handle);
		verifyNoMoreInteractions(win);
	}

	/**
	 * Verifies that a {@link NativeCodeException} is thrown, when <code>CloseHandle()</code> fails.
	 */
	@Test
	public void close_fails() throws Exception {
		when(win.CloseHandle(handle)).thenReturn(false);
		when(win.getPreservedError()).thenReturn(DUMMY_ERROR_CODE);

		exception.expect(NativeCodeException.class);
		exception.expectMessage("CloseHandle failed unexpected!\r\nError-Code " + DUMMY_ERROR_CODE);

		port.close();

		verify(win).CloseHandle(handle);
		verify(win).getPreservedError();
		verifyNoMoreInteractions(win);
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/** matches any {@link OVERLAPPED} */
	private OVERLAPPED anyOVERLAPPED() {
		return any(OVERLAPPED.class);
	}

	/** matches any {@link INT} */
	private DWORD anyDWORD() {
		return any(DWORD.class);
	}

	private void verifyResourcesDisposed() {
		verify(win, times(1)).CloseHandle(eventHandle);
		verify(win, times(1)).free(ptrOverlapped);
		verify(win, times(1)).free(ptrBytesTransferred);
	}

}
