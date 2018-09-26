/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.dubbo.remoting.buffer;

import java.io.IOException;
import java.io.InputStream;

public class ChannelBufferInputStream extends InputStream {

    private final ChannelBuffer buffer;
    /**
     * 起始位置，设置之后不会改变
     */
    private final int startIndex;
    /**
     * 结束位置，设置之后不会改变
     */
    private final int endIndex;

    public ChannelBufferInputStream(ChannelBuffer buffer) {
        // 可读字节数量
        this(buffer, buffer.readableBytes());
    }

    /**
     * 限制长度为length个字节
     * @param buffer
     * @param length
     */
    public ChannelBufferInputStream(ChannelBuffer buffer, int length) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length: " + length);
        }
        // 在判断一下，是为了防止构造的时候buffer又被读了，就会报错
        if (length > buffer.readableBytes()) {
            throw new IndexOutOfBoundsException();
        }

        this.buffer = buffer;
        // startIndex = buffer.readerIndex
        // endIndex = buffer.writerIndex
        startIndex = buffer.readerIndex();
        endIndex = startIndex + length;
        buffer.markReaderIndex();
    }

    /**
     * 返回已经读了多少个字节
     * @return
     */
    public int readBytes() {
        return buffer.readerIndex() - startIndex;
    }

    /**
     * 还有几个可以读取
     * @return
     * @throws IOException
     */
    @Override
    public int available() throws IOException {
        return endIndex - buffer.readerIndex();
    }

    /**
     * 标记，将当前读取下标打标记，方便重置，从这里开始
     * @param readlimit
     */
    @Override
    public void mark(int readlimit) {
        buffer.markReaderIndex();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * 读一个字节出来
     * @return
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        if (!buffer.readable()) {
            return -1;
        }
        // 将高24位清零
        return buffer.readByte() & 0xff;
    }

    /**
     * 从off开始读len个字节到字节数组b中，从流里读取到数组
     * @param b
     * @param off
     * @param len
     * @return
     * @throws IOException
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int available = available();
        if (available == 0) {
            return -1;
        }

        len = Math.min(available, len);
        buffer.readBytes(b, off, len);
        return len;
    }

    /**
     * 将读取下标，重置到上次达标的地方
     * @throws IOException
     */
    @Override
    public void reset() throws IOException {
        buffer.resetReaderIndex();
    }

    /**
     * 跳过多少个字节，最多到endIndex
     * @param n
     * @return
     * @throws IOException
     */
    @Override
    public long skip(long n) throws IOException {
        if (n > Integer.MAX_VALUE) {
            return skipBytes(Integer.MAX_VALUE);
        } else {
            return skipBytes((int) n);
        }
    }

    private int skipBytes(int n) throws IOException {
        int nBytes = Math.min(available(), n);
        buffer.skipBytes(nBytes);
        return nBytes;
    }

}
