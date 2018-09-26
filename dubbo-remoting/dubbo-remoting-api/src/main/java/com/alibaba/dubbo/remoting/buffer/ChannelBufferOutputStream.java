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
import java.io.OutputStream;

public class ChannelBufferOutputStream extends OutputStream {

    private final ChannelBuffer buffer;
    /**
     * 初始的开始位置，设置之后不会改变
     */
    private final int startIndex;

    public ChannelBufferOutputStream(ChannelBuffer buffer) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        this.buffer = buffer;
        startIndex = buffer.writerIndex();
    }

    /**
     * 已经写入了多少字节
     * @return
     */
    public int writtenBytes() {
        return buffer.writerIndex() - startIndex;
    }

    /**
     * 从数组b中，从off开始读取len的长度，写入到流里，从数组里写入到流里
     * @param b
     * @param off
     * @param len
     * @throws IOException
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return;
        }

        buffer.writeBytes(b, off, len);
    }

    /**
     * 写入一个字节
     * @param b
     * @throws IOException
     */
    @Override
    public void write(byte[] b) throws IOException {
        buffer.writeBytes(b);
    }

    /**
     * 写入一个字节，将init强转为byte，高24位直接舍弃
     * @param b
     * @throws IOException
     */
    @Override
    public void write(int b) throws IOException {
        buffer.writeByte((byte) b);
    }

    public ChannelBuffer buffer() {
        return buffer;
    }
}
