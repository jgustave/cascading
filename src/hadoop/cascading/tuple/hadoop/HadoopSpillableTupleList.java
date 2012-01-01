/*
 * Copyright (c) 2007-2012 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
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

package cascading.tuple.hadoop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cascading.flow.hadoop.HadoopFlowProcess;
import cascading.tuple.SpillableTupleList;
import cascading.tuple.TupleException;
import cascading.tuple.TupleInputStream;
import cascading.tuple.TupleOutputStream;
import org.apache.hadoop.io.compress.CompressionCodec;

/**
 * SpillableTupleList is a simple {@link Iterable} object that can store an unlimited number of {@link cascading.tuple.Tuple} instances by spilling
 * excess to a temporary disk file.
 */
public class HadoopSpillableTupleList extends SpillableTupleList
  {
  /** Field codec */
  private final CompressionCodec codec;
  /** Field serializationElementWriter */
  private final TupleSerialization tupleSerialization;

  /**
   * Constructor SpillableTupleList creates a new SpillableTupleList instance using the given threshold value, and
   * the first available compression codec, if any.
   *
   * @param threshold of type long
   * @param codec     of type CompressionCodec
   */
  public HadoopSpillableTupleList( long threshold, CompressionCodec codec )
    {
    super( threshold, null );
    this.codec = codec;
    this.tupleSerialization = new TupleSerialization();
    }

  public HadoopSpillableTupleList( long threshold, HadoopFlowProcess flowProcess )
    {
    super( threshold, flowProcess );
    this.codec = flowProcess.getCoGroupCompressionCodec();

    if( flowProcess == null )
      this.tupleSerialization = new TupleSerialization();
    else
      this.tupleSerialization = new TupleSerialization( flowProcess.getJobConf() );
    }

  @Override
  protected TupleOutputStream createTupleOutputStream( File file )
    {
    OutputStream outputStream;

    try
      {
      if( codec == null )
        outputStream = new FileOutputStream( file );
      else
        outputStream = codec.createOutputStream( new FileOutputStream( file ) );

      return new HadoopTupleOutputStream( outputStream, tupleSerialization.getElementWriter() );
      }
    catch( IOException exception )
      {
      throw new TupleException( "unable to create temporary file input stream", exception );
      }
    }

  @Override
  protected TupleInputStream createTupleInputStream( File file )
    {
    try
      {
      InputStream inputStream;

      if( codec == null )
        inputStream = new FileInputStream( file );
      else
        inputStream = codec.createInputStream( new FileInputStream( file ) );

      return new HadoopTupleInputStream( inputStream, tupleSerialization.getElementReader() );
      }
    catch( IOException exception )
      {
      throw new TupleException( "unable to create temporary file output stream", exception );
      }
    }
  }