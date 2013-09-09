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
package org.xidobi.rfc2217.internal.commands;

import static org.xidobi.rfc2217.internal.RFC2217.SIGNATURE_REQ;
import static org.xidobi.rfc2217.internal.RFC2217.SIGNATURE_RESP;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * This command may be sent by either the client or the access server to exchange signature
 * information. If the command is sent without <text> it is a request from the sender to receive the
 * signature text of the receiver. The text may be a combination of any characters. There is no
 * structure to the <text> field. It may contain manufacturer information, version number
 * information, or any other information desired. If an IAC character appears in the text it must be
 * translated to IAC-IAC to avoid conflict with the IAC which terminates the command.
 * 
 * @author Christin Nitsche
 */
public class SignatureControlCmd extends AbstractControlCmd {

	private String signature;

	public SignatureControlCmd(@Nonnull String signature) {
		super(SIGNATURE_REQ);
		if (signature == null)
			throw new IllegalArgumentException("The parameter >signatur< must not be null");
		this.signature = signature;
	}

	/**
	 * Creates a new {@link SignatureControlCmd}
	 * 
	 * @param input
	 *            used to decode the content of the command, must not be <code>null</code>
	 * @throws IOException
	 *             if the message is malformed or the underlying media can't be read
	 */
	SignatureControlCmd(DataInput input) throws IOException {
		super(SIGNATURE_RESP);

		signature = input.readLine();
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeChars(signature);
	}

	/**
	 * Returns the preferred signatur.
	 * 
	 * @return
	 */
	public String getSignature() {
		return signature;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((signature == null) ? 0 : signature.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SignatureControlCmd other = (SignatureControlCmd) obj;
		if (signature == null) {
			if (other.signature != null)
				return false;
		} else if (!signature.equals(other.signature))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SignatureControlCmd [signature=" + signature + "]";
	}

}
