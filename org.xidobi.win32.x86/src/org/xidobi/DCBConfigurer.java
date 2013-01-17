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

import static org.xidobi.internal.Preconditions.checkArgumentNotNull;
import static org.xidobi.structs.DCB.EVENPARITY;
import static org.xidobi.structs.DCB.MARKPARITY;
import static org.xidobi.structs.DCB.NOPARITY;
import static org.xidobi.structs.DCB.ODDPARITY;
import static org.xidobi.structs.DCB.ONE5STOPBITS;
import static org.xidobi.structs.DCB.ONESTOPBIT;
import static org.xidobi.structs.DCB.RTS_CONTROL_ENABLE;
import static org.xidobi.structs.DCB.RTS_CONTROL_HANDSHAKE;
import static org.xidobi.structs.DCB.SPACEPARITY;
import static org.xidobi.structs.DCB.TWOSTOPBITS;

import org.xidobi.structs.DCB;

/**
 * 
 * @author Tobias Bre�ler
 * 
 * @see DCB
 * @see SerialPortSettings
 */
public class DCBConfigurer {

	private static final int TRUE = 1;
	private static final int FALSE = 0;

	/**
	 * Configures the serial port to the given settings.
	 * 
	 * @param dcb
	 *            the DCB "struct", must not be <code>null</code>
	 * @param settings
	 *            the settings for the serial port, must not be <code>null</code>
	 */
	public void configureDCB(DCB dcb, SerialPortSettings settings) {
		checkArgumentNotNull(dcb, "dcb");
		checkArgumentNotNull(settings, "settings");

		configureBaudRate(dcb, settings);
		configureDataBits(dcb, settings);
		configureStopBits(dcb, settings);
		configureParity(dcb, settings);
		configureFlowControl(dcb, settings);

		// if(setRTS == JNI_TRUE){
		// dcb->fRtsControl = RTS_CONTROL_ENABLE;
		// }
		// else {
		// dcb->fRtsControl = RTS_CONTROL_DISABLE;
		// }
		// if(setDTR == JNI_TRUE){
		// dcb->fDtrControl = DTR_CONTROL_ENABLE;
		// }
		// else {
		// dcb->fDtrControl = DTR_CONTROL_DISABLE;
		// }

		configureFixValues(dcb);
	}

	/** Configures the baud rate on the DCB "struct". */
	private void configureBaudRate(DCB dcb, SerialPortSettings settings) {
		dcb.BaudRate = settings.getBauds();
	}

	/** Configures the data bits on the DCB "struct". */
	private void configureDataBits(DCB dcb, SerialPortSettings settings) {
		switch (settings.getDataBits()) {
			case DataBits_5:
				dcb.ByteSize = 5;
				return;
			case DataBits_6:
				dcb.ByteSize = 6;
				return;
			case DataBits_7:
				dcb.ByteSize = 7;
				return;
			case DataBits_8:
				dcb.ByteSize = 8;
				return;
			case DataBits_9:
				dcb.ByteSize = 9;
				return;
		}
	}

	/** Configures the stop bits on the DCB "struct". */
	private void configureStopBits(DCB dcb, SerialPortSettings settings) {
		switch (settings.getStopBits()) {
			case StopBits_1:
				dcb.StopBits = ONESTOPBIT;
				return;
			case StopBits_1_5:
				dcb.StopBits = ONE5STOPBITS;
				return;
			case StopBits_2:
				dcb.StopBits = TWOSTOPBITS;
				return;
		}
	}

	/** Configures the parity on the DCB "struct". */
	private void configureParity(DCB dcb, SerialPortSettings settings) {
		switch (settings.getParity()) {
			case Parity_None:
				dcb.Parity = NOPARITY;
				return;
			case Parity_Even:
				dcb.Parity = EVENPARITY;
				return;
			case Parity_Odd:
				dcb.Parity = ODDPARITY;
				return;
			case Parity_Mark:
				dcb.Parity = MARKPARITY;
				return;
			case Parity_Space:
				dcb.Parity = SPACEPARITY;
				return;
		}
	}

	/** Configures the flow control on the DCB "struct". */
	private void configureFlowControl(DCB dcb, SerialPortSettings settings) {

		// Reset flow control settings:
		dcb.fRtsControl = RTS_CONTROL_ENABLE;
		dcb.fOutxCtsFlow = FALSE;
		dcb.fOutX = FALSE;
		dcb.fInX = FALSE;

		switch (settings.getFlowControl()) {
			case FlowControl_None:
				return;
			case FlowControl_RTSCTS_In:
				dcb.fRtsControl = RTS_CONTROL_HANDSHAKE;
				return;
			case FlowControl_RTSCTS_Out:
				dcb.fOutxCtsFlow = TRUE;
				return;
			case FlowControl_RTSCTS_In_Out:
				dcb.fRtsControl = RTS_CONTROL_HANDSHAKE;
				dcb.fOutxCtsFlow = TRUE;
				return;
			case FlowControl_XONXOFF_In:
				dcb.fInX = TRUE;
				return;
			case FlowControl_XONXOFF_Out:
				dcb.fOutX = TRUE;
				return;
			case FlowControl_XONXOFF_In_Out:
				dcb.fInX = TRUE;
				dcb.fOutX = TRUE;
				return;
		}
	}

	private void configureFixValues(DCB dcb) {
		dcb.fOutxCtsFlow = FALSE;
		dcb.fOutxDsrFlow = FALSE;
		dcb.fDsrSensitivity = FALSE;
		dcb.fTXContinueOnXoff = TRUE;
		dcb.fOutX = FALSE;
		dcb.fInX = FALSE;
		dcb.fErrorChar = FALSE;
		dcb.fNull = FALSE;
		dcb.fAbortOnError = FALSE;
		dcb.XonLim = 2048;
		dcb.XoffLim = 512;
		dcb.XonChar = (char) 17; // DC1
		dcb.XoffChar = (char) 19; // DC3
	}
}