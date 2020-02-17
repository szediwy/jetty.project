//
// ========================================================================
// Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.websocket.util.messages;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.core.Frame;
import org.eclipse.jetty.websocket.core.WebSocketConstants;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Support class for reading a (single) WebSocket TEXT message via a Reader.
 * <p>
 * In compliance to the WebSocket spec, this reader always uses the {@link StandardCharsets#UTF_8}.
 */
public class MessageReader extends Reader implements MessageSink
{
    private static final int BUFFER_SIZE = WebSocketConstants.DEFAULT_INPUT_BUFFER_SIZE;

    private final ByteBuffer buffer;
    private final MessageInputStream stream;
    private final CharsetDecoder utf8Decoder = UTF_8.newDecoder()
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .onMalformedInput(CodingErrorAction.REPORT);

    public MessageReader()
    {
        this(BUFFER_SIZE);
    }

    public MessageReader(int bufferSize)
    {
        this.stream = new MessageInputStream();
        this.buffer = BufferUtil.allocate(bufferSize);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException
    {
        CharBuffer charBuffer = CharBuffer.wrap(cbuf, off, len);
        if (!buffer.hasRemaining())
        {
            int read = stream.read(buffer);
            if (read == 0)
                return read;
            if (read < 0)
            {
                utf8Decoder.decode(BufferUtil.EMPTY_BUFFER, charBuffer, true);
                return (charBuffer.position() > 0) ? charBuffer.position() : read;
            }
        }

        utf8Decoder.decode(buffer, charBuffer, false);
        return charBuffer.position();
    }

    @Override
    public void close() throws IOException
    {
        stream.close();
    }

    @Override
    public void accept(Frame frame, Callback callback)
    {
        stream.accept(frame, callback);
    }
}
