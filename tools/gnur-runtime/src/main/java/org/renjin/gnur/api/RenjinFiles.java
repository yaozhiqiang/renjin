/*
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2019 BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.gnu.org/licenses/gpl-2.0.txt
 */
package org.renjin.gnur.api;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.RandomAccessContent;
import org.renjin.gcc.runtime.*;
import org.renjin.primitives.Native;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.renjin.gcc.runtime.Stdlib.nullTerminatedString;

/**
 * Provides Renjin-specific hooks into the Session's FileSystemManager
 */
public class RenjinFiles {

  public static Ptr fopen(Ptr filename, Ptr mode) {
    String filenameString = nullTerminatedString(filename);
    String modeString = nullTerminatedString(mode);

    FileObject fileObject;
    try {
      fileObject = Native.currentContext().resolveFile(filenameString);
    } catch (FileSystemException e) {
      return BytePtr.NULL;
    }

    try {
      return new RecordUnitPtr<>(fopen(fileObject, modeString));

    } catch (IOException e) {
      return BytePtr.NULL;
    }
  }

  private static FileHandle fopen(FileObject fileObject, String mode) throws FileSystemException {
    switch (mode) {
      case "r":
      case "rb":
        return new InputStreamHandle(fileObject.getContent());

      case "w":
      case "wb":
        return new OutputStreamHandle(fileObject.getContent().getOutputStream());

      default:
        throw new UnsupportedOperationException("mode: " + mode);
    }
  }

  private static class InputStreamHandle extends AbstractFileHandle {

    private FileContent content;
    private InputStream inputStream;
    private long position = 0;

    public InputStreamHandle(FileContent content) throws FileSystemException {
      this.content = content;
      this.inputStream = content.getInputStream();
    }

    @Override
    public int read() throws IOException {
      int b = inputStream.read();
      if(b != -1) {
        position ++;
      }
      return b;
    }

    @Override
    public void write(int b) throws IOException {
      throw new UnsupportedOperationException("Cannot write on input stream handle.");
    }

    @Override
    public void rewind() throws IOException {
      inputStream.close();
      inputStream = content.getInputStream();
      position = 0;
    }

    @Override
    public void flush() throws IOException {
      throw new UnsupportedOperationException("Cannot flush an input stream handle.");
    }

    @Override
    public void close() throws IOException {
      inputStream.close();
    }

    @Override
    public void seekSet(long offset) throws IOException {
      if(offset < position) {
        throw new IOException("Cannot rewind the stream");
      }
      long toSkip = offset - position;
      long skipped = inputStream.skip(toSkip);
      if(skipped < toSkip) {
        throw new EOFException();
      }
    }

    @Override
    public void seekCurrent(long offset) throws IOException {
      long skipped = inputStream.skip(offset);
      if(skipped < offset) {
        throw new EOFException();
      }
    }

    @Override
    public void seekEnd(long offset) {
      throw new UnsupportedOperationException("TODO");
    }
  }

  private static class OutputStreamHandle extends AbstractFileHandle {

    private OutputStream outputStream;

    public OutputStreamHandle(OutputStream outputStream) {
      this.outputStream = outputStream;
    }

    @Override
    public int read() throws IOException {
      throw new UnsupportedOperationException("Cannot read from output stream handle.");
    }

    @Override
    public void write(int b) throws IOException {
      outputStream.write(b);
    }

    @Override
    public void rewind() throws IOException {
      throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void flush() throws IOException {
      outputStream.flush();
    }

    @Override
    public void close() throws IOException {
      outputStream.close();
    }

    @Override
    public void seekSet(long offset) throws IOException {
      throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void seekCurrent(long offset) throws IOException {
      throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void seekEnd(long offset) {
      throw new UnsupportedOperationException("TODO");
    }
  }

}
